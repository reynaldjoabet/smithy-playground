# AWS WAFV2

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: AWSWAF_20190729.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `WAFV2`
- **sigv4 signing name:** `wafv2`
- **Endpoint prefix:** `wafv2`
- **ARN namespace:** `wafv2`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `wafv2`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed.

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
X-Amz-Date: 20261116T121017Z
Content-Length: 109
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
date: Mon, 16 Nov 2026 12:10:17 GMT

{
  "Summary": {
    "Name": "docs-demo-blocked",
    "Id": "2cac5f68-91ae-4f38-94da-f67122c6b34c",
    "Description": "",
    "LockToken": "44834cf0-a986-482a-b67b-f68a453c218a",
    "ARN": "arn:aws:wafv2:us-east-1:000000000000:regional/ipset/docs-demo-blocked/2cac5f68-91ae-4f38-94da-f67122c6b34c"
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
X-Amz-Date: 20261116T121017Z
Content-Length: 548
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
          "ARN": "arn:aws:wafv2:us-east-1:000000000000:regional/ipset/docs-demo-blocked/2cac5f68-91ae-4f38-94da-f67122c6b34c"
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
date: Mon, 16 Nov 2026 12:10:17 GMT

{
  "Summary": {
    "Name": "docs-demo-acl",
    "Id": "107210c8-4e75-4ecb-88b5-016da1d497b6",
    "Description": "",
    "LockToken": "116737c2-9dac-4971-97ee-3d634e69bde3",
    "ARN": "arn:aws:wafv2:us-east-1:000000000000:regional/webacl/docs-demo-acl/107210c8-4e75-4ecb-88b5-016da1d497b6"
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
X-Amz-Date: 20261116T121017Z
Content-Length: 92
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "docs-demo-acl",
  "Scope": "REGIONAL",
  "Id": "107210c8-4e75-4ecb-88b5-016da1d497b6"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 952
date: Mon, 16 Nov 2026 12:10:17 GMT

{
  "WebACL": {
    "Name": "docs-demo-acl",
    "Id": "107210c8-4e75-4ecb-88b5-016da1d497b6",
    "ARN": "arn:aws:wafv2:us-east-1:000000000000:regional/webacl/docs-demo-acl/107210c8-4e75-4ecb-88b5-016da1d497b6",
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
            "ARN": "arn:aws:wafv2:us-east-1:000000000000:regional/ipset/docs-demo-blocked/2cac5f68-91ae-4f38-94da-f67122c6b34c"
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
  "LockToken": "116737c2-9dac-4971-97ee-3d634e69bde3"
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
X-Amz-Date: 20261116T121017Z
Content-Length: 149
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "docs-demo-blocked",
  "Scope": "REGIONAL",
  "Id": "2cac5f68-91ae-4f38-94da-f67122c6b34c",
  "LockToken": "44834cf0-a986-482a-b67b-f68a453c218a"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 192
date: Mon, 16 Nov 2026 12:10:17 GMT

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
X-Amz-Date: 20261116T121017Z
Content-Length: 145
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "docs-demo-acl",
  "Scope": "REGIONAL",
  "Id": "107210c8-4e75-4ecb-88b5-016da1d497b6",
  "LockToken": "00000000-0000-0000-0000-000000000000"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 177
date: Mon, 16 Nov 2026 12:10:17 GMT

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
X-Amz-Date: 20261116T121017Z
Content-Length: 145
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "docs-demo-acl",
  "Scope": "REGIONAL",
  "Id": "107210c8-4e75-4ecb-88b5-016da1d497b6",
  "LockToken": "116737c2-9dac-4971-97ee-3d634e69bde3"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 2
date: Mon, 16 Nov 2026 12:10:17 GMT

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
X-Amz-Date: 20261116T121017Z
Content-Length: 149
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/wafv2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "docs-demo-blocked",
  "Scope": "REGIONAL",
  "Id": "2cac5f68-91ae-4f38-94da-f67122c6b34c",
  "LockToken": "44834cf0-a986-482a-b67b-f68a453c218a"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 2
date: Mon, 16 Nov 2026 12:10:17 GMT

{}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 59 operations, 15 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AssociateWebACL

Associates a web ACL with a resource, to protect the resource.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.AssociateWebACL

{
  "WebACLArn": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.CheckCapacity

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

{
  "Capacity": 0
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateAPIKey

Creates an API key that contains a set of token domains.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.CreateAPIKey

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

{
  "APIKey": "<redacted>"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateIPSet

Creates an IPSet , which you use to identify web requests that originate from specific IP addresses or ranges of IP addresses.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.CreateIPSet

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

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: \"example\" is not in CIDR notation; expected <address>/<prefix length>, field: IP_ADDRESS, parameter: example",
  "Field": "IP_ADDRESS",
  "Parameter": "example",
  "Reason": "\"example\" is not in CIDR notation; expected <address>/<prefix length>"
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-createipset) for a successful call.

### CreateRegexPatternSet

Creates a RegexPatternSet , which you reference in a RegexPatternSetReferenceStatement , to have WAF inspect a web request component for the specified patterns.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.CreateRegexPatternSet

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

{
  "Summary": {
    "Name": "example",
    "Id": "59176e7b-1c4a-440e-902b-079ba572f7d9",
    "Description": "",
    "LockToken": "682728b5-e2df-4be8-8e0d-aa643f46ea75",
    "ARN": "arn:aws:wafv2:us-east-1:000000000000:global/regexpatternset/example/59176e7b-1c4a-440e-902b-079ba572f7d9"
  }
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateRuleGroup

Creates a RuleGroup per the specifications provided.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.CreateRuleGroup

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

{
  "Summary": {
    "Name": "example",
    "Id": "1b7b1042-625b-4169-a6d5-f16de13ee51e",
    "Description": "",
    "LockToken": "36b9f3dc-9596-4325-ad6b-29b7b8bcb372",
    "ARN": "arn:aws:wafv2:us-east-1:000000000000:global/rulegroup/example/1b7b1042-625b-4169-a6d5-f16de13ee51e"
  }
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateWebACL

Creates a WebACL per the specifications provided.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.CreateWebACL

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
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "Summary": {
    "Name": "example",
    "Id": "a9a7d870-bc6c-42d5-b787-0082e39925a8",
    "Description": "",
    "LockToken": "136ce73d-b08c-4932-abeb-9d4d99e499cf",
    "ARN": "arn:aws:wafv2:us-east-1:000000000000:global/webacl/example/a9a7d870-bc6c-42d5-b787-0082e39925a8"
  }
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#2-createwebacl) for a successful call.

### DeleteAPIKey

Deletes the specified API key.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.DeleteAPIKey

{
  "Scope": "CLOUDFRONT",
  "APIKey": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The parameter contains formatting that is not valid., field: null, parameter: example",
  "Parameter": "example",
  "Reason": "The parameter contains formatting that is not valid."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteFirewallManagerRuleGroups

Deletes all rule groups that are managed by Firewall Manager from the specified WebACL .

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.DeleteFirewallManagerRuleGroups

{
  "WebACLArn": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "WebACLLockToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.DeleteIPSet

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
X-Amz-Target: AWSWAF_20190729.DeleteLoggingConfiguration

{
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.DeletePermissionPolicy

{
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.DeleteRegexPatternSet

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
X-Amz-Target: AWSWAF_20190729.DeleteRuleGroup

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
X-Amz-Target: AWSWAF_20190729.DeleteWebACL

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
X-Amz-Target: AWSWAF_20190729.DescribeAllManagedProducts

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "ManagedProducts": [
    {
      "VendorName": "AWS",
      "ManagedRuleSetName": "AWSManagedRulesCommonRuleSet",
      "ProductId": "AWSManagedRulesCommonRuleSet",
      "ProductTitle": "Core rule set (CRS)",
      "ProductDescription": "Provides protection against exploitation of a wide range of vulnerabilities, including high risk and commonly occurring vulnerabilities described in OWASP publications.",
      "SnsTopicArn": "arn:aws:sns:us-east-1:000000000000:aws-managed-waf-rule-notifications",
      "IsVersioningSupported": true,
      "IsAdvancedManagedRuleSet": false
    },
    {
      "VendorName": "AWS",
      "ManagedRuleSetName": "AWSManagedRulesAdminProtectionRuleSet",
      "ProductId": "AWSManagedRulesAdminProtectionRuleSet",
      "ProductTitle": "Admin protection",
      "ProductDescription": "Blocks external access to exposed administrative pages.",
      "SnsTopicArn": "arn:aws:sns:us-east-1:000000000000:aws-managed-waf-rule-notifications",
      "IsVersioningSupported": true,
… (134 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeManagedProductsByVendor

Provides high-level information for the managed rule groups owned by a specific vendor.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.DescribeManagedProductsByVendor

{
  "VendorName": "example",
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "ManagedProducts": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeManagedRuleGroup

Provides high-level information for a managed rule group, including descriptions of the rules.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.DescribeManagedRuleGroup

{
  "VendorName": "example",
  "Name": "example",
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.DisassociateWebACL

{
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.GenerateMobileSdkReleaseUrl

{
  "Platform": "IOS",
  "ReleaseVersion": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.GetDecryptedAPIKey

{
  "Scope": "CLOUDFRONT",
  "APIKey": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "WAFInvalidParameterException",
  "message": "Error reason: The parameter contains formatting that is not valid., field: null, parameter: example",
  "Parameter": "example",
  "Reason": "The parameter contains formatting that is not valid."
}
```

`400` `WAFInvalidParameterException`: the placeholder values failed the operation's input validation.

### GetIPSet

Retrieves the specified IPSet .

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.GetIPSet

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.GetLoggingConfiguration

{
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.GetManagedRuleSet

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.GetMobileSdkRelease

{
  "Platform": "IOS",
  "ReleaseVersion": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.GetPermissionPolicy

{
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.GetRateBasedStatementManagedKeys

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
X-Amz-Target: AWSWAF_20190729.GetRegexPatternSet

{
  "Name": "example",
  "Scope": "CLOUDFRONT",
  "Id": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.GetRevenueStatistics

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
X-Amz-Target: AWSWAF_20190729.GetRevenueStatisticsSummary

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
X-Amz-Target: AWSWAF_20190729.GetRevenueStatisticsTimeSeries

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
X-Amz-Target: AWSWAF_20190729.GetRuleGroup

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.GetSampledRequests

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
X-Amz-Target: AWSWAF_20190729.GetTopPathStatisticsByTraffic

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
X-Amz-Target: AWSWAF_20190729.GetWebACL

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.GetWebACLForResource

{
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.ListAPIKeys

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "NextMarker": "00000000000000000001",
  "APIKeySummaries": [
    {
      "TokenDomains": [
        "example"
      ],
      "APIKey": "<redacted>",
      "CreationTimestamp": 1794831084.701,
      "Version": 1
    }
  ],
  "ApplicationIntegrationURL": "https://aa730e0b0523ce83942711e1baf4286e.us-east-1.captcha-sdk.awswaf.com/aa730e0b0523ce83942711e1baf4286e/"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListAvailableManagedRuleGroupVersions

Returns a list of the available versions for the specified managed rule group.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.ListAvailableManagedRuleGroupVersions

{
  "VendorName": "example",
  "Name": "example",
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.ListAvailableManagedRuleGroups

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.ListIPSets

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "IPSets": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListLoggingConfigurations

Retrieves an array of your LoggingConfiguration objects.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.ListLoggingConfigurations

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "LoggingConfigurations": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListManagedRuleSets

Retrieves the managed rule sets that you own.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.ListManagedRuleSets

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "ManagedRuleSets": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListMobileSdkReleases

Retrieves a list of the available releases for the mobile SDK and the specified device platform.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.ListMobileSdkReleases

{
  "Platform": "IOS"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.ListRegexPatternSets

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "NextMarker": "example",
  "RegexPatternSets": [
    {
      "Name": "example",
      "Id": "59176e7b-1c4a-440e-902b-079ba572f7d9",
      "Description": "",
      "LockToken": "682728b5-e2df-4be8-8e0d-aa643f46ea75",
      "ARN": "arn:aws:wafv2:us-east-1:000000000000:global/regexpatternset/example/59176e7b-1c4a-440e-902b-079ba572f7d9"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListResourcesForWebACL

Retrieves an array of the Amazon Resource Names (ARNs) for the resources that are associated with the specified web ACL.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.ListResourcesForWebACL

{
  "WebACLArn": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.ListRuleGroups

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "NextMarker": "example",
  "RuleGroups": [
    {
      "Name": "example",
      "Id": "1b7b1042-625b-4169-a6d5-f16de13ee51e",
      "Description": "",
      "LockToken": "36b9f3dc-9596-4325-ad6b-29b7b8bcb372",
      "ARN": "arn:aws:wafv2:us-east-1:000000000000:global/rulegroup/example/1b7b1042-625b-4169-a6d5-f16de13ee51e"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListSettlementRecords

Retrieves individual settlement transaction records for monetization.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.ListSettlementRecords

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
X-Amz-Target: AWSWAF_20190729.ListTagsForResource

{
  "ResourceARN": "arn:aws:wafv2:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.ListWebACLs

{
  "Scope": "CLOUDFRONT"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "NextMarker": "example",
  "WebACLs": [
    {
      "Name": "example",
      "Id": "a9a7d870-bc6c-42d5-b787-0082e39925a8",
      "Description": "",
      "LockToken": "136ce73d-b08c-4932-abeb-9d4d99e499cf",
      "ARN": "arn:aws:wafv2:us-east-1:000000000000:global/webacl/example/a9a7d870-bc6c-42d5-b787-0082e39925a8"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### PutLoggingConfiguration

Enables the specified LoggingConfiguration , to start logging from a web ACL, according to the configuration provided.

```http
POST / HTTP/1.1
X-Amz-Target: AWSWAF_20190729.PutLoggingConfiguration

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
X-Amz-Target: AWSWAF_20190729.PutManagedRuleSetVersions

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
X-Amz-Target: AWSWAF_20190729.PutPermissionPolicy

{
  "ResourceArn": "arn:aws:wafv2:us-east-1:000000000000:example/example",
  "Policy": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

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
X-Amz-Target: AWSWAF_20190729.TagResource

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
X-Amz-Target: AWSWAF_20190729.UntagResource

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
X-Amz-Target: AWSWAF_20190729.UpdateIPSet

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
X-Amz-Target: AWSWAF_20190729.UpdateManagedRuleSetVersionExpiryDate

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
X-Amz-Target: AWSWAF_20190729.UpdateRegexPatternSet

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
X-Amz-Target: AWSWAF_20190729.UpdateRuleGroup

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
X-Amz-Target: AWSWAF_20190729.UpdateWebACL

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
