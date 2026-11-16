# Amazon Cognito Identity Pools

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: AWSCognitoIdentityService.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `Cognito Identity`
- **sigv4 signing name:** `cognito-identity`
- **Endpoint prefix:** `cognito-identity`
- **ARN namespace:** `cognito-identity`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `cognito-identity`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed.

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
X-Amz-Date: 20261116T124405Z
Content-Length: 347
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateRole
&Version=2010-05-08
&RoleName=docs-demo-guest
&AssumeRolePolicyDocument=%7B%22Version%22%3A%20%222012-10-17%22%2C%20%22Statement%22%3A%20%5B%7B%22Effect%22%3A%20%22Allow%22%2C%20%22Principal%22%3A%20%7B%22Federated%22%3A%20%22cognito-identity.amazonaws.com%22%7D%2C%20%22Action%22%3A%20%22sts%3AAssumeRoleWithWebIdentity%22%7D%5D%7D
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 812
date: Mon, 16 Nov 2026 12:44:05 GMT

<CreateRoleResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateRoleResult>
    <Role xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <RoleName>docs-demo-guest</RoleName>
      <RoleId>AROAKWCHDRLOD744GNFOK</RoleId>
      <Arn>arn:aws:iam::000000000000:role/docs-demo-guest</Arn>
      <CreateDate>2026-11-16T12:44:05.971Z</CreateDate>
      <AssumeRolePolicyDocument>%7B%22Version%22%3A%222012-10-17%22%2C%22Statement%22%3A%5B%7B%22Effect%22%3A%22Allow%22%2C%22Principal%22%3A%7B%22Federated%22%3A%22cognito-identity.amazonaws.com%22%7D%2C%22Action%22%3A%22sts%3AAssumeRoleWithWebIdentity%22%7D%5D%7D</AssumeRolePolicyDocument>
      <MaxSessionDuration>3600</MaxSessionDuration>
    </Role>
  </CreateRoleResult>
  <ResponseMetadata>
    <RequestId>11a8ed74-da18-4151-ab25-5cd431d6bc94</RequestId>
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
X-Amz-Date: 20261116T124405Z
Content-Length: 78
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityPoolName": "docs_demo_pool",
  "AllowUnauthenticatedIdentities": true
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 71caf49c-9a46-4ccd-8a5d-98206b47e733
content-length: 363
date: Mon, 16 Nov 2026 12:44:05 GMT

{
  "IdentityPoolId": "us-east-1:e3f5e833-1d17-4ab9-ba9b-04f9478b9d44",
  "IdentityPoolName": "docs_demo_pool",
  "AllowUnauthenticatedIdentities": true,
  "IdentityPoolTags": {},
  "IdentityPoolArn": "arn:aws:cognito-identity:us-east-1:000000000000:identitypool/us-east-1:e3f5e833-1d17-4ab9-ba9b-04f9478b9d44",
  "CreationDate": 1794833046.0133097,
  "LastModifiedDate": 1794833046.0133097
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
X-Amz-Date: 20261116T124406Z
Content-Length: 148
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityPoolId": "us-east-1:e3f5e833-1d17-4ab9-ba9b-04f9478b9d44",
  "Roles": {
    "unauthenticated": "arn:aws:iam::000000000000:role/docs-demo-guest"
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 221efdbf-997f-4cd7-93d0-d9347cf9d351
content-length: 2
date: Mon, 16 Nov 2026 12:44:06 GMT

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
X-Amz-Date: 20261116T124406Z
Content-Length: 97
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccountId": "000000000000",
  "IdentityPoolId": "us-east-1:e3f5e833-1d17-4ab9-ba9b-04f9478b9d44"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 7e27312f-cc90-42ec-a10f-3aefe34ea776
content-length: 63
date: Mon, 16 Nov 2026 12:44:06 GMT

{
  "IdentityId": "us-east-1:ce228140-591f-46d1-93bf-c147540c61b0"
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
X-Amz-Date: 20261116T124406Z
Content-Length: 64
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityId": "us-east-1:ce228140-591f-46d1-93bf-c147540c61b0"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: b4533331-bae3-43bb-868c-ae320371d9ed
content-length: 935
date: Mon, 16 Nov 2026 12:44:06 GMT

{
  "IdentityId": "us-east-1:ce228140-591f-46d1-93bf-c147540c61b0",
  "Credentials": {
    "AccessKeyId": "EMULATORASIPK4R4B2JS",
    "SecretKey": "<redacted>",
    "SessionToken": "<redacted>",
    "Expiration": 1794836646.0957317
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
X-Amz-Date: 20261116T124406Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityPoolId": "us-east-1:e3f5e833-1d17-4ab9-ba9b-04f9478b9d44"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: b7b52fb4-9af0-40e7-b2fd-1924b4996988
content-length: 363
date: Mon, 16 Nov 2026 12:44:06 GMT

{
  "IdentityPoolId": "us-east-1:e3f5e833-1d17-4ab9-ba9b-04f9478b9d44",
  "IdentityPoolName": "docs_demo_pool",
  "AllowUnauthenticatedIdentities": true,
  "IdentityPoolTags": {},
  "IdentityPoolArn": "arn:aws:cognito-identity:us-east-1:000000000000:identitypool/us-east-1:e3f5e833-1d17-4ab9-ba9b-04f9478b9d44",
  "CreationDate": 1794833046.0133097,
  "LastModifiedDate": 1794833046.0133097
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
X-Amz-Date: 20261116T124406Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityPoolId": "us-east-1:e3f5e833-1d17-4ab9-ba9b-04f9478b9d44"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 43ee0ed6-770e-4356-91d1-f575f1b79aa8
content-length: 2
date: Mon, 16 Nov 2026 12:44:06 GMT

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
X-Amz-Date: 20261116T124406Z
Content-Length: 97
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-identity/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccountId": "000000000000",
  "IdentityPoolId": "us-east-1:e3f5e833-1d17-4ab9-ba9b-04f9478b9d44"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: f11df820-3322-4941-adc1-bdfd3c280ce3
x-amzn-errortype: ResourceNotFoundException:
content-length: 123
date: Mon, 16 Nov 2026 12:44:06 GMT

{
  "__type": "ResourceNotFoundException",
  "message": "IdentityPool 'us-east-1:e3f5e833-1d17-4ab9-ba9b-04f9478b9d44' not found."
}
```

### 9. AWS Identity and Access Management: DeleteRole

Clean up the role.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124406Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteRole
&Version=2010-05-08
&RoleName=docs-demo-guest
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 224
date: Mon, 16 Nov 2026 12:44:06 GMT

<DeleteRoleResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteRoleResult/>
  <ResponseMetadata>
    <RequestId>abe494a6-0745-4664-b843-72e873600b9e</RequestId>
  </ResponseMetadata>
</DeleteRoleResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 23 operations, 2 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### CreateIdentityPool

Creates a new identity pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityService.CreateIdentityPool

{
  "IdentityPoolName": "example",
  "AllowUnauthenticatedIdentities": false
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "IdentityPoolId": "us-east-1:fa1b6e18-724e-44f7-ad64-86e6fb4909b6",
  "IdentityPoolName": "example",
  "AllowUnauthenticatedIdentities": false,
  "IdentityPoolTags": {},
  "IdentityPoolArn": "arn:aws:cognito-identity:us-east-1:000000000000:identitypool/us-east-1:fa1b6e18-724e-44f7-ad64-86e6fb4909b6",
  "CreationDate": 1794833126.7345343,
  "LastModifiedDate": 1794833126.7345343
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#2-createidentitypool) for a successful call.

### DeleteIdentities

Deletes identities from an identity pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityService.DeleteIdentities

{
  "IdentityIdsToDelete": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.DeleteIdentityPool

{
  "IdentityPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.DescribeIdentity

{
  "IdentityId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.DescribeIdentityPool

{
  "IdentityPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.GetCredentialsForIdentity

{
  "IdentityId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.GetId

{
  "IdentityPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.GetIdentityPoolRoles

{
  "IdentityPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.GetOpenIdToken

{
  "IdentityId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.GetOpenIdTokenForDeveloperIdentity

{
  "IdentityPoolId": "example",
  "Logins": {
    "key": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.GetPrincipalTagAttributeMap

{
  "IdentityPoolId": "example",
  "IdentityProviderName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.ListIdentities

{
  "IdentityPoolId": "example",
  "MaxResults": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.ListIdentityPools

{
  "MaxResults": 1
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "IdentityPools": [
    {
      "IdentityPoolId": "us-east-1:fa1b6e18-724e-44f7-ad64-86e6fb4909b6",
      "IdentityPoolName": "example",
      "IdentityPoolArn": "arn:aws:cognito-identity:us-east-1:000000000000:identitypool/us-east-1:fa1b6e18-724e-44f7-ad64-86e6fb4909b6",
      "CreationDate": 1794833126.7345343,
      "LastModifiedDate": 1794833126.7345343
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTagsForResource

Lists the tags that are assigned to an Amazon Cognito identity pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityService.ListTagsForResource

{
  "ResourceArn": "arn:aws:cognitoidentity:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

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
X-Amz-Target: AWSCognitoIdentityService.LookupDeveloperIdentity

{
  "IdentityPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.MergeDeveloperIdentities

{
  "SourceUserIdentifier": "example",
  "DestinationUserIdentifier": "example",
  "DeveloperProviderName": "example",
  "IdentityPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.SetIdentityPoolRoles

{
  "IdentityPoolId": "example",
  "Roles": {
    "key": "example0000000000000"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.SetPrincipalTagAttributeMap

{
  "IdentityPoolId": "example",
  "IdentityProviderName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.TagResource

{
  "ResourceArn": "arn:aws:cognitoidentity:us-east-1:000000000000:example/example",
  "Tags": {
    "key": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

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
X-Amz-Target: AWSCognitoIdentityService.UnlinkDeveloperIdentity

{
  "IdentityId": "example",
  "IdentityPoolId": "example",
  "DeveloperProviderName": "example",
  "DeveloperUserIdentifier": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.UnlinkIdentity

{
  "IdentityId": "example",
  "Logins": {
    "key": "example"
  },
  "LoginsToRemove": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
X-Amz-Target: AWSCognitoIdentityService.UntagResource

{
  "ResourceArn": "arn:aws:cognitoidentity:us-east-1:000000000000:example/example",
  "TagKeys": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

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
X-Amz-Target: AWSCognitoIdentityService.UpdateIdentityPool

{
  "IdentityPoolId": "example",
  "IdentityPoolName": "example",
  "AllowUnauthenticatedIdentities": false
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ValidationException:

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
