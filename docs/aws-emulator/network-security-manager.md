# AWS Network Security Manager Customer API

## Wire protocol

- **Protocol:** `restJson1`
- **SDK id:** `Network Security Manager`
- **sigv4 signing name:** `network-security-manager`
- **Endpoint prefix:** `network-security-manager`
- **ARN namespace:** `network-security-manager`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `network-security-manager`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListPolicies`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:network-security-manager" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  'https://127.0.0.1:9297/policies'
```

## End-to-end walkthrough

Create, read, update, list and delete a firewall policy, the five operations the emulator implements. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreatePolicy

Answers 201 with the policy's id and an `updateToken` that guards later writes.

**Request**

```http
POST /policies HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 347
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "0a527e29-3085-4c74-a8b9-b10f2ee7b4df",
  "policyName": "docs-demo-waf",
  "policyDescription": "baseline WAF policy",
  "priority": 10,
  "firewallType": "WAF",
  "policyConfiguration": {
    "remediationEnabled": false,
    "resourcesCleanUp": true,
    "wafConfig": {
      "existingCustomerWebACLResolution": "NO_REMEDIATION",
      "conflictResolution": "MERGE_WHERE_APPLICABLE"
    }
  }
}
```

**Response**

```http
HTTP/1.1 201 Created
content-type: application/json
content-length: 660
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "policyId": "52cefb6edc68f7faaeaae56b58eeab991",
  "policyArn": "arn:aws:network-security-manager:us-east-1:000000000000:policy/52cefb6edc68f7faaeaae56b58eeab991",
  "policyName": "docs-demo-waf",
  "policyDescription": "baseline WAF policy",
  "status": "ACTIVE",
  "priority": 10,
  "associatedTemplateAndRuleList": [],
  "version": "1",
  "updateToken": "9f0a1c46-3bc2-4166-8bcf-c06090ea591c",
  "isSnapshot": false,
  "hasPublishedVersion": true,
  "firewallType": "WAF",
  "policyConfiguration": {
    "remediationEnabled": false,
    "resourcesCleanUp": true,
    "wafConfig": {
      "existingCustomerWebACLResolution": "NO_REMEDIATION",
      "conflictResolution": "MERGE_WHERE_APPLICABLE"
    }
  },
  "updatedAt": "2026-11-26T18:30:47.976832Z"
}
```

### 2. GetPolicy

Reads the stored policy.

**Request**

```http
GET /policies/52cefb6edc68f7faaeaae56b58eeab991 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 660
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "policyId": "52cefb6edc68f7faaeaae56b58eeab991",
  "policyArn": "arn:aws:network-security-manager:us-east-1:000000000000:policy/52cefb6edc68f7faaeaae56b58eeab991",
  "policyName": "docs-demo-waf",
  "policyDescription": "baseline WAF policy",
  "status": "ACTIVE",
  "priority": 10,
  "associatedTemplateAndRuleList": [],
  "version": "1",
  "updateToken": "9f0a1c46-3bc2-4166-8bcf-c06090ea591c",
  "isSnapshot": false,
  "hasPublishedVersion": true,
  "firewallType": "WAF",
  "policyConfiguration": {
    "remediationEnabled": false,
    "resourcesCleanUp": true,
    "wafConfig": {
      "existingCustomerWebACLResolution": "NO_REMEDIATION",
      "conflictResolution": "MERGE_WHERE_APPLICABLE"
    }
  },
  "updatedAt": "2026-11-26T18:30:47.976832Z"
}
```

### 3. UpdatePolicy

PATCH with the current update token; a new token comes back.

**Request**

```http
PATCH /policies/52cefb6edc68f7faaeaae56b58eeab991 HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 140
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "updateToken": "9f0a1c46-3bc2-4166-8bcf-c06090ea591c",
  "priority": 20,
  "isPublished": true,
  "clientToken": "127c12be-416a-45a5-b873-49c38df5ee50"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 660
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "policyId": "52cefb6edc68f7faaeaae56b58eeab991",
  "policyArn": "arn:aws:network-security-manager:us-east-1:000000000000:policy/52cefb6edc68f7faaeaae56b58eeab991",
  "policyName": "docs-demo-waf",
  "policyDescription": "baseline WAF policy",
  "status": "ACTIVE",
  "priority": 20,
  "associatedTemplateAndRuleList": [],
  "version": "2",
  "updateToken": "7385f6c0-6af3-444e-9547-b4ccd6e56d07",
  "isSnapshot": false,
  "hasPublishedVersion": true,
  "firewallType": "WAF",
  "policyConfiguration": {
    "remediationEnabled": false,
    "resourcesCleanUp": true,
    "wafConfig": {
      "existingCustomerWebACLResolution": "NO_REMEDIATION",
      "conflictResolution": "MERGE_WHERE_APPLICABLE"
    }
  },
  "updatedAt": "2026-11-26T18:30:47.979685Z"
}
```

### 4. UpdatePolicy

Reusing the old token is refused: concurrent writers cannot overwrite each other.

**Request**

```http
PATCH /policies/52cefb6edc68f7faaeaae56b58eeab991 HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 140
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "updateToken": "9f0a1c46-3bc2-4166-8bcf-c06090ea591c",
  "priority": 30,
  "isPublished": true,
  "clientToken": "9c8b0b4e-3c72-494e-b13a-6c6b804fe66f"
}
```

**Response**

```http
HTTP/1.1 409 Conflict
content-type: application/json
x-amzn-errortype: ConflictException
content-length: 228
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ConflictException",
  "message": "updateToken does not match the policy's current token; read the policy again and retry with the token it returns",
  "resourceId": "52cefb6edc68f7faaeaae56b58eeab991",
  "resourceType": "Policy"
}
```

### 5. ListPolicies

Policies of the account.

**Request**

```http
GET /policies HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 339
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "policies": [
    {
      "policyId": "52cefb6edc68f7faaeaae56b58eeab991",
      "policyArn": "arn:aws:network-security-manager:us-east-1:000000000000:policy/52cefb6edc68f7faaeaae56b58eeab991",
      "policyName": "docs-demo-waf",
      "status": "ACTIVE",
      "version": "2",
      "hasPublishedVersion": true,
      "firewallType": "WAF",
      "priority": 20,
      "updatedAt": "2026-11-26T18:30:47.979685Z"
    }
  ]
}
```

### 6. DeletePolicy

Delete it.

**Request**

```http
DELETE /policies/52cefb6edc68f7faaeaae56b58eeab991 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 204 No Content
content-type: application/json
date: Thu, 26 Nov 2026 18:30:47 GMT
```

### 7. GetPolicy

Now 404.

**Request**

```http
GET /policies/52cefb6edc68f7faaeaae56b58eeab991 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 197
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "message": "no policy answers to the identifier \"52cefb6edc68f7faaeaae56b58eeab991\"",
  "resourceId": "52cefb6edc68f7faaeaae56b58eeab991",
  "resourceType": "Policy"
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 46 operations, 9 accepted the placeholder call, 23 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.629* answered `501` in 1.0.595. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### CreateDeployment

Creates a deployment.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST /deployments HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 256
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "4b150168-77dd-49f0-b381-c8905acbbe5d",
  "deploymentName": "example",
  "deploymentConfiguration": {
    "enableCrossAccountVisibility": false
  },
  "associatedPolicyList": [
    {
      "policyIdentifier": "example"
    }
  ],
  "associatedScopeList": [
    {
      "scopeIdentifier": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 383
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "associatedPolicyList policyIdentifier must be an Amazon Resource Name of 20 to 1010 characters, but \"example\" is not one",
  "reason": "FIELD_VALIDATION_FAILED",
  "fieldList": [
    {
      "name": "associatedPolicyList",
      "message": "associatedPolicyList policyIdentifier must be an Amazon Resource Name of 20 to 1010 characters, but \"example\" is not one"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateDeploymentSnapshot

Creates a snapshot of the current published version of the specified deployment.

```http
POST /deployments/example/snapshots HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "23270b5b-423a-405c-80ec-74de5852075f"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation CreateDeploymentSnapshot is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreatePolicy

Creates a policy.

```http
POST /policies HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 187
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "ed052113-5d86-499b-aa67-7acbd2b42eb1",
  "policyName": "example",
  "priority": 1,
  "firewallType": "WAF",
  "policyConfiguration": {
    "remediationEnabled": false,
    "resourcesCleanUp": false
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 379
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "existingCustomerWebACLResolution is required for WAF policies",
  "reason": "FIELD_VALIDATION_FAILED",
  "fieldList": [
    {
      "name": "policyConfiguration.wafConfig.existingCustomerWebACLResolution",
      "message": "existingCustomerWebACLResolution must be specified when firewallType is WAF. Valid values: RETROFIT, OVERRIDE_ASSOCIATION, NO_REMEDIATION"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-createpolicy) for a successful call.

### CreatePolicySnapshot

Creates a snapshot of the current published version of the specified policy.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST /policies/example/snapshots HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "d48d9deb-5da8-4b59-aa59-34e6fc106f4e"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 135
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource was not found.",
  "resourceId": "example",
  "resourceType": "Policy"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateRule

Creates a rule.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST /rules HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 142
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "9e3e718e-e8b4-4bce-9248-0858b3580eef",
  "ruleName": "example",
  "firewallType": "WAF",
  "ruleType": "CONFIGURATION",
  "configuration": {}
}
```

```http
HTTP/1.1 201 Created
content-type: application/json
content-length: 414
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "ruleId": "89b613181be8e638fbd2b7071256ab581",
  "ruleArn": "arn:aws:network-security-manager:us-east-1:000000000000:rule/89b613181be8e638fbd2b7071256ab581",
  "ruleName": "example",
  "firewallType": "WAF",
  "ruleType": "CONFIGURATION",
  "configuration": {},
  "status": "ACTIVE",
  "version": "1",
  "updateToken": "67b5b9be-4399-45f9-8dbf-638938a99deb",
  "isSnapshot": false,
  "hasPublishedVersion": true,
  "updatedAt": "2026-11-26T18:30:47.987159Z"
}
```

Accepted: the emulator answered `201 Created` even with placeholder values.

### CreateRuleSnapshot

Creates a snapshot of the current published version of the specified rule.

```http
POST /rules/example/snapshots HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "981dd5f2-1a1b-45eb-bc08-727f20346d8a"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 119
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation CreateRuleSnapshot is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateScope

Creates a scope.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST /scopes HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 127
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "30393cbb-019e-48fc-a13b-ff43207a1401",
  "scopeName": "example",
  "scopeConfiguration": {
    "resourceScopes": {
      "key": {}
    }
  }
}
```

```http
HTTP/1.1 201 Created
content-type: application/json
content-length: 402
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "scopeId": "85314d12aa87b7b7e23d93731dda15a11",
  "scopeArn": "arn:aws:network-security-manager:us-east-1:000000000000:scope/85314d12aa87b7b7e23d93731dda15a11",
  "scopeName": "example",
  "scopeConfiguration": {
    "resourceScopes": {
      "key": {}
    }
  },
  "status": "ACTIVE",
  "version": "1",
  "updateToken": "81c605b4-2456-4b06-a610-15fdb6f52d7f",
  "isSnapshot": false,
  "hasPublishedVersion": true,
  "updatedAt": "2026-11-26T18:30:47.989107Z"
}
```

Accepted: the emulator answered `201 Created` even with placeholder values.

### CreateScopeSnapshot

Creates a snapshot of the current published version of the specified scope.

```http
POST /scopes/example/snapshots HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "dd21483f-457a-45a6-942e-1bfe592b1be1"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 116
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation POST /scopes/example/snapshots is not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateTemplate

Creates a template.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST /templates HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 152
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "a698db72-af68-45ed-8df1-17dc2c15a847",
  "templateName": "example",
  "associatedRuleList": [
    {
      "ruleIdentifier": "example"
    }
  ],
  "firewallType": "WAF"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 373
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "associatedRuleList ruleIdentifier must be an Amazon Resource Name of 20 to 1010 characters, but \"example\" is not one",
  "reason": "FIELD_VALIDATION_FAILED",
  "fieldList": [
    {
      "name": "associatedRuleList",
      "message": "associatedRuleList ruleIdentifier must be an Amazon Resource Name of 20 to 1010 characters, but \"example\" is not one"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateTemplateSnapshot

Creates a snapshot of the current published version of the specified template.

```http
POST /templates/example/snapshots HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "bb2f9a0e-240d-4201-8615-7a609f2eb09c"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 123
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation CreateTemplateSnapshot is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteAdminAccount

Removes the specified AWS Network Security Manager administrator account.

```http
DELETE /admin-account/100000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 119
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DeleteAdminAccount is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteDeployment

Deletes the specified deployment.

```http
DELETE /deployments/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 117
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DeleteDeployment is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeletePolicy

Deletes the specified policy.

```http
DELETE /policies/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 204 No Content
content-type: application/json
date: Thu, 26 Nov 2026 18:30:47 GMT
```

Accepted: the emulator answered `204 No Content` even with placeholder values. See [the walkthrough](#6-deletepolicy) for a successful call.

### DeleteRule

Deletes the specified rule.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
DELETE /rules/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 122
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "ruleIdentifier must be the resource's ARN.",
  "reason": "FIELD_VALIDATION_FAILED"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteScope

Deletes the specified scope.

```http
DELETE /scopes/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 112
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DeleteScope is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteTemplate

Deletes the specified template.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
DELETE /templates/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 204 No Content
content-type: application/json
date: Thu, 26 Nov 2026 18:30:47 GMT
```

Accepted: the emulator answered `204 No Content` even with placeholder values.

### GenerateRuleConfiguration

Generates a rule configuration from a natural-language description.

```http
POST /GenerateRuleConfiguration HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 125
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "prompt": "example",
  "ruleFirewallType": "WAF",
  "ruleType": "CONFIGURATION",
  "clientToken": "fccfca78-b68e-4cb3-9a21-22d6034a3248"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 126
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation GenerateRuleConfiguration is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetAdminAccount

Retrieves the details of the specified AWS Network Security Manager administrator account.

```http
GET /admin-account/100000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 117
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation GET /admin-account/100000000000 is not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetDeployment

Retrieves the details of the specified deployment, including coverage information and any warnings.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /deployments/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 139
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource was not found.",
  "resourceId": "example",
  "resourceType": "Deployment"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetPolicy

Retrieves the details of the specified policy.

```http
GET /policies/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 145
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "message": "no policy answers to the identifier \"example\"",
  "resourceId": "example",
  "resourceType": "Policy"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#2-getpolicy) for a successful call.

### GetRule

Retrieves the details of the specified rule.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /rules/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 122
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "ruleIdentifier must be the resource's ARN.",
  "reason": "FIELD_VALIDATION_FAILED"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetScope

Retrieves the details of the specified scope.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /scopes/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 134
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource was not found.",
  "resourceId": "example",
  "resourceType": "Scope"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetTemplate

Retrieves the details of the specified template.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /templates/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 137
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource was not found.",
  "resourceId": "example",
  "resourceType": "Template"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListAdminAccounts

Lists the AWS Network Security Manager administrator accounts in the organization.

```http
GET /admin-accounts HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 118
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListAdminAccounts is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListAggregateResourceSynchronizationStatuses

Lists the aggregated synchronization statuses of resources across the deployments in your administrator account.

```http
GET /aggregate-sync-statuses HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 145
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListAggregateResourceSynchronizationStatuses is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListDeploymentSnapshots

Lists the snapshots of the specified deployment.

```http
GET /deployments/example/snapshots HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 124
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListDeploymentSnapshots is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListDeployments

Lists the deployments in the account.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /deployments HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 18
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "deployments": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListPolicies

Lists the policies in the account.

```http
GET /policies HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 15
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "policies": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#5-listpolicies) for a successful call.

### ListPolicySnapshots

Lists the snapshots of the specified policy.

```http
GET /policies/example/snapshots HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 120
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListPolicySnapshots is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListResourceAssociations

Lists the resources associated with the specified resource.

```http
GET /resource-associations?resourceIdentifier=example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 125
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListResourceAssociations is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListResourceSynchronizationStatuses

Lists the synchronization statuses of the resources covered by the specified deployment.

```http
GET /resource-sync-statuses?deploymentIdentifier=example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 136
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListResourceSynchronizationStatuses is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListRuleSnapshots

Lists the snapshots of the specified rule.

```http
GET /rules/example/snapshots HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 118
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListRuleSnapshots is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListRules

Lists the rules in the account.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
GET /rules HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 335
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "rules": [
    {
      "ruleId": "89b613181be8e638fbd2b7071256ab581",
      "ruleArn": "arn:aws:network-security-manager:us-east-1:000000000000:rule/89b613181be8e638fbd2b7071256ab581",
      "ruleName": "example",
      "firewallType": "WAF",
      "ruleType": "CONFIGURATION",
      "status": "ACTIVE",
      "version": "1",
      "hasPublishedVersion": true,
      "updatedAt": "2026-11-26T18:30:47.987159Z"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListScopeSnapshots

Lists the snapshots of the specified scope.

```http
GET /scopes/example/snapshots HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 119
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListScopeSnapshots is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListScopes

Lists the scopes in the account.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
GET /scopes HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 292
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "scopes": [
    {
      "scopeId": "85314d12aa87b7b7e23d93731dda15a11",
      "scopeArn": "arn:aws:network-security-manager:us-east-1:000000000000:scope/85314d12aa87b7b7e23d93731dda15a11",
      "scopeName": "example",
      "status": "ACTIVE",
      "version": "1",
      "hasPublishedVersion": true,
      "updatedAt": "2026-11-26T18:30:47.989107Z"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTagsForResource

Lists the tags associated with the specified resource.

```http
GET /tags/arn%3Aaws%3Anetworksecuritymanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 120
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListTagsForResource is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListTemplateSnapshots

Lists the snapshots of the specified template.

```http
GET /templates/example/snapshots HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 122
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListTemplateSnapshots is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListTemplates

Lists the templates in the account.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /templates HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 16
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "templates": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### PutAdminAccount

Sets the AWS account that serves as an AWS Network Security Manager administrator account, and optionally configures the scope of resources that the administrator can manage.

```http
PUT /admin-account HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Content-Length: 41
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "accountId": "100000000000",
  "priority": 1
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 104
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "NotImplemented",
  "message": "operation PUT /admin-account is not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### TagResource

Adds or overwrites the specified tags on the given resource.

```http
POST /tags/arn%3Aaws%3Anetworksecuritymanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Content-Length: 26
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "tags": {
    "key": "example"
  }
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 112
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "NotImplemented",
  "message": "operation TagResource is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UntagResource

Removes the specified tags from the given resource.

```http
DELETE /tags/arn%3Aaws%3Anetworksecuritymanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample?tagKeys=example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 114
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "NotImplemented",
  "message": "operation UntagResource is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UpdateDeployment

Updates the specified deployment.

```http
PATCH /deployments/example HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Content-Length: 127
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "updateToken": "example00000000000000000000000000000",
  "isPublished": false,
  "clientToken": "80bd55c5-185b-4b6a-98a7-4a1ae5508204"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 259
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example00000000000000000000000000000' at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdatePolicy

Updates the specified policy.

```http
PATCH /policies/example HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Content-Length: 127
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "updateToken": "example00000000000000000000000000000",
  "isPublished": false,
  "clientToken": "d84f4a77-44f3-4f6c-843e-6ed5729bba8a"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 259
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example00000000000000000000000000000' at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-updatepolicy) for a successful call.

### UpdateRule

Updates the specified rule.

```http
PATCH /rules/example HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Content-Length: 127
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "updateToken": "example00000000000000000000000000000",
  "isPublished": false,
  "clientToken": "8ae2d8aa-c584-4821-8372-2f071f8a977a"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 259
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example00000000000000000000000000000' at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateScope

Updates the specified scope.

```http
PATCH /scopes/example HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Content-Length: 127
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "updateToken": "example00000000000000000000000000000",
  "isPublished": false,
  "clientToken": "058794f5-4442-4d2b-94d2-96be960ee48d"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 107
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "NotImplemented",
  "message": "operation PATCH /scopes/example is not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UpdateTemplate

Updates the specified template.

```http
PATCH /templates/example HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Content-Length: 127
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "updateToken": "example00000000000000000000000000000",
  "isPublished": false,
  "clientToken": "3558ded9-9eed-4579-a304-fc64b37176cc"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 259
date: Thu, 26 Nov 2026 18:30:48 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example00000000000000000000000000000' at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/network-security-manager/aws4_request` in the `Authorization` header) and the operation from the request itself: an HTTP method and path per operation (the `HTTP binding`), with identifiers in the path, options in the query string or headers, and the rest of the input as a JSON body.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way AWS Network Security Manager Customer API answers in AWS: a JSON body and an HTTP status chosen per operation. Errors captured on this page are a JSON body with `__type`, `message`, plus the `x-amzn-errortype` header.
