# Amazon Cognito Identity Pools

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: AWSCognitoIdentityService.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `Cognito Identity`
- **sigv4 signing name:** `cognito-identity`
- **Endpoint prefix:** `cognito-identity`
- **ARN namespace:** `cognito-identity`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `cognito-identity`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListIdentityPools`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:cognito-identity" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.1' \
  -H 'X-Amz-Target: AWSCognitoIdentityService.ListIdentityPools' \
  --data-binary '{"MaxResults": 10}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create an identity pool that allows guest access, and exchange a guest identity for temporary AWS credentials. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. AWS Identity and Access Management: CreateRole

The IAM role guests receive; Cognito assumes it on their behalf with web identity federation.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 347
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateRole
&Version=2010-05-08
&RoleName=docs-demo-guest
&AssumeRolePolicyDocument=%7B%22Version%22%3A%20%222012-10-17%22%2C%20%22Statement%22%3A%20%5B%7B%22Effect%22%3A%20%22Allow%22%2C%20%22Principal%22%3A%20%7B%22Federated%22%3A%20%22cognito-identity.amazonaws.com%22%7D%2C%20%22Action%22%3A%20%22sts%3AAssumeRoleWithWebIdentity%22%7D%5D%7D
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 854
date: Thu, 26 Nov 2026 18:27:55 GMT

<CreateRoleResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateRoleResult>
    <Role xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <RoleName>docs-demo-guest</RoleName>
      <RoleId>AROAOWVOJOY3VBWOJCSHY</RoleId>
      <Arn>arn:aws:iam::000000000000:role/docs-demo-guest</Arn>
      <CreateDate>2026-11-26T18:27:55.591Z</CreateDate>
      <AssumeRolePolicyDocument>%7B%22Version%22%3A%222012-10-17%22%2C%22Statement%22%3A%5B%7B%22Effect%22%3A%22Allow%22%2C%22Principal%22%3A%7B%22Federated%22%3A%22cognito-identity.amazonaws.com%22%7D%2C%22Action%22%3A%22sts%3AAssumeRoleWithWebIdentity%22%7D%5D%7D</AssumeRolePolicyDocument>
      <MaxSessionDuration>3600</MaxSessionDuration>
      <Tags></Tags>
      <RoleLastUsed></RoleLastUsed>
    </Role>
  </CreateRoleResult>
  <ResponseMetadata>
    <RequestId>bd403d74-cf06-4c5a-a2cd-d1ef4e45bcd3</RequestId>
  </ResponseMetadata>
</CreateRoleResponse>
```

### 2. CreateIdentityPool

Pool ids have the form `region:uuid`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityService.CreateIdentityPool
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityPoolName": "docs_demo_pool",
  "AllowUnauthenticatedIdentities": true
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 8b3ad744-6231-4549-b31f-47ff916d2c86
content-length: 367
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "IdentityPoolId": "us-east-1:69f4e3aa-5b14-4761-8119-17dd99addc33",
  "IdentityPoolName": "docs_demo_pool",
  "AllowUnauthenticatedIdentities": true,
  "IdentityPoolTags": {},
  "IdentityPoolArn": "arn:aws:cognito-identity:us-east-1:000000000000:identitypool/us-east-1:69f4e3aa-5b14-4761-8119-17dd99addc33",
  "CreationDate": 1795717675.5937343,
  "LastModifiedDate": 1795717675.5937343
}
```

### 3. SetIdentityPoolRoles

Map unauthenticated (guest) identities to the role.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityService.SetIdentityPoolRoles
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 144
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityPoolId": "us-east-1:69f4e3aa-5b14-4761-8119-17dd99addc33",
  "Roles": {
    "unauthenticated": "arn:aws:iam::000000000000:role/docs-demo-guest"
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 2e45a7ae-28b2-482c-b105-a46d1cec3ac0
content-length: 2
date: Thu, 26 Nov 2026 18:27:55 GMT

{}
```

### 4. GetId

Without any login provider this mints a guest identity id.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityService.GetId
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 94
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccountId": "000000000000",
  "IdentityPoolId": "us-east-1:69f4e3aa-5b14-4761-8119-17dd99addc33"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: b861ace0-b728-4d15-afd1-602e03892d8c
content-length: 63
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "IdentityId": "us-east-1:d552b1db-16df-4148-b28f-0f8a91740295"
}
```

### 5. GetCredentialsForIdentity

Cognito assumes the guest role and returns temporary credentials (the secret key and session token are redacted).

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityService.GetCredentialsForIdentity
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 63
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityId": "us-east-1:d552b1db-16df-4148-b28f-0f8a91740295"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: e9bb4a2e-ea54-426b-8ede-6150597ce438
content-length: 935
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "IdentityId": "us-east-1:d552b1db-16df-4148-b28f-0f8a91740295",
  "Credentials": {
    "AccessKeyId": "EMULATORAS4AHJAYIXVS",
    "SecretKey": "<redacted>",
    "SessionToken": "<redacted>",
    "Expiration": 1795721275.5973651
  }
}
```

### 6. DescribeIdentityPool

The pool's settings.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityService.DescribeIdentityPool
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityPoolId": "us-east-1:69f4e3aa-5b14-4761-8119-17dd99addc33"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 49d05959-be7f-4d6c-98e4-cb376f0b0b81
content-length: 367
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "IdentityPoolId": "us-east-1:69f4e3aa-5b14-4761-8119-17dd99addc33",
  "IdentityPoolName": "docs_demo_pool",
  "AllowUnauthenticatedIdentities": true,
  "IdentityPoolTags": {},
  "IdentityPoolArn": "arn:aws:cognito-identity:us-east-1:000000000000:identitypool/us-east-1:69f4e3aa-5b14-4761-8119-17dd99addc33",
  "CreationDate": 1795717675.5937343,
  "LastModifiedDate": 1795717675.5937343
}
```

### 7. DeleteIdentityPool

Delete the pool ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityService.DeleteIdentityPool
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityPoolId": "us-east-1:69f4e3aa-5b14-4761-8119-17dd99addc33"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: ce0d7f09-02c3-4e3f-9842-6e3aa0a8bcb4
content-length: 2
date: Thu, 26 Nov 2026 18:27:55 GMT

{}
```

### 8. GetId

... after which identities can no longer be minted from it: `ResourceNotFoundException`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityService.GetId
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 94
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccountId": "000000000000",
  "IdentityPoolId": "us-east-1:69f4e3aa-5b14-4761-8119-17dd99addc33"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 8a5af675-04ba-4a9b-8602-f1621064dbaa
x-amzn-errortype: ResourceNotFoundException:
content-length: 123
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ResourceNotFoundException",
  "message": "IdentityPool 'us-east-1:69f4e3aa-5b14-4761-8119-17dd99addc33' not found."
}
```

### 9. AWS Identity and Access Management: DeleteRole

Clean up the role.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteRole
&Version=2010-05-08
&RoleName=docs-demo-guest
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 224
date: Thu, 26 Nov 2026 18:27:55 GMT

<DeleteRoleResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteRoleResult></DeleteRoleResult>
  <ResponseMetadata>
    <RequestId>5b3e77a3-a776-4ee5-bbd2-c17c8c8993c8</RequestId>
  </ResponseMetadata>
</DeleteRoleResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 23 operations, 2 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### CreateIdentityPool

Creates a new identity pool.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.CreateIdentityPool
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityPoolName": "example","AllowUnauthenticatedIdentities": false}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 8342b23c-60de-492b-a8ec-920c9ce36ca9
content-length: 361
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "IdentityPoolId": "us-east-1:60e11652-424d-46b3-a372-5061cc734821",
  "IdentityPoolName": "example",
  "AllowUnauthenticatedIdentities": false,
  "IdentityPoolTags": {},
  "IdentityPoolArn": "arn:aws:cognito-identity:us-east-1:000000000000:identitypool/us-east-1:60e11652-424d-46b3-a372-5061cc734821",
  "CreationDate": 1795717675.602676,
  "LastModifiedDate": 1795717675.602676
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#2-createidentitypool) for a successful call.

### DeleteIdentities

Deletes identities from an identity pool.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.DeleteIdentities
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 36
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityIdsToDelete": ["example"]}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: f773043b-3c83-4a42-8c6b-6c8a66aaa065
x-amzn-errortype: ValidationException:
content-length: 349
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value '[example]' at 'identityIdsToDelete' failed to satisfy constraint: Member must satisfy constraint: [Member must have length less than or equal to 55, Member must have length greater than or equal to 1, Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+]"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteIdentityPool

Deletes an identity pool.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.DeleteIdentityPool
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 29
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityPoolId": "example"}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: b80196b6-d097-4d5f-afac-aad235907684
x-amzn-errortype: ValidationException:
content-length: 206
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#7-deleteidentitypool) for a successful call.

### DescribeIdentity

Returns metadata related to the given identity, including when the identity was created and any associated linked logins.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.DescribeIdentity
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 25
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityId": "example"}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: e0a5d951-3b7c-452d-9d0f-ca13b996f0dd
x-amzn-errortype: ValidationException:
content-length: 202
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeIdentityPool

Gets details about a particular identity pool, including the pool name, ID description, creation date, and current number of users.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.DescribeIdentityPool
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 29
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityPoolId": "example"}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 9b4404b8-fe74-41f8-8d10-8a6eb973152f
x-amzn-errortype: ValidationException:
content-length: 206
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#6-describeidentitypool) for a successful call.

### GetCredentialsForIdentity

Returns credentials for the provided identity ID.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.GetCredentialsForIdentity
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 25
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityId": "example"}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 80a52ca1-3062-49d2-8920-f83a8770b4ac
x-amzn-errortype: ValidationException:
content-length: 202
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#5-getcredentialsforidentity) for a successful call.

### GetId

Generates (or retrieves) IdentityID.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.GetId
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 29
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityPoolId": "example"}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 62b0036e-ce2f-4eac-a50c-de806f1d8ef8
x-amzn-errortype: ValidationException:
content-length: 206
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#4-getid) for a successful call.

### GetIdentityPoolRoles

Gets the roles for an identity pool.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.GetIdentityPoolRoles
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 29
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityPoolId": "example"}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 91a38c08-5c2a-4bb8-96ad-054b15e930fa
x-amzn-errortype: ValidationException:
content-length: 206
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetOpenIdToken

Gets an OpenID token, using a known Cognito ID.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.GetOpenIdToken
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 25
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityId": "example"}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 81c98e5a-a6e8-4c57-bd49-2d79206a650d
x-amzn-errortype: ValidationException:
content-length: 202
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetOpenIdTokenForDeveloperIdentity

Registers (or retrieves) a Cognito IdentityId and an OpenID Connect token for a user authenticated by your backend authentication process.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.GetOpenIdTokenForDeveloperIdentity
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 58
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityPoolId": "example","Logins": {"key": "example"}}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: a3aaeace-1798-4190-bf60-d674df70be6e
x-amzn-errortype: ValidationException:
content-length: 206
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetPrincipalTagAttributeMap

Use GetPrincipalTagAttributeMap to list all mappings between PrincipalTags and user attributes.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.GetPrincipalTagAttributeMap
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 63
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityPoolId": "example","IdentityProviderName": "example"}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 7621f80f-9799-4eb2-8f35-f2068b3404a9
x-amzn-errortype: ValidationException:
content-length: 206
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListIdentities

Lists the identities in an identity pool.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.ListIdentities
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityPoolId": "example","MaxResults": 1}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 7fda638a-96db-4d82-bb06-b1c25ea0e6c1
x-amzn-errortype: ValidationException:
content-length: 206
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListIdentityPools

Lists all of the Cognito identity pools registered for your account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.ListIdentityPools
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 17
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"MaxResults": 1}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 5c5fb56e-574b-42cf-becf-023eb371d5e2
content-length: 320
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "IdentityPools": [
    {
      "IdentityPoolId": "us-east-1:60e11652-424d-46b3-a372-5061cc734821",
      "IdentityPoolName": "example",
      "IdentityPoolArn": "arn:aws:cognito-identity:us-east-1:000000000000:identitypool/us-east-1:60e11652-424d-46b3-a372-5061cc734821",
      "CreationDate": 1795717675.602676,
      "LastModifiedDate": 1795717675.602676
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTagsForResource

Lists the tags that are assigned to an Amazon Cognito identity pool.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.ListTagsForResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 81
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"ResourceArn": "arn:aws:cognitoidentity:us-east-1:000000000000:example/example"}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 173d64dd-10c9-4cf2-b687-507413f0671a
x-amzn-errortype: InvalidParameterException:
content-length: 102
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "InvalidParameterException",
  "message": "ARN in SecureServiceAuthStrategy failed validation:"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### LookupDeveloperIdentity

Retrieves the IdentityID associated with a DeveloperUserIdentifier or the list of DeveloperUserIdentifier values associated with an IdentityId for an existing identity.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.LookupDeveloperIdentity
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 29
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityPoolId": "example"}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: e6384972-f23e-4940-81a9-9fe54898c01c
x-amzn-errortype: ValidationException:
content-length: 206
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### MergeDeveloperIdentities

Merges two users having different IdentityId s, existing in the same identity pool, and identified by the same developer provider.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.MergeDeveloperIdentities
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 137
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"SourceUserIdentifier": "example","DestinationUserIdentifier": "example","DeveloperProviderName": "example","IdentityPoolId": "example"}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: f29e5808-973f-4d94-83cc-986bafed69f8
x-amzn-errortype: ValidationException:
content-length: 206
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### SetIdentityPoolRoles

Sets the roles for an identity pool.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.SetIdentityPoolRoles
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityPoolId": "example","Roles": {"key": "example0000000000000"}}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 70b36f5f-062c-4946-9abe-7637c52921fc
x-amzn-errortype: ValidationException:
content-length: 206
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-setidentitypoolroles) for a successful call.

### SetPrincipalTagAttributeMap

You can use this operation to use default (username and clientID) attribute or custom attribute mappings.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.SetPrincipalTagAttributeMap
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 63
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityPoolId": "example","IdentityProviderName": "example"}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 93e13c05-140b-4424-9bbc-2025a5289e9b
x-amzn-errortype: ValidationException:
content-length: 206
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### TagResource

Assigns a set of tags to the specified Amazon Cognito identity pool.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.TagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 108
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"ResourceArn": "arn:aws:cognitoidentity:us-east-1:000000000000:example/example","Tags": {"key": "example"}}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 3336f2ef-d901-4b16-ad17-64885a279798
x-amzn-errortype: InvalidParameterException:
content-length: 102
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "InvalidParameterException",
  "message": "ARN in SecureServiceAuthStrategy failed validation:"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UnlinkDeveloperIdentity

Unlinks a DeveloperUserIdentifier from an existing identity.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.UnlinkDeveloperIdentity
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 125
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityId": "example","IdentityPoolId": "example","DeveloperProviderName": "example","DeveloperUserIdentifier": "example"}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 06337ca4-8edb-41df-a0e0-94461bc52624
x-amzn-errortype: ValidationException:
content-length: 337
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'identityId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+; Value 'example' at 'identityPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UnlinkIdentity

Unlinks a federated identity from an existing account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.UnlinkIdentity
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 84
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityId": "example","Logins": {"key": "example"},"LoginsToRemove": ["example"]}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: ed7a9d48-848d-4286-850f-891117e31687
x-amzn-errortype: ValidationException:
content-length: 202
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UntagResource

Removes the specified tags from the specified Amazon Cognito identity pool.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.UntagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 104
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"ResourceArn": "arn:aws:cognitoidentity:us-east-1:000000000000:example/example","TagKeys": ["example"]}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: e93e5b49-3cc2-413f-90dc-8fbb16151427
x-amzn-errortype: InvalidParameterException:
content-length: 102
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "InvalidParameterException",
  "message": "ARN in SecureServiceAuthStrategy failed validation:"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateIdentityPool

Updates the configuration of an identity pool.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Target: AWSCognitoIdentityService.UpdateIdentityPool
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 99
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{"IdentityPoolId": "example","IdentityPoolName": "example","AllowUnauthenticatedIdentities": false}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 2232ddaf-1520-4cb2-bc17-9d5cf29411e0
x-amzn-errortype: ValidationException:
content-length: 206
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+:[0-9a-f-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/cognito-identity/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon Cognito Identity Pools answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `message`, plus the `x-amzn-errortype` header.

### Other services involved

The walkthrough also called [AWS Identity and Access Management](iam.md). They are separate services behind the same endpoint, sharing one account and one in-memory engine, so a resource created through one is immediately visible to the other.
