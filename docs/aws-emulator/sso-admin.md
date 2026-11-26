# AWS Single Sign-On Admin

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: SWBExternalService.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `SSO Admin`
- **sigv4 signing name:** `sso`
- **Endpoint prefix:** `sso`
- **ARN namespace:** `sso`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `sso`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListInstances`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:sso" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.1' \
  -H 'X-Amz-Target: SWBExternalService.ListInstances' \
  --data-binary '{}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Inspect the emulator's IAM Identity Center instance, see what an account instance refuses, and manage an OAuth application on it. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. ListInstances

The emulator starts with one Identity Center instance; its ARN scopes every later call.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListInstances
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 624
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "Instances": [
    {
      "InstanceArn": "arn:aws:sso:::instance/ssoins-7223b1d1ac5e1f0e",
      "IdentityStoreId": "d-9067f3a1c2",
      "IdentityStoreArn": "arn:aws:identitystore::000000000000:identitystore/d-9067f3a1c2",
      "OwnerAccountId": "000000000000",
      "Name": "IdentityCenter",
      "CreatedDate": 1795717806.1406298,
      "Status": "ACTIVE",
      "PrimaryRegion": "us-east-1",
      "Regions": [
        {
          "RegionName": "us-east-1",
          "Status": "ACTIVE",
          "AddedDate": 1795717806.1406298,
          "IsPrimaryRegion": true
        }
      ],
      "State": {
        "Name": "ACTIVE"
      }
    }
  ],
  "NextToken": "73736f61646d696e3a76313a696e7374616e6365733a61726e3a6177733a73736f3a3a3a696e7374616e63652f73736f696e732d37323233623164316163356531663065"
}
```

### 2. DescribeInstance

Its status and the identity store it owns.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DescribeInstance
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 64
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "arn:aws:sso:::instance/ssoins-7223b1d1ac5e1f0e"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 1024
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "InstanceArn": "arn:aws:sso:::instance/ssoins-7223b1d1ac5e1f0e",
  "IdentityStoreId": "d-9067f3a1c2",
  "IdentityStoreArn": "arn:aws:identitystore::000000000000:identitystore/d-9067f3a1c2",
  "OwnerAccountId": "000000000000",
  "Name": "IdentityCenter",
  "CreatedDate": 1795717806.1406298,
  "Status": "ACTIVE",
  "PrimaryRegion": "us-east-1",
  "Regions": [
    {
      "RegionName": "us-east-1",
      "Status": "ACTIVE",
      "AddedDate": 1795717806.1406298,
      "IsPrimaryRegion": true
    }
  ],
  "EncryptionConfigurationDetails": {
    "KeyType": "AWS_OWNED_KMS_KEY",
    "EncryptionStatus": "ENABLED"
  },
  "PermissionSetsEnabled": false,
  "Instance": {
    "InstanceArn": "arn:aws:sso:::instance/ssoins-7223b1d1ac5e1f0e",
    "IdentityStoreId": "d-9067f3a1c2",
    "IdentityStoreArn": "arn:aws:identitystore::000000000000:identitystore/d-9067f3a1c2",
    "OwnerAccountId": "000000000000",
    "Name": "IdentityCenter",
    "CreatedDate": 1795717806.1406298,
    "Status": "ACTIVE",
    "PrimaryRegion": "us-east-1",
    "Regions": [
      {
        "RegionName": "us-east-1",
        "Status": "ACTIVE",
        "AddedDate": 1795717806.1406298,
        "IsPrimaryRegion": true
      }
    ],
    "State": {
      "Name": "ACTIVE"
    }
  }
}
```

### 3. CreatePermissionSet

Permission sets need an organization instance of Identity Center; on an account instance, which is what the emulator provides, AWS refuses them, and so does the emulator.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.CreatePermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 115
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "DocsDemoReadOnly",
  "InstanceArn": "arn:aws:sso:::instance/ssoins-7223b1d1ac5e1f0e",
  "SessionDuration": "PT4H"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 122
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "This operation is not supported for account instances of IAM Identity Center."
}
```

### 4. CreateApplication

Applications are supported on account instances. The custom provider is a generic OAuth application.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.CreateApplication
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 256
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "arn:aws:sso:::instance/ssoins-7223b1d1ac5e1f0e",
  "ApplicationProviderArn": "arn:aws:sso::aws:applicationProvider/custom",
  "Name": "docs-demo-app",
  "Description": "docs demo",
  "Status": "ENABLED",
  "ClientToken": "d6094409-aaa7-4a79-8f77-c670874a7bcf"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 250
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "ApplicationArn": "arn:aws:sso::000000000000:application/ssoins-7223b1d1ac5e1f0e/apl-04ca4e75c6246978",
  "InstanceArn": "arn:aws:sso:::instance/ssoins-7223b1d1ac5e1f0e",
  "IdentityStoreArn": "arn:aws:identitystore::000000000000:identitystore/d-9067f3a1c2"
}
```

### 5. DescribeApplication

The stored application, its provider and its status.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DescribeApplication
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 103
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "arn:aws:sso::000000000000:application/ssoins-7223b1d1ac5e1f0e/apl-04ca4e75c6246978"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 527
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "ApplicationArn": "arn:aws:sso::000000000000:application/ssoins-7223b1d1ac5e1f0e/apl-04ca4e75c6246978",
  "ApplicationProviderArn": "arn:aws:sso::aws:applicationProvider/custom",
  "Name": "docs-demo-app",
  "ApplicationAccount": "000000000000",
  "InstanceArn": "arn:aws:sso:::instance/ssoins-7223b1d1ac5e1f0e",
  "IdentityStoreArn": "arn:aws:identitystore::000000000000:identitystore/d-9067f3a1c2",
  "Status": "ENABLED",
  "PortalOptions": {
    "Visibility": "ENABLED"
  },
  "Description": "docs demo",
  "CreatedDate": 1795717851.804251,
  "CreatedFrom": "us-east-1"
}
```

### 6. ListApplications

Applications of the instance.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListApplications
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 64
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "arn:aws:sso:::instance/ssoins-7223b1d1ac5e1f0e"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 546
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "Applications": [
    {
      "ApplicationArn": "arn:aws:sso::000000000000:application/ssoins-7223b1d1ac5e1f0e/apl-04ca4e75c6246978",
      "ApplicationProviderArn": "arn:aws:sso::aws:applicationProvider/custom",
      "Name": "docs-demo-app",
      "ApplicationAccount": "000000000000",
      "InstanceArn": "arn:aws:sso:::instance/ssoins-7223b1d1ac5e1f0e",
      "IdentityStoreArn": "arn:aws:identitystore::000000000000:identitystore/d-9067f3a1c2",
      "Status": "ENABLED",
      "PortalOptions": {
        "Visibility": "ENABLED"
      },
      "Description": "docs demo",
      "CreatedDate": 1795717851.804251,
      "CreatedFrom": "us-east-1"
    }
  ]
}
```

### 7. DeleteApplication

Delete it.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DeleteApplication
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 103
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "arn:aws:sso::000000000000:application/ssoins-7223b1d1ac5e1f0e/apl-04ca4e75c6246978"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 2
date: Thu, 26 Nov 2026 18:30:51 GMT

{}
```

### 8. DescribeApplication

Now `ResourceNotFoundException`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DescribeApplication
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 103
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "arn:aws:sso::000000000000:application/ssoins-7223b1d1ac5e1f0e/apl-04ca4e75c6246978"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 172
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "No resource found for application arn: arn:aws:sso::000000000000:application/ssoins-7223b1d1ac5e1f0e/apl-04ca4e75c6246978"
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 79 operations, 2 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AddRegion

Adds a Region to an IAM Identity Center instance.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.AddRegion
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "RegionName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 345
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'regionName' failed to satisfy constraint: Member must satisfy regular expression pattern: ([a-z]+-){2,3}\\d; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### AttachCustomerManagedPolicyReferenceToPermissionSet

Attaches the specified customer managed policy to the specified PermissionSet .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.AttachCustomerManagedPolicyReferenceToPermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 112
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000",
  "CustomerManagedPolicyReference": {
    "Name": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### AttachManagedPolicyToPermissionSet

Attaches an Amazon Web Services managed policy ARN to a permission set.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.AttachManagedPolicyToPermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 102
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000",
  "ManagedPolicyArn": "example0000000000000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 639
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'managedPolicyArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:iam::aws:policy((/[A-Za-z0-9\\.,\\+@=_-]+)*)/([A-Za-z0-9\\.,\\+=@_-]+); Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateAccountAssignment

Assigns access to a principal for a specified Amazon Web Services account using a specified permission set.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.CreateAccountAssignment
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 160
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "TargetId": "100000000000",
  "TargetType": "AWS_ACCOUNT",
  "PermissionSetArn": "example000",
  "PrincipalType": "USER",
  "PrincipalId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 629
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'principalId' failed to satisfy constraint: Member must satisfy regular expression pattern: ([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}; Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateApplication

Creates an OAuth 2.0 customer managed application in IAM Identity Center for the given application provider.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.CreateApplication
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 136
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "ApplicationProviderArn": "example000",
  "Name": "example",
  "ClientToken": "304afeb4-d5e3-492a-97d2-4bcf39e44507"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 409
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationProviderArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::aws:applicationProvider/[a-zA-Z0-9-/]+; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#4-createapplication) for a successful call.

### CreateApplicationAssignment

Grant application access to a user or group.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.CreateApplicationAssignment
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 78
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000",
  "PrincipalId": "example",
  "PrincipalType": "USER"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 453
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'principalId' failed to satisfy constraint: Member must satisfy regular expression pattern: ([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}; Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateInstance

Creates an instance of IAM Identity Center for a standalone Amazon Web Services account that is not managed by Organizations or a member Amazon Web Services account in an organization.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.CreateInstance
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClientToken": "c7c5b6c9-c81e-4f9e-8669-bacd6e8f2117"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 239
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ServiceQuotaExceededException",
  "Message": "You have exceeded IAM Identity Center quotas. Cannot create more than 1 instance for 000000000000. Please refer to https://docs.aws.amazon.com/singlesignon/latest/userguide/limits.html"
}
```

`400` `ServiceQuotaExceededException`.

### CreateInstanceAccessControlAttributeConfiguration

Enables the attributes-based access control (ABAC) feature for the specified IAM Identity Center instance.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.CreateInstanceAccessControlAttributeConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 153
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "InstanceAccessControlAttributeConfiguration": {
    "AccessControlAttributes": [
      {
        "Key": "example",
        "Value": {
          "Source": [
            "example"
          ]
        }
      }
    ]
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreatePermissionSet

Creates a permission set within a specified IAM Identity Center instance.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.CreatePermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "InstanceArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateTrustedTokenIssuer

Creates a connection to a trusted token issuer in an instance of IAM Identity Center.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.CreateTrustedTokenIssuer
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 329
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "Name": "example",
  "TrustedTokenIssuerType": "OIDC_JWT",
  "TrustedTokenIssuerConfiguration": {
    "OidcJwtConfiguration": {
      "IssuerUrl": "example",
      "ClaimAttributePath": "example",
      "IdentityStoreAttributePath": "example",
      "JwksRetrievalOption": "OPEN_ID_DISCOVERY"
    }
  },
  "ClientToken": "124fac85-e4c5-496a-8ddf-75757dc31785"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 445
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'trustedTokenIssuerConfiguration.oidcJwtConfiguration.issuerUrl' failed to satisfy constraint: Member must satisfy regular expression pattern: https?:\\/\\/[-a-zA-Z0-9+&@\\/%=~_|!:,.;]*[-a-zA-Z0-9+&@\\/%=~_|]; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteAccountAssignment

Deletes a principal's access from a specified Amazon Web Services account using a specified permission set.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DeleteAccountAssignment
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 160
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "TargetId": "100000000000",
  "TargetType": "AWS_ACCOUNT",
  "PermissionSetArn": "example000",
  "PrincipalType": "USER",
  "PrincipalId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 629
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'principalId' failed to satisfy constraint: Member must satisfy regular expression pattern: ([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}; Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteApplication

Deletes the association with the application.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DeleteApplication
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#7-deleteapplication) for a successful call.

### DeleteApplicationAccessScope

Deletes an IAM Identity Center access scope from an application.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DeleteApplicationAccessScope
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000",
  "Scope": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteApplicationAssignment

Revoke application access to an application by deleting application assignments for a user or group.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DeleteApplicationAssignment
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 78
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000",
  "PrincipalId": "example",
  "PrincipalType": "USER"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 453
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'principalId' failed to satisfy constraint: Member must satisfy regular expression pattern: ([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}; Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteApplicationAuthenticationMethod

Deletes an authentication method from an application.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DeleteApplicationAuthenticationMethod
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 64
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000",
  "AuthenticationMethodType": "IAM"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteApplicationGrant

Deletes a grant from an application.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DeleteApplicationGrant
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 64
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000",
  "GrantType": "authorization_code"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteInlinePolicyFromPermissionSet

Deletes the inline policy from a specified permission set.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DeleteInlinePolicyFromPermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteInstance

Deletes the instance of IAM Identity Center.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DeleteInstance
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteInstanceAccessControlAttributeConfiguration

Disables the attributes-based access control (ABAC) feature for the specified IAM Identity Center instance and deletes all of the attribute mappings that have been configured.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DeleteInstanceAccessControlAttributeConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeletePermissionSet

Deletes the specified permission set.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DeletePermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeletePermissionsBoundaryFromPermissionSet

Deletes the permissions boundary from a specified PermissionSet .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DeletePermissionsBoundaryFromPermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteTrustedTokenIssuer

Deletes a trusted token issuer configuration from an instance of IAM Identity Center.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DeleteTrustedTokenIssuer
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 38
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TrustedTokenIssuerArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 312
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'trustedTokenIssuerArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:trustedTokenIssuer/(sso)?ins-[a-zA-Z0-9-.]{16}/tti-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeAccountAssignmentCreationStatus

Describes the status of the assignment creation request.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DescribeAccountAssignmentCreationStatus
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 104
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "AccountAssignmentCreationRequestId": "example00000000000000000000000000000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 424
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'accountAssignmentCreationRequestId' failed to satisfy constraint: Member must satisfy regular expression pattern: \\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeAccountAssignmentDeletionStatus

Describes the status of the assignment deletion request.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DescribeAccountAssignmentDeletionStatus
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 104
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "AccountAssignmentDeletionRequestId": "example00000000000000000000000000000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 424
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'accountAssignmentDeletionRequestId' failed to satisfy constraint: Member must satisfy regular expression pattern: \\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeApplication

Retrieves the details of an application associated with an instance of IAM Identity Center.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DescribeApplication
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#5-describeapplication) for a successful call.

### DescribeApplicationAssignment

Retrieves a direct assignment of a user or group to an application.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DescribeApplicationAssignment
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 78
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000",
  "PrincipalId": "example",
  "PrincipalType": "USER"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 453
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'principalId' failed to satisfy constraint: Member must satisfy regular expression pattern: ([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}; Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeApplicationProvider

Retrieves details about a provider that can be used to connect an Amazon Web Services managed application or customer managed application to IAM Identity Center.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DescribeApplicationProvider
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 39
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationProviderArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 232
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationProviderArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::aws:applicationProvider/[a-zA-Z0-9-/]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeInstance

Returns the details of an instance of IAM Identity Center.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DescribeInstance
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-describeinstance) for a successful call.

### DescribeInstanceAccessControlAttributeConfiguration

Returns the list of IAM Identity Center identity store attributes that have been configured to work with attributes-based access control (ABAC) for the specified IAM Identity Center instance.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DescribeInstanceAccessControlAttributeConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribePermissionSet

Gets the details of the permission set.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DescribePermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribePermissionSetProvisioningStatus

Describes the status for the given permission set provisioning request.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DescribePermissionSetProvisioningStatus
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 101
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "ProvisionPermissionSetRequestId": "example00000000000000000000000000000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 421
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'provisionPermissionSetRequestId' failed to satisfy constraint: Member must satisfy regular expression pattern: \\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeRegion

Retrieves details about a specific Region enabled in an IAM Identity Center instance.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DescribeRegion
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "RegionName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 345
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'regionName' failed to satisfy constraint: Member must satisfy regular expression pattern: ([a-z]+-){2,3}\\d; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeTrustedTokenIssuer

Retrieves details about a trusted token issuer configuration stored in an instance of IAM Identity Center.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DescribeTrustedTokenIssuer
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 38
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TrustedTokenIssuerArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 312
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'trustedTokenIssuerArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:trustedTokenIssuer/(sso)?ins-[a-zA-Z0-9-.]{16}/tti-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DetachCustomerManagedPolicyReferenceFromPermissionSet

Detaches the specified customer managed policy from the specified PermissionSet .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DetachCustomerManagedPolicyReferenceFromPermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 112
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000",
  "CustomerManagedPolicyReference": {
    "Name": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DetachManagedPolicyFromPermissionSet

Detaches the attached Amazon Web Services managed policy ARN from the specified permission set.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.DetachManagedPolicyFromPermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 102
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000",
  "ManagedPolicyArn": "example0000000000000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 639
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'managedPolicyArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:iam::aws:policy((/[A-Za-z0-9\\.,\\+@=_-]+)*)/([A-Za-z0-9\\.,\\+=@_-]+); Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetApplicationAccessScope

Retrieves the authorized targets for an IAM Identity Center access scope for an application.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.GetApplicationAccessScope
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000",
  "Scope": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetApplicationAssignmentConfiguration

Retrieves the configuration of PutApplicationAssignmentConfiguration .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.GetApplicationAssignmentConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetApplicationAuthenticationMethod

Retrieves details about an authentication method used by an application.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.GetApplicationAuthenticationMethod
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 64
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000",
  "AuthenticationMethodType": "IAM"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetApplicationGrant

Retrieves details about an application grant.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.GetApplicationGrant
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 64
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000",
  "GrantType": "authorization_code"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetApplicationSessionConfiguration

Retrieves the session configuration for an application in IAM Identity Center.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.GetApplicationSessionConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetInlinePolicyForPermissionSet

Obtains the inline policy assigned to the permission set.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.GetInlinePolicyForPermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetPermissionsBoundaryForPermissionSet

Obtains the permissions boundary for a specified PermissionSet .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.GetPermissionsBoundaryForPermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListAccountAssignmentCreationStatus

Lists the status of the Amazon Web Services account assignment creation requests for a specified IAM Identity Center instance.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListAccountAssignmentCreationStatus
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListAccountAssignmentDeletionStatus

Lists the status of the Amazon Web Services account assignment deletion requests for a specified IAM Identity Center instance.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListAccountAssignmentDeletionStatus
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListAccountAssignments

Lists the assignee of the specified Amazon Web Services account with the specified permission set.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListAccountAssignments
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 87
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "AccountId": "100000000000",
  "PermissionSetArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListAccountAssignmentsForPrincipal

Retrieves a list of the IAM Identity Center associated Amazon Web Services accounts that the principal has access to.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListAccountAssignmentsForPrincipal
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PrincipalId": "example",
  "PrincipalType": "USER"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 420
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'principalId' failed to satisfy constraint: Member must satisfy regular expression pattern: ([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListAccountsForProvisionedPermissionSet

Lists all the Amazon Web Services accounts where the specified permission set is provisioned.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListAccountsForProvisionedPermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListApplicationAccessScopes

Lists the access scopes and authorized targets associated with an application.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListApplicationAccessScopes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListApplicationAssignments

Lists Amazon Web Services account users that are assigned to an application.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListApplicationAssignments
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListApplicationAssignmentsForPrincipal

Lists the applications to which a specified principal is assigned.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListApplicationAssignmentsForPrincipal
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PrincipalId": "example",
  "PrincipalType": "USER"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 420
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'principalId' failed to satisfy constraint: Member must satisfy regular expression pattern: ([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListApplicationAuthenticationMethods

Lists all of the authentication methods supported by the specified application.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListApplicationAuthenticationMethods
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListApplicationGrants

List the grants associated with an application.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListApplicationGrants
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListApplicationProviders

Lists the application providers configured in the IAM Identity Center identity store.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListApplicationProviders
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 28973
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "ApplicationProviders": [
    {
      "ApplicationProviderArn": "arn:aws:sso::aws:applicationProvider/catalog/Statuspage",
      "FederationProtocol": "SAML",
      "DisplayData": {
        "DisplayName": "Statuspage",
        "IconUrl": "https://127.0.0.1:9297/_aws/sso-application-provider-icons/catalog/Statuspage/icon.png",
        "Description": "Statuspage Application"
      }
    },
    {
      "ApplicationProviderArn": "arn:aws:sso::aws:applicationProvider/catalog/DuoNetworkGateway",
      "FederationProtocol": "SAML",
      "DisplayData": {
        "DisplayName": "Duo Network Gateway",
        "IconUrl": "https://127.0.0.1:9297/_aws/sso-application-provider-icons/catalog/DuoNetworkGateway/icon.png",
        "Description": "Duo Network Gateway Application"
      }
    },
… (909 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListApplications

Lists all applications associated with the instance of IAM Identity Center.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListApplications
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#6-listapplications) for a successful call.

### ListCustomerManagedPolicyReferencesInPermissionSet

Lists all customer managed policies attached to a specified PermissionSet .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListCustomerManagedPolicyReferencesInPermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListInstances

Lists the details of the organization and account instances of IAM Identity Center that were created in or visible to the account calling this API.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListInstances
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 624
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "Instances": [
    {
      "InstanceArn": "arn:aws:sso:::instance/ssoins-7223b1d1ac5e1f0e",
      "IdentityStoreId": "d-9067f3a1c2",
      "IdentityStoreArn": "arn:aws:identitystore::000000000000:identitystore/d-9067f3a1c2",
      "OwnerAccountId": "000000000000",
      "Name": "IdentityCenter",
      "CreatedDate": 1795717806.1406298,
      "Status": "ACTIVE",
      "PrimaryRegion": "us-east-1",
      "Regions": [
        {
          "RegionName": "us-east-1",
          "Status": "ACTIVE",
          "AddedDate": 1795717806.1406298,
          "IsPrimaryRegion": true
        }
      ],
      "State": {
… (6 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#1-listinstances) for a successful call.

### ListManagedPoliciesInPermissionSet

Lists the Amazon Web Services managed policy that is attached to a specified permission set.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListManagedPoliciesInPermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListPermissionSetProvisioningStatus

Lists the status of the permission set provisioning requests for a specified IAM Identity Center instance.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListPermissionSetProvisioningStatus
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListPermissionSets

Lists the PermissionSet s in an IAM Identity Center instance.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListPermissionSets
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListPermissionSetsProvisionedToAccount

Lists all the permission sets that are provisioned to a specified Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListPermissionSetsProvisionedToAccount
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 55
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "AccountId": "100000000000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListRegions

Lists all enabled Regions of an IAM Identity Center instance, including those that are being added or removed.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListRegions
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListTagsForResource

Lists the tags that are attached to a specified resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListTagsForResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 483
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'resourceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::((:instance/(sso)?ins-[a-zA-Z0-9-.]{16})|(:permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16})|(\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16})|(\\d{12}:trustedTokenIssuer/(sso)?ins-[a-zA-Z0-9-.]{16}/tti-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListTrustedTokenIssuers

Lists all the trusted token issuers configured in an instance of IAM Identity Center.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListTrustedTokenIssuers
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ProvisionPermissionSet

The process by which a specified permission set is provisioned to the specified target.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ProvisionPermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 87
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000",
  "TargetType": "AWS_ACCOUNT"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutApplicationAccessScope

Adds or updates the list of authorized targets for an IAM Identity Center access scope for an application.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.PutApplicationAccessScope
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Scope": "example",
  "ApplicationArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutApplicationAssignmentConfiguration

Configure how users gain access to an application.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.PutApplicationAssignmentConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 58
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000",
  "AssignmentRequired": false
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutApplicationAuthenticationMethod

Adds or updates an authentication method for an application.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.PutApplicationAuthenticationMethod
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 114
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000",
  "AuthenticationMethodType": "IAM",
  "AuthenticationMethod": {
    "Iam": {
      "ActorPolicy": {}
    }
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutApplicationGrant

Creates a configuration for an application to use grants.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.PutApplicationGrant
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 97
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000",
  "GrantType": "authorization_code",
  "Grant": {
    "AuthorizationCode": {}
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutApplicationSessionConfiguration

Updates the session configuration for an application in IAM Identity Center.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.PutApplicationSessionConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutInlinePolicyToPermissionSet

Attaches an inline policy to a permission set.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.PutInlinePolicyToPermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 85
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000",
  "InlinePolicy": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutPermissionsBoundaryToPermissionSet

Attaches an Amazon Web Services managed or customer managed policy to the specified PermissionSet as a permissions boundary.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.PutPermissionsBoundaryToPermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 85
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000",
  "PermissionsBoundary": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### RemoveRegion

Removes an additional Region from an IAM Identity Center instance.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.RemoveRegion
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "RegionName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 345
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'regionName' failed to satisfy constraint: Member must satisfy regular expression pattern: ([a-z]+-){2,3}\\d; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### TagResource

Associates a set of tags with a specified resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.TagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 73
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "example000",
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
content-length: 483
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'resourceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::((:instance/(sso)?ins-[a-zA-Z0-9-.]{16})|(:permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16})|(\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16})|(\\d{12}:trustedTokenIssuer/(sso)?ins-[a-zA-Z0-9-.]{16}/tti-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UntagResource

Disassociates a set of tags from a specified resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.UntagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "example000",
  "TagKeys": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 483
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'resourceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::((:instance/(sso)?ins-[a-zA-Z0-9-.]{16})|(:permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16})|(\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16})|(\\d{12}:trustedTokenIssuer/(sso)?ins-[a-zA-Z0-9-.]{16}/tti-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateApplication

Updates application properties.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.UpdateApplication
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ApplicationArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 253
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'applicationArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:application/(sso)?ins-[a-zA-Z0-9-.]{16}/apl-[a-zA-Z0-9]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateInstance

Update the details for the instance of IAM Identity Center that is owned by the Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.UpdateInstance
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateInstanceAccessControlAttributeConfiguration

Updates the IAM Identity Center identity store attributes that you can use with the IAM Identity Center instance for attributes-based access control (ABAC).

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.UpdateInstanceAccessControlAttributeConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 153
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "InstanceAccessControlAttributeConfiguration": {
    "AccessControlAttributes": [
      {
        "Key": "example",
        "Value": {
          "Source": [
            "example"
          ]
        }
      }
    ]
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdatePermissionSet

Updates an existing permission set.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.UpdatePermissionSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "InstanceArn": "example000",
  "PermissionSetArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 429
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'permissionSetArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::permissionSet/(sso)?ins-[a-zA-Z0-9-.]{16}/ps-[a-zA-Z0-9-./]{16}; Value of input 'instanceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso:::instance/(sso)?ins-[a-zA-Z0-9-.]{16}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateTrustedTokenIssuer

Updates the name of the trusted token issuer, or the path of a source attribute or destination attribute for a trusted token issuer configuration.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.UpdateTrustedTokenIssuer
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 38
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TrustedTokenIssuerArn": "example000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 312
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ValidationException",
  "Message": "Value of input 'trustedTokenIssuerArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws(-[a-z]{1,5}){0,3}:sso::\\d{12}:trustedTokenIssuer/(sso)?ins-[a-zA-Z0-9-.]{16}/tti-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/sso/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way AWS Single Sign-On Admin answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `Message`.
