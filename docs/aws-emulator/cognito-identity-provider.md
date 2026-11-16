# Amazon Cognito User Pools

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: AWSCognitoIdentityProviderService.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `Cognito Identity Provider`
- **sigv4 signing name:** `cognito-idp`
- **Endpoint prefix:** `cognito-idp`
- **ARN namespace:** `cognito-idp`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `cognito-idp`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListUserPools`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:cognito-idp" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.1' \
  -H 'X-Amz-Target: AWSCognitoIdentityProviderService.ListUserPools' \
  --data-binary '{"MaxResults": 10}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create a user pool and an app client, create a user with a temporary password, sign in, set a permanent password, and read the user with the access token. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateUserPool

Users of this pool sign in with their email address.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateUserPool
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124456Z
Content-Length: 256
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "PoolName": "docs-demo-users",
  "Policies": {
    "PasswordPolicy": {
      "MinimumLength": 8,
      "RequireUppercase": true,
      "RequireLowercase": true,
      "RequireNumbers": true,
      "RequireSymbols": false
    }
  },
  "AutoVerifiedAttributes": [
    "email"
  ],
  "UsernameAttributes": [
    "email"
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: aea3dab9-874d-4554-8693-ab0463687ddd
content-length: 4651
date: Mon, 16 Nov 2026 12:44:56 GMT

{
  "UserPool": {
    "Id": "us-east-1_3fc6a71aa",
    "Name": "docs-demo-users",
    "Policies": {
      "PasswordPolicy": {
        "MinimumLength": 8,
        "RequireUppercase": true,
        "RequireLowercase": true,
        "RequireNumbers": true,
        "RequireSymbols": false,
        "TemporaryPasswordValidityDays": 7
      },
      "SignInPolicy": {
        "AllowedFirstAuthFactors": [
          "PASSWORD"
        ]
      }
    },
    "DeletionProtection": "INACTIVE",
    "LambdaConfig": {},
    "LastModifiedDate": 1794833096.6118102,
    "CreationDate": 1794833096.6118102,
    "SchemaAttributes": [
      {
        "Name": "sub",
        "AttributeDataType": "String",
        "DeveloperOnlyAttribute": false,
        "Mutable": false,
        "Required": true,
        "StringAttributeConstraints": {
          "MinLength": "1",
          "MaxLength": "2048"
        }
      },
      {
        "Name": "name",
        "AttributeDataType": "String",
        "DeveloperOnlyAttribute": false,
        "Mutable": true,
        "Required": false,
        "StringAttributeConstraints": {
          "MinLength": "0",
          "MaxLength": "2048"
        }
      },
      {
        "Name": "given_name",
        "AttributeDataType": "String",
        "DeveloperOnlyAttribute": false,
        "Mutable": true,
        "Required": false,
        "StringAttributeConstraints": {
          "MinLength": "0",
          "MaxLength": "2048"
        }
      },
      {
        "Name": "family_name",
        "AttributeDataType": "String",
… (220 more lines)
```

### 2. CreateUserPoolClient

An app client is what a front end signs in through; this one allows plain username and password sign-in.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateUserPoolClient
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124456Z
Content-Length: 147
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "us-east-1_3fc6a71aa",
  "ClientName": "docs-demo-web",
  "ExplicitAuthFlows": [
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH"
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 8eaf8a72-0110-49c0-809a-286a704b0022
content-length: 504
date: Mon, 16 Nov 2026 12:44:56 GMT

{
  "UserPoolClient": {
    "UserPoolId": "us-east-1_3fc6a71aa",
    "ClientName": "docs-demo-web",
    "ClientId": "50ade59e01f14ae6b7942f13ed",
    "LastModifiedDate": 1794833096.9576101,
    "CreationDate": 1794833096.9576101,
    "RefreshTokenValidity": 30,
    "TokenValidityUnits": {},
    "ExplicitAuthFlows": [
      "ALLOW_USER_PASSWORD_AUTH",
      "ALLOW_REFRESH_TOKEN_AUTH"
    ],
    "AllowedOAuthFlowsUserPoolClient": false,
    "PreventUserExistenceErrors": "LEGACY",
    "EnableTokenRevocation": true,
    "EnablePropagateAdditionalUserContextData": false,
    "AuthSessionValidity": 3
  }
}
```

### 3. AdminCreateUser

An administrator creates the user with a temporary password (redacted); `SUPPRESS` skips the invitation email. The user starts in `FORCE_CHANGE_PASSWORD`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminCreateUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124456Z
Content-Length: 244
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "us-east-1_3fc6a71aa",
  "Username": "ada@example.com",
  "UserAttributes": [
    {
      "Name": "email",
      "Value": "ada@example.com"
    },
    {
      "Name": "email_verified",
      "Value": "true"
    }
  ],
  "TemporaryPassword": "<redacted>",
  "MessageAction": "SUPPRESS"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 5950ffc7-1393-41ba-9278-11f34c2fb5c2
content-length: 355
date: Mon, 16 Nov 2026 12:44:56 GMT

{
  "User": {
    "Username": "aeff9250-f471-4e62-b632-ddcd8a504329",
    "Attributes": [
      {
        "Name": "email",
        "Value": "ada@example.com"
      },
      {
        "Name": "email_verified",
        "Value": "true"
      },
      {
        "Name": "sub",
        "Value": "aeff9250-f471-4e62-b632-ddcd8a504329"
      }
    ],
    "UserCreateDate": 1794833096.9666524,
    "UserLastModifiedDate": 1794833096.9666524,
    "Enabled": true,
    "UserStatus": "FORCE_CHANGE_PASSWORD"
  }
}
```

### 4. InitiateAuth

Signing in with the temporary password does not return tokens yet: it returns the `NEW_PASSWORD_REQUIRED` challenge and a `Session` (redacted) to answer it with.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.InitiateAuth
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124456Z
Content-Length: 153
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AuthFlow": "USER_PASSWORD_AUTH",
  "AuthParameters": {
    "USERNAME": "ada@example.com",
    "PASSWORD": "<redacted>"
  },
  "ClientId": "50ade59e01f14ae6b7942f13ed"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 36c80336-e323-49c7-82e3-50c284928986
content-length: 304
date: Mon, 16 Nov 2026 12:44:56 GMT

{
  "ChallengeName": "NEW_PASSWORD_REQUIRED",
  "Session": "<redacted>",
  "ChallengeParameters": {
    "USER_ID_FOR_SRP": "aeff9250-f471-4e62-b632-ddcd8a504329",
    "requiredAttributes": "[]",
    "userAttributes": "{\"email\":\"ada@example.com\",\"email_verified\":\"true\"}"
  }
}
```

### 5. RespondToAuthChallenge

In AWS, answering the challenge with a new password completes sign-in. This emulator build answers `501 NotImplemented`, so the next step sets the password as an administrator instead.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.RespondToAuthChallenge
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124456Z
Content-Length: 253
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClientId": "50ade59e01f14ae6b7942f13ed",
  "ChallengeName": "NEW_PASSWORD_REQUIRED",
  "Session": "<redacted>",
  "ChallengeResponses": {
    "USERNAME": "ada@example.com",
    "NEW_PASSWORD": "<redacted>"
  }
}
```

**Response**

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotImplemented:
x-amzn-requestid: 04adb806-2e40-4f02-a486-4056dde3676b
content-length: 164
date: Mon, 16 Nov 2026 12:44:56 GMT

{
  "__type": "NotImplemented",
  "message": "operation RespondToAuthChallenge with ChallengeName=NEW_PASSWORD_REQUIRED is scaffolded but not implemented in this emulator"
}
```

### 6. AdminSetUserPassword

A permanent password (redacted) moves the user from `FORCE_CHANGE_PASSWORD` to `CONFIRMED`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminSetUserPassword
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124456Z
Content-Length: 118
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "us-east-1_3fc6a71aa",
  "Username": "ada@example.com",
  "Password": "<redacted>",
  "Permanent": true
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 617f364e-717e-4203-a9c5-23a6828e9f35
content-length: 2
date: Mon, 16 Nov 2026 12:44:56 GMT

{}
```

### 7. InitiateAuth

Signing in with the new password returns the ID, access and refresh tokens (all redacted).

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.InitiateAuth
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124456Z
Content-Length: 158
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AuthFlow": "USER_PASSWORD_AUTH",
  "AuthParameters": {
    "USERNAME": "ada@example.com",
    "PASSWORD": "<redacted>"
  },
  "ClientId": "50ade59e01f14ae6b7942f13ed"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 1dcada27-8e78-4cdf-b991-a4bf9e8932e5
content-length: 3562
date: Mon, 16 Nov 2026 12:44:56 GMT

{
  "ChallengeParameters": {},
  "AuthenticationResult": {
    "AccessToken": "<redacted>",
    "ExpiresIn": 3600,
    "TokenType": "Bearer",
    "RefreshToken": "<redacted>",
    "IdToken": "<redacted>"
  }
}
```

### 8. GetUser

The access token alone identifies the user; in AWS this call needs no SigV4 credentials.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124457Z
Content-Length: 1035
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 42455ac5-09bf-4306-9e54-9ec0aeab902c
content-length: 216
date: Mon, 16 Nov 2026 12:44:57 GMT

{
  "Username": "aeff9250-f471-4e62-b632-ddcd8a504329",
  "UserAttributes": [
    {
      "Name": "email",
      "Value": "ada@example.com"
    },
    {
      "Name": "email_verified",
      "Value": "true"
    },
    {
      "Name": "sub",
      "Value": "aeff9250-f471-4e62-b632-ddcd8a504329"
    }
  ]
}
```

### 9. InitiateAuth

A wrong password answers `NotAuthorizedException`, with the same message as a user that does not exist.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.InitiateAuth
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124457Z
Content-Length: 154
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AuthFlow": "USER_PASSWORD_AUTH",
  "AuthParameters": {
    "USERNAME": "ada@example.com",
    "PASSWORD": "<redacted>"
  },
  "ClientId": "50ade59e01f14ae6b7942f13ed"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: 809fa47c-a16a-47f1-a15e-b5560089ebe2
content-length: 79
date: Mon, 16 Nov 2026 12:44:57 GMT

{
  "__type": "NotAuthorizedException",
  "message": "Incorrect username or password."
}
```

### 10. AdminGetUser

The user is now `CONFIRMED`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminGetUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124457Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "us-east-1_3fc6a71aa",
  "Username": "ada@example.com"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 59d33495-5e58-45fd-a7f0-7b4857577ee2
content-length: 338
date: Mon, 16 Nov 2026 12:44:57 GMT

{
  "Username": "aeff9250-f471-4e62-b632-ddcd8a504329",
  "UserAttributes": [
    {
      "Name": "email",
      "Value": "ada@example.com"
    },
    {
      "Name": "email_verified",
      "Value": "true"
    },
    {
      "Name": "sub",
      "Value": "aeff9250-f471-4e62-b632-ddcd8a504329"
    }
  ],
  "UserCreateDate": 1794833096.9666524,
  "UserLastModifiedDate": 1794833096.9938064,
  "Enabled": true,
  "UserStatus": "CONFIRMED"
}
```

### 11. DeleteUserPool

Deleting the pool deletes its clients and users.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUserPool
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124457Z
Content-Length: 37
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "us-east-1_3fc6a71aa"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: abe4534b-81b4-46ea-8ff3-6919e006f9f0
content-length: 2
date: Mon, 16 Nov 2026 12:44:57 GMT

{}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 132 operations, 3 accepted the placeholder call, 1 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AddCustomAttributes

Adds additional user attributes to the user pool schema.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AddCustomAttributes

{
  "UserPoolId": "example",
  "CustomAttributes": [
    {}
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AddUserPoolClientSecret

Creates a new client secret for an existing confidential user pool app client.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AddUserPoolClientSecret

{
  "UserPoolId": "example",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminAddUserToGroup

Adds a user to a group.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminAddUserToGroup

{
  "UserPoolId": "example",
  "Username": "example",
  "GroupName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminConfirmSignUp

Confirms user sign-up as an administrator.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminConfirmSignUp

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminCreateUser

Creates a new user in the specified user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminCreateUser

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-admincreateuser) for a successful call.

### AdminDeleteSoftwareToken

Deletes a user's registered time-based one-time password (TOTP) multi-factor authentication (MFA) factor, also known as a software token.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminDeleteSoftwareToken

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminDeleteUser

Deletes a user profile in your user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminDeleteUser

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminDeleteUserAttributes

Deletes attribute values from a user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminDeleteUserAttributes

{
  "UserPoolId": "example",
  "Username": "example",
  "UserAttributeNames": [
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
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminDisableProviderForUser

Prevents the user from signing in with the specified external (SAML or social) identity provider (IdP).

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminDisableProviderForUser

{
  "UserPoolId": "example",
  "User": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "Missing required parameter ProviderName"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminDisableUser

Deactivates a user profile and revokes all access tokens for the user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminDisableUser

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminEnableUser

Activates sign-in for a user profile that previously had sign-in access disabled.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminEnableUser

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminForgetDevice

Forgets, or deletes, a remembered device from a user's profile.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminForgetDevice

{
  "UserPoolId": "example",
  "Username": "example",
  "DeviceKey": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "2 validation errors detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+; Value 'example' at 'deviceKey' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-f-]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminGetDevice

Given the device key, returns details for a user's device.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminGetDevice

{
  "DeviceKey": "example",
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "2 validation errors detected: Value 'example' at 'deviceKey' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-f-]+; Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminGetUser

Given a username, returns details about a user profile in a user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminGetUser

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation. See [the walkthrough](#10-admingetuser) for a successful call.

### AdminGetUserAuthFactors

Lists the authentication options for a user in a user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminGetUserAuthFactors

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminInitiateAuth

Starts sign-in for applications with a server-side component, for example a traditional web application.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminInitiateAuth

{
  "UserPoolId": "example",
  "ClientId": "example",
  "AuthFlow": "USER_SRP_AUTH"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminLinkProviderForUser

Links an existing user account in a user pool, or DestinationUser , to an identity from an external IdP, or SourceUser , based on a specified attribute name and value from the external IdP.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminLinkProviderForUser

{
  "UserPoolId": "example",
  "DestinationUser": {},
  "SourceUser": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "Missing required parameter ProviderName"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminListDevices

Lists a user's registered devices.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminListDevices

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminListGroupsForUser

Lists the groups that a user belongs to.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminListGroupsForUser

{
  "Username": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminListUserAuthEvents

Requests a history of user activity and any risks detected as part of Amazon Cognito threat protection.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminListUserAuthEvents

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminRemoveUserFromGroup

Given a username and a group name, removes them from the group.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminRemoveUserFromGroup

{
  "UserPoolId": "example",
  "Username": "example",
  "GroupName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminResetUserPassword

Begins the password reset process.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminResetUserPassword

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminRespondToAuthChallenge

Some API operations in a user pool generate a challenge, like a prompt for an MFA code, for device authentication that bypasses MFA, or for a custom authentication challenge.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminRespondToAuthChallenge

{
  "UserPoolId": "example",
  "ClientId": "example",
  "ChallengeName": "SMS_MFA"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminSetUserMFAPreference

Sets the user's multi-factor authentication (MFA) preference, including which MFA options are activated, and if any are preferred.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminSetUserMFAPreference

{
  "Username": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminSetUserPassword

Sets the specified user's password in a user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminSetUserPassword

{
  "UserPoolId": "example",
  "Username": "example",
  "Password": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation. See [the walkthrough](#6-adminsetuserpassword) for a successful call.

### AdminSetUserSettings

This action is no longer supported.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminSetUserSettings

{
  "UserPoolId": "example",
  "Username": "example",
  "MFAOptions": [
    {}
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminUpdateAuthEventFeedback

Provides the feedback for an authentication event generated by threat protection features.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminUpdateAuthEventFeedback

{
  "UserPoolId": "example",
  "Username": "example",
  "EventId": "example",
  "FeedbackValue": "Valid"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminUpdateDeviceStatus

Updates the status of a user's device so that it is marked as remembered or not remembered for the purpose of device authentication.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminUpdateDeviceStatus

{
  "UserPoolId": "example",
  "Username": "example",
  "DeviceKey": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "2 validation errors detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+; Value 'example' at 'deviceKey' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-f-]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminUpdateUserAttributes

Updates the specified user's attributes.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminUpdateUserAttributes

{
  "UserPoolId": "example",
  "Username": "example",
  "UserAttributes": [
    {
      "Name": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AdminUserGlobalSignOut

Invalidates the identity, access, and refresh tokens that Amazon Cognito issued to a user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminUserGlobalSignOut

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### AssociateSoftwareToken

Begins setup of time-based one-time password (TOTP) multi-factor authentication (MFA) for a user, with a unique private key that Amazon Cognito generates and returns in the API response.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AssociateSoftwareToken

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "Invalid request. You can provide either an access token or a session ID in the request."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ChangePassword

Changes the password for the currently signed-in user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ChangePassword

{
  "ProposedPassword": "example",
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### CompleteWebAuthnRegistration

Completes registration of a passkey authenticator for the currently signed-in user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CompleteWebAuthnRegistration

{
  "AccessToken": "<redacted>",
  "Credential": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### ConfirmDevice

Confirms a device that a user wants to remember.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ConfirmDevice

{
  "AccessToken": "<redacted>",
  "DeviceKey": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'deviceKey' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-f-]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ConfirmForgotPassword

This public API operation accepts a confirmation code that Amazon Cognito sent to a user and accepts a new password for that user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ConfirmForgotPassword

{
  "ClientId": "example",
  "Username": "example",
  "ConfirmationCode": "example",
  "Password": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ResourceNotFoundException:

{
  "__type": "ResourceNotFoundException",
  "message": "User pool client example does not exist."
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ConfirmSignUp

Confirms the account of a new user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ConfirmSignUp

{
  "ClientId": "example",
  "Username": "example",
  "ConfirmationCode": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ResourceNotFoundException:

{
  "__type": "ResourceNotFoundException",
  "message": "User pool client example does not exist."
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateGroup

Creates a new group in the specified user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateGroup

{
  "GroupName": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateIdentityProvider

Adds a configuration and trust relationship between a third-party identity provider (IdP) and a user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateIdentityProvider

{
  "UserPoolId": "example",
  "ProviderName": "example",
  "ProviderType": "SAML",
  "ProviderDetails": {
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
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateManagedLoginBranding

Creates a new set of branding settings for a user pool style and associates it with an app client.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateManagedLoginBranding

{
  "UserPoolId": "example",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateResourceServer

Creates a new OAuth2.0 resource server and defines custom scopes within it.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateResourceServer

{
  "UserPoolId": "example",
  "Identifier": "example",
  "Name": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateTerms

Creates terms documents for the requested app client.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateTerms

{
  "UserPoolId": "example",
  "ClientId": "example",
  "TermsName": "example",
  "TermsSource": "LINK",
  "Enforcement": "NONE"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "2 validation errors detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+; Value 'example' at 'termsName' failed to satisfy constraint: Member must satisfy regular expression pattern: ^(terms-of-use|privacy-policy)$"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateUserImportJob

Creates a user import job.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateUserImportJob

{
  "JobName": "example",
  "UserPoolId": "example",
  "CloudWatchLogsRoleArn": "arn:aws:cognitoidentityprovider:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateUserPool

Creates a new Amazon Cognito user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateUserPool

{
  "PoolName": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "UserPool": {
    "Id": "us-east-1_84445d807",
    "Name": "example",
    "Policies": {
      "PasswordPolicy": {
        "MinimumLength": 8,
        "RequireUppercase": true,
        "RequireLowercase": true,
        "RequireNumbers": true,
        "RequireSymbols": true,
        "TemporaryPasswordValidityDays": 7
      },
      "SignInPolicy": {
        "AllowedFirstAuthFactors": [
          "PASSWORD"
        ]
      }
    },
    "DeletionProtection": "INACTIVE",
… (254 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#1-createuserpool) for a successful call.

### CreateUserPoolClient

Creates an app client in a user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateUserPoolClient

{
  "UserPoolId": "example",
  "ClientName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-createuserpoolclient) for a successful call.

### CreateUserPoolDomain

A user pool domain hosts managed login, an authorization server and web server for authentication in your application.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateUserPoolDomain

{
  "Domain": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateUserPoolReplica

Creates a replica of an existing user pool in a specified Amazon Web Services Region.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateUserPoolReplica

{
  "UserPoolId": "example",
  "RegionName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteGroup

Deletes a group from the specified user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteGroup

{
  "GroupName": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteIdentityProvider

Deletes a user pool identity provider (IdP).

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteIdentityProvider

{
  "UserPoolId": "example",
  "ProviderName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteManagedLoginBranding

Deletes a managed login branding style.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteManagedLoginBranding

{
  "ManagedLoginBrandingId": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "2 validation errors detected: Value 'example' at 'managedLoginBrandingId' failed to satisfy constraint: Member must satisfy regular expression pattern: [0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[4][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}; Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteResourceServer

Deletes a resource server.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteResourceServer

{
  "UserPoolId": "example",
  "Identifier": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteTerms

Deletes the terms documents with the requested ID from your app client.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteTerms

{
  "TermsId": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "2 validation errors detected: Value 'example' at 'termsId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[4][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$; Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteUser

Deletes the profile of the currently signed-in user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUser

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### DeleteUserAttributes

Deletes attributes from the currently signed-in user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUserAttributes

{
  "UserAttributeNames": [
    "example"
  ],
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### DeleteUserPool

Deletes a user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUserPool

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation. See [the walkthrough](#11-deleteuserpool) for a successful call.

### DeleteUserPoolClient

Deletes a user pool app client.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUserPoolClient

{
  "UserPoolId": "example",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteUserPoolClientSecret

Deletes a specific client secret from a user pool app client.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUserPoolClientSecret

{
  "UserPoolId": "example",
  "ClientId": "example",
  "ClientSecretId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteUserPoolDomain

Given a user pool ID and domain identifier, deletes a user pool domain.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUserPoolDomain

{
  "Domain": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteUserPoolReplica

Deletes a secondary replica user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUserPoolReplica

{
  "UserPoolId": "example",
  "RegionName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteWebAuthnCredential

Deletes a registered passkey, or WebAuthn, authenticator for the currently signed-in user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteWebAuthnCredential

{
  "AccessToken": "<redacted>",
  "CredentialId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### DescribeIdentityProvider

Given a user pool ID and identity provider (IdP) name, returns details about the IdP.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeIdentityProvider

{
  "UserPoolId": "example",
  "ProviderName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeManagedLoginBranding

Given the ID of a managed login branding style, returns detailed information about the style.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeManagedLoginBranding

{
  "UserPoolId": "example",
  "ManagedLoginBrandingId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "2 validation errors detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+; Value 'example' at 'managedLoginBrandingId' failed to satisfy constraint: Member must satisfy regular expression pattern: [0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[4][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeManagedLoginBrandingByClient

Given the ID of a user pool app client, returns detailed information about the style assigned to the app client.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeManagedLoginBrandingByClient

{
  "UserPoolId": "example",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeResourceServer

Describes a resource server.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeResourceServer

{
  "UserPoolId": "example",
  "Identifier": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeRiskConfiguration

Given an app client or user pool ID where threat protection is configured, describes the risk configuration.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeRiskConfiguration

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeTerms

Returns details for the requested terms documents ID.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeTerms

{
  "TermsId": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "2 validation errors detected: Value 'example' at 'termsId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[4][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$; Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeTermsByClient

Returns details for the terms documents that are associated with an app client, identified by the app client ID, user pool ID, and terms name.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeTermsByClient

{
  "ClientId": "example",
  "UserPoolId": "example",
  "TermsName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "2 validation errors detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+; Value 'example' at 'termsName' failed to satisfy constraint: Member must satisfy regular expression pattern: ^(terms-of-use|privacy-policy)$"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeUserImportJob

Describes a user import job.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeUserImportJob

{
  "UserPoolId": "example",
  "JobId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "2 validation errors detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+; Value 'example' at 'jobId' failed to satisfy constraint: Member must satisfy regular expression pattern: import-[0-9a-zA-Z-]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeUserPool

Given a user pool ID, returns configuration information.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeUserPool

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeUserPoolClient

Given an app client ID, returns configuration information.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeUserPoolClient

{
  "UserPoolId": "example",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeUserPoolDomain

Given a user pool domain name, returns information about the domain configuration.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeUserPoolDomain

{
  "Domain": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "DomainDescription": {}
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ForgetDevice

Given a device key, deletes a remembered device as the currently signed-in user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ForgetDevice

{
  "DeviceKey": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'deviceKey' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-f-]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ForgotPassword

Sends a password-reset confirmation code to the email address or phone number of the requested username.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ForgotPassword

{
  "ClientId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ResourceNotFoundException:

{
  "__type": "ResourceNotFoundException",
  "message": "User pool client example does not exist."
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetCSVHeader

Given a user pool ID, generates a comma-separated value (CSV) list populated with available user attributes in the user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetCSVHeader

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### GetClientToken

Issues an access token for machine-to-machine (M2M) authorization.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetClientToken

{
  "ClientId": "example",
  "Secret": "example00000000000000000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ResourceNotFoundException:

{
  "__type": "ResourceNotFoundException",
  "message": "User pool client example does not exist."
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetDevice

Given a device key, returns information about a remembered device for the current user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetDevice

{
  "DeviceKey": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'deviceKey' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-f-]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### GetGroup

Given a user pool ID and a group name, returns information about the user group.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetGroup

{
  "GroupName": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### GetIdentityProviderByIdentifier

Given the identifier of an identity provider (IdP), for example examplecorp , returns information about the user pool configuration for that IdP.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetIdentityProviderByIdentifier

{
  "UserPoolId": "example",
  "IdpIdentifier": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### GetLogDeliveryConfiguration

Given a user pool ID, returns the logging configuration.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetLogDeliveryConfiguration

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### GetProvisionedLimit

Returns the current provisioned limit for a specific API category.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetProvisionedLimit

{
  "LimitDefinition": {
    "LimitClass": "API_CATEGORY",
    "Attributes": {
      "key": "example"
    }
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "The limit definition must include the attribute Category to identify the API_CATEGORY limit it means."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### GetSigningCertificate

Given a user pool ID, returns the signing certificate for SAML 2.0 federation.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetSigningCertificate

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### GetTokensFromRefreshToken

Given a refresh token, issues new ID, access, and optionally refresh tokens for the user who owns the submitted token.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetTokensFromRefreshToken

{
  "RefreshToken": "<redacted>",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ResourceNotFoundException:

{
  "__type": "ResourceNotFoundException",
  "message": "User pool client example does not exist."
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetUICustomization

Given a user pool ID or app client, returns information about classic hosted UI branding that you applied, if any.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetUICustomization

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### GetUser

Gets user attributes and and MFA settings for the currently signed-in user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetUser

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`. See [the walkthrough](#8-getuser) for a successful call.

### GetUserAttributeVerificationCode

Given an attribute name, sends a user attribute verification code for the specified attribute name to the currently signed-in user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetUserAttributeVerificationCode

{
  "AccessToken": "<redacted>",
  "AttributeName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### GetUserAuthFactors

Lists the authentication options for the currently signed-in user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetUserAuthFactors

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### GetUserPoolMfaConfig

Given a user pool ID, returns configuration for sign-in with WebAuthn authenticators and for multi-factor authentication (MFA).

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetUserPoolMfaConfig

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### GlobalSignOut

Invalidates the identity, access, and refresh tokens that Amazon Cognito issued to a user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GlobalSignOut

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### InitiateAuth

Declares an authentication flow and initiates sign-in for a user in the Amazon Cognito user directory.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.InitiateAuth

{
  "AuthFlow": "USER_SRP_AUTH",
  "ClientId": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotImplemented:

{
  "__type": "NotImplemented",
  "message": "operation InitiateAuth with AuthFlow=USER_SRP_AUTH is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation. See [the walkthrough](#4-initiateauth) for a successful call.

### ListDevices

Lists the devices that Amazon Cognito has registered to the currently signed-in user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListDevices

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### ListGroups

Given a user pool ID, returns user pool groups and their details.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListGroups

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ListIdentityProviders

Given a user pool ID, returns information about configured identity providers (IdPs).

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListIdentityProviders

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ListResourceServers

Given a user pool ID, returns all resource servers and their details.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListResourceServers

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ListTagsForResource

Lists the tags that are assigned to an Amazon Cognito user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListTagsForResource

{
  "ResourceArn": "arn:aws:cognitoidentityprovider:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "ResourceArn 'arn:aws:cognitoidentityprovider:us-east-1:000000000000:example/example' is not the ARN of an Amazon Cognito user pool."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ListTerms

Returns details about all terms documents for the requested user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListTerms

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ListUserImportJobs

Given a user pool ID, returns user import jobs and their details.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListUserImportJobs

{
  "UserPoolId": "example",
  "MaxResults": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ListUserPoolClientSecrets

Lists all client secrets associated with a user pool app client.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListUserPoolClientSecrets

{
  "UserPoolId": "example",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ListUserPoolClients

Given a user pool ID, lists app clients.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListUserPoolClients

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ListUserPoolReplicas

Lists all replicas for a user pool, including both primary and secondary replicas.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListUserPoolReplicas

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ListUserPools

Lists user pools and their details in the current Amazon Web Services account.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListUserPools

{
  "MaxResults": 1
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "UserPools": [
    {
      "Id": "us-east-1_84445d807",
      "Name": "example",
      "LambdaConfig": {},
      "LastModifiedDate": 1794833126.002344,
      "CreationDate": 1794833126.002344,
      "ReplicaRegions": [
        "us-east-1"
      ]
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListUsers

Given a user pool ID, returns a list of users and their basic details in a user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListUsers

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ListUsersInGroup

Given a user pool ID and a group name, returns a list of users in the group.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListUsersInGroup

{
  "UserPoolId": "example",
  "GroupName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ListWebAuthnCredentials

Generates a list of the currently signed-in user's registered passkey, or WebAuthn, credentials.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListWebAuthnCredentials

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### ResendConfirmationCode

Resends the code that confirms a new account for a user who has signed up in your user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ResendConfirmationCode

{
  "ClientId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ResourceNotFoundException:

{
  "__type": "ResourceNotFoundException",
  "message": "User pool client example does not exist."
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RespondToAuthChallenge

Some API operations in a user pool generate a challenge, like a prompt for an MFA code, for device authentication that bypasses MFA, or for a custom authentication challenge.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.RespondToAuthChallenge

{
  "ClientId": "example",
  "ChallengeName": "SMS_MFA"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "Missing required parameter Session"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### RevokeToken

Revokes all of the access tokens generated by, and at the same time as, the specified refresh token.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.RevokeToken

{
  "Token": "<redacted>",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: UnauthorizedException:

{
  "__type": "UnauthorizedException",
  "message": "Invalid client id."
}
```

`400` `UnauthorizedException`.

### SetLogDeliveryConfiguration

Sets up or modifies the logging configuration of a user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.SetLogDeliveryConfiguration

{
  "UserPoolId": "example",
  "LogConfigurations": [
    {
      "LogLevel": "ERROR",
      "EventSource": "userNotification"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### SetRiskConfiguration

Configures threat protection for a user pool or app client.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.SetRiskConfiguration

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### SetUICustomization

Configures UI branding settings for domains with the hosted UI (classic) branding version.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.SetUICustomization

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### SetUserMFAPreference

Set the user's multi-factor authentication (MFA) method preference, including which MFA factors are activated and if any are preferred.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.SetUserMFAPreference

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### SetUserPoolMfaConfig

Sets user pool multi-factor authentication (MFA) and passkey configuration.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.SetUserPoolMfaConfig

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### SetUserSettings

This action is no longer supported.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.SetUserSettings

{
  "AccessToken": "<redacted>",
  "MFAOptions": [
    {}
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### SignUp

Registers a user with an app client and requests a user name, password, and user attributes in the user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.SignUp

{
  "ClientId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ResourceNotFoundException:

{
  "__type": "ResourceNotFoundException",
  "message": "User pool client example does not exist."
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StartUserImportJob

Instructs your user pool to start importing users from a CSV file that contains their usernames and attributes.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.StartUserImportJob

{
  "UserPoolId": "example",
  "JobId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "2 validation errors detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+; Value 'example' at 'jobId' failed to satisfy constraint: Member must satisfy regular expression pattern: import-[0-9a-zA-Z-]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### StartWebAuthnRegistration

Requests credential creation options from your user pool for the currently signed-in user.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.StartWebAuthnRegistration

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### StopUserImportJob

Instructs your user pool to stop a running job that's importing users from a CSV file that contains their usernames and attributes.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.StopUserImportJob

{
  "UserPoolId": "example",
  "JobId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "2 validation errors detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+; Value 'example' at 'jobId' failed to satisfy constraint: Member must satisfy regular expression pattern: import-[0-9a-zA-Z-]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### TagResource

Assigns a set of tags to an Amazon Cognito user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.TagResource

{
  "ResourceArn": "arn:aws:cognitoidentityprovider:us-east-1:000000000000:example/example",
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
  "message": "ResourceArn 'arn:aws:cognitoidentityprovider:us-east-1:000000000000:example/example' is not the ARN of an Amazon Cognito user pool."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UntagResource

Given tag IDs that you previously assigned to a user pool, removes them.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UntagResource

{
  "ResourceArn": "arn:aws:cognitoidentityprovider:us-east-1:000000000000:example/example",
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
  "message": "ResourceArn 'arn:aws:cognitoidentityprovider:us-east-1:000000000000:example/example' is not the ARN of an Amazon Cognito user pool."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateAuthEventFeedback

Provides the feedback for an authentication event generated by threat protection features.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateAuthEventFeedback

{
  "UserPoolId": "example",
  "Username": "example",
  "EventId": "example",
  "FeedbackToken": "example",
  "FeedbackValue": "Valid"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateDeviceStatus

Updates the status of a the currently signed-in user's device so that it is marked as remembered or not remembered for the purpose of device authentication.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateDeviceStatus

{
  "AccessToken": "<redacted>",
  "DeviceKey": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'deviceKey' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-f-]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateGroup

Given the name of a user pool group, updates any of the properties for precedence, IAM role, or description.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateGroup

{
  "GroupName": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateIdentityProvider

Modifies the configuration and trust relationship between a third-party identity provider (IdP) and a user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateIdentityProvider

{
  "UserPoolId": "example",
  "ProviderName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateManagedLoginBranding

Configures the branding settings for a user pool style.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateManagedLoginBranding

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "Missing required parameter UserPoolId"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateProvisionedLimit

Sets the provisioned limit for a specific API category.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateProvisionedLimit

{
  "LimitDefinition": {
    "LimitClass": "API_CATEGORY",
    "Attributes": {
      "key": "example"
    }
  },
  "RequestedLimitValue": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "The limit definition must include the attribute Category to identify the API_CATEGORY limit it means."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateResourceServer

Updates the name and scopes of a resource server.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateResourceServer

{
  "UserPoolId": "example",
  "Identifier": "example",
  "Name": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateTerms

Modifies existing terms documents for the requested app client.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateTerms

{
  "TermsId": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "2 validation errors detected: Value 'example' at 'termsId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[4][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$; Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateUserAttributes

Updates the currently signed-in user's attributes.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateUserAttributes

{
  "UserAttributes": [
    {
      "Name": "example"
    }
  ],
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### UpdateUserPool

Updates the configuration of a user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateUserPool

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateUserPoolClient

Given a user pool app client ID, updates the configuration.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateUserPoolClient

{
  "UserPoolId": "example",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateUserPoolDomain

A user pool domain hosts managed login, an authorization server and web server for authentication in your application.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateUserPoolDomain

{
  "Domain": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateUserPoolReplica

Updates replica-specific settings for a user pool replica.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateUserPoolReplica

{
  "UserPoolId": "example",
  "RegionName": "example",
  "Status": "ACTIVE"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "1 validation error detected: Value 'example' at 'userPoolId' failed to satisfy constraint: Member must satisfy regular expression pattern: [\\w-]+_[0-9a-zA-Z]+"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### VerifySoftwareToken

Registers the current user's time-based one-time password (TOTP) authenticator with a code generated in their authenticator app from a private key that's supplied by your user pool.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.VerifySoftwareToken

{
  "UserCode": "100000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:

{
  "__type": "InvalidParameterException",
  "message": "Invalid request. You can provide either an access token or a session ID in the request."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### VerifyUserAttribute

Submits a verification code for a signed-in user who has added or changed a value of an auto-verified attribute.

```http
POST / HTTP/1.1
X-Amz-Target: AWSCognitoIdentityProviderService.VerifyUserAttribute

{
  "AccessToken": "<redacted>",
  "AttributeName": "example",
  "Code": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/cognito-idp/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon Cognito User Pools answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `message`, plus the `x-amzn-errortype` header.
