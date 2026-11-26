# AWS WAFV2

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: AWSWAF_20190729.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `WAFV2`
- **sigv4 signing name:** `wafv2`
- **Endpoint prefix:** `wafv2`
- **ARN namespace:** `wafv2`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `wafv2`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListWebACLs`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:wafv2" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.1' \
  -H 'X-Amz-Target: AWSWAF_20190729.ListWebACLs' \
  --data-binary '{"Scope": "REGIONAL"}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Block an IP range with a regional web ACL, then delete it using lock tokens. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateIPSet

Every WAF resource comes back with a `LockToken`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.CreateIPSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 102
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "docs-demo-blocked",
  "Scope": "REGIONAL",
  "IPAddressVersion": "IPV4",
  "Addresses": [
    "192.0.2.0/24"
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 267
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "Summary": {
    "Name": "docs-demo-blocked",
    "Id": "352d8842-1b4e-4bc3-9cca-641b63a039fd",
    "Description": "",
    "LockToken": "c0394157-fc66-4422-950b-5e7eb401b24b",
    "ARN": "arn:aws:wafv2:us-east-1:000000000000:regional/ipset/docs-demo-blocked/352d8842-1b4e-4bc3-9cca-641b63a039fd"
  }
}
```

### 2. CreateWebACL

Allow by default, block the IP set.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.CreateWebACL
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 516
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "docs-demo-acl",
  "Scope": "REGIONAL",
  "DefaultAction": {
    "Allow": {}
  },
  "Rules": [
    {
      "Name": "block-range",
      "Priority": 0,
      "Statement": {
        "IPSetReferenceStatement": {
          "ARN": "arn:aws:wafv2:us-east-1:000000000000:regional/ipset/docs-demo-blocked/352d8842-1b4e-4bc3-9cca-641b63a039fd"
        }
      },
      "Action": {
        "Block": {}
      },
      "VisibilityConfig": {
        "SampledRequestsEnabled": true,
        "CloudWatchMetricsEnabled": true,
        "MetricName": "blockRange"
      }
    }
  ],
  "VisibilityConfig": {
    "SampledRequestsEnabled": true,
    "CloudWatchMetricsEnabled": true,
    "MetricName": "docsDemoAcl"
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 260
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "Summary": {
    "Name": "docs-demo-acl",
    "Id": "05722548-a0c5-4902-9e46-fae9f54b8a6c",
    "Description": "",
    "LockToken": "ad92ed3b-d828-49ef-a87d-763fa94a72fa",
    "ARN": "arn:aws:wafv2:us-east-1:000000000000:regional/webacl/docs-demo-acl/05722548-a0c5-4902-9e46-fae9f54b8a6c"
  }
}
```

### 3. GetWebACL

The ACL, its rules and its current lock token.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetWebACL
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 87
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "docs-demo-acl",
  "Scope": "REGIONAL",
  "Id": "05722548-a0c5-4902-9e46-fae9f54b8a6c"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 952
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "WebACL": {
    "Name": "docs-demo-acl",
    "Id": "05722548-a0c5-4902-9e46-fae9f54b8a6c",
    "ARN": "arn:aws:wafv2:us-east-1:000000000000:regional/webacl/docs-demo-acl/05722548-a0c5-4902-9e46-fae9f54b8a6c",
    "DefaultAction": {
      "Allow": {}
    },
    "Description": "",
    "Rules": [
      {
        "Name": "block-range",
        "Priority": 0,
        "Statement": {
          "IPSetReferenceStatement": {
            "ARN": "arn:aws:wafv2:us-east-1:000000000000:regional/ipset/docs-demo-blocked/352d8842-1b4e-4bc3-9cca-641b63a039fd"
          }
        },
        "Action": {
          "Block": {}
        },
        "VisibilityConfig": {
          "SampledRequestsEnabled": true,
          "CloudWatchMetricsEnabled": true,
          "MetricName": "blockRange"
        }
      }
    ],
    "VisibilityConfig": {
      "SampledRequestsEnabled": true,
      "CloudWatchMetricsEnabled": true,
      "MetricName": "docsDemoAcl"
    },
    "Capacity": 1,
    "ManagedByFirewallManager": false,
    "LabelNamespace": "awswaf:000000000000:webacl:docs-demo-acl:",
    "RetrofittedByFirewallManager": false,
    "OnSourceDDoSProtectionConfig": {
      "ALBLowReputationMode": "ACTIVE_UNDER_DDOS"
    }
  },
  "LockToken": "ad92ed3b-d828-49ef-a87d-763fa94a72fa"
}
```

### 4. DeleteIPSet

An IP set a rule references cannot be deleted: `WAFAssociatedItemException`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DeleteIPSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 142
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "docs-demo-blocked",
  "Scope": "REGIONAL",
  "Id": "352d8842-1b4e-4bc3-9cca-641b63a039fd",
  "LockToken": "c0394157-fc66-4422-950b-5e7eb401b24b"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 192
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFAssociatedItemException",
  "Message": "AWS WAF couldn’t perform the operation because your resource is being used by another resource or it’s associated with another resource."
}
```

### 5. DeleteWebACL

A stale lock token is refused with `WAFOptimisticLockException`: writes are optimistic.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DeleteWebACL
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 138
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "docs-demo-acl",
  "Scope": "REGIONAL",
  "Id": "05722548-a0c5-4902-9e46-fae9f54b8a6c",
  "LockToken": "00000000-0000-0000-0000-000000000000"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 177
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFOptimisticLockException",
  "Message": "AWS WAF couldn’t save your changes because someone changed the resource after you started to edit it. Reapply your changes."
}
```

### 6. DeleteWebACL

With the current token the ACL deletes ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DeleteWebACL
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 138
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "docs-demo-acl",
  "Scope": "REGIONAL",
  "Id": "05722548-a0c5-4902-9e46-fae9f54b8a6c",
  "LockToken": "ad92ed3b-d828-49ef-a87d-763fa94a72fa"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 2
date: Thu, 26 Nov 2026 18:30:52 GMT

{}
```

### 7. DeleteIPSet

... and then the IP set.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DeleteIPSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 142
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "docs-demo-blocked",
  "Scope": "REGIONAL",
  "Id": "352d8842-1b4e-4bc3-9cca-641b63a039fd",
  "LockToken": "c0394157-fc66-4422-950b-5e7eb401b24b"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 2
date: Thu, 26 Nov 2026 18:30:52 GMT

{}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 59 operations, 15 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AssociateWebACL

Associates a web ACL with a resource, to protect the resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.AssociateWebACL
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 137
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "WebACLArn": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 480
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes., field: RESOURCE_ARN, parameter: arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Field": "RESOURCE_ARN",
  "Parameter": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Reason": "The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### CheckCapacity

Returns the web ACL capacity unit (WCU) requirements for a specified scope and set of rules.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.CheckCapacity
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 187
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "CLOUDFRONT",
  "Rules": [
    {
      "Name": "example",
      "Priority": 0,
      "Statement": {},
      "VisibilityConfig": {
        "SampledRequestsEnabled": false,
        "CloudWatchMetricsEnabled": false,
        "MetricName": "example"
      }
    }
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 14
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "Capacity": 0
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateAPIKey

Creates an API key that contains a set of token domains.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.CreateAPIKey
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "CLOUDFRONT",
  "TokenDomains": [
    "example"
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 109
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "APIKey": "<redacted>"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateIPSet

Creates an IPSet , which you use to identify web requests that originate from specific IP addresses or ranges of IP addresses.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.CreateIPSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 89
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "IPAddressVersion": "IPV4",
  "Addresses": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 266
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The parameter contains formatting that is not valid., field: IP_ADDRESS, parameter: example",
  "Field": "IP_ADDRESS",
  "Parameter": "example",
  "Reason": "The parameter contains formatting that is not valid."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-createipset) for a successful call.

### CreateRegexPatternSet

Creates a RegexPatternSet , which you reference in a RegexPatternSetReferenceStatement , to have WAF inspect a web request component for the specified patterns.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.CreateRegexPatternSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "RegularExpressionList": [
    {}
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 255
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "Summary": {
    "Name": "example",
    "Id": "107e0077-acac-4189-9aa5-232916ade12f",
    "Description": "",
    "LockToken": "1bf784aa-6a6e-461b-bd66-8265acf6a690",
    "ARN": "arn:aws:wafv2:us-east-1:000000000000:global/regexpatternset/example/107e0077-acac-4189-9aa5-232916ade12f"
  }
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateRuleGroup

Creates a RuleGroup per the specifications provided.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.CreateRuleGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 160
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Capacity": 1,
  "VisibilityConfig": {
    "SampledRequestsEnabled": false,
    "CloudWatchMetricsEnabled": false,
    "MetricName": "example"
  }
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 249
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "Summary": {
    "Name": "example",
    "Id": "9e008753-92b4-4543-9c31-2562882200ca",
    "Description": "",
    "LockToken": "104b7f86-f666-4a35-a7bf-3b70d357e963",
    "ARN": "arn:aws:wafv2:us-east-1:000000000000:global/rulegroup/example/9e008753-92b4-4543-9c31-2562882200ca"
  }
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateWebACL

Creates a WebACL per the specifications provided.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.CreateWebACL
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 166
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "DefaultAction": {},
  "VisibilityConfig": {
    "SampledRequestsEnabled": false,
    "CloudWatchMetricsEnabled": false,
    "MetricName": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 346
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: You have used none or multiple values for a field that requires exactly one value., field: DEFAULT_ACTION, parameter: DefaultAction",
  "Field": "DEFAULT_ACTION",
  "Parameter": "DefaultAction",
  "Reason": "You have used none or multiple values for a field that requires exactly one value."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-createwebacl) for a successful call.

### DeleteAPIKey

Deletes the specified API key.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DeleteAPIKey
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 130
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "CLOUDFRONT",
  "APIKey": "<redacted>"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 2
date: Thu, 26 Nov 2026 18:30:52 GMT

{}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteFirewallManagerRuleGroups

Deletes all rule groups that are managed by Firewall Manager from the specified WebACL .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DeleteFirewallManagerRuleGroups
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 96
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "WebACLArn": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "WebACLLockToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 234
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'webACLLockToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteIPSet

Deletes the specified IPSet .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DeleteIPSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example",
  "LockToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 378
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'id' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$; Value 'example' at 'lockToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#7-deleteipset) for a successful call.

### DeleteLoggingConfiguration

Deletes the LoggingConfiguration from the specified web ACL.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DeleteLoggingConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 480
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes., field: RESOURCE_ARN, parameter: arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Field": "RESOURCE_ARN",
  "Parameter": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Reason": "The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### DeletePermissionPolicy

Permanently deletes an IAM policy from the specified rule group.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DeletePermissionPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 480
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes., field: RESOURCE_ARN, parameter: arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Field": "RESOURCE_ARN",
  "Parameter": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Reason": "The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteRegexPatternSet

Deletes the specified RegexPatternSet .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DeleteRegexPatternSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example",
  "LockToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 378
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'id' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$; Value 'example' at 'lockToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteRuleGroup

Deletes the specified RuleGroup .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DeleteRuleGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example",
  "LockToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 378
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'id' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$; Value 'example' at 'lockToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteWebACL

Deletes the specified WebACL .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DeleteWebACL
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example",
  "LockToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 378
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'id' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$; Value 'example' at 'lockToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#6-deletewebacl) for a successful call.

### DescribeAllManagedProducts

Provides high-level information for the Amazon Web Services Managed Rules rule groups and Amazon Web Services Marketplace managed rule groups.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DescribeAllManagedProducts
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 8195
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "ManagedProducts": [
    {
      "VendorName": "Cyber Security Cloud Inc.",
      "ManagedRuleSetName": "CyberSecurityCloud-APIGatewayServerless-",
      "ProductId": "8bbf295b-34b3-40c0-bee7-5ba8f602705f",
      "ProductLink": "http://aws.amazon.com/marketplace/pp/B081TKTBQ8",
      "ProductTitle": "Cyber Security Cloud Managed Rules for AWS WAF -API Gateway/Serverless-",
      "ProductDescription": "The Cyber Security Cloud OWASP ruleset is designed to mitigate and minimize vulnerabilities, including all those on OWASP API Security/Serverless Top 10 Threats.",
      "SnsTopicArn": "arn:aws:sns:us-east-1:343255486711:CyberSecurityCloud-ManagedRule",
      "IsVersioningSupported": true,
      "IsAdvancedManagedRuleSet": false
    },
    {
      "VendorName": "Cyber Security Cloud Inc.",
      "ManagedRuleSetName": "CyberSecurityCloud-HighSecurityOWASPSet-",
      "ProductId": "fa8c6ca7-12c3-4e3f-8524-782177f2f2bd",
      "ProductLink": "http://aws.amazon.com/marketplace/pp/B081TD6XL1",
      "ProductTitle": "Cyber Security Cloud Managed Rules for AWS WAF -HighSecurity OWASP Set-",
      "ProductDescription": "The Cyber Security Cloud OWASP ruleset is designed to mitigate and minimize vulnerabilities, including all those on OWASP Top 10 Web Application Threats list.",
… (156 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeManagedProductsByVendor

Provides high-level information for the managed rule groups owned by a specific vendor.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DescribeManagedProductsByVendor
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "VendorName": "example",
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 22
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "ManagedProducts": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeManagedRuleGroup

Provides high-level information for a managed rule group, including descriptions of the rules.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DescribeManagedRuleGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 62
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "VendorName": "example",
  "Name": "example",
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 143
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFNonexistentItemException",
  "Message": "ManagedRules with name example by vendor example does not exist. Please check your inputs."
}
```

`400` `WAFNonexistentItemException`.

### DisassociateWebACL

Disassociates the specified resource from its web ACL association, if it has one.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.DisassociateWebACL
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 480
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes., field: RESOURCE_ARN, parameter: arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Field": "RESOURCE_ARN",
  "Parameter": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Reason": "The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### GenerateMobileSdkReleaseUrl

Generates a presigned download URL for the specified release of the mobile SDK.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GenerateMobileSdkReleaseUrl
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Platform": "IOS",
  "ReleaseVersion": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 102
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidOperationException",
  "Message": "This account is not authorized to use Mobile-SDK"
}
```

`400` `WAFInvalidOperationException`.

### GetDecryptedAPIKey

Returns your API key in decrypted form.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetDecryptedAPIKey
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 130
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "CLOUDFRONT",
  "APIKey": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 132
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFNonexistentItemException",
  "Message": "AWS WAF couldn’t perform the operation because your resource doesn’t exist."
}
```

`400` `WAFNonexistentItemException`.

### GetIPSet

Retrieves the specified IPSet .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetIPSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 221
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'id' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetLoggingConfiguration

Returns the LoggingConfiguration for the specified web ACL.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetLoggingConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 480
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes., field: RESOURCE_ARN, parameter: arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Field": "RESOURCE_ARN",
  "Parameter": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Reason": "The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### GetManagedRuleSet

Retrieves the specified managed rule set.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetManagedRuleSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 221
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'id' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetMobileSdkRelease

Retrieves information for the specified mobile SDK release, including release notes and tags.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetMobileSdkRelease
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Platform": "IOS",
  "ReleaseVersion": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 102
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidOperationException",
  "Message": "This account is not authorized to use Mobile-SDK"
}
```

`400` `WAFInvalidOperationException`.

### GetPermissionPolicy

Returns the IAM policy that is attached to the specified rule group.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetPermissionPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 480
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes., field: RESOURCE_ARN, parameter: arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Field": "RESOURCE_ARN",
  "Parameter": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Reason": "The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### GetRateBasedStatementManagedKeys

Retrieves the IP addresses that are currently blocked by a rate-based rule instance.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetRateBasedStatementManagedKeys
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 87
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "CLOUDFRONT",
  "WebACLName": "example",
  "WebACLId": "example",
  "RuleName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 227
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'webACLId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetRegexPatternSet

Retrieves the specified RegexPatternSet .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetRegexPatternSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 221
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'id' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetRevenueStatistics

Retrieves ranked monetization statistics.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetRevenueStatistics
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 164
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "StatisticType": "TOP_SOURCES_BY_REVENUE",
  "TimeWindow": {
    "StartTime": "2026-11-16T12:11:24Z",
    "EndTime": "2026-11-16T12:11:24Z"
  },
  "Scope": "CLOUDFRONT",
  "Currency": "USDC"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 83
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "invalid timestamp: failed to parse int"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetRevenueStatisticsSummary

Retrieves a summary of monetization revenue for the specified time window.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetRevenueStatisticsSummary
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 123
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TimeWindow": {
    "StartTime": "2026-11-16T12:11:24Z",
    "EndTime": "2026-11-16T12:11:24Z"
  },
  "Scope": "CLOUDFRONT",
  "Currency": "USDC"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 83
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "invalid timestamp: failed to parse int"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetRevenueStatisticsTimeSeries

Retrieves time series data for monetization revenue.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetRevenueStatisticsTimeSeries
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 178
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "StatisticType": "DATE_HISTOGRAM",
  "TimeWindow": {
    "StartTime": "2026-11-16T12:11:24Z",
    "EndTime": "2026-11-16T12:11:24Z"
  },
  "Scope": "CLOUDFRONT",
  "Interval": "MINUTELY",
  "Currency": "USDC"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 83
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "invalid timestamp: failed to parse int"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetRuleGroup

Retrieves the specified RuleGroup .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetRuleGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 284
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: a rule group is addressed by its ARN or by its name, scope and id, field: RULE_GROUP, parameter: ARN",
  "Field": "RULE_GROUP",
  "Parameter": "ARN",
  "Reason": "a rule group is addressed by its ARN or by its name, scope and id"
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### GetSampledRequests

Gets detailed information about a specified number of requests--a sample--that WAF randomly selects from among the first 5,000 requests that your Amazon Web Services resource received during a time range that you choose.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetSampledRequests
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 212
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "WebAclArn": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "RuleMetricName": "example",
  "Scope": "CLOUDFRONT",
  "TimeWindow": {
    "StartTime": "2026-11-16T12:11:24Z",
    "EndTime": "2026-11-16T12:11:24Z"
  },
  "MaxItems": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 83
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "invalid timestamp: failed to parse int"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetTopPathStatisticsByTraffic

Retrieves aggregated statistics about the top URI paths accessed by bot traffic for a specified web ACL and time window.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetTopPathStatisticsByTraffic
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 216
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "WebAclArn": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Scope": "CLOUDFRONT",
  "TimeWindow": {
    "StartTime": "2026-11-16T12:11:24Z",
    "EndTime": "2026-11-16T12:11:24Z"
  },
  "Limit": 1,
  "NumberOfTopTrafficBotsPerPath": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 83
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "invalid timestamp: failed to parse int"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetWebACL

Retrieves the specified WebACL .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetWebACL
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 155
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "AccessDeniedException",
  "Message": "Critical information is missing in your request: GetWebACLRequest(name=null, scope=null, id=null, aRN=null)."
}
```

`400` `AccessDeniedException`. See [the walkthrough](#3-getwebacl) for a successful call.

### GetWebACLForResource

Retrieves the WebACL for the specified resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.GetWebACLForResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 480
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes., field: RESOURCE_ARN, parameter: arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Field": "RESOURCE_ARN",
  "Parameter": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Reason": "The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### ListAPIKeys

Retrieves a list of the API keys that you've defined for the specified scope.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.ListAPIKeys
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 120
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "APIKeySummaries": [],
  "ApplicationIntegrationURL": "https://aa730e0b0523.us-east-1.captcha-sdk.awswaf.com/aa730e0b0523/"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListAvailableManagedRuleGroupVersions

Returns a list of the available versions for the specified managed rule group.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.ListAvailableManagedRuleGroupVersions
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 62
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "VendorName": "example",
  "Name": "example",
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 143
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFNonexistentItemException",
  "Message": "ManagedRules with name example by vendor example does not exist. Please check your inputs."
}
```

`400` `WAFNonexistentItemException`.

### ListAvailableManagedRuleGroups

Retrieves an array of managed rule groups that are available for you to use.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.ListAvailableManagedRuleGroups
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 5084
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "NextMarker": "AWSManagedRulesAntiDDoSRuleSet",
  "ManagedRuleGroups": [
    {
      "VendorName": "AWS",
      "Name": "AWSManagedRulesCommonRuleSet",
      "VersioningSupported": true,
      "Description": "Contains rules that are generally applicable to web applications. This provides protection against exploitation of a wide range of vulnerabilities, including those described in OWASP publications."
    },
    {
      "VendorName": "AWS",
      "Name": "AWSManagedRulesAdminProtectionRuleSet",
      "VersioningSupported": true,
      "Description": "Contains rules that allow you to block external access to exposed admin pages. This may be useful if you are running third-party software or would like to reduce the risk of a malicious actor gaining administrative access to your application."
    },
    {
      "VendorName": "AWS",
      "Name": "AWSManagedRulesKnownBadInputsRuleSet",
      "VersioningSupported": true,
      "Description": "Contains rules that allow you to block request patterns that are known to be invalid and are associated with exploitation or discovery of vulnerabilities. This can help reduce the risk of a malicious actor discovering a vulnerable application."
… (75 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListIPSets

Retrieves an array of IPSetSummary objects for the IP sets that you manage.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.ListIPSets
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 13
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "IPSets": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListLoggingConfigurations

Retrieves an array of your LoggingConfiguration objects.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.ListLoggingConfigurations
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 28
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "LoggingConfigurations": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListManagedRuleSets

Retrieves the managed rule sets that you own.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.ListManagedRuleSets
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 22
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "ManagedRuleSets": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListMobileSdkReleases

Retrieves a list of the available releases for the mobile SDK and the specified device platform.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.ListMobileSdkReleases
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 18
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Platform": "IOS"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 102
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidOperationException",
  "Message": "This account is not authorized to use Mobile-SDK"
}
```

`400` `WAFInvalidOperationException`.

### ListRegexPatternSets

Retrieves an array of RegexPatternSetSummary objects for the regex pattern sets that you manage.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.ListRegexPatternSets
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 289
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "NextMarker": "example",
  "RegexPatternSets": [
    {
      "Name": "example",
      "Id": "107e0077-acac-4189-9aa5-232916ade12f",
      "Description": "",
      "LockToken": "1bf784aa-6a6e-461b-bd66-8265acf6a690",
      "ARN": "arn:aws:wafv2:us-east-1:000000000000:global/regexpatternset/example/107e0077-acac-4189-9aa5-232916ade12f"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListResourcesForWebACL

Retrieves an array of the Amazon Resource Names (ARNs) for the resources that are associated with the specified web ACL.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.ListResourcesForWebACL
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "WebACLArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 480
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes., field: RESOURCE_ARN, parameter: arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Field": "RESOURCE_ARN",
  "Parameter": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Reason": "The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### ListRuleGroups

Retrieves an array of RuleGroupSummary objects for the rule groups that you manage.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.ListRuleGroups
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 277
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "NextMarker": "example",
  "RuleGroups": [
    {
      "Name": "example",
      "Id": "9e008753-92b4-4543-9c31-2562882200ca",
      "Description": "",
      "LockToken": "104b7f86-f666-4a35-a7bf-3b70d357e963",
      "ARN": "arn:aws:wafv2:us-east-1:000000000000:global/rulegroup/example/9e008753-92b4-4543-9c31-2562882200ca"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListSettlementRecords

Retrieves individual settlement transaction records for monetization.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.ListSettlementRecords
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 123
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TimeWindow": {
    "StartTime": "2026-11-16T12:11:24Z",
    "EndTime": "2026-11-16T12:11:24Z"
  },
  "Scope": "CLOUDFRONT",
  "Currency": "USDC"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 83
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "invalid timestamp: failed to parse int"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListTagsForResource

Retrieves the TagInfoForResource for the specified resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.ListTagsForResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceARN": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 480
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes., field: RESOURCE_ARN, parameter: arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Field": "RESOURCE_ARN",
  "Parameter": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Reason": "The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### ListWebACLs

Retrieves an array of WebACLSummary objects for the web ACLs that you manage.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.ListWebACLs
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 14
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "WebACLs": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### PutLoggingConfiguration

Enables the specified LoggingConfiguration , to start logging from a web ACL, according to the configuration provided.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.PutLoggingConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 144
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "LoggingConfiguration": {
    "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example",
    "LogDestinationConfigs": [
      "example0000000000000"
    ]
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 480
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes., field: RESOURCE_ARN, parameter: arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Field": "RESOURCE_ARN",
  "Parameter": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Reason": "The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### PutManagedRuleSetVersions

Defines the versions of your managed rule set that you are offering to the customers.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.PutManagedRuleSetVersions
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example",
  "LockToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 221
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'id' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutPermissionPolicy

Use this to share a rule group with other accounts.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.PutPermissionPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 89
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Policy": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 154
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidPermissionPolicyException",
  "Message": "The policy specification doesn't meet the requirements to be attached to an AWS WAF resource."
}
```

`400` `WAFInvalidPermissionPolicyException`.

### TagResource

Associates tags with the specified Amazon Web Services resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.TagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 115
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceARN": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Tags": [
    {
      "Key": "example",
      "Value": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 480
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes., field: RESOURCE_ARN, parameter: arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Field": "RESOURCE_ARN",
  "Parameter": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Reason": "The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### UntagResource

Disassociates tags from an Amazon Web Services resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.UntagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 92
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceARN": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "TagKeys": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 480
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes., field: RESOURCE_ARN, parameter: arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Field": "RESOURCE_ARN",
  "Parameter": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Reason": "The ARN isn't valid. A valid ARN begins with arn: and includes other information separated by colons or slashes."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateIPSet

Updates the specified IPSet .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.UpdateIPSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 100
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example",
  "Addresses": [
    "example"
  ],
  "LockToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 378
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'id' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$; Value 'example' at 'lockToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateManagedRuleSetVersionExpiryDate

Updates the expiration information for your managed rule set.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.UpdateManagedRuleSetVersionExpiryDate
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 145
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example",
  "LockToken": "example",
  "VersionToExpire": "example",
  "ExpiryTimestamp": "2026-11-16T12:11:24Z"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 83
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "invalid timestamp: failed to parse int"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateRegexPatternSet

Updates the specified RegexPatternSet .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.UpdateRegexPatternSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 105
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example",
  "RegularExpressionList": [
    {}
  ],
  "LockToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 378
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'id' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$; Value 'example' at 'lockToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateRuleGroup

Updates the specified RuleGroup .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.UpdateRuleGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 184
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example",
  "VisibilityConfig": {
    "SampledRequestsEnabled": false,
    "CloudWatchMetricsEnabled": false,
    "MetricName": "example"
  },
  "LockToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 378
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'id' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$; Value 'example' at 'lockToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateWebACL

Updates the specified WebACL .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSWAF_20190729.UpdateWebACL
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 203
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example",
  "DefaultAction": {},
  "VisibilityConfig": {
    "SampledRequestsEnabled": false,
    "CloudWatchMetricsEnabled": false,
    "MetricName": "example"
  },
  "LockToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 378
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'id' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$; Value 'example' at 'lockToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/wafv2/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way AWS WAFV2 answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `Message`, `message`.
