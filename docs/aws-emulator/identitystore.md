# AWS SSO Identity Store

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: AWSIdentityStore.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `identitystore`
- **sigv4 signing name:** `identitystore`
- **Endpoint prefix:** `identitystore`
- **ARN namespace:** `identitystore`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `identitystore`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListUsers`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:identitystore" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.1' \
  -H 'X-Amz-Target: AWSIdentityStore.ListUsers' \
  --data-binary '{"IdentityStoreId": "d-9067f3a1c2"}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create a user and a group in the Identity Center identity store and put the user in the group. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. AWS Single Sign-On Admin: ListInstances

The identity store id comes from the Identity Center instance.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListInstances
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121017Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 624
date: Mon, 16 Nov 2026 12:10:17 GMT

{
  "Instances": [
    {
      "InstanceArn": "arn:aws:sso:::instance/ssoins-7223b1d1ac5e1f0e",
      "IdentityStoreId": "d-9067f3a1c2",
      "IdentityStoreArn": "arn:aws:identitystore::000000000000:identitystore/d-9067f3a1c2",
      "OwnerAccountId": "000000000000",
      "Name": "IdentityCenter",
      "CreatedDate": 1794830491.9161334,
      "Status": "ACTIVE",
      "PrimaryRegion": "us-east-1",
      "Regions": [
        {
          "RegionName": "us-east-1",
          "Status": "ACTIVE",
          "AddedDate": 1794830491.9161334,
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

### 2. CreateUser

Answers with the new user's id.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSIdentityStore.CreateUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121017Z
Content-Length: 216
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/identitystore/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityStoreId": "d-9067f3a1c2",
  "UserName": "ada",
  "Name": {
    "FamilyName": "Lovelace",
    "GivenName": "Ada"
  },
  "DisplayName": "Ada Lovelace",
  "Emails": [
    {
      "Value": "ada@example.com",
      "Type": "work",
      "Primary": true
    }
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 175
date: Mon, 16 Nov 2026 12:10:17 GMT

{
  "IdentityStoreId": "d-9067f3a1c2",
  "UserId": "795f2cc7-6c10-47d4-97c5-029d251f0a7f",
  "UserArn": "arn:aws:identitystore:::user/795f2cc7-6c10-47d4-97c5-029d251f0a7f",
  "Revision": "1"
}
```

### 3. CreateGroup

Answers with the group id.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSIdentityStore.CreateGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121017Z
Content-Length: 93
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/identitystore/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityStoreId": "d-9067f3a1c2",
  "DisplayName": "engineers",
  "Description": "engineering"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 178
date: Mon, 16 Nov 2026 12:10:17 GMT

{
  "IdentityStoreId": "d-9067f3a1c2",
  "GroupId": "1d419ca6-250b-4073-9fbb-f5840ee9d7d5",
  "GroupArn": "arn:aws:identitystore:::group/1d419ca6-250b-4073-9fbb-f5840ee9d7d5",
  "Revision": "1"
}
```

### 4. CreateGroupMembership

Membership is its own resource with its own id.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSIdentityStore.CreateGroupMembership
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121017Z
Content-Length: 150
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/identitystore/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityStoreId": "d-9067f3a1c2",
  "GroupId": "1d419ca6-250b-4073-9fbb-f5840ee9d7d5",
  "MemberId": {
    "UserId": "795f2cc7-6c10-47d4-97c5-029d251f0a7f"
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 178
date: Mon, 16 Nov 2026 12:10:17 GMT

{
  "IdentityStoreId": "d-9067f3a1c2",
  "MembershipId": "f8cec501-caab-47c1-a536-70f214d12a9a",
  "MembershipArn": "arn:aws:identitystore:::membership/f8cec501-caab-47c1-a536-70f214d12a9a"
}
```

### 5. ListGroupMemberships

Members of the group.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSIdentityStore.ListGroupMemberships
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121017Z
Content-Length: 86
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/identitystore/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityStoreId": "d-9067f3a1c2",
  "GroupId": "1d419ca6-250b-4073-9fbb-f5840ee9d7d5"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 509
date: Mon, 16 Nov 2026 12:10:17 GMT

{
  "GroupMemberships": [
    {
      "IdentityStoreId": "d-9067f3a1c2",
      "MembershipId": "f8cec501-caab-47c1-a536-70f214d12a9a",
      "MembershipArn": "arn:aws:identitystore:::membership/f8cec501-caab-47c1-a536-70f214d12a9a",
      "GroupId": "1d419ca6-250b-4073-9fbb-f5840ee9d7d5",
      "MemberId": {
        "UserId": "795f2cc7-6c10-47d4-97c5-029d251f0a7f"
      },
      "CreatedAt": 1794831017.679292,
      "UpdatedAt": 1794831017.679292,
      "CreatedBy": "arn:aws:iam::000000000000:user/<access-key-id>",
      "UpdatedBy": "arn:aws:iam::000000000000:user/<access-key-id>"
    }
  ]
}
```

### 6. CreateUser

User names are unique: `ConflictException`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSIdentityStore.CreateUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121017Z
Content-Length: 138
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/identitystore/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityStoreId": "d-9067f3a1c2",
  "UserName": "ada",
  "Name": {
    "FamilyName": "Lovelace",
    "GivenName": "Ada"
  },
  "DisplayName": "Ada again"
}
```

**Response**

```http
HTTP/1.1 409 Conflict
content-type: application/x-amz-json-1.1
content-length: 155
date: Mon, 16 Nov 2026 12:10:17 GMT

{
  "__type": "ConflictException",
  "Message": "Duplicate UserName",
  "RequestId": "1ce3d796-3586-46d4-b157-d775f698b289",
  "Reason": "UNIQUENESS_CONSTRAINT_VIOLATION"
}
```

### 7. DeleteGroupMembership

Clean up the membership ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSIdentityStore.DeleteGroupMembership
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121017Z
Content-Length: 91
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/identitystore/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityStoreId": "d-9067f3a1c2",
  "MembershipId": "f8cec501-caab-47c1-a536-70f214d12a9a"
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

### 8. DeleteGroup

... the group ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSIdentityStore.DeleteGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121017Z
Content-Length: 86
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/identitystore/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityStoreId": "d-9067f3a1c2",
  "GroupId": "1d419ca6-250b-4073-9fbb-f5840ee9d7d5"
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

### 9. DeleteUser

... and the user.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AWSIdentityStore.DeleteUser
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121017Z
Content-Length: 85
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/identitystore/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "IdentityStoreId": "d-9067f3a1c2",
  "UserId": "795f2cc7-6c10-47d4-97c5-029d251f0a7f"
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

Of 22 operations, 0 accepted the placeholder call, 1 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### CreateGroup

Creates a group within the specified identity store.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.CreateGroup

{
  "IdentityStoreId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-creategroup) for a successful call.

### CreateGroupMembership

Creates a relationship between a member and a group.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.CreateGroupMembership

{
  "IdentityStoreId": "example",
  "GroupId": "example",
  "MemberId": {
    "UserId": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}); Value 'example' at 'groupId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore:::(user|group|membership)/)?([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#4-creategroupmembership) for a successful call.

### CreateUser

Creates a user within the specified identity store.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.CreateUser

{
  "IdentityStoreId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-createuser) for a successful call.

### DeleteGroup

Delete a group within an identity store given GroupId .

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.DeleteGroup

{
  "IdentityStoreId": "example",
  "GroupId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}); Value 'example' at 'groupId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore:::(user|group|membership)/)?([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#8-deletegroup) for a successful call.

### DeleteGroupMembership

Delete a membership within a group given MembershipId .

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.DeleteGroupMembership

{
  "IdentityStoreId": "example",
  "MembershipId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}); Value 'example' at 'membershipId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore:::(user|group|membership)/)?([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#7-deletegroupmembership) for a successful call.

### DeleteUser

Deletes a user within an identity store given UserId .

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.DeleteUser

{
  "IdentityStoreId": "example",
  "UserId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}); Value 'example' at 'userId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore:::(user|group|membership)/)?([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#9-deleteuser) for a successful call.

### DescribeGroup

Retrieves the group metadata and attributes from GroupId in an identity store.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.DescribeGroup

{
  "IdentityStoreId": "example",
  "GroupId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}); Value 'example' at 'groupId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore:::(user|group|membership)/)?([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeGroupMembership

Retrieves membership metadata and attributes from MembershipId in an identity store.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.DescribeGroupMembership

{
  "IdentityStoreId": "example",
  "MembershipId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}); Value 'example' at 'membershipId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore:::(user|group|membership)/)?([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeIdentityStore

Retrieves details about the specified identity store, including its Amazon Resource Name (ARN) and network configuration.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.DescribeIdentityStore

{
  "IdentityStoreId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeUser

Retrieves the user metadata and attributes from the UserId in an identity store.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.DescribeUser

{
  "IdentityStoreId": "example",
  "UserId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}); Value 'example' at 'userId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore:::(user|group|membership)/)?([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetGroupId

Retrieves GroupId in an identity store.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.GetGroupId

{
  "IdentityStoreId": "example",
  "AlternateIdentifier": {
    "ExternalId": {
      "Issuer": "example",
      "Id": "example"
    }
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetGroupMembershipId

Retrieves the MembershipId in an identity store.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.GetGroupMembershipId

{
  "IdentityStoreId": "example",
  "GroupId": "example",
  "MemberId": {
    "UserId": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}); Value 'example' at 'groupId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore:::(user|group|membership)/)?([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetUserId

Retrieves the UserId in an identity store.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.GetUserId

{
  "IdentityStoreId": "example",
  "AlternateIdentifier": {
    "ExternalId": {
      "Issuer": "example",
      "Id": "example"
    }
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### IsMemberInGroups

Checks the user's membership in all requested groups and returns if the member exists in all queried groups.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.IsMemberInGroups

{
  "IdentityStoreId": "example",
  "MemberId": {
    "UserId": "example"
  },
  "GroupIds": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}); Value '[example]' at 'groupIds' failed to satisfy constraint: Member must satisfy constraint: [Member must have length less than or equal to 100, Member must have length greater than or equal to 1, Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore:::(user|group|membership)/)?([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}, Member must not be null]"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListGroupMemberships

For the specified group in the specified identity store, returns the list of all GroupMembership objects and returns results in paginated form.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.ListGroupMemberships

{
  "IdentityStoreId": "example",
  "GroupId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}); Value 'example' at 'groupId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore:::(user|group|membership)/)?([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#5-listgroupmemberships) for a successful call.

### ListGroupMembershipsForMember

For the specified member in the specified identity store, returns the list of all GroupMembership objects and returns results in paginated form.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.ListGroupMembershipsForMember

{
  "IdentityStoreId": "example",
  "MemberId": {
    "UserId": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListGroups

Lists all groups in the identity store.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.ListGroups

{
  "IdentityStoreId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListIdentityStores

Lists the identity stores that you have access to.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.ListIdentityStores

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation ListIdentityStores is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListUsers

Lists all users in the identity store.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.ListUsers

{
  "IdentityStoreId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateGroup

Updates the specified group metadata and attributes in the specified identity store.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.UpdateGroup

{
  "IdentityStoreId": "example",
  "GroupId": "example",
  "Operations": [
    {
      "AttributePath": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}); Value 'example' at 'groupId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore:::(user|group|membership)/)?([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateIdentityStore

Updates the configuration of the specified identity store, including its network configuration.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.UpdateIdentityStore

{
  "IdentityStoreId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateUser

Updates the specified user metadata and attributes in the specified identity store.

```http
POST / HTTP/1.1
X-Amz-Target: AWSIdentityStore.UpdateUser

{
  "IdentityStoreId": "example",
  "UserId": "example",
  "Operations": [
    {
      "AttributePath": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'identityStoreId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore::\\d{12}:identitystore/)?(d-[0-9a-f]{10}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}); Value 'example' at 'userId' failed to satisfy constraint: Member must satisfy regular expression pattern: (arn:aws(-[a-z]{1,5}){0,3}:identitystore:::(user|group|membership)/)?([0-9a-f]{10}-|)[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/identitystore/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way AWS SSO Identity Store answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `Message`, `message`.

### Other services involved

The walkthrough also called [AWS Single Sign-On Admin](sso-admin.md). They are separate services behind the same endpoint, sharing one account and one in-memory engine, so a resource created through one is immediately visible to the other.
