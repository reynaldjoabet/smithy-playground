# AWS Network Security Manager Customer API

## Wire protocol

- **Protocol:** `restJson1`
- **SDK id:** `Network Security Manager`
- **sigv4 signing name:** `network-security-manager`
- **Endpoint prefix:** `network-security-manager`
- **ARN namespace:** `network-security-manager`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `network-security-manager`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed.

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
X-Amz-Date: 20261116T121058Z
Content-Length: 367
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "0a527e29-3085-4c74-a8b9-b10f2ee7b4df",
  "policyName": "docs-demo-waf",
  "policyDescription": "baseline WAF policy",
  "priority": 10,
  "firewallType": "WAF",
  "policyConfiguration": {
    "remediationEnabled": false,
    "resourcesCleanUp": false,
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
content-length: 651
date: Mon, 16 Nov 2026 12:10:58 GMT

{
  "policyId": "5fc13387863d374ed2df2196259724f43",
  "policyArn": "arn:aws:network-security-manager:us-east-1:000000000000:policy/5fc13387863d374ed2df2196259724f43",
  "policyName": "docs-demo-waf",
  "policyDescription": "baseline WAF policy",
  "status": "ACTIVE",
  "priority": 10,
  "associatedTemplateAndRuleList": [],
  "version": "1",
  "updateToken": "210a0cf0-a58f-4170-908f-025a2fc54bd5",
  "isSnapshot": false,
  "hasPublishedVersion": true,
  "firewallType": "WAF",
  "policyConfiguration": {
    "remediationEnabled": false,
    "resourcesCleanUp": false,
    "wafConfig": {
      "existingCustomerWebACLResolution": "NO_REMEDIATION",
      "conflictResolution": "MERGE_WHERE_APPLICABLE"
    }
  },
  "updatedAt": 1794831058.2312768
}
```

### 2. GetPolicy

Reads the stored policy.

**Request**

```http
GET /policies/5fc13387863d374ed2df2196259724f43 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121058Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 651
date: Mon, 16 Nov 2026 12:10:58 GMT

{
  "policyId": "5fc13387863d374ed2df2196259724f43",
  "policyArn": "arn:aws:network-security-manager:us-east-1:000000000000:policy/5fc13387863d374ed2df2196259724f43",
  "policyName": "docs-demo-waf",
  "policyDescription": "baseline WAF policy",
  "status": "ACTIVE",
  "priority": 10,
  "associatedTemplateAndRuleList": [],
  "version": "1",
  "updateToken": "210a0cf0-a58f-4170-908f-025a2fc54bd5",
  "isSnapshot": false,
  "hasPublishedVersion": true,
  "firewallType": "WAF",
  "policyConfiguration": {
    "remediationEnabled": false,
    "resourcesCleanUp": false,
    "wafConfig": {
      "existingCustomerWebACLResolution": "NO_REMEDIATION",
      "conflictResolution": "MERGE_WHERE_APPLICABLE"
    }
  },
  "updatedAt": 1794831058.2312768
}
```

### 3. UpdatePolicy

PATCH with the current update token; a new token comes back.

**Request**

```http
PATCH /policies/5fc13387863d374ed2df2196259724f43 HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121058Z
Content-Length: 147
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "updateToken": "210a0cf0-a58f-4170-908f-025a2fc54bd5",
  "priority": 20,
  "isPublished": true,
  "clientToken": "127c12be-416a-45a5-b873-49c38df5ee50"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 652
date: Mon, 16 Nov 2026 12:10:58 GMT

{
  "policyId": "5fc13387863d374ed2df2196259724f43",
  "policyArn": "arn:aws:network-security-manager:us-east-1:000000000000:policy/5fc13387863d374ed2df2196259724f43",
  "policyName": "docs-demo-waf",
  "policyDescription": "baseline WAF policy",
  "status": "ACTIVE",
  "priority": 20,
  "associatedTemplateAndRuleList": [],
  "version": "2",
  "updateToken": "6e515ea2-a1e8-4a24-90ed-b62091eb3118",
  "isSnapshot": false,
  "hasPublishedVersion": true,
  "firewallType": "WAF",
  "policyConfiguration": {
    "remediationEnabled": false,
    "resourcesCleanUp": false,
    "wafConfig": {
      "existingCustomerWebACLResolution": "NO_REMEDIATION",
      "conflictResolution": "MERGE_WHERE_APPLICABLE"
    }
  },
  "updatedAt": 1794831058.2515435
}
```

### 4. UpdatePolicy

Reusing the old token is refused: concurrent writers cannot overwrite each other.

**Request**

```http
PATCH /policies/5fc13387863d374ed2df2196259724f43 HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121058Z
Content-Length: 147
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-security-manager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "updateToken": "210a0cf0-a58f-4170-908f-025a2fc54bd5",
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
date: Mon, 16 Nov 2026 12:10:58 GMT

{
  "__type": "ConflictException",
  "message": "updateToken does not match the policy's current token; read the policy again and retry with the token it returns",
  "resourceId": "5fc13387863d374ed2df2196259724f43",
  "resourceType": "Policy"
}
```

### 5. ListPolicies

Policies of the account.

**Request**

```http
GET /policies HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121058Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 330
date: Mon, 16 Nov 2026 12:10:58 GMT

{
  "policies": [
    {
      "policyId": "5fc13387863d374ed2df2196259724f43",
      "policyArn": "arn:aws:network-security-manager:us-east-1:000000000000:policy/5fc13387863d374ed2df2196259724f43",
      "policyName": "docs-demo-waf",
      "status": "ACTIVE",
      "version": "2",
      "hasPublishedVersion": true,
      "firewallType": "WAF",
      "priority": 20,
      "updatedAt": 1794831058.2515435
    }
  ]
}
```

### 6. DeletePolicy

Delete it.

**Request**

```http
DELETE /policies/5fc13387863d374ed2df2196259724f43 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121058Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 204 No Content
content-type: application/json
date: Mon, 16 Nov 2026 12:10:58 GMT
```

### 7. GetPolicy

Now 404.

**Request**

```http
GET /policies/5fc13387863d374ed2df2196259724f43 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121058Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-security-manager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 197
date: Mon, 16 Nov 2026 12:10:58 GMT

{
  "__type": "ResourceNotFoundException",
  "message": "no policy answers to the identifier \"5fc13387863d374ed2df2196259724f43\"",
  "resourceId": "5fc13387863d374ed2df2196259724f43",
  "resourceType": "Policy"
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`) are left out here; the walkthrough shows them in full.

Of 46 operations, 8 accepted the placeholder call, 26 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### CreateDeployment

Creates a deployment.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST /deployments HTTP/1.1
Content-Type: application/json

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

{
  "clientToken": "23270b5b-423a-405c-80ec-74de5852075f"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
HTTP/1.1 201 Created
content-type: application/json

{
  "policyId": "5d7cbe9ddb1c412a4b64bd2af8b02f3d4",
  "policyArn": "arn:aws:network-security-manager:us-east-1:000000000000:policy/5d7cbe9ddb1c412a4b64bd2af8b02f3d4",
  "policyName": "example",
  "status": "ACTIVE",
  "priority": 1,
  "associatedTemplateAndRuleList": [],
  "version": "1",
  "updateToken": "140b2d3b-6cce-4269-b688-7c46287f3a35",
  "isSnapshot": false,
  "hasPublishedVersion": true,
  "firewallType": "WAF",
  "policyConfiguration": {
    "remediationEnabled": false,
    "resourcesCleanUp": false
  },
  "updatedAt": 1794831087.502944
}
```

Accepted: the emulator answered `201 Created` even with placeholder values. See [the walkthrough](#1-createpolicy) for a successful call.

### CreatePolicySnapshot

Creates a snapshot of the current published version of the specified policy.

```http
POST /policies/example/snapshots HTTP/1.1
Content-Type: application/json

{
  "clientToken": "d48d9deb-5da8-4b59-aa59-34e6fc106f4e"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

{
  "__type": "NotImplemented",
  "message": "operation CreatePolicySnapshot is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateRule

Creates a rule.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST /rules HTTP/1.1
Content-Type: application/json

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

{
  "ruleId": "7fcbe1a1aa5ee1268a8baccb042532381",
  "ruleArn": "arn:aws:network-security-manager:us-east-1:000000000000:rule/7fcbe1a1aa5ee1268a8baccb042532381",
  "ruleName": "example",
  "firewallType": "WAF",
  "ruleType": "CONFIGURATION",
  "configuration": {},
  "status": "ACTIVE",
  "version": "1",
  "updateToken": "faae2b05-8597-4bd7-b089-dca98512f3c1",
  "isSnapshot": false,
  "hasPublishedVersion": true,
  "updatedAt": "2026-11-24T01:44:53.252952Z"
}
```

Accepted: the emulator answered `201 Created` even with placeholder values.

### CreateRuleSnapshot

Creates a snapshot of the current published version of the specified rule.

```http
POST /rules/example/snapshots HTTP/1.1
Content-Type: application/json

{
  "clientToken": "981dd5f2-1a1b-45eb-bc08-727f20346d8a"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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

{
  "scopeId": "0d2624465b45bb3cde090764620edcf61",
  "scopeArn": "arn:aws:network-security-manager:us-east-1:000000000000:scope/0d2624465b45bb3cde090764620edcf61",
  "scopeName": "example",
  "scopeConfiguration": {
    "resourceScopes": {
      "key": {}
    }
  },
  "status": "ACTIVE",
  "version": "1",
  "updateToken": "adb73e77-860d-4d3a-9443-6955bd377687",
  "isSnapshot": false,
  "hasPublishedVersion": true,
  "updatedAt": "2026-11-24T01:44:53.339832Z"
}
```

Accepted: the emulator answered `201 Created` even with placeholder values.

### CreateScopeSnapshot

Creates a snapshot of the current published version of the specified scope.

```http
POST /scopes/example/snapshots HTTP/1.1
Content-Type: application/json

{
  "clientToken": "dd21483f-457a-45a6-942e-1bfe592b1be1"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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

{
  "clientToken": "bb2f9a0e-240d-4201-8615-7a609f2eb09c"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
```

```http
HTTP/1.1 204 No Content
content-type: application/json
```

Accepted: the emulator answered `204 No Content` even with placeholder values. See [the walkthrough](#6-deletepolicy) for a successful call.

### DeleteRule

Deletes the specified rule.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
DELETE /rules/example HTTP/1.1
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException

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
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
```

```http
HTTP/1.1 204 No Content
content-type: application/json
```

Accepted: the emulator answered `204 No Content` even with placeholder values.

### GenerateRuleConfiguration

Generates a rule configuration from a natural-language description.

```http
POST /GenerateRuleConfiguration HTTP/1.1
Content-Type: application/json

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
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

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
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

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
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException

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
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

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
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

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
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
```

```http
HTTP/1.1 200 OK
content-type: application/json

{
  "deployments": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListPolicies

Lists the policies in the account.

```http
GET /policies HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/json

{
  "policies": [
    {
      "policyId": "5d7cbe9ddb1c412a4b64bd2af8b02f3d4",
      "policyArn": "arn:aws:network-security-manager:us-east-1:000000000000:policy/5d7cbe9ddb1c412a4b64bd2af8b02f3d4",
      "policyName": "example",
      "status": "ACTIVE",
      "version": "1",
      "hasPublishedVersion": true,
      "firewallType": "WAF",
      "priority": 1,
      "updatedAt": 1794831087.502944
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#5-listpolicies) for a successful call.

### ListPolicySnapshots

Lists the snapshots of the specified policy.

```http
GET /policies/example/snapshots HTTP/1.1
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

{
  "__type": "NotImplemented",
  "message": "operation ListRuleSnapshots is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListRules

Lists the rules in the account.

```http
GET /rules HTTP/1.1
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

{
  "__type": "NotImplemented",
  "message": "operation ListRules is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListScopeSnapshots

Lists the snapshots of the specified scope.

```http
GET /scopes/example/snapshots HTTP/1.1
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

{
  "__type": "NotImplemented",
  "message": "operation ListScopeSnapshots is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListScopes

Lists the scopes in the account.

```http
GET /scopes HTTP/1.1
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

{
  "__type": "NotImplemented",
  "message": "operation ListScopes is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListTagsForResource

Lists the tags associated with the specified resource.

```http
GET /tags/arn%3Aaws%3Anetworksecuritymanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
```

```http
HTTP/1.1 200 OK
content-type: application/json

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

{
  "accountId": "100000000000",
  "priority": 1
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented

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

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example00000000000000000000000000000' at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdatePolicy

Updates the specified policy.

```http
PATCH /policies/example HTTP/1.1
Content-Type: application/json

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

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example00000000000000000000000000000' at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-updatepolicy) for a successful call.

### UpdateRule

Updates the specified rule.

```http
PATCH /rules/example HTTP/1.1
Content-Type: application/json

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

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example00000000000000000000000000000' at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateScope

Updates the specified scope.

```http
PATCH /scopes/example HTTP/1.1
Content-Type: application/json

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

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example00000000000000000000000000000' at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
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
