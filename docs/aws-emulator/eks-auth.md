# Amazon EKS Auth

## Wire protocol

- **Protocol:** `restJson1`
- **SDK id:** `EKS Auth`
- **sigv4 signing name:** `eks-auth`
- **Endpoint prefix:** `eks-auth`
- **ARN namespace:** `eks-auth`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `eks-auth`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `AssumeRoleForPodIdentity`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:eks-auth" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/json' \
  --data-binary '{"token": "<service account token>"}' \
  'https://127.0.0.1:9297/clusters/my-cluster/assume-role-for-pod-identity'
```

This one answers `400` until you substitute a real token, as the walkthrough below shows.

## End-to-end walkthrough

Exchange a pod's service account token for IAM credentials. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. AssumeRoleForPodIdentity

The token must be a service account token (a JWT) that the cluster issued for a pod with a Pod Identity association. The emulator checks the token first, and refuses this one because its header carries no key id (`kid`).

**Request**

```http
POST /clusters/docs-demo/assume-role-for-pod-identity HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120554Z
Content-Length: 42
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/eks-auth/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "token": "eyJhbGciOiJSUzI1NiJ9.e30.c2ln"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: InvalidTokenException
content-length: 106
date: Mon, 16 Nov 2026 12:05:54 GMT

{
  "__type": "InvalidTokenException",
  "message": "The token included in the request has missing header - kid."
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`) are left out here; the walkthrough shows them in full.

Of 1 operations, 0 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AssumeRoleForPodIdentity

The Amazon EKS Auth API and the AssumeRoleForPodIdentity action are only used by the EKS Pod Identity Agent.

```http
POST /clusters/example/assume-role-for-pod-identity HTTP/1.1
Content-Type: application/json

{
  "token": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: InvalidParameterException

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value at 'token' failed to satisfy constraint: Member must satisfy regular expression pattern: [A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/eks-auth/aws4_request` in the `Authorization` header) and the operation from the request itself: an HTTP method and path per operation (the `HTTP binding`), with identifiers in the path, options in the query string or headers, and the rest of the input as a JSON body.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon EKS Auth answers in AWS: a JSON body and an HTTP status chosen per operation. Errors captured on this page are a JSON body with `__type`, `message`, plus the `x-amzn-errortype` header.
