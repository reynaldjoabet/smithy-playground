#!/usr/bin/env python3
"""Recaptures the request and response examples in docs/aws-emulator/*.md from a running AWS Emulator.

Every documented exchange is sent again, in page order, and its block is replaced with what went over
the wire: the request line and every request header as sent, the status line and every response header
as received, and both bodies (pretty-printed, long bodies cut off as before). Credentials, signatures,
tokens and passwords are redacted.

Walkthroughs are stateful: later steps use ids that earlier responses returned. The script learns how
each id changed by comparing the page's old response with the new one, and substitutes the new values
into later requests (also inside URL-encoded ARNs, and for EC2-style ids that an old, cut-off body never
showed). Reference sections use placeholder values and need no state.

For a clean capture, restart the emulator first so that it starts empty, and run against pages as they
are in git (the old exchanges are the input). Needs only the Python standard library.

Usage:
  scripts/recapture-emulator-docs.py                       # report what would change, all pages
  scripts/recapture-emulator-docs.py --write --emulator-version 1.0.629 [docs/aws-emulator/sqs.md ...]

The report lists every exchange whose status or error code differs from the page. Walkthrough
differences usually mean a step needs attention (see OVERRIDES and STEP_SECRETS below); reference
differences are the emulator's behaviour changing, and --write updates their explanation lines,
the per-page counts and the *Implemented as of* markers to match.
"""
import argparse, datetime, hashlib, hmac, http.client, json, re, ssl, sys, time, urllib.parse
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PAGES = ROOT / "docs" / "aws-emulator"
HOST, REGION = "127.0.0.1", "us-east-1"
PORT = 9297
EMU = Path.home() / ".aws/emulator" / str(PORT)
AKID = SECRET = TOKEN = CTX = None

# Field names whose values are secrets: redacted for display (JSON keys, XML tags, form params, headers).
SECRET_FIELDS = {
    "AccessToken", "accessToken", "SecretAccessKey", "secretAccessKey", "clientSecret", "ClientSecret",
    "SessionToken", "sessionToken", "APIKey", "SshKey", "SshPublicKey", "SecretString", "SecretBinary",
    "SecretKey", "PASSWORD", "Password", "NEW_PASSWORD", "NewPassword", "OldPassword", "ProposedPassword",
    "PreviousPassword", "RandomPassword", "MasterUserPassword", "TemporaryPassword", "deviceCode", "userCode",
    "authorizationToken", "AuthToken", "Session", "RefreshToken", "refreshToken", "IdToken", "idToken",
    "Token", "QRCodePNG", "x-amz-sso_bearer_token", "x-amz-security-token",
}
PASSWORD_LIKE = re.compile(r"password", re.I)
DEMO_PASSWORD = "Docs-Demo-Passw0rd!"

# Walkthrough request values a newer emulator refuses, and what replaces them: page -> [(old, new)].
# Recapturing for emulator 1.0.629 needed EKS "1.31" -> "1.34", MSK "3.6.0" -> "3.9.x" and Network
# Security Manager "resourcesCleanUp": false -> true; the pages now contain the new values.
OVERRIDES: dict[str, list[tuple[str, str]]] = {}

# Redacted secrets that must differ between walkthrough steps: (page, step heading) -> {field: value}.
# The page shows them as <redacted>, so the script has to supply a value that makes the step behave as
# documented: a temporary password, the permanent one, and a deliberately wrong one.
TEMP, PERM, WRONG = "Temp-Passw0rd!", DEMO_PASSWORD, "Wrong-Passw0rd!"
STEP_SECRETS = {
    ("cognito-identity-provider.md", "3. AdminCreateUser"): {"TemporaryPassword": TEMP},
    ("cognito-identity-provider.md", "4. InitiateAuth"): {"PASSWORD": TEMP},
    ("cognito-identity-provider.md", "5. RespondToAuthChallenge"): {"NEW_PASSWORD": PERM},
    ("cognito-identity-provider.md", "6. AdminSetUserPassword"): {"Password": PERM},
    ("cognito-identity-provider.md", "7. InitiateAuth"): {"PASSWORD": PERM},
    ("cognito-identity-provider.md", "9. InitiateAuth"): {"PASSWORD": WRONG},
}


def load_credentials():
    global AKID, SECRET, TOKEN, CTX
    kv = {}
    for line in (EMU / "emulator_credentials").read_text().splitlines():
        if "=" in line:
            k, v = line.split("=", 1)
            kv[k.strip()] = v.strip()
    AKID, SECRET, TOKEN = kv["aws_access_key_id"], kv["aws_secret_access_key"], kv.get("aws_session_token")
    CTX = ssl.create_default_context(cafile=str(EMU / "ca.pem"))


# ------------------------------------------------------------------------------------------------ parsing

def parse_page(path):
    text = Path(path).read_text()
    blocks = [(m.start(1), m.end(1), m.group(1)) for m in re.finditer(r"```http\n(.*?)```", text, re.S)]
    ops_at = text.find("\n## Operations")
    signing = re.search(r"\*\*sigv4 signing name:\*\* `([^`]+)`", text)
    exchanges, i = [], 0
    while i + 1 < len(blocks):
        req, resp = blocks[i], blocks[i + 1]
        if re.match(r"^[A-Z]+ \S* HTTP/1\.1", req[2]) and resp[2].startswith("HTTP/"):
            head = re.findall(r"^### (.+)$", text[:req[0]], re.M)
            exchanges.append({
                "op": head[-1] if head else "",
                "section": "reference" if ops_at >= 0 and req[0] > ops_at else "walkthrough",
                "req_span": req[:2], "resp_span": resp[:2],
                "req": parse_req(req[2]), "resp": parse_resp(resp[2]),
            })
            i += 2
        else:
            i += 1
    return {"path": Path(path), "text": text, "signing": signing.group(1) if signing else None, "exchanges": exchanges}


def split_head(block):
    head, _, body = block.partition("\n\n")
    lines = head.rstrip("\n").split("\n")
    return lines[0], [tuple(x.split(":", 1)) for x in lines[1:] if ":" in x], body.rstrip("\n")


def parse_req(block):
    first, headers, body = split_head(block)
    method, target, _ = first.split(" ", 2)
    return {"method": method, "target": target, "headers": [(k.strip(), v.strip()) for k, v in headers], "body": body}


def parse_resp(block):
    first, headers, body = split_head(block)
    return {"status": int(first.split(" ", 2)[1]), "headers": [(k.strip(), v.strip()) for k, v in headers], "body": body}


# ------------------------------------------------------------------------------------------------ bodies

def kind(content_type, body):
    b = body.lstrip()
    if "x-www-form-urlencoded" in (content_type or "").lower(): return "form"
    if b.startswith(("{", "[")): return "json"
    if b.startswith("<"): return "xml"
    return "text"


def to_wire(body, k):
    """The page shows bodies pretty-printed; this is the compact form that is sent."""
    if not body: return ""
    if k == "form": return "".join(x.strip() for x in body.split("\n"))
    if k == "json":
        try: return json.dumps(json.loads(body), separators=(",", ":"), ensure_ascii=False)
        except ValueError: return body
    if k == "xml": return re.sub(r">\s+<", "><", body.strip())
    return body


def pretty(body, ct):
    if not body: return ""
    k = kind(ct, body)
    if k == "json":
        try: return json.dumps(json.loads(body), indent=2, ensure_ascii=False)
        except ValueError: return body
    if k == "form": return body.replace("&", "\n&")
    if k == "xml": return pretty_xml(body)
    return body


def pretty_xml(s):
    s = s.strip()
    if re.search(r">\n\s+<", s): return s  # already indented
    tokens = re.findall(r"<[^>]+>|[^<]+", s)
    out, depth, i = [], 0, 0
    while i < len(tokens):
        t = tokens[i]
        if t.startswith("<?"):
            out.append(t)
        elif t.startswith("</"):
            depth -= 1
            out.append("  " * depth + t)
        elif t.startswith("<") and t.endswith("/>"):
            out.append("  " * depth + t)
        elif t.startswith("<"):
            # <a>text</a> and <a></a> stay on one line
            if i + 2 < len(tokens) and not tokens[i + 1].startswith("<") and tokens[i + 2].startswith("</"):
                out.append("  " * depth + t + tokens[i + 1] + tokens[i + 2]); i += 3; continue
            if i + 1 < len(tokens) and tokens[i + 1].startswith("</"):
                out.append("  " * depth + t + tokens[i + 1]); i += 2; continue
            out.append("  " * depth + t)
            depth += 1
        elif t.strip():
            out.append("  " * depth + t.strip())
        i += 1
    return "\n".join(out)


def truncate(body, limit):
    lines = body.split("\n")
    if len(lines) <= limit: return body
    return "\n".join(lines[:limit]) + f"\n… ({len(lines) - limit} more lines)"


def limits(page):
    """Lines kept per body: 60 in the walkthrough; in the reference, whatever the page used before."""
    c = Counter()
    for e in page["exchanges"]:
        if e["section"] == "reference" and re.search(r"\n… \(\d+ more lines?\)$", e["resp"]["body"]):
            c[len(e["resp"]["body"].split("\n")) - 1] += 1
    return {"walkthrough": 60, "reference": c.most_common(1)[0][0] if c else 20}


# ------------------------------------------------------------------------------------------------ signing and sending

def enc(s, safe="-_.~"):
    return urllib.parse.quote(s, safe=safe)


def sign(method, target, headers, body, service, now):
    """SigV4 over every header that is sent. Returns the headers in send order, with Host, X-Amz-Date,
    x-amz-content-sha256 (S3), Content-Length, X-Amz-Security-Token and Authorization added."""
    amz, payload_hash = now.strftime("%Y%m%dT%H%M%SZ"), hashlib.sha256(body).hexdigest()
    day = amz[:8]
    regenerated = ("host", "x-amz-date", "authorization", "content-length", "x-amz-content-sha256",
                   "x-amz-security-token", "user-agent")
    out = [h for h in headers if h[0].lower() not in regenerated]
    out += [("Host", f"{HOST}:{PORT}"), ("X-Amz-Date", amz)]
    if service == "s3": out.append(("x-amz-content-sha256", payload_hash))
    if body: out.append(("Content-Length", str(len(body))))
    if TOKEN: out.append(("X-Amz-Security-Token", TOKEN))
    path, _, query = target.partition("?")
    if service == "s3":
        cpath = "/".join(enc(urllib.parse.unquote(p)) for p in path.split("/")) or "/"
    else:  # other services encode the (already once-encoded) raw path again
        cpath = "/".join(enc(p) for p in path.split("/")) or "/"
    pairs = []
    for p in filter(None, query.split("&")):
        k, _, v = p.partition("=")
        pairs.append((enc(urllib.parse.unquote(k)), enc(urllib.parse.unquote(v))))
    lower = defaultdict(list)
    for k, v in out:
        lower[k.lower()].append(re.sub(r"\s+", " ", v.strip()))
    names = sorted(lower)
    signed = ";".join(names)
    creq = "\n".join([method, cpath, "&".join(f"{k}={v}" for k, v in sorted(pairs)),
                      "".join(f"{n}:{','.join(lower[n])}\n" for n in names), signed, payload_hash])
    scope = f"{day}/{REGION}/{service}/aws4_request"
    to_sign = "\n".join(["AWS4-HMAC-SHA256", amz, scope, hashlib.sha256(creq.encode()).hexdigest()])
    key = ("AWS4" + SECRET).encode()
    for part in (day, REGION, service, "aws4_request"):
        key = hmac.new(key, part.encode(), hashlib.sha256).digest()
    sig = hmac.new(key, to_sign.encode(), hashlib.sha256).hexdigest()
    out.append(("Authorization", f"AWS4-HMAC-SHA256 Credential={AKID}/{scope}, SignedHeaders={signed}, Signature={sig}"))
    return out


def send(conn, method, target, headers, body):
    """Sends exactly these headers: http.client adds no Host, Accept-Encoding or User-Agent here."""
    conn.putrequest(method, target, skip_host=True, skip_accept_encoding=True)
    for k, v in headers: conn.putheader(k, v)
    conn.endheaders(body if body else None)
    r = conn.getresponse()
    return {"status": r.status, "reason": r.reason, "version": "HTTP/1.1" if r.version == 11 else "HTTP/1.0",
            "headers": r.getheaders(), "body": r.read()}


def scope_service(req):
    """The signing name in the old request's credential scope: a walkthrough step may call another service."""
    auth = next((v for k, v in req["headers"] if k.lower() == "authorization"), "")
    m = re.search(r"Credential=[^/]+/\d{8}/[^/]+/([^/]+)/aws4_request", auth)
    return m.group(1) if m else None


def error_code(r):
    body = r["body"].decode("utf-8", "replace") if isinstance(r["body"], bytes) else r["body"]
    for k, v in r["headers"]:
        if k.lower() == "x-amzn-errortype": return v.split(":")[0]
    m = re.search(r'"__type"\s*:\s*"([^"]+)"', body) or re.search(r'"code"\s*:\s*"([^"]+)"', body) or \
        re.search(r"<Code>([^<]+)</Code>", body)
    return m.group(1).split("#")[-1] if m else None


# ------------------------------------------------------------------------------------------------ ids and secrets

class State:
    EC2_ID = re.compile(r"\b([a-z]{1,10}(?:-[a-z]+)?)-([0-9a-f]{8,17})\b")

    def __init__(self):
        self.ids = {}           # old value -> new value, learnt by comparing old and new responses
        self.secrets = {}       # field name -> real value of something the page shows as <redacted>
        self.last_secret = None
        self.real_secrets = set()
        self.new_order = []     # EC2-style ids in the order new responses returned them
        self.step = {}          # STEP_SECRETS for the current step

    def learn(self, old, new, key=None):
        if isinstance(old, dict) and isinstance(new, dict):
            for k in old:
                if k in new: self.learn(old[k], new[k], k)
        elif isinstance(old, list) and isinstance(new, list):
            for a, b in zip(old, new): self.learn(a, b, key)
        elif isinstance(old, str) and isinstance(new, str) and old != new:
            if "<redacted>" in old:
                if key: self.secrets[key] = new
                self.last_secret = new
                self.real_secrets.add(new)
            elif len(old) >= 4 and old not in ("true", "false", "null"):
                self.ids[old] = new

    def learn_text(self, old, new):
        """Pairs values by field name, in order, straight from the text: works on cut-off bodies too."""
        for pat in (r'"([^"]+)"\s*:\s*"((?:[^"\\]|\\.)*)"', r"<([A-Za-z0-9_.:-]+)>([^<]+)</\1>"):
            o, n = defaultdict(list), defaultdict(list)
            for m in re.finditer(pat, old): o[m.group(1)].append(m.group(2))
            for m in re.finditer(pat, new): n[m.group(1)].append(m.group(2))
            for k in o:
                for a, b in zip(o[k], n.get(k, [])): self.learn(a, b, k)

    def see_new(self, text):
        for m in self.EC2_ID.finditer(text):
            if m.group(0) not in self.new_order: self.new_order.append(m.group(0))

    def map_unseen(self, text):
        """An old EC2-style id that no response taught us (its body was cut off) maps to the next new id
        with the same prefix that nothing else maps to yet."""
        for m in self.EC2_ID.finditer(text):
            o = m.group(0)
            if o in self.ids or o in self.new_order: continue
            used = set(self.ids.values())
            n = next((n for n in self.new_order if n.split("-")[0] == o.split("-")[0] and n not in used), None)
            if n: self.ids[o] = n

    def substitute(self, s):
        for old in sorted(self.ids, key=len, reverse=True):
            new = self.ids[old]
            s = s.replace(old, new)
            for safe in ("", "/"):  # ARNs travel URL-encoded in form bodies and URL paths
                qo = urllib.parse.quote(old, safe=safe)
                if qo != old: s = s.replace(qo, urllib.parse.quote(new, safe=safe))
        return s

    def fill_json(self, v, key=None):
        if isinstance(v, dict): return {k: self.fill_json(x, k) for k, x in v.items()}
        if isinstance(v, list): return [self.fill_json(x, key) for x in v]
        if v == "<redacted>": return self.secret_for(key)
        if isinstance(v, str): return self.substitute(v)
        return v

    def secret_for(self, key):
        if key in self.step:
            self.real_secrets.add(self.step[key])
            return self.step[key]
        if key in self.secrets: return self.secrets[key]
        if key and PASSWORD_LIKE.search(key):
            self.real_secrets.add(DEMO_PASSWORD)
            return DEMO_PASSWORD
        return self.last_secret or "docs-demo-secret"


def tree(body):
    b = (body if isinstance(body, str) else body.decode("utf-8", "replace")).strip()
    if b.startswith(("{", "[")):
        try: return json.loads(b)
        except ValueError: return None
    if b.startswith("<"):
        try: return xml_to_obj(ET.fromstring(b))
        except ET.ParseError: return None
    return None


def xml_to_obj(e):
    """Children grouped by tag, so two responses pair element by element name, not position."""
    if not len(e): return e.text or ""
    out = defaultdict(list)
    for k in e: out[k.tag.split("}")[-1]].append(xml_to_obj(k))
    return dict(out)


# ------------------------------------------------------------------------------------------------ replay

def run_page(page):
    st = State()
    conn = http.client.HTTPSConnection(HOST, PORT, context=CTX, timeout=30)
    name = page["path"].name
    default_ct = next((v for e in page["exchanges"] if e["section"] == "walkthrough"
                       for k, v in e["req"]["headers"] if k.lower() == "content-type"), None)
    results = []
    for e in page["exchanges"]:
        req = e["req"]
        if e["section"] == "walkthrough":
            for a, b in OVERRIDES.get(name, []):
                req["body"], req["target"] = req["body"].replace(a, b), req["target"].replace(a, b)
        st.step = STEP_SECRETS.get((name, e["op"]), {})
        st.map_unseen("\n".join([req["target"], req["body"]] + [v for _, v in req["headers"]]))
        ct = next((v for k, v in req["headers"] if k.lower() == "content-type"), None)
        if ct is None and req["body"] and e["section"] == "reference": ct = default_ct
        k = kind(ct, req["body"])
        if k == "json" and req["body"]:
            try: wire_body = json.dumps(st.fill_json(json.loads(req["body"])), separators=(",", ":"), ensure_ascii=False)
            except ValueError: wire_body = st.substitute(req["body"])
        else:
            wire_body = re.sub(r"([A-Za-z0-9_.]+)=<redacted>",
                               lambda m: m.group(1) + "=" + enc(st.secret_for(m.group(1).split(".")[-1]), safe=""),
                               st.substitute(to_wire(req["body"], k)))
        target = st.substitute(req["target"])
        base = [(hk, st.substitute(st.secret_for(hk) if hv == "<redacted>" else hv)) for hk, hv in req["headers"]
                if hk.lower() not in ("host", "x-amz-date", "authorization", "content-length", "x-amz-security-token")]
        if ct and not any(hk.lower() == "content-type" for hk, _ in base): base.insert(0, ("Content-Type", ct))
        body = wire_body.encode("utf-8")
        unsigned = any(hk.lower() == "x-amz-sso_bearer_token" for hk, _ in base) or \
            (e["section"] == "walkthrough" and not any(hk.lower() == "authorization" for hk, _ in req["headers"]))
        # Retry only where a different answer can be a matter of time: never for an operation that
        # answered 501, and for writes only when the page shows them succeeding.
        read_only = re.match(r"\d+\. (?:.*: )?(Describe|Get|List)", e["op"]) is not None
        attempt = 0
        while True:
            if unsigned:
                hs = base + [("Host", f"{HOST}:{PORT}")] + ([("Content-Length", str(len(body)))] if body else [])
            else:
                hs = sign(req["method"], target, base, body, scope_service(req) or page["signing"],
                          datetime.datetime.now(datetime.timezone.utc))
            try:
                r = send(conn, req["method"], target, hs, body)
            except (http.client.HTTPException, OSError):
                conn.close()
                conn = http.client.HTTPSConnection(HOST, PORT, context=CTX, timeout=30)
                r = send(conn, req["method"], target, hs, body)
            differs = r["status"] != e["resp"]["status"] or (read_only and error_code(r) != error_code(e["resp"]))
            if e["section"] == "walkthrough" and differs and e["resp"]["status"] != 501 and attempt < 20 and \
                    (read_only or 200 <= e["resp"]["status"] < 300):
                attempt += 1
                time.sleep(0.5)
                continue
            break
        new_text = r["body"].decode("utf-8", "replace")
        old_tree, new_tree = tree(e["resp"]["body"]), tree(r["body"])
        if old_tree is not None and new_tree is not None: st.learn(old_tree, new_tree)
        st.learn_text(e["resp"]["body"], new_text)
        st.see_new(new_text)
        for (ok, ov), (nk, nv) in zip(e["resp"]["headers"], r["headers"]):
            if ok.lower() == nk.lower() == "location" and ov != nv: st.ids[ov] = nv
        results.append({"e": e, "headers": hs, "body": wire_body, "ct": ct, "target": target, "resp": r,
                        "retries": attempt})
    conn.close()
    return st, results


# ------------------------------------------------------------------------------------------------ redaction

def redact_text(s, st):
    s = s.replace(AKID, "<access-key-id>").replace(SECRET, "<redacted>")
    if TOKEN: s = s.replace(TOKEN, "<redacted>")
    for v in sorted(st.real_secrets, key=len, reverse=True):
        if len(v) >= 6: s = s.replace(v, "<redacted>")
    for f in SECRET_FIELDS:
        s = re.sub(r'("%s"\s*:\s*)"(?:[^"\\]|\\.)*"' % re.escape(f), r'\1"<redacted>"', s)
        s = re.sub(r"(<%s>)[^<]*(</%s>)" % (re.escape(f), re.escape(f)), r"\1<redacted>\2", s)
        s = re.sub(r"(^|\n&|&)(%s=)[^&\n]*" % re.escape(f), r"\1\2<redacted>", s)
    return s


def redact_header(k, v):
    if k.lower() == "authorization":
        v = re.sub(r"Credential=[^/]+/", "Credential=<access-key-id>/", v)
        return re.sub(r"Signature=[0-9a-f]+", "Signature=<signature>", v)
    return "<redacted>" if k in SECRET_FIELDS or k.lower() in SECRET_FIELDS else v


# ------------------------------------------------------------------------------------------------ explanation lines

EXPLANATION = re.compile(r"\n((?:`\d{3}`|Accepted:|Not implemented in the emulator yet:)[^\n]*)")
WALKTHROUGH_LINK = re.compile(r" See \[the walkthrough\]\([^)]*\) for a successful call\.$")


def previous_categories(pages_dir):
    """Error code -> explanation category, as the pages classify them now."""
    seen = defaultdict(Counter)
    for f in Path(pages_dir).glob("*.md"):
        s = f.read_text()
        i = s.find("\n## Operations")
        if i < 0: continue
        for m in re.finditer(r"```\n\n`(\d{3})` `([^`]*)`([^\n]*)", s[i:]):
            _, code, rest = m.groups()
            seen[code]["validation" if "input validation" in rest else "notfound" if "name nothing" in rest
                       else "auth" if "authorization" in rest else "plain"] += 1
    return {c: n.most_common(1)[0][0] for c, n in seen.items()}


def category(code, known):
    if code in known: return known[code]
    if re.search(r"NotFound|NoSuch|DoesNotExist|NotExist|\.Unknown|Nonexistent", code or ""): return "notfound"
    if re.search(r"AccessDenied|Unauthorized(?!Operation)|Forbidden", code or ""): return "auth"
    if re.search(r"Validation|Invalid|Missing|Malformed|BadRequest", code or ""): return "validation"
    return "plain"


def explanation(r, known):
    status, code = r["status"], error_code(r)
    if 200 <= status < 300:
        return f"Accepted: the emulator answered `{status} {r['reason']}` even with placeholder values."
    if status == 501:
        return "Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation."
    head = f"`{status}` `{code}`" if code else f"`{status}`"
    return head + {"validation": ": the placeholder values failed the operation's input validation.",
                   "notfound": ": the placeholder identifiers name nothing that exists, so this is the operation's not-found error.",
                   "auth": ": the request was refused by authorization.",
                   "plain": "."}[category(code, known)]


# ------------------------------------------------------------------------------------------------ page rewrite

def render_blocks(st, results, lim):
    edits = []
    for res in results:
        e, r = res["e"], res["resp"]
        n = lim[e["section"]]
        lines = [f"{e['req']['method']} {res['target']} HTTP/1.1"] + [f"{k}: {redact_header(k, v)}" for k, v in res["headers"]]
        body = truncate(redact_text(pretty(res["body"], res["ct"]), st), n)
        edits.append((e["req_span"], redact_text("\n".join(lines) + ("\n\n" + body if body else "") + "\n", st)))
        ct = next((v for k, v in r["headers"] if k.lower() == "content-type"), "")
        lines = [f"{r['version']} {r['status']} {r['reason']}"] + [f"{k}: {redact_header(k, v)}" for k, v in r["headers"]]
        body = truncate(redact_text(pretty(r["body"].decode("utf-8", "replace"), ct), st), n)
        edits.append((e["resp_span"], redact_text("\n".join(lines) + ("\n\n" + body if body else "") + "\n", st)))
    return edits


def rewrite(page, st, results, known, version, today):
    text = page["text"]
    # The version the page was captured with: its capture note, else the newest *Implemented as of* marker.
    prev = re.search(r"\(emulator ([0-9.]+)\)\.", text)
    markers = re.findall(r"Implemented as of emulator ([0-9.]+)", text)
    prev = prev.group(1) if prev else max(markers, key=lambda v: [int(x) for x in v.split(".")]) if markers \
        else "the previous version"
    edits = render_blocks(st, results, limits(page))
    ref = [r for r in results if r["e"]["section"] == "reference"]
    for res in ref:
        e, r = res["e"], res["resp"]
        if (e["resp"]["status"], error_code(e["resp"])) == (r["status"], error_code(r)): continue
        pos = e["resp_span"][1] + len("```\n")
        m = EXPLANATION.match(text, pos)
        line = explanation(r, known)
        if m:
            link = WALKTHROUGH_LINK.search(m.group(1))
            edits.append(((m.start(1), m.end(1)), line + (link.group(0) if link else "")))
        else:
            edits.append(((pos, pos), "\n" + line + "\n"))
        if e["resp"]["status"] == 501:
            a = e["req_span"][0] - len("```http\n")
            edits.append(((a, a), f"*Implemented as of emulator {version} (captured {today}).*\n\n"))
    for (a, b), new in sorted(edits, key=lambda x: (-x[0][0], -x[0][1])):
        text = text[:a] + new + text[b:]

    accepted = sum(1 for r in ref if 200 <= r["resp"]["status"] < 300)
    nyi = sum(1 for r in ref if r["resp"]["status"] == 501)
    text = re.sub(r"(Of \d+ operations, )\d+( accepted the placeholder call, )\d+( answer `501 NotImplemented`)",
                  lambda m: f"{m.group(1)}{accepted}{m.group(2)}{nyi}{m.group(3)}", text, count=1)
    if any(r["e"]["resp"]["status"] == 501 and r["resp"]["status"] != 501 for r in ref):
        marker = f"Operations marked *Implemented as of emulator {version}* answered `501` in {prev}."
        if marker not in text:
            text = re.sub(r"(and the rest refused it with a modelled error\.)", r"\1 " + marker.replace("\\", r"\\"), text, count=1)
    text = re.sub(r"(Every request and response on this page was sent to and returned by a running AWS Emulator at "
                  r"`https://127\.0\.0\.1:9297` on )\d{4}-\d{2}-\d{2}( \(emulator [0-9.]+\))?\.",
                  r"\g<1>%s (emulator %s)." % (today, version), text, count=1)
    note = (" Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, "
            "and each response keeps all of the emulator's headers.")
    text = re.sub(r"(and bodies are pretty-printed\.)(?! Every header)", r"\1" + note, text, count=1)
    text = re.sub(r" ?Headers repeated on every request \([^)]*\) are left out here; the walkthrough shows them in full\.",
                  " Every request and response shows all of its headers, as in the walkthrough.", text, count=1)
    # one blank line before headings and explanation lines, however the edits above left them
    return re.sub(r"\n{3,}(?=### |`\d{3}`|Accepted:|\*Implemented|Not implemented)", "\n\n", text)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("pages", nargs="*", type=Path, help="pages to recapture (default: all of docs/aws-emulator)")
    ap.add_argument("--write", action="store_true", help="rewrite the pages (default: only report)")
    ap.add_argument("--emulator-version", help="version of the running emulator, for the page notes (with --write)")
    args = ap.parse_args()
    if args.write and not args.emulator_version: ap.error("--write needs --emulator-version")
    pages = args.pages or sorted(p for p in PAGES.glob("*.md") if p.name != "README.md")
    load_credentials()
    known = previous_categories(pages[0].parent)
    today = datetime.date.today().isoformat()
    for p in pages:
        page = parse_page(p)
        st, results = run_page(page)
        changed = [(res["e"]["section"], res["e"]["op"], (res["e"]["resp"]["status"], error_code(res["e"]["resp"])),
                    (res["resp"]["status"], error_code(res["resp"]))) for res in results]
        changed = [c for c in changed if c[2] != c[3]]
        walk = [c for c in changed if c[0] == "walkthrough"]
        print(f"{p.name}: {len(results)} exchanges, {len(changed)} with a different status or error code "
              f"({len(walk)} in the walkthrough)")
        for c in walk: print(f"    walkthrough {c[1]}: {c[2]} -> {c[3]}")
        if args.write: p.write_text(rewrite(page, st, results, known, args.emulator_version, today))


if __name__ == "__main__":
    sys.exit(main())
