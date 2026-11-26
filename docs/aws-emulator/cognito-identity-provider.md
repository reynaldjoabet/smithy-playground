# Amazon Cognito User Pools

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: AWSCognitoIdentityProviderService.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `Cognito Identity Provider`
- **sigv4 signing name:** `cognito-idp`
- **Endpoint prefix:** `cognito-idp`
- **ARN namespace:** `cognito-idp`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `cognito-idp`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

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
X-Amz-Date: 20261126T183322Z
Content-Length: 239
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: db439e14-c9de-4242-a568-c1b9b483105d
content-length: 4828
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "UserPool": {
    "Id": "us-east-1_54b513520",
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
    "LastModifiedDate": 1795718002.913,
    "CreationDate": 1795718002.913,
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
… (234 more lines)
```

### 2. CreateUserPoolClient

An app client is what a front end signs in through; this one allows plain username and password sign-in.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateUserPoolClient
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 141
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "us-east-1_54b513520",
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
x-amzn-requestid: f3537ca8-f578-4f37-90a4-bab1d3a8577c
content-length: 517
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "UserPoolClient": {
    "UserPoolId": "us-east-1_54b513520",
    "ClientName": "docs-demo-web",
    "ClientId": "447520a5b6d246f5ba960af563",
    "LastModifiedDate": 1795718003.477,
    "CreationDate": 1795718003.477,
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
    "AuthSessionValidity": 3,
    "ClientType": "REGULAR"
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
X-Amz-Date: 20261126T183323Z
Content-Length: 232
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "us-east-1_54b513520",
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
x-amzn-requestid: 4a40abb3-8256-4824-9e3b-1b67cd62589a
content-length: 341
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "User": {
    "Username": "8436801b-50b9-41ec-a3f0-118eb00c8897",
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
        "Value": "8436801b-50b9-41ec-a3f0-118eb00c8897"
      }
    ],
    "UserCreateDate": 1795718003.48,
    "UserLastModifiedDate": 1795718003.48,
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
X-Amz-Date: 20261126T183323Z
Content-Length: 149
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AuthFlow": "USER_PASSWORD_AUTH",
  "AuthParameters": {
    "USERNAME": "ada@example.com",
    "PASSWORD": "<redacted>"
  },
  "ClientId": "447520a5b6d246f5ba960af563"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 7645ea71-2132-48d1-aa36-d5d125914b0c
content-length: 624
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "ChallengeName": "NEW_PASSWORD_REQUIRED",
  "Session": "<redacted>",
  "ChallengeParameters": {
    "userAttributes": "{\"email\":\"ada@example.com\",\"email_verified\":\"true\"}",
    "requiredAttributes": "[]",
    "USER_ID_FOR_SRP": "8436801b-50b9-41ec-a3f0-118eb00c8897"
  }
}
```

### 5. RespondToAuthChallenge

Answering the challenge with a new password (redacted) completes sign-in: the response carries the ID, access and refresh tokens (all redacted). Emulator 1.0.595 answered `501 NotImplemented` here; 1.0.629 implements it.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.RespondToAuthChallenge
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 567
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClientId": "447520a5b6d246f5ba960af563",
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
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: ce4162a6-5987-4615-8a1d-cd7586b70a06
content-length: 3506
date: Thu, 26 Nov 2026 18:33:23 GMT

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

### 6. AdminSetUserPassword

An administrator can also set a permanent password (redacted), which moves a user out of `FORCE_CHANGE_PASSWORD` without the user's help. Here the user already changed it in step 5, and the call still succeeds.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminSetUserPassword
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 115
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "us-east-1_54b513520",
  "Username": "ada@example.com",
  "Password": "<redacted>",
  "Permanent": true
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: cd4664b0-35b1-49ec-86ad-3a87ed9eaceb
content-length: 2
date: Thu, 26 Nov 2026 18:33:23 GMT

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
X-Amz-Date: 20261126T183323Z
Content-Length: 154
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AuthFlow": "USER_PASSWORD_AUTH",
  "AuthParameters": {
    "USERNAME": "ada@example.com",
    "PASSWORD": "<redacted>"
  },
  "ClientId": "447520a5b6d246f5ba960af563"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: f6ddf38b-7a74-45b5-b9d3-37d906f68511
content-length: 3506
date: Thu, 26 Nov 2026 18:33:23 GMT

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
X-Amz-Date: 20261126T183323Z
Content-Length: 1006
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 46deb47e-0993-4039-b23f-97c3f748a147
content-length: 216
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "Username": "8436801b-50b9-41ec-a3f0-118eb00c8897",
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
      "Value": "8436801b-50b9-41ec-a3f0-118eb00c8897"
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
X-Amz-Date: 20261126T183323Z
Content-Length: 150
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AuthFlow": "USER_PASSWORD_AUTH",
  "AuthParameters": {
    "USERNAME": "ada@example.com",
    "PASSWORD": "<redacted>"
  },
  "ClientId": "447520a5b6d246f5ba960af563"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: 5b5b775d-f726-4a3f-9491-6654f055eea3
content-length: 79
date: Thu, 26 Nov 2026 18:33:23 GMT

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
X-Amz-Date: 20261126T183323Z
Content-Length: 65
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "us-east-1_54b513520",
  "Username": "ada@example.com"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 8586b9b3-85e5-4396-b140-a83a1cceddf1
content-length: 325
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "Username": "8436801b-50b9-41ec-a3f0-118eb00c8897",
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
      "Value": "8436801b-50b9-41ec-a3f0-118eb00c8897"
    }
  ],
  "UserCreateDate": 1795718003.48,
  "UserLastModifiedDate": 1795718003.489,
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
X-Amz-Date: 20261126T183323Z
Content-Length: 36
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "us-east-1_54b513520"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 0e5539db-8fd1-4162-9e45-a1bb203f1934
content-length: 2
date: Thu, 26 Nov 2026 18:33:23 GMT

{}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 132 operations, 3 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.629* answered `501` in 1.0.595.

### AddCustomAttributes

Adds additional user attributes to the user pool schema.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AddCustomAttributes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 48
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 699d9eaf-572f-4fce-a54b-5f9705581fdc
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AddUserPoolClientSecret
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 90e81396-ea76-4caf-8d68-b1695c76ca4a
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminAddUserToGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example",
  "GroupName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 8fa2eb07-f3b0-4bef-8bb9-9dfa12cd60fe
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminConfirmSignUp
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: f2dadc8c-90ba-4203-a1e3-cf9d93485eb5
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminCreateUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 21810dc4-007a-4341-9a6b-de0592dfd1c9
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminDeleteSoftwareToken
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 7927e0dc-6f5e-4cf5-b6d7-269e688c0bb5
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminDeleteUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: da522f59-d3e3-4432-b768-18c991e2e509
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminDeleteUserAttributes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 78
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 548006be-e508-4e9a-935c-efd122c275f3
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminDisableProviderForUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 34
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "User": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:
x-amzn-requestid: 7ec4b4b1-0427-46d1-bacb-971b2391448b
content-length: 90
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminDisableUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 98b64987-58d6-45d5-aec5-2f5ffde9d5a9
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminEnableUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 37adfcc6-cf75-4f35-a874-125d4f4db43e
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminForgetDevice
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example",
  "DeviceKey": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 6ecaee0d-f440-40d8-bcd5-6d6130854e15
x-amzn-errortype: InvalidParameterException:
content-length: 340
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminGetDevice
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DeviceKey": "example",
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 7c4c2db9-f3cd-4428-beac-7d9b11a2c570
x-amzn-errortype: InvalidParameterException:
content-length: 340
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminGetUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: df09fad5-cfab-4f5b-8b87-e41ae11ecb6c
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminGetUserAuthFactors
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: f268b68f-c8d0-4843-b737-0b61f01dfea7
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminInitiateAuth
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 72
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ClientId": "example",
  "AuthFlow": "USER_SRP_AUTH"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 1276f935-5d26-46bb-81f3-911bcbe1cd71
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminLinkProviderForUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: c9005895-52a4-445b-b4fe-97345bc39173
content-length: 90
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminListDevices
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: b249a4a6-32ff-44ef-949f-86254891d6a5
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminListGroupsForUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Username": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 9c2520a1-6d18-4193-823c-933f2606121d
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminListUserAuthEvents
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: f19dd093-617d-47c5-bffe-be968db82d36
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminRemoveUserFromGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example",
  "GroupName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 4131684c-46dd-48f1-ba2b-1d0dd7d5860a
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminResetUserPassword
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: e9945f37-28b9-4677-966b-2e5162ed3a26
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminRespondToAuthChallenge
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ClientId": "example",
  "ChallengeName": "SMS_MFA"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 5ff5eaed-b627-4f86-83df-ffef6069ed69
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminSetUserMFAPreference
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Username": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 0da71bcc-eb48-47fc-af4a-24487c760195
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminSetUserPassword
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 78
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example",
  "Password": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: af528c7c-bf88-4318-9a7d-439c7d8605ef
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminSetUserSettings
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 63
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 55a38c62-c963-46ec-bb5d-9447eba1a7c8
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminUpdateAuthEventFeedback
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 89
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 2eb7ed65-c30b-4bc1-affc-b642fd1f032b
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminUpdateDeviceStatus
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example",
  "DeviceKey": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: e2d3a569-9e73-4693-9bc5-4fd886b1bbc7
x-amzn-errortype: InvalidParameterException:
content-length: 340
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminUpdateUserAttributes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 83
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 4553c09c-8844-47a7-8b48-c3fcdce6593a
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AdminUserGlobalSignOut
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: a3cbfbda-a25d-42fc-ab51-b5c44b448356
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.AssociateSoftwareToken
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: f5655983-0391-4d7d-acb1-696b455282bf
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### ChangePassword

Changes the password for the currently signed-in user.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ChangePassword
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1035
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ProposedPassword": "<redacted>",
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: b7e0f1a2-d4f8-4cb6-9f2e-00426ca70b39
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CompleteWebAuthnRegistration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1022
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>",
  "Credential": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: 6706ee66-f5df-4310-acf9-de1ee18b7ea9
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ConfirmDevice
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1028
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>",
  "DeviceKey": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 058f1599-baa5-45fe-80dd-43db9b3afb56
x-amzn-errortype: InvalidParameterException:
content-length: 207
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ConfirmForgotPassword
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 105
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 3574e23d-3498-43ef-9c1f-87e133e5b80e
content-length: 92
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "__type": "ResourceNotFoundException",
  "message": "Username/client id combination not found."
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ConfirmSignUp

Confirms the account of a new user.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ConfirmSignUp
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 72
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 44c3bc67-9353-419c-bfda-d7ccfdc63fc9
content-length: 92
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "__type": "ResourceNotFoundException",
  "message": "Username/client id combination not found."
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateGroup

Creates a new group in the specified user pool.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "GroupName": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 1af0afc9-c8ea-4d31-a151-e9b773019bc1
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateIdentityProvider
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 107
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 25f0b147-86f2-4146-8416-f812e727239a
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateManagedLoginBranding
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: a1f3bfd4-d8d1-43d0-ad4b-b19043a4391e
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateResourceServer
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 64
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Identifier": "example",
  "Name": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 4c78883c-06f9-4901-a827-ecdb4d8486de
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateTerms
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 109
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: c86250ec-1057-4ffd-962b-f857701ca9ed
x-amzn-errortype: InvalidParameterException:
content-length: 353
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateUserImportJob
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 141
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "JobName": "example",
  "UserPoolId": "example",
  "CloudWatchLogsRoleArn": "arn:aws:cognitoidentityprovider:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: ed943e1a-b828-44d0-8d49-b147b3014a3b
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateUserPool
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "PoolName": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 71b66eff-b8b9-4661-bb27-e1b49b528c06
content-length: 4753
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "UserPool": {
    "Id": "us-east-1_e921068cb",
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
… (268 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#1-createuserpool) for a successful call.

### CreateUserPoolClient

Creates an app client in a user pool.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateUserPoolClient
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ClientName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 25b5bba1-edb2-4373-ad2a-7b2d488bd3d6
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateUserPoolDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Domain": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: efb4645a-8405-48a3-ae26-27b8d55ebc11
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.CreateUserPoolReplica
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "RegionName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 8b17949b-21d1-4f76-8078-5a0de1b5e7b0
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "GroupName": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 52c8c6d1-931c-4faa-974d-3e59bc085de9
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteIdentityProvider
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ProviderName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 5be40c62-f501-42e5-9e2b-b08e41970f01
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteManagedLoginBranding
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ManagedLoginBrandingId": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 2086f58a-d3b1-48d3-83a2-452abe711fd0
x-amzn-errortype: InvalidParameterException:
content-length: 421
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteResourceServer
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Identifier": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: ba628c23-2e78-4f86-9e99-a6cef39c0fbe
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteTerms
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TermsId": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 6eed3b0d-e1b5-4209-90d6-4fe5261a5454
x-amzn-errortype: InvalidParameterException:
content-length: 408
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1006
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: 4eabafba-4765-44e6-994e-c8c7feab7172
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUserAttributes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1039
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 311d25b5-eb2d-487e-826e-b84f90629828
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUserPool
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: d5088445-d0bc-48c8-9a89-6306b94a19b2
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUserPoolClient
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 54cd1f14-43fa-456a-845f-96dadf031bd5
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUserPoolClientSecret
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 72
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ClientId": "example",
  "ClientSecretId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 8938b461-b0b8-423f-8ce7-ff1a4997a08d
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUserPoolDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Domain": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: d6f14160-4c45-4ec1-b27d-4b7d60f1868b
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteUserPoolReplica
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "RegionName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: e6fee0ab-7429-4c18-8424-f1b5413c001c
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DeleteWebAuthnCredential
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1031
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>",
  "CredentialId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: d406b6c5-4586-4343-914d-a43328e83aa1
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeIdentityProvider
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ProviderName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: f434cba2-526f-40fa-9eda-015ac0181fdc
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeManagedLoginBranding
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ManagedLoginBrandingId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 5393dcba-4db1-486a-8caf-2cbcb16e1d30
x-amzn-errortype: InvalidParameterException:
content-length: 421
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeManagedLoginBrandingByClient
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 9777c822-055c-4de4-96b8-098d4d27f245
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeResourceServer
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Identifier": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: c208885e-f4d9-4917-be1e-9f2ce1d4c96b
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeRiskConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: e691426d-21b1-4458-b2b2-b8f6945d1f84
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeTerms
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TermsId": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 98555573-9b76-4e5c-b40d-8dabefc2365b
x-amzn-errortype: InvalidParameterException:
content-length: 408
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeTermsByClient
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClientId": "example",
  "UserPoolId": "example",
  "TermsName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: bebe4ebd-40dc-41c0-be8c-fe9c827fa623
x-amzn-errortype: InvalidParameterException:
content-length: 353
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeUserImportJob
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 42
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "JobId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 7db71e72-9f55-46a4-b1a1-e222ff0bdc96
x-amzn-errortype: InvalidParameterException:
content-length: 338
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeUserPool
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 78e3d811-34e2-423c-a549-b898b5c6e4e5
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeUserPoolClient
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 9cf841f8-13b7-4779-9f15-12cc8aaceca5
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.DescribeUserPoolDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 20
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Domain": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: fa73211d-ce5b-4f8b-b726-40f3cfd12241
content-length: 24
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "DomainDescription": {}
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ForgetDevice

Given a device key, deletes a remembered device as the currently signed-in user.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ForgetDevice
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DeviceKey": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 4d44c6fa-8223-4878-aa8c-fc55d1c2baa4
x-amzn-errortype: InvalidParameterException:
content-length: 207
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ForgotPassword
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClientId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ResourceNotFoundException:
x-amzn-requestid: 3eb08085-02f4-4f84-a0fc-72e359de713e
content-length: 92
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "__type": "ResourceNotFoundException",
  "message": "Username/client id combination not found."
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetCSVHeader

Given a user pool ID, generates a comma-separated value (CSV) list populated with available user attributes in the user pool.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetCSVHeader
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: c63afc81-b0be-46ba-ab4c-732ad8d8e883
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetClientToken
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 58
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClientId": "example",
  "Secret": "example00000000000000000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ResourceNotFoundException:
x-amzn-requestid: 66e8bd6d-25cb-4879-bdb7-f0f48d295ce3
content-length: 91
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetDevice
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DeviceKey": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 3575d025-c188-493b-b6c5-6c8d569f0e4e
x-amzn-errortype: InvalidParameterException:
content-length: 207
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "GroupName": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 5366c5fc-9319-41ed-9c8f-1a25fc77b49a
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetIdentityProviderByIdentifier
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "IdpIdentifier": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 58521218-0f7f-4967-868a-e7c67a726826
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetLogDeliveryConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 963a2b3f-b5e5-4a7b-bc0e-68b3735eb62b
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetProvisionedLimit
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 80
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 02730be6-dde7-411d-b268-c8e28094deef
content-length: 152
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetSigningCertificate
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 7e17fff9-524b-4141-83e9-e0c91ef7f755
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetTokensFromRefreshToken
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1425
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "RefreshToken": "<redacted>",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ResourceNotFoundException:
x-amzn-requestid: df889a70-3c8f-45a2-940a-7681180c0ea1
content-length: 91
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetUICustomization
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 6b983fd4-c62f-4ab3-853a-16122d063def
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1006
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: ce5dc8fb-e277-4e76-914d-c1c444629230
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetUserAttributeVerificationCode
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1032
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>",
  "AttributeName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: cea0ee21-4a26-400f-bc9b-7e7ab1a8cb20
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetUserAuthFactors
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1006
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: bc352de7-3e8e-41cc-bf77-6f1ae32990e1
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GetUserPoolMfaConfig
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 8cb37e62-945b-447e-84bc-40a98e41d761
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.GlobalSignOut
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1006
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: 85b65e04-273c-4ff4-9e35-12368520c749
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### InitiateAuth

Declares an authentication flow and initiates sign-in for a user in the Amazon Cognito user directory.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.InitiateAuth
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AuthFlow": "USER_SRP_AUTH",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:
x-amzn-requestid: d28e69ad-c9c7-4ae2-9043-2a3d0e185a5f
content-length: 86
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "__type": "InvalidParameterException",
  "message": "Missing required parameter USERNAME"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation. See [the walkthrough](#4-initiateauth) for a successful call.

### ListDevices

Lists the devices that Amazon Cognito has registered to the currently signed-in user.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListDevices
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1006
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: 95650d68-09c7-4a16-9ac2-5db8d7935ea5
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListGroups
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 317d185d-9cc7-4b7e-acf0-030e24fc6da6
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListIdentityProviders
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 17bf3ff4-27c6-4382-b3a8-94cdb480f1a3
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListResourceServers
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: fe49289f-280d-4661-a379-5ea637d3c122
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListTagsForResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 88
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:cognitoidentityprovider:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:
x-amzn-requestid: b814b23b-e086-4d8a-88ed-d4fd6434384c
content-length: 144
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid user pool ARN: arn:aws:cognitoidentityprovider:us-east-1:000000000000:example/example"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ListTerms

Returns details about all terms documents for the requested user pool.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListTerms
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 570e61ab-0269-455b-8250-8c2db3b00199
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListUserImportJobs
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 39
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "MaxResults": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: bc99fbc8-8661-4077-b025-a58adc5235a4
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListUserPoolClientSecrets
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 37bec9b0-e847-4d5f-a626-7e8d60ada2cb
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListUserPoolClients
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 59db830b-e543-4466-ac7e-5a004a707abc
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListUserPoolReplicas
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 88136ee6-8308-4e79-b790-710c48946ef5
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListUserPools
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 16
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "MaxResults": 1
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
x-amzn-requestid: 1a0fed41-4af5-488f-a1cd-a53fe711982c
content-length: 174
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "UserPools": [
    {
      "Id": "us-east-1_e921068cb",
      "Name": "example",
      "LambdaConfig": {},
      "LastModifiedDate": 1795718003.569,
      "CreationDate": 1795718003.569,
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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListUsers
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 90a9189b-a004-4a03-9a4d-4d2e55298678
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListUsersInGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "GroupName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 69c3b249-dded-41ee-aba8-cbc0ef9406c4
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ListWebAuthnCredentials
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1006
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: cd3de558-de73-4837-9e3d-7e339832903d
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.ResendConfirmationCode
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClientId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ResourceNotFoundException:
x-amzn-requestid: 3a162a13-c74c-4467-8c4c-30b4d5d439cd
content-length: 92
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "__type": "ResourceNotFoundException",
  "message": "Username/client id combination not found."
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RespondToAuthChallenge

Some API operations in a user pool generate a challenge, like a prompt for an MFA code, for device authentication that bypasses MFA, or for a custom authentication challenge.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.RespondToAuthChallenge
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 48
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClientId": "example",
  "ChallengeName": "SMS_MFA"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:
x-amzn-requestid: 44814015-02e7-4511-be10-8aedacf3239d
content-length: 85
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.RevokeToken
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1029
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Token": "<redacted>",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: UnauthorizedException:
x-amzn-requestid: 2de042b2-31ff-4cf6-af9f-7d47d4847250
content-length: 65
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.SetLogDeliveryConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 100
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: c6e32c00-b9ee-44db-b761-7a6ef9996777
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.SetRiskConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 191a0ead-54f3-44e4-abb2-3914c60c8c0a
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.SetUICustomization
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 83c03684-7f18-45bf-bb27-4d77829e5709
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.SetUserMFAPreference
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1006
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: 3a3bf856-d033-4bd7-92b4-61f10b0e6194
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.SetUserPoolMfaConfig
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 0654e397-80c7-4107-8d56-816458a646fb
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.SetUserSettings
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1024
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: cf93d768-7171-4f01-a84b-7fc13764196b
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.SignUp
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClientId": "example",
  "Username": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: ResourceNotFoundException:
x-amzn-requestid: 6fa7dee9-8cce-4259-8c20-5072ba9bc0ee
content-length: 91
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.StartUserImportJob
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 42
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "JobId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 9f69a612-9ddb-4c8b-b3fa-5082d0edbe22
x-amzn-errortype: InvalidParameterException:
content-length: 338
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.StartWebAuthnRegistration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1006
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: 91cbe5c5-91a5-48c8-8d61-e6e6d0beea61
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.StopUserImportJob
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 42
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "JobId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 29a6ef8b-b7dd-4599-8790-fb0521aa93f4
x-amzn-errortype: InvalidParameterException:
content-length: 338
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.TagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 113
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: a4ee0ea7-644d-44ff-8d83-e4890fab8655
content-length: 144
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid user pool ARN: arn:aws:cognitoidentityprovider:us-east-1:000000000000:example/example"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UntagResource

Given tag IDs that you previously assigned to a user pool, removes them.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UntagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 110
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 0657251a-5f2a-4a6a-bb83-4ab4c83fdca9
content-length: 144
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid user pool ARN: arn:aws:cognitoidentityprovider:us-east-1:000000000000:example/example"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateAuthEventFeedback

Provides the feedback for an authentication event generated by threat protection features.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateAuthEventFeedback
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 115
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 071ee92e-08cc-4cbb-82ee-dfc0a50480ec
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateDeviceStatus
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1028
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AccessToken": "<redacted>",
  "DeviceKey": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 4c0fc084-71e8-4e4b-8600-b2efe5eb36a3
x-amzn-errortype: InvalidParameterException:
content-length: 207
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "GroupName": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: fe4aa768-91c4-46f7-a254-1cdfc11d628f
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateIdentityProvider
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ProviderName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 9eadb287-51dd-4e24-aeae-cc3829961c96
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateManagedLoginBranding
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: InvalidParameterException:
x-amzn-requestid: 49183679-af2e-4e7c-851f-3b9f7e714d49
content-length: 88
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateProvisionedLimit
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 104
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 79a21361-250f-4623-927c-03f31900934b
content-length: 152
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateResourceServer
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 64
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "Identifier": "example",
  "Name": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 99745ed8-aa7b-435b-b256-72526b5a21e7
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateTerms
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TermsId": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 7b8ad937-59a2-4fef-ad80-5ef115e67724
x-amzn-errortype: InvalidParameterException:
content-length: 408
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateUserAttributes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1044
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: beb84dbb-b5f9-4282-824b-b1c8ea5dc79d
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateUserPool
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: d29b061b-ad22-4ca5-8e02-7026e2955aaf
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateUserPoolClient
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "ClientId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 9774cf2b-b83d-413f-9bab-b4564200c8bb
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateUserPoolDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Domain": "example",
  "UserPoolId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 3852cd37-5970-4dc5-aba6-bba757ddffda
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.UpdateUserPoolReplica
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 65
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserPoolId": "example",
  "RegionName": "example",
  "Status": "ACTIVE"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-requestid: 7bd894ae-b62e-4949-bc18-81ba68851eb0
x-amzn-errortype: InvalidParameterException:
content-length: 210
date: Thu, 26 Nov 2026 18:33:23 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.VerifySoftwareToken
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 21
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UserCode": "100000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
x-amzn-errortype: NotAuthorizedException:
x-amzn-requestid: 3e0fed99-9f94-42ef-b64a-cfed5b2b2e0e
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

{
  "__type": "NotAuthorizedException",
  "message": "Invalid Access Token"
}
```

`400` `NotAuthorizedException`.

### VerifyUserAttribute

Submits a verification code for a signed-in user who has added or changed a value of an auto-verified attribute.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSCognitoIdentityProviderService.VerifyUserAttribute
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183323Z
Content-Length: 1049
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cognito-idp/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 962d10dc-ae93-43ca-ac04-15c9ee936f39
content-length: 68
date: Thu, 26 Nov 2026 18:33:23 GMT

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
