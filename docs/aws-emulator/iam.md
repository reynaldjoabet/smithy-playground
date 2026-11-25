# AWS Identity and Access Management

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `IAM`
- **sigv4 signing name:** `iam`
- **Endpoint prefix:** `iam`
- **ARN namespace:** `iam`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `iam`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListUsers`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:iam" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-www-form-urlencoded; charset=utf-8' \
  --data-binary 'Action=ListUsers&Version=2010-05-08' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create a user, give it a managed policy and an access key, then remove everything. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateUser

IAM is global: the ARN has no Region. The user id starts with `AIDA`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120456Z
Content-Length: 108
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateUser
&Version=2010-05-08
&UserName=docs-demo-user
&Tags.member.1.Key=team
&Tags.member.1.Value=docs
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 543
date: Mon, 16 Nov 2026 12:04:56 GMT

<CreateUserResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateUserResult>
    <User xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <UserName>docs-demo-user</UserName>
      <UserId>AIDABGXEQES5NRCCJJWAN</UserId>
      <Arn>arn:aws:iam::000000000000:user/docs-demo-user</Arn>
      <CreateDate>2026-11-16T12:04:56.474Z</CreateDate>
      <Tags>
        <member>
          <Key>team</Key>
          <Value>docs</Value>
        </member>
      </Tags>
    </User>
  </CreateUserResult>
  <ResponseMetadata>
    <RequestId>28569296-2d50-4875-8319-ce153c50b295</RequestId>
  </ResponseMetadata>
</CreateUserResponse>
```

### 2. CreatePolicy

A customer managed policy, created at version `v1`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120456Z
Content-Length: 301
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreatePolicy
&Version=2010-05-08
&PolicyName=docs-demo-read
&PolicyDocument=%7B%22Version%22%3A%20%222012-10-17%22%2C%20%22Statement%22%3A%20%5B%7B%22Effect%22%3A%20%22Allow%22%2C%20%22Action%22%3A%20%5B%22s3%3AGetObject%22%2C%20%22s3%3AListBucket%22%5D%2C%20%22Resource%22%3A%20%22%2A%22%7D%5D%7D
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 722
date: Mon, 16 Nov 2026 12:04:56 GMT

<CreatePolicyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreatePolicyResult>
    <Policy xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <PolicyName>docs-demo-read</PolicyName>
      <PolicyId>ANPABGXEQES5NRCCJJWAN</PolicyId>
      <Arn>arn:aws:iam::000000000000:policy/docs-demo-read</Arn>
      <Path>/</Path>
      <DefaultVersionId>v1</DefaultVersionId>
      <AttachmentCount>0</AttachmentCount>
      <PermissionsBoundaryUsageCount>0</PermissionsBoundaryUsageCount>
      <IsAttachable>true</IsAttachable>
      <CreateDate>2026-11-16T12:04:56.483Z</CreateDate>
      <UpdateDate>2026-11-16T12:04:56.483Z</UpdateDate>
    </Policy>
  </CreatePolicyResult>
  <ResponseMetadata>
    <RequestId>b6f8679c-925c-4f7f-ac56-ac0a7b7cabee</RequestId>
  </ResponseMetadata>
</CreatePolicyResponse>
```

### 3. AttachUserPolicy

Attaching returns an empty result.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120456Z
Content-Length: 136
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AttachUserPolicy
&Version=2010-05-08
&UserName=docs-demo-user
&PolicyArn=arn%3Aaws%3Aiam%3A%3A000000000000%3Apolicy%2Fdocs-demo-read
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 248
date: Mon, 16 Nov 2026 12:04:56 GMT

<AttachUserPolicyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <AttachUserPolicyResult/>
  <ResponseMetadata>
    <RequestId>5d150358-642c-4dd7-a8cc-f34793f26a33</RequestId>
  </ResponseMetadata>
</AttachUserPolicyResponse>
```

### 4. ListAttachedUserPolicies

The attachment is visible on the user.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120456Z
Content-Length: 74
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListAttachedUserPolicies
&Version=2010-05-08
&UserName=docs-demo-user
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 525
date: Mon, 16 Nov 2026 12:04:56 GMT

<ListAttachedUserPoliciesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListAttachedUserPoliciesResult>
    <AttachedPolicies>
      <member xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
        <PolicyName>docs-demo-read</PolicyName>
        <PolicyArn>arn:aws:iam::000000000000:policy/docs-demo-read</PolicyArn>
      </member>
    </AttachedPolicies>
    <IsTruncated>false</IsTruncated>
  </ListAttachedUserPoliciesResult>
  <ResponseMetadata>
    <RequestId>be2400e5-df24-4453-94f9-7306d3336465</RequestId>
  </ResponseMetadata>
</ListAttachedUserPoliciesResponse>
```

### 5. CreateAccessKey

The secret access key is only ever returned here; it is redacted on this page.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120456Z
Content-Length: 65
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateAccessKey
&Version=2010-05-08
&UserName=docs-demo-user
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 546
date: Mon, 16 Nov 2026 12:04:56 GMT

<CreateAccessKeyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateAccessKeyResult>
    <AccessKey xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <UserName>docs-demo-user</UserName>
      <AccessKeyId>EMULATORAKJPV4TEAMUJ</AccessKeyId>
      <Status>Active</Status>
      <SecretAccessKey><redacted></SecretAccessKey>
      <CreateDate>2026-11-16T12:04:56.506Z</CreateDate>
    </AccessKey>
  </CreateAccessKeyResult>
  <ResponseMetadata>
    <RequestId>b796b5a7-4f01-43eb-ad0f-d867c4c34c5e</RequestId>
  </ResponseMetadata>
</CreateAccessKeyResponse>
```

### 6. DeleteUser

A user that still has keys and policies cannot be deleted: 409 `DeleteConflict`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120456Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteUser
&Version=2010-05-08
&UserName=docs-demo-user
```

**Response**

```http
HTTP/1.1 409 Conflict
content-type: text/xml
x-amzn-requestid: 61f8b769-4e45-4aae-9838-393830a475ce
content-length: 273
date: Mon, 16 Nov 2026 12:04:56 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>DeleteConflict</Code>
    <Message>Cannot delete entity, must detach all policies first.</Message>
  </Error>
  <RequestId>61f8b769-4e45-4aae-9838-393830a475ce</RequestId>
</ErrorResponse>
```

### 7. DeleteAccessKey

Remove the key ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120456Z
Content-Length: 98
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteAccessKey
&Version=2010-05-08
&UserName=docs-demo-user
&AccessKeyId=EMULATORAKJPV4TEAMUJ
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 244
date: Mon, 16 Nov 2026 12:04:56 GMT

<DeleteAccessKeyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteAccessKeyResult/>
  <ResponseMetadata>
    <RequestId>608fcb7c-d1d2-48dc-a1d7-f0b43e249e2f</RequestId>
  </ResponseMetadata>
</DeleteAccessKeyResponse>
```

### 8. DetachUserPolicy

... detach the policy ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120456Z
Content-Length: 136
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DetachUserPolicy
&Version=2010-05-08
&UserName=docs-demo-user
&PolicyArn=arn%3Aaws%3Aiam%3A%3A000000000000%3Apolicy%2Fdocs-demo-read
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 248
date: Mon, 16 Nov 2026 12:04:56 GMT

<DetachUserPolicyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DetachUserPolicyResult/>
  <ResponseMetadata>
    <RequestId>8c085ecc-746c-4602-8046-82937906e6cb</RequestId>
  </ResponseMetadata>
</DetachUserPolicyResponse>
```

### 9. DeletePolicy

... delete it ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120456Z
Content-Length: 108
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeletePolicy
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3A%3A000000000000%3Apolicy%2Fdocs-demo-read
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 232
date: Mon, 16 Nov 2026 12:04:56 GMT

<DeletePolicyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeletePolicyResult/>
  <ResponseMetadata>
    <RequestId>ad5cb914-42d9-41c9-958c-d0bf108e381c</RequestId>
  </ResponseMetadata>
</DeletePolicyResponse>
```

### 10. DeleteUser

... and now the user can be deleted.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120456Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteUser
&Version=2010-05-08
&UserName=docs-demo-user
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 224
date: Mon, 16 Nov 2026 12:04:56 GMT

<DeleteUserResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteUserResult/>
  <ResponseMetadata>
    <RequestId>d2ee6268-8fb2-44d0-a34b-c62c076429a4</RequestId>
  </ResponseMetadata>
</DeleteUserResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 180 operations, 35 accepted the placeholder call, 4 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### AcceptDelegationRequest

Accepts a delegation request, granting the requested temporary access.

```http
POST / HTTP/1.1

Action=AcceptDelegationRequest
&Version=2010-05-08
&DelegationRequestId=example000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>DelegationRequest not found: example000000000</Message>
  </Error>
  <RequestId>e0b8e359-0acf-44f5-81a1-9fdad76eb0ea</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AcquireRole

Creates an IAM role from the specified role template.

```http
POST / HTTP/1.1

Action=AcquireRole
&Version=2010-05-08
&TemplateArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Role template not found: arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>dbbbba55-8747-40f8-9d6a-3d51980212bd</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AddClientIDToOpenIDConnectProvider

Adds a new client ID (also known as audience) to the list of client IDs already registered for the specified IAM OpenID Connect (OIDC) provider resource.

```http
POST / HTTP/1.1

Action=AddClientIDToOpenIDConnectProvider
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&ClientID=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenIDConnect Provider not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>ff6df48b-8a22-4114-a346-1874791129f7</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AddRoleToInstanceProfile

Adds the specified IAM role to the specified instance profile.

```http
POST / HTTP/1.1

Action=AddRoleToInstanceProfile
&Version=2010-05-08
&InstanceProfileName=example
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Instance Profile example cannot be found.</Message>
  </Error>
  <RequestId>bd251feb-aff5-43da-abfe-ec624815a84d</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AddUserToGroup

Adds the specified user to the specified group.

```http
POST / HTTP/1.1

Action=AddUserToGroup
&Version=2010-05-08
&GroupName=example
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The group with name example cannot be found.</Message>
  </Error>
  <RequestId>92f6d44b-612e-4fdc-b937-c0ada5250cac</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AssociateDelegationRequest

Associates a delegation request with the current identity.

```http
POST / HTTP/1.1

Action=AssociateDelegationRequest
&Version=2010-05-08
&DelegationRequestId=example000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Delegation request not found: example000000000</Message>
  </Error>
  <RequestId>1a40d9d1-50e9-4b3d-ac1e-a4ca423aedb0</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AttachGroupPolicy

Attaches the specified managed policy to the specified IAM group.

```http
POST / HTTP/1.1

Action=AttachGroupPolicy
&Version=2010-05-08
&GroupName=example
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>c29f9772-30a8-4d09-8a74-caf2dc0af15d</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### AttachRolePolicy

Attaches the specified managed policy to the specified IAM role.

```http
POST / HTTP/1.1

Action=AttachRolePolicy
&Version=2010-05-08
&RoleName=example
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>334b3bbc-f7a4-43d0-ad01-05edd3d34b48</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### AttachUserPolicy

Attaches the specified managed policy to the specified user.

```http
POST / HTTP/1.1

Action=AttachUserPolicy
&Version=2010-05-08
&UserName=example
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>35f1e8d3-fadc-4501-8033-86c222a2808d</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-attachuserpolicy) for a successful call.

### ChangePassword

Changes the password of the IAM user who is calling this operation.

```http
POST / HTTP/1.1

Action=ChangePassword
&Version=2010-05-08
&OldPassword=example
&NewPassword=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Login Profile for User emulator cannot be found.</Message>
  </Error>
  <RequestId>21c8191c-9879-4749-8751-9fe2425ffb9a</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateAccessKey

Creates a new Amazon Web Services secret access key and corresponding Amazon Web Services access key ID for the specified user.

```http
POST / HTTP/1.1

Action=CreateAccessKey
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<CreateAccessKeyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateAccessKeyResult>
    <AccessKey xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <UserName>emulator</UserName>
      <AccessKeyId>EMULATORAKKYPDOTCMUU</AccessKeyId>
      <Status>Active</Status>
      <SecretAccessKey><redacted></SecretAccessKey>
      <CreateDate>2026-11-16T12:11:28.317Z</CreateDate>
    </AccessKey>
  </CreateAccessKeyResult>
  <ResponseMetadata>
    <RequestId>6bfd3238-a3eb-4914-82bf-50bce7764947</RequestId>
  </ResponseMetadata>
</CreateAccessKeyResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#5-createaccesskey) for a successful call.

### CreateAccountAlias

Creates an alias for your Amazon Web Services account.

```http
POST / HTTP/1.1

Action=CreateAccountAlias
&Version=2010-05-08
&AccountAlias=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<CreateAccountAliasResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateAccountAliasResult/>
  <ResponseMetadata>
    <RequestId>cdedaee2-eba5-4dc4-9801-b513045cc9d4</RequestId>
  </ResponseMetadata>
</CreateAccountAliasResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateDelegationRequest

Creates an IAM delegation request for temporary access delegation.

```http
POST / HTTP/1.1

Action=CreateDelegationRequest
&Version=2010-05-08
&Description=example
&RequestorWorkflowId=example
&NotificationChannel=example
&SessionDuration=300
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value at 'permissions' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>39414226-7ed4-4a97-aa75-d1fdb4e8853b</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CreateGroup

Creates a new group.

```http
POST / HTTP/1.1

Action=CreateGroup
&Version=2010-05-08
&GroupName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<CreateGroupResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateGroupResult>
    <Group xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <GroupName>example</GroupName>
      <GroupId>AGPABGXEQES5NRCCJJWAN</GroupId>
      <Arn>arn:aws:iam::000000000000:group/example</Arn>
      <CreateDate>2026-11-16T12:11:28.327Z</CreateDate>
    </Group>
  </CreateGroupResult>
  <ResponseMetadata>
    <RequestId>db62cbf3-d882-42d8-b841-f189a62a23b2</RequestId>
  </ResponseMetadata>
</CreateGroupResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateInstanceProfile

Creates a new instance profile.

```http
POST / HTTP/1.1

Action=CreateInstanceProfile
&Version=2010-05-08
&InstanceProfileName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<CreateInstanceProfileResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateInstanceProfileResult>
    <InstanceProfile xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <InstanceProfileName>example</InstanceProfileName>
      <InstanceProfileId>AIPABGXEQES5NRCCJJWAN</InstanceProfileId>
      <Arn>arn:aws:iam::000000000000:instance-profile/example</Arn>
      <CreateDate>2026-11-16T12:11:28.332Z</CreateDate>
      <Roles/>
    </InstanceProfile>
  </CreateInstanceProfileResult>
  <ResponseMetadata>
    <RequestId>6e0f2529-0698-45da-b758-7af289351662</RequestId>
  </ResponseMetadata>
</CreateInstanceProfileResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateLoginProfile

Creates a password for the specified IAM user.

```http
POST / HTTP/1.1

Action=CreateLoginProfile
&Version=2010-05-08
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Password must be specified unless the request is made with an AssumeRoot session</Message>
  </Error>
  <RequestId>93df7faa-1cd4-49c2-8a29-33405b424d9c</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CreateOpenIDConnectProvider

Creates an IAM entity to describe an identity provider (IdP) that supports OpenID Connect (OIDC) .

```http
POST / HTTP/1.1

Action=CreateOpenIDConnectProvider
&Version=2010-05-08
&Url=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Invalid Url example: the URL must begin with https://</Message>
  </Error>
  <RequestId>82830482-9911-4759-b1e2-965e284924fd</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### CreatePolicy

Creates a new managed policy for your Amazon Web Services account.

```http
POST / HTTP/1.1

Action=CreatePolicy
&Version=2010-05-08
&PolicyName=example
&PolicyDocument=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>MalformedPolicyDocument</Code>
    <Message>Syntax errors in policy.</Message>
  </Error>
  <RequestId>8f3df264-77e3-44d2-9ec2-cbd0822897a0</RequestId>
</ErrorResponse>
```

`400` `MalformedPolicyDocument`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-createpolicy) for a successful call.

### CreatePolicyVersion

Creates a new version of the specified managed policy.

```http
POST / HTTP/1.1

Action=CreatePolicyVersion
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&PolicyDocument=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>339c5a61-89a7-4e7b-8e5c-e55d2a208b88</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### CreateRole

Creates a new role for your Amazon Web Services account.

```http
POST / HTTP/1.1

Action=CreateRole
&Version=2010-05-08
&RoleName=example
&AssumeRolePolicyDocument=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>MalformedPolicyDocument</Code>
    <Message>This policy contains invalid Json</Message>
  </Error>
  <RequestId>731b3a6f-415d-420c-8713-eee42de24b9c</RequestId>
</ErrorResponse>
```

`400` `MalformedPolicyDocument`: the placeholder values failed the operation's input validation.

### CreateSAMLProvider

Creates an IAM resource that describes an identity provider (IdP) that supports SAML 2.0.

```http
POST / HTTP/1.1

Action=CreateSAMLProvider
&Version=2010-05-08
&SAMLMetadataDocument=example000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
&Name=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Invalid SAML metadata document</Message>
  </Error>
  <RequestId>613b4378-027d-49a6-8d62-d6bec66461e0</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### CreateServiceLinkedRole

Creates an IAM role that is linked to a specific Amazon Web Services service.

```http
POST / HTTP/1.1

Action=CreateServiceLinkedRole
&Version=2010-05-08
&AWSServiceName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>AWSServiceName must be a service principal, such as elasticbeanstalk.amazonaws.com; got example</Message>
  </Error>
  <RequestId>b1a24c1b-ebb7-4724-bfb6-6da241c5bfba</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### CreateServiceSpecificCredential

Generates a set of credentials consisting of a user name and password that can be used to access the service specified in the request.

```http
POST / HTTP/1.1

Action=CreateServiceSpecificCredential
&Version=2010-05-08
&UserName=example
&ServiceName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NotSupportedService</Code>
    <Message>The service example does not support service-specific credentials.</Message>
  </Error>
  <RequestId>b2583604-3602-49f7-a5d7-e9e567b33c1d</RequestId>
</ErrorResponse>
```

`404` `NotSupportedService`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateUser

Creates a new IAM user for your Amazon Web Services account.

```http
POST / HTTP/1.1

Action=CreateUser
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<CreateUserResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateUserResult>
    <User xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <UserName>example</UserName>
      <UserId>AIDAOWVOJOY3VBWOJCSHY</UserId>
      <Arn>arn:aws:iam::000000000000:user/example</Arn>
      <CreateDate>2026-11-16T12:11:28.358Z</CreateDate>
    </User>
  </CreateUserResult>
  <ResponseMetadata>
    <RequestId>1ee488a3-0fe9-4747-8bec-2afa475eb14f</RequestId>
  </ResponseMetadata>
</CreateUserResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#1-createuser) for a successful call.

### CreateVirtualMFADevice

Creates a new virtual MFA device for the Amazon Web Services account.

```http
POST / HTTP/1.1

Action=CreateVirtualMFADevice
&Version=2010-05-08
&VirtualMFADeviceName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<CreateVirtualMFADeviceResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateVirtualMFADeviceResult>
    <VirtualMFADevice xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <SerialNumber>arn:aws:iam::000000000000:mfa/example</SerialNumber>
      <Base32StringSeed><redacted></Base32StringSeed>
      <QRCodePNG><redacted></QRCodePNG>
    </VirtualMFADevice>
  </CreateVirtualMFADeviceResult>
  <ResponseMetadata>
    <RequestId>6eaa5cb7-e9b4-4f8d-84b1-fdf0df16113d</RequestId>
  </ResponseMetadata>
</CreateVirtualMFADeviceResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeactivateMFADevice

Deactivates the specified MFA device and removes it from association with the user name for which it was originally enabled.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DeactivateMFADevice
&Version=2010-05-08
&SerialNumber=example00
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>MFA Device with serial number example00 does not exist.</Message>
  </Error>
  <RequestId>ba18e7d1-a56b-462e-8df6-19638597bd1f</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteAccessKey

Deletes the access key pair associated with the specified IAM user.

```http
POST / HTTP/1.1

Action=DeleteAccessKey
&Version=2010-05-08
&AccessKeyId=example000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Access Key with id example000000000 cannot be found.</Message>
  </Error>
  <RequestId>df855cb9-7118-4d60-9084-01915114b8a3</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#7-deleteaccesskey) for a successful call.

### DeleteAccountAlias

Deletes the specified Amazon Web Services account alias.

```http
POST / HTTP/1.1

Action=DeleteAccountAlias
&Version=2010-05-08
&AccountAlias=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DeleteAccountAliasResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteAccountAliasResult/>
  <ResponseMetadata>
    <RequestId>c1e9454e-795d-4840-a614-b5b6a87d726d</RequestId>
  </ResponseMetadata>
</DeleteAccountAliasResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteAccountPasswordPolicy

Deletes the password policy for the Amazon Web Services account.

```http
POST / HTTP/1.1

Action=DeleteAccountPasswordPolicy
&Version=2010-05-08
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The account policy with name PasswordPolicy cannot be found.</Message>
  </Error>
  <RequestId>4b8bcd96-c446-4e05-a5d7-8edbe77aa748</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteGroup

Deletes the specified IAM group.

```http
POST / HTTP/1.1

Action=DeleteGroup
&Version=2010-05-08
&GroupName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DeleteGroupResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteGroupResult/>
  <ResponseMetadata>
    <RequestId>066c02d7-c484-45c1-bc6b-e038726989ab</RequestId>
  </ResponseMetadata>
</DeleteGroupResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteGroupPolicy

Deletes the specified inline policy that is embedded in the specified IAM group.

```http
POST / HTTP/1.1

Action=DeleteGroupPolicy
&Version=2010-05-08
&GroupName=example
&PolicyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The group with name example cannot be found.</Message>
  </Error>
  <RequestId>3c52e516-2f92-414c-ae2e-35d70f4f2376</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteInstanceProfile

Deletes the specified instance profile.

```http
POST / HTTP/1.1

Action=DeleteInstanceProfile
&Version=2010-05-08
&InstanceProfileName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DeleteInstanceProfileResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteInstanceProfileResult/>
  <ResponseMetadata>
    <RequestId>0fafb80b-c1a9-41b1-a7a9-2e0f4eb80e9b</RequestId>
  </ResponseMetadata>
</DeleteInstanceProfileResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteLoginProfile

Deletes the password for the specified IAM user or root user, For more information, see Managing passwords for IAM users .

```http
POST / HTTP/1.1

Action=DeleteLoginProfile
&Version=2010-05-08
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Login Profile for User emulator cannot be found.</Message>
  </Error>
  <RequestId>2e4cd585-06f0-47fb-bb38-50879b05a256</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteOpenIDConnectProvider

Deletes an OpenID Connect identity provider (IdP) resource object in IAM.

```http
POST / HTTP/1.1

Action=DeleteOpenIDConnectProvider
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenId connect Provider arn:aws:iam:us-east-1:000000000000:example/example cannot be found.</Message>
  </Error>
  <RequestId>23da86e6-3264-4936-8521-af0a060d47b6</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeletePolicy

Deletes the specified managed policy.

```http
POST / HTTP/1.1

Action=DeletePolicy
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>45d1b4b7-b300-4ae9-90d5-e1a379fa866d</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation. See [the walkthrough](#9-deletepolicy) for a successful call.

### DeletePolicyVersion

Deletes the specified version from the specified managed policy.

```http
POST / HTTP/1.1

Action=DeletePolicyVersion
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&VersionId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value at 'versionId' failed to satisfy constraint: Member must satisfy regular expression pattern: v[1-9][0-9]*(\.[A-Za-z0-9-]*)?</Message>
  </Error>
  <RequestId>55106f1b-1319-4e5c-a00f-77a97e052263</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteRole

Deletes the specified role.

```http
POST / HTTP/1.1

Action=DeleteRole
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>46f3af1c-e21c-403b-a4c9-bd9876524c80</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteRolePermissionsBoundary

Deletes the permissions boundary for the specified IAM role.

```http
POST / HTTP/1.1

Action=DeleteRolePermissionsBoundary
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>51d41da7-63a2-4882-9523-fc2237b64828</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteRolePolicy

Deletes the specified inline policy that is embedded in the specified IAM role.

```http
POST / HTTP/1.1

Action=DeleteRolePolicy
&Version=2010-05-08
&RoleName=example
&PolicyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>89320ab2-6dd4-4cb5-aefd-adad7f2eaa1f</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteSAMLProvider

Deletes a SAML provider resource in IAM.

```http
POST / HTTP/1.1

Action=DeleteSAMLProvider
&Version=2010-05-08
&SAMLProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Manifest not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>53fe7d6e-b642-487f-ba55-b34d51e12f8a</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteSSHPublicKey

Deletes the specified SSH public key.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DeleteSSHPublicKey
&Version=2010-05-08
&UserName=example
&SSHPublicKeyId=example0000000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>259b317b-aaf5-4d37-b4e0-90b2ae77ac64</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteServerCertificate

Deletes the specified server certificate.

```http
POST / HTTP/1.1

Action=DeleteServerCertificate
&Version=2010-05-08
&ServerCertificateName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Server Certificate with name example cannot be found.</Message>
  </Error>
  <RequestId>e12b6f00-708c-462a-a8d3-680779546e46</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteServiceLinkedRole

Submits a service-linked role deletion request and returns a DeletionTaskId , which you can use to check the status of the deletion.

```http
POST / HTTP/1.1

Action=DeleteServiceLinkedRole
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Cannot find the service role to delete.</Message>
  </Error>
  <RequestId>940cc6a6-26f4-4269-846b-2a5b455bb326</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteServiceSpecificCredential

Deletes the specified service-specific credential.

```http
POST / HTTP/1.1

Action=DeleteServiceSpecificCredential
&Version=2010-05-08
&ServiceSpecificCredentialId=example0000000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>No such credential example0000000000000 exists</Message>
  </Error>
  <RequestId>f8528305-63c7-43e1-8152-91591e47bfd5</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteSigningCertificate

Deletes a signing certificate associated with the specified IAM user.

```http
POST / HTTP/1.1

Action=DeleteSigningCertificate
&Version=2010-05-08
&CertificateId=example00000000000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Certificate with id example00000000000000000 cannot be found.</Message>
  </Error>
  <RequestId>d398a5aa-b66e-4c30-8164-451e24729864</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteUser

Deletes the specified IAM user.

```http
POST / HTTP/1.1

Action=DeleteUser
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DeleteUserResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteUserResult/>
  <ResponseMetadata>
    <RequestId>034070ff-c341-4b56-9bba-6fa94204c8c5</RequestId>
  </ResponseMetadata>
</DeleteUserResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#10-deleteuser) for a successful call.

### DeleteUserPermissionsBoundary

Deletes the permissions boundary for the specified IAM user.

```http
POST / HTTP/1.1

Action=DeleteUserPermissionsBoundary
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>User example was not found.</Message>
  </Error>
  <RequestId>0c35b55c-f5ec-49eb-a483-2c418042e36b</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteUserPolicy

Deletes the specified inline policy that is embedded in the specified IAM user.

```http
POST / HTTP/1.1

Action=DeleteUserPolicy
&Version=2010-05-08
&UserName=example
&PolicyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>af1b509f-05c5-448e-8913-045f9263b6d6</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteVirtualMFADevice

Deletes a virtual MFA device.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DeleteVirtualMFADevice
&Version=2010-05-08
&SerialNumber=example00
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>MFA Device with serial number example00 does not exist.</Message>
  </Error>
  <RequestId>9868a807-83bc-4af4-9f8c-1ed34ac78d0f</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DetachGroupPolicy

Removes the specified managed policy from the specified IAM group.

```http
POST / HTTP/1.1

Action=DetachGroupPolicy
&Version=2010-05-08
&GroupName=example
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>91246f88-f1d1-46ab-95f8-fad2481c6299</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### DetachRolePolicy

Removes the specified managed policy from the specified role.

```http
POST / HTTP/1.1

Action=DetachRolePolicy
&Version=2010-05-08
&RoleName=example
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>da452913-5388-45c2-85cd-e3ba5f1235a7</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### DetachUserPolicy

Removes the specified managed policy from the specified user.

```http
POST / HTTP/1.1

Action=DetachUserPolicy
&Version=2010-05-08
&UserName=example
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>ba6d6228-69d6-411d-aec2-e32d0456bc76</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation. See [the walkthrough](#8-detachuserpolicy) for a successful call.

### DisableOrganizationsRootCredentialsManagement

Disables the management of privileged root user credentials across member accounts in your organization.

```http
POST / HTTP/1.1

Action=DisableOrganizationsRootCredentialsManagement
&Version=2010-05-08
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DisableOrganizationsRootCredentialsManagement is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>0ba05086-0959-4c87-ba0e-d436d57d9762</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisableOrganizationsRootSessions

Disables root user sessions for privileged tasks across member accounts in your organization.

```http
POST / HTTP/1.1

Action=DisableOrganizationsRootSessions
&Version=2010-05-08
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DisableOrganizationsRootSessions is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>7806a884-3c02-468f-b534-6faceff160de</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisableOutboundWebIdentityFederation

Disables the outbound identity federation feature for your Amazon Web Services account.

```http
POST / HTTP/1.1

Action=DisableOutboundWebIdentityFederation
&Version=2010-05-08
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>FeatureDisabled</Code>
    <Message>Outbound identity federation is already disabled for account 000000000000</Message>
  </Error>
  <RequestId>ec096dfc-c9d4-4fd5-a3f4-b63fde74f007</RequestId>
</ErrorResponse>
```

`404` `FeatureDisabled`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### EnableMFADevice

Enables the specified MFA device and associates it with the specified IAM user.

```http
POST / HTTP/1.1

Action=EnableMFADevice
&Version=2010-05-08
&UserName=example
&SerialNumber=example00
&AuthenticationCode1=100000
&AuthenticationCode2=100000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>7a5f7f53-9652-4742-b931-16d9015287cd</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### EnableOrganizationsRootCredentialsManagement

Enables the management of privileged root user credentials across member accounts in your organization.

```http
POST / HTTP/1.1

Action=EnableOrganizationsRootCredentialsManagement
&Version=2010-05-08
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation EnableOrganizationsRootCredentialsManagement is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>c6b4d1dd-acb3-41b1-9586-2e59f7ae2819</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableOrganizationsRootSessions

Allows the management account or delegated administrator to perform privileged tasks on member accounts in your organization.

```http
POST / HTTP/1.1

Action=EnableOrganizationsRootSessions
&Version=2010-05-08
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation EnableOrganizationsRootSessions is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>ff171e7a-bc1f-4cfd-8649-0ae875979e83</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableOutboundWebIdentityFederation

Enables the outbound identity federation feature for your Amazon Web Services account.

```http
POST / HTTP/1.1

Action=EnableOutboundWebIdentityFederation
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<EnableOutboundWebIdentityFederationResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <EnableOutboundWebIdentityFederationResult>
    <IssuerIdentifier>https://127.0.0.1:9297/F7B11509F4D675C3C44F0DD37CA830BB</IssuerIdentifier>
  </EnableOutboundWebIdentityFederationResult>
  <ResponseMetadata>
    <RequestId>9aa83304-11fd-4e0f-bd20-35fd9e16399d</RequestId>
  </ResponseMetadata>
</EnableOutboundWebIdentityFederationResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GenerateCredentialReport

Generates a credential report for the Amazon Web Services account.

```http
POST / HTTP/1.1

Action=GenerateCredentialReport
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<GenerateCredentialReportResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <GenerateCredentialReportResult>
    <State>STARTED</State>
    <Description>No report exists. Starting a new report generation task</Description>
  </GenerateCredentialReportResult>
  <ResponseMetadata>
    <RequestId>9bc76f9c-d601-4c9c-9a64-f5300750383f</RequestId>
  </ResponseMetadata>
</GenerateCredentialReportResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GenerateOrganizationsAccessReport

Generates a report for service last accessed data for Organizations.

```http
POST / HTTP/1.1

Action=GenerateOrganizationsAccessReport
&Version=2010-05-08
&EntityPath=example000000000000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value at 'entityPath' failed to satisfy constraint: Member must satisfy regular expression pattern: o-[0-9a-z]{10,32}\/r-[0-9a-z]{4,32}[0-9a-z-\/]*</Message>
  </Error>
  <RequestId>337196ae-ee83-4346-8e8c-d26f6c0363b9</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GenerateServiceLastAccessedDetails

Generates a report that includes details about when an IAM resource (user, group, role, or policy) was last used in an attempt to access Amazon Web Services services.

```http
POST / HTTP/1.1

Action=GenerateServiceLastAccessedDetails
&Version=2010-05-08
&Arn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid: Arn must be the ARN of an IAM user, group, role or managed policy</Message>
  </Error>
  <RequestId>a967960b-9389-4393-bf01-0a367545cf02</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### GetAccessKeyLastUsed

Retrieves information about when the specified access key was last used.

```http
POST / HTTP/1.1

Action=GetAccessKeyLastUsed
&Version=2010-05-08
&AccessKeyId=example000000000
```

```http
HTTP/1.1 403 Forbidden
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>AccessDenied</Code>
    <Message>User: arn:aws:iam::000000000000:user/emulator is not authorized to perform iam:GetAccessKeyLastUsed on resource: access key example000000000</Message>
  </Error>
  <RequestId>910efe6a-5bce-4cc3-a540-8aafeaf7a2bc</RequestId>
</ErrorResponse>
```

`403` `AccessDenied`: the request was refused by authorization.

### GetAccountAuthorizationDetails

Retrieves information about all IAM users, groups, roles, and policies in your Amazon Web Services account, including their relationships to one another.

```http
POST / HTTP/1.1

Action=GetAccountAuthorizationDetails
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<GetAccountAuthorizationDetailsResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <GetAccountAuthorizationDetailsResult>
    <UserDetailList>
      <member xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
        <Path>/</Path>
        <UserName>emulator</UserName>
        <UserId>AIDAH5I7GEMO7ADXNFAZU</UserId>
        <Arn>arn:aws:iam::000000000000:user/emulator</Arn>
        <CreateDate>1970-01-01T00:00:00Z</CreateDate>
        <UserPolicyList/>
        <GroupList/>
        <AttachedManagedPolicies/>
      </member>
    </UserDetailList>
    <GroupDetailList/>
    <RoleDetailList>
      <member>
        <Path>/aws-service-role/dynamodb.application-autoscaling.amazonaws.com/</Path>
        <RoleName>AWSServiceRoleForApplicationAutoScaling_DynamoDBTable</RoleName>
        <RoleId>AROAN7DDQNT4UYC2BHD35</RoleId>
… (20 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetAccountPasswordPolicy

Retrieves the password policy for the Amazon Web Services account.

```http
POST / HTTP/1.1

Action=GetAccountPasswordPolicy
&Version=2010-05-08
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Password Policy with domain name 000000000000 cannot be found.</Message>
  </Error>
  <RequestId>828c22bd-fb5e-4761-872b-dd6095f56f8d</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetAccountProperties

Retrieves the account-level properties for the caller's Amazon Web Services account.

```http
POST / HTTP/1.1

Action=GetAccountProperties
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<GetAccountPropertiesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <GetAccountPropertiesResult>
    <Properties>
      <entry>
        <key>RoleManager/Enabled</key>
        <value>false</value>
      </entry>
    </Properties>
  </GetAccountPropertiesResult>
  <ResponseMetadata>
    <RequestId>65af76db-a890-43b5-90b0-164eda9051b5</RequestId>
  </ResponseMetadata>
</GetAccountPropertiesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetAccountSummary

Retrieves information about IAM entity usage and IAM quotas in the Amazon Web Services account.

```http
POST / HTTP/1.1

Action=GetAccountSummary
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<GetAccountSummaryResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <GetAccountSummaryResult>
    <SummaryMap>
      <entry>
        <key>GlobalEndpointTokenVersion</key>
        <value>1</value>
      </entry>
      <entry>
        <key>Policies</key>
        <value>0</value>
      </entry>
      <entry>
        <key>VersionsPerPolicyQuota</key>
        <value>5</value>
      </entry>
      <entry>
        <key>PoliciesQuota</key>
        <value>1500</value>
      </entry>
      <entry>
… (125 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetContextKeysForCustomPolicy

Gets a list of all of the context keys referenced in the input policies.

```http
POST / HTTP/1.1

Action=GetContextKeysForCustomPolicy
&Version=2010-05-08
&PolicyInputList.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Policy input list item 1 has invalid content</Message>
  </Error>
  <RequestId>55295160-991f-40ce-968b-860dc83b41c2</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### GetContextKeysForPrincipalPolicy

Gets a list of all of the context keys referenced in all the IAM policies that are attached to the specified IAM entity.

```http
POST / HTTP/1.1

Action=GetContextKeysForPrincipalPolicy
&Version=2010-05-08
&PolicySourceArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Invalid ARN provided in the request: arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>150b7a5a-a735-4ec2-a33c-61602b96a64c</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### GetCredentialReport

Retrieves a credential report for the Amazon Web Services account.

```http
POST / HTTP/1.1

Action=GetCredentialReport
&Version=2010-05-08
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ReportInProgress</Code>
  </Error>
  <RequestId>54ebe43c-690d-45e6-b4d3-8b8502d6a6c0</RequestId>
</ErrorResponse>
```

`404` `ReportInProgress`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetDelegationRequest

Retrieves information about a specific delegation request.

```http
POST / HTTP/1.1

Action=GetDelegationRequest
&Version=2010-05-08
&DelegationRequestId=example000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>DelegationRequest not found: example000000000</Message>
  </Error>
  <RequestId>a7c9e883-cbdd-4572-a4dd-412f41e85792</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetGroup

Returns a list of IAM users that are in the specified IAM group.

```http
POST / HTTP/1.1

Action=GetGroup
&Version=2010-05-08
&GroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The group with name example cannot be found.</Message>
  </Error>
  <RequestId>715cbd3b-9b99-441f-85d2-af2dc8c1be84</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetGroupPolicy

Retrieves the specified inline policy document that is embedded in the specified IAM group.

```http
POST / HTTP/1.1

Action=GetGroupPolicy
&Version=2010-05-08
&GroupName=example
&PolicyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The group with name example cannot be found.</Message>
  </Error>
  <RequestId>f27954a2-e705-465b-b31c-a4f2c0508b66</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetHumanReadableSummary

Retrieves a human readable summary for a given entity.

```http
POST / HTTP/1.1

Action=GetHumanReadableSummary
&Version=2010-05-08
&EntityArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Invalid resource type in ARN</Message>
  </Error>
  <RequestId>6dea5fdf-2e88-4d0c-b030-d86d862fb2d1</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetInstanceProfile

Retrieves information about the specified instance profile, including the instance profile's path, GUID, ARN, and role.

```http
POST / HTTP/1.1

Action=GetInstanceProfile
&Version=2010-05-08
&InstanceProfileName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Instance Profile example cannot be found.</Message>
  </Error>
  <RequestId>cac29dbd-007f-4bc4-897e-08a84eb56a77</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetLoginProfile

Retrieves the user name for the specified IAM user.

```http
POST / HTTP/1.1

Action=GetLoginProfile
&Version=2010-05-08
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Login Profile for User emulator cannot be found.</Message>
  </Error>
  <RequestId>94c1a329-db5b-4a0c-b61a-076251c975fc</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetMFADevice

Retrieves information about an MFA device for a specified user.

```http
POST / HTTP/1.1

Action=GetMFADevice
&Version=2010-05-08
&SerialNumber=example00
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>The MFA device serial number sent in the request is invalid or not supported by this API. Currently, only hardware FIDO security keys are supported. Please use the compatible device or API.</Message>
  </Error>
  <RequestId>d607b0b5-3cef-4886-b075-d037bfa1e28a</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetOpenIDConnectProvider

Returns information about the specified OpenID Connect (OIDC) provider resource object in IAM.

```http
POST / HTTP/1.1

Action=GetOpenIDConnectProvider
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenIDConnect Provider not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>7c9adf50-ca64-42e5-866d-6a1c81dff3c4</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetOrganizationsAccessReport

Retrieves the service last accessed data report for Organizations that was previously generated using the GenerateOrganizationsAccessReport operation.

```http
POST / HTTP/1.1

Action=GetOrganizationsAccessReport
&Version=2010-05-08
&JobId=example00000000000000000000000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Job example00000000000000000000000000000 does not exist.</Message>
  </Error>
  <RequestId>b8d980d4-bcd7-4a99-9c35-c9f72449132e</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetOutboundWebIdentityFederationInfo

Retrieves the configuration information for the outbound identity federation feature in your Amazon Web Services account.

```http
POST / HTTP/1.1

Action=GetOutboundWebIdentityFederationInfo
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<GetOutboundWebIdentityFederationInfoResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <GetOutboundWebIdentityFederationInfoResult>
    <IssuerIdentifier>https://127.0.0.1:9297/F7B11509F4D675C3C44F0DD37CA830BB</IssuerIdentifier>
    <JwtVendingEnabled>true</JwtVendingEnabled>
  </GetOutboundWebIdentityFederationInfoResult>
  <ResponseMetadata>
    <RequestId>47004837-71a1-49a7-904b-250a2422e2a1</RequestId>
  </ResponseMetadata>
</GetOutboundWebIdentityFederationInfoResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetPolicy

Retrieves information about the specified managed policy, including the policy's default version and the total number of IAM users, groups, and roles to which the policy is attached.

```http
POST / HTTP/1.1

Action=GetPolicy
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>15732a99-9928-4f4c-b293-57f721995614</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### GetPolicyVersion

Retrieves information about the specified version of the specified managed policy, including the policy document.

```http
POST / HTTP/1.1

Action=GetPolicyVersion
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&VersionId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value at 'versionId' failed to satisfy constraint: Member must satisfy regular expression pattern: v[1-9][0-9]*(\.[A-Za-z0-9-]*)?</Message>
  </Error>
  <RequestId>f72016a0-4ce9-4916-b6c7-52eccd501f33</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetRole

Retrieves information about the specified role, including the role's path, GUID, ARN, and the role's trust policy that grants permission to assume the role.

```http
POST / HTTP/1.1

Action=GetRole
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>425ddec6-aaee-4497-9d41-45de47cea250</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetRolePolicy

Retrieves the specified inline policy document that is embedded with the specified IAM role.

```http
POST / HTTP/1.1

Action=GetRolePolicy
&Version=2010-05-08
&RoleName=example
&PolicyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>e1df5e21-1b10-4679-a97e-3d6fc0c18b09</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetRoleTemplateVersion

Retrieves information about a version of the specified role template.

```http
POST / HTTP/1.1

Action=GetRoleTemplateVersion
&Version=2010-05-08
&TemplateArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Invalid resource type in ARN</Message>
  </Error>
  <RequestId>72648e84-9ce5-4bae-bfea-bf3ff4d3fd80</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetSAMLProvider

Returns the SAML provider metadocument that was uploaded when the IAM SAML provider resource object was created or updated.

```http
POST / HTTP/1.1

Action=GetSAMLProvider
&Version=2010-05-08
&SAMLProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Manifest not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>0fb0806b-489a-448e-8cbc-1f665bc1a874</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetSSHPublicKey

Retrieves the specified SSH public key, including metadata about the key.

```http
POST / HTTP/1.1

Action=GetSSHPublicKey
&Version=2010-05-08
&UserName=example
&SSHPublicKeyId=example0000000000000
&Encoding=SSH
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>8e7d5ba6-6ab0-40be-bb4a-bb9f57d17d74</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetServerCertificate

Retrieves information about the specified server certificate stored in IAM.

```http
POST / HTTP/1.1

Action=GetServerCertificate
&Version=2010-05-08
&ServerCertificateName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Server Certificate with name example cannot be found.</Message>
  </Error>
  <RequestId>c3272e02-0994-4439-af41-be9ab048c533</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetServiceLastAccessedDetails

Retrieves a service last accessed report that was created using the GenerateServiceLastAccessedDetails operation.

```http
POST / HTTP/1.1

Action=GetServiceLastAccessedDetails
&Version=2010-05-08
&JobId=example00000000000000000000000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Job example00000000000000000000000000000 does not exist.</Message>
  </Error>
  <RequestId>334f8b3f-bd74-46aa-a7de-05fcb28f4802</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetServiceLastAccessedDetailsWithEntities

After you generate a group or policy report using the GenerateServiceLastAccessedDetails operation, you can use the JobId parameter in GetServiceLastAccessedDetailsWithEntities .

```http
POST / HTTP/1.1

Action=GetServiceLastAccessedDetailsWithEntities
&Version=2010-05-08
&JobId=example00000000000000000000000000000
&ServiceNamespace=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Job example00000000000000000000000000000 does not exist.</Message>
  </Error>
  <RequestId>8bd4b105-17cf-4a89-ab6a-ed07600bf620</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetServiceLinkedRoleDeletionStatus

Retrieves the status of your service-linked role deletion.

```http
POST / HTTP/1.1

Action=GetServiceLinkedRoleDeletionStatus
&Version=2010-05-08
&DeletionTaskId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>The deletion task ID example is not valid: DeletionTaskId must be in the format task/aws-service-role/&lt;service-principal-name&gt;/&lt;role-name&gt;/&lt;task-uuid&gt;</Message>
  </Error>
  <RequestId>84740e41-162e-4ef3-90ae-5d3f17b05ade</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### GetUser

Retrieves information about the specified IAM user, including the user's creation date, path, unique ID, and ARN.

```http
POST / HTTP/1.1

Action=GetUser
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<GetUserResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <GetUserResult>
    <User xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <UserName>emulator</UserName>
      <UserId>AIDAH5I7GEMO7ADXNFAZU</UserId>
      <Arn>arn:aws:iam::000000000000:user/emulator</Arn>
      <CreateDate>1970-01-01T00:00:00Z</CreateDate>
    </User>
  </GetUserResult>
  <ResponseMetadata>
    <RequestId>058ccb96-bc21-48e2-9060-1f31233e3ec3</RequestId>
  </ResponseMetadata>
</GetUserResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetUserPolicy

Retrieves the specified inline policy document that is embedded in the specified IAM user.

```http
POST / HTTP/1.1

Action=GetUserPolicy
&Version=2010-05-08
&UserName=example
&PolicyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>cdf9b7f2-8d10-4a39-9a61-cfeef75df3bf</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListAccessKeys

Returns information about the access key IDs associated with the specified IAM user.

```http
POST / HTTP/1.1

Action=ListAccessKeys
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListAccessKeysResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListAccessKeysResult>
    <AccessKeyMetadata>
      <member xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
        <UserName>emulator</UserName>
        <AccessKeyId>EMULATORAKKYPDOTCMUU</AccessKeyId>
        <Status>Active</Status>
        <CreateDate>2026-11-16T12:11:28.317Z</CreateDate>
      </member>
      <member>
        <UserName>emulator</UserName>
        <AccessKeyId><access-key-id></AccessKeyId>
        <Status>Active</Status>
        <CreateDate>1970-01-01T00:00:00Z</CreateDate>
      </member>
    </AccessKeyMetadata>
    <IsTruncated>false</IsTruncated>
  </ListAccessKeysResult>
  <ResponseMetadata>
    <RequestId>12a90232-02b4-4f34-ad9c-e82baa780025</RequestId>
… (2 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListAccountAliases

Lists the account alias associated with the Amazon Web Services account (Note: you can have only one).

```http
POST / HTTP/1.1

Action=ListAccountAliases
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListAccountAliasesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListAccountAliasesResult>
    <AccountAliases/>
    <IsTruncated>false</IsTruncated>
  </ListAccountAliasesResult>
  <ResponseMetadata>
    <RequestId>801f171c-e213-4ba7-ab37-846f58703f41</RequestId>
  </ResponseMetadata>
</ListAccountAliasesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListAttachedGroupPolicies

Lists all managed policies that are attached to the specified IAM group.

```http
POST / HTTP/1.1

Action=ListAttachedGroupPolicies
&Version=2010-05-08
&GroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The group with name example cannot be found.</Message>
  </Error>
  <RequestId>33d899fc-62c2-4c03-94e3-9f3050ad8759</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListAttachedRolePolicies

Lists all managed policies that are attached to the specified IAM role.

```http
POST / HTTP/1.1

Action=ListAttachedRolePolicies
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>1b0eb271-346c-42c8-a053-9e4955faa306</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListAttachedUserPolicies

Lists all managed policies that are attached to the specified IAM user.

```http
POST / HTTP/1.1

Action=ListAttachedUserPolicies
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>20884067-5aff-4ec1-b482-f24c2f95e86f</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#4-listattacheduserpolicies) for a successful call.

### ListDelegationRequests

Lists delegation requests based on the specified criteria.

```http
POST / HTTP/1.1

Action=ListDelegationRequests
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListDelegationRequestsResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListDelegationRequestsResult>
    <DelegationRequests/>
    <isTruncated>false</isTruncated>
  </ListDelegationRequestsResult>
  <ResponseMetadata>
    <RequestId>2129f8d0-6e33-4836-985f-ca121a792a1c</RequestId>
  </ResponseMetadata>
</ListDelegationRequestsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListEntitiesForPolicy

Lists all IAM users, groups, and roles that the specified managed policy is attached to.

```http
POST / HTTP/1.1

Action=ListEntitiesForPolicy
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>26f03fae-450e-4370-9cd2-191f230a03bd</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### ListGroupPolicies

Lists the names of the inline policies that are embedded in the specified IAM group.

```http
POST / HTTP/1.1

Action=ListGroupPolicies
&Version=2010-05-08
&GroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The group with name example cannot be found.</Message>
  </Error>
  <RequestId>fe67073b-2c43-40b0-9496-b602c52aee9a</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListGroups

Lists the IAM groups that have the specified path prefix.

```http
POST / HTTP/1.1

Action=ListGroups
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListGroupsResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListGroupsResult>
    <Groups/>
    <IsTruncated>false</IsTruncated>
  </ListGroupsResult>
  <ResponseMetadata>
    <RequestId>12055abd-0d5f-479d-a2cc-439c0a3d9439</RequestId>
  </ResponseMetadata>
</ListGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListGroupsForUser

Lists the IAM groups that the specified IAM user belongs to.

```http
POST / HTTP/1.1

Action=ListGroupsForUser
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>d3df126e-62f8-48a1-954d-8c7ddb6a522d</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListInstanceProfileTags

Lists the tags that are attached to the specified IAM instance profile.

```http
POST / HTTP/1.1

Action=ListInstanceProfileTags
&Version=2010-05-08
&InstanceProfileName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Instance Profile example cannot be found.</Message>
  </Error>
  <RequestId>d21a11d1-f9c2-49a8-a7cd-690c7e478eac</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListInstanceProfiles

Lists the instance profiles that have the specified path prefix.

```http
POST / HTTP/1.1

Action=ListInstanceProfiles
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListInstanceProfilesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListInstanceProfilesResult>
    <InstanceProfiles/>
    <IsTruncated>false</IsTruncated>
  </ListInstanceProfilesResult>
  <ResponseMetadata>
    <RequestId>90766519-7e78-4a59-a22a-2d624e4b47ed</RequestId>
  </ResponseMetadata>
</ListInstanceProfilesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListInstanceProfilesForRole

Lists the instance profiles that have the specified associated IAM role.

```http
POST / HTTP/1.1

Action=ListInstanceProfilesForRole
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>4bdf6aaa-3e0a-4dbf-ad5b-681766bac1e0</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListMFADeviceTags

Lists the tags that are attached to the specified IAM virtual multi-factor authentication (MFA) device.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=ListMFADeviceTags
&Version=2010-05-08
&SerialNumber=example00
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>MFA Device with serial number example00 does not exist.</Message>
  </Error>
  <RequestId>6cbe6f3c-f6ee-4325-b3fd-4cabf5b8e42f</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListMFADevices

Lists the MFA devices for an IAM user.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=ListMFADevices
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListMFADevicesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListMFADevicesResult>
    <MFADevices/>
    <IsTruncated>false</IsTruncated>
  </ListMFADevicesResult>
  <ResponseMetadata>
    <RequestId>18974012-bd8b-44b0-8773-f852987d31c2</RequestId>
  </ResponseMetadata>
</ListMFADevicesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListOpenIDConnectProviderTags

Lists the tags that are attached to the specified OpenID Connect (OIDC)-compatible identity provider.

```http
POST / HTTP/1.1

Action=ListOpenIDConnectProviderTags
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenIDConnect Provider not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>fea1efb2-615d-4603-9ca4-3ddd89e6fd6e</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListOpenIDConnectProviders

Lists information about the IAM OpenID Connect (OIDC) provider resource objects defined in the Amazon Web Services account.

```http
POST / HTTP/1.1

Action=ListOpenIDConnectProviders
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListOpenIDConnectProvidersResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListOpenIDConnectProvidersResult>
    <OpenIDConnectProviderList/>
  </ListOpenIDConnectProvidersResult>
  <ResponseMetadata>
    <RequestId>893b3acc-e855-476f-bf2c-cd02d37ccb72</RequestId>
  </ResponseMetadata>
</ListOpenIDConnectProvidersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListOrganizationsFeatures

Lists the centralized root access features enabled for your organization.

```http
POST / HTTP/1.1

Action=ListOrganizationsFeatures
&Version=2010-05-08
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>OrganizationNotFoundException</Code>
    <Message>The request was rejected because no organization is associated with your account.</Message>
  </Error>
  <RequestId>55fa13eb-508b-4fd3-ab98-c7699b189880</RequestId>
</ErrorResponse>
```

`400` `OrganizationNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListPolicies

Lists all the managed policies that are available in your Amazon Web Services account, including your own customer-defined managed policies and all Amazon Web Services managed policies.

```http
POST / HTTP/1.1

Action=ListPolicies
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListPoliciesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListPoliciesResult>
    <Policies>
      <member xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
        <PolicyName>AWSLakeFormationDataAdmin</PolicyName>
        <PolicyId>ANPAQT4ZJRMWH6PVKLSND</PolicyId>
        <Arn>arn:aws:iam::aws:policy/AWSLakeFormationDataAdmin</Arn>
        <Path>/</Path>
        <DefaultVersionId>v4</DefaultVersionId>
        <AttachmentCount>0</AttachmentCount>
        <PermissionsBoundaryUsageCount>0</PermissionsBoundaryUsageCount>
        <IsAttachable>true</IsAttachable>
        <CreateDate>2019-08-08T19:00:40Z</CreateDate>
        <UpdateDate>2023-10-10T17:28:23Z</UpdateDate>
      </member>
      <member>
        <PolicyName>AWSLambdaExecute</PolicyName>
        <PolicyId>ANPABX7MQWRTZ3KJHVNDC</PolicyId>
        <Arn>arn:aws:iam::aws:policy/AWSLambdaExecute</Arn>
        <Path>/</Path>
… (758 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListPoliciesGrantingServiceAccess

Retrieves a list of policies that the IAM identity (user, group, or role) can use to access each specified service.

```http
POST / HTTP/1.1

Action=ListPoliciesGrantingServiceAccess
&Version=2010-05-08
&Arn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&ServiceNamespaces.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Invalid Entity Arn: arn:aws:iam:us-east-1:000000000000:example/example is not supported.</Message>
  </Error>
  <RequestId>b5b03eb5-ad91-4461-9fb5-52e858026bd7</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### ListPolicyTags

Lists the tags that are attached to the specified IAM customer managed policy.

```http
POST / HTTP/1.1

Action=ListPolicyTags
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>c570f419-cf5c-4861-a459-f216e08be780</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### ListPolicyVersions

Lists information about the versions of the specified managed policy, including the version that is currently set as the policy's default version.

```http
POST / HTTP/1.1

Action=ListPolicyVersions
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>6be1fa1a-e3af-4387-bf24-835058e18a6e</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### ListRolePolicies

Lists the names of the inline policies that are embedded in the specified IAM role.

```http
POST / HTTP/1.1

Action=ListRolePolicies
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>d5f2e4de-b855-4a5f-9846-2ec2c764b9c5</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListRoleTags

Lists the tags that are attached to the specified role.

```http
POST / HTTP/1.1

Action=ListRoleTags
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>5ad78f8e-e234-4593-8c5b-21b7e9686b1f</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListRoles

Lists the IAM roles that have the specified path prefix.

```http
POST / HTTP/1.1

Action=ListRoles
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListRolesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListRolesResult>
    <Roles>
      <member xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
        <Path>/aws-service-role/dynamodb.application-autoscaling.amazonaws.com/</Path>
        <RoleName>AWSServiceRoleForApplicationAutoScaling_DynamoDBTable</RoleName>
        <RoleId>AROAN7DDQNT4UYC2BHD35</RoleId>
        <Arn>arn:aws:iam::000000000000:role/aws-service-role/dynamodb.application-autoscaling.amazonaws.com/AWSServiceRoleForApplicationAutoScaling_DynamoDBTable</Arn>
        <CreateDate>2026-11-16T12:08:37.757Z</CreateDate>
        <AssumeRolePolicyDocument>%7B%22Version%22%3A%222012-10-17%22%2C%22Statement%22%3A%5B%7B%22Effect%22%3A%22Allow%22%2C%22Principal%22%3A%7B%22Service%22%3A%22dynamodb.application-autoscaling.amazonaws.com%22%7D%2C%22Action%22%3A%5B%22sts%3AAssumeRole%22%5D%7D%5D%7D</AssumeRolePolicyDocument>
        <MaxSessionDuration>3600</MaxSessionDuration>
      </member>
    </Roles>
    <IsTruncated>false</IsTruncated>
  </ListRolesResult>
  <ResponseMetadata>
    <RequestId>69e92c2e-77e9-4d95-b688-48912a6e218b</RequestId>
  </ResponseMetadata>
</ListRolesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListSAMLProviderTags

Lists the tags that are attached to the specified Security Assertion Markup Language (SAML) identity provider.

```http
POST / HTTP/1.1

Action=ListSAMLProviderTags
&Version=2010-05-08
&SAMLProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Manifest not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>043bc00a-e77b-4097-9f32-070d3da454ce</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListSAMLProviders

Lists the SAML provider resource objects defined in IAM in the account.

```http
POST / HTTP/1.1

Action=ListSAMLProviders
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListSAMLProvidersResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListSAMLProvidersResult>
    <SAMLProviderList/>
  </ListSAMLProvidersResult>
  <ResponseMetadata>
    <RequestId>fdd9a310-4460-4dfa-9ea9-e9917eea6bf2</RequestId>
  </ResponseMetadata>
</ListSAMLProvidersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListSSHPublicKeys

Returns information about the SSH public keys associated with the specified IAM user.

```http
POST / HTTP/1.1

Action=ListSSHPublicKeys
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListSSHPublicKeysResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListSSHPublicKeysResult>
    <SSHPublicKeys/>
    <IsTruncated>false</IsTruncated>
  </ListSSHPublicKeysResult>
  <ResponseMetadata>
    <RequestId>297b2ed8-030f-4c5c-a66b-048cfb2bef59</RequestId>
  </ResponseMetadata>
</ListSSHPublicKeysResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListServerCertificateTags

Lists the tags that are attached to the specified IAM server certificate.

```http
POST / HTTP/1.1

Action=ListServerCertificateTags
&Version=2010-05-08
&ServerCertificateName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Server Certificate with name example cannot be found.</Message>
  </Error>
  <RequestId>bdd1c7bc-6019-4c30-bb46-81c85d178bd5</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListServerCertificates

Lists the server certificates stored in IAM that have the specified path prefix.

```http
POST / HTTP/1.1

Action=ListServerCertificates
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListServerCertificatesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListServerCertificatesResult>
    <ServerCertificateMetadataList/>
    <IsTruncated>false</IsTruncated>
  </ListServerCertificatesResult>
  <ResponseMetadata>
    <RequestId>13e6fb9d-aafe-4983-b7ae-6f905ddde7dd</RequestId>
  </ResponseMetadata>
</ListServerCertificatesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListServiceSpecificCredentials

Returns information about the service-specific credentials associated with the specified IAM user.

```http
POST / HTTP/1.1

Action=ListServiceSpecificCredentials
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListServiceSpecificCredentialsResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListServiceSpecificCredentialsResult>
    <ServiceSpecificCredentials/>
    <IsTruncated>false</IsTruncated>
  </ListServiceSpecificCredentialsResult>
  <ResponseMetadata>
    <RequestId>84880106-2fc8-4452-bbc4-e86273848e15</RequestId>
  </ResponseMetadata>
</ListServiceSpecificCredentialsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListSigningCertificates

Returns information about the signing certificates associated with the specified IAM user.

```http
POST / HTTP/1.1

Action=ListSigningCertificates
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListSigningCertificatesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListSigningCertificatesResult>
    <Certificates/>
    <IsTruncated>false</IsTruncated>
  </ListSigningCertificatesResult>
  <ResponseMetadata>
    <RequestId>425868b7-575b-49a5-8357-6908d0725322</RequestId>
  </ResponseMetadata>
</ListSigningCertificatesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListUserPolicies

Lists the names of the inline policies embedded in the specified IAM user.

```http
POST / HTTP/1.1

Action=ListUserPolicies
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>16a4b867-a52f-4157-a113-519cbb685b85</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListUserTags

Lists the tags that are attached to the specified IAM user.

```http
POST / HTTP/1.1

Action=ListUserTags
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>15e28ab4-be12-4c86-ba7b-cd06b9571192</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListUsers

Lists the IAM users that have the specified path prefix.

```http
POST / HTTP/1.1

Action=ListUsers
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListUsersResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListUsersResult>
    <Users>
      <member xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
        <Path>/</Path>
        <UserName>emulator</UserName>
        <UserId>AIDAH5I7GEMO7ADXNFAZU</UserId>
        <Arn>arn:aws:iam::000000000000:user/emulator</Arn>
        <CreateDate>1970-01-01T00:00:00Z</CreateDate>
      </member>
    </Users>
    <IsTruncated>false</IsTruncated>
  </ListUsersResult>
  <ResponseMetadata>
    <RequestId>f6ad54cb-9c03-4ac5-9f90-3cb1253b7263</RequestId>
  </ResponseMetadata>
</ListUsersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListVirtualMFADevices

Lists the virtual MFA devices defined in the Amazon Web Services account by assignment status.

```http
POST / HTTP/1.1

Action=ListVirtualMFADevices
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListVirtualMFADevicesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListVirtualMFADevicesResult>
    <VirtualMFADevices>
      <member xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
        <SerialNumber>arn:aws:iam::000000000000:mfa/example</SerialNumber>
      </member>
    </VirtualMFADevices>
    <IsTruncated>false</IsTruncated>
  </ListVirtualMFADevicesResult>
  <ResponseMetadata>
    <RequestId>0b96372f-2cb0-4fef-a426-965a4370911d</RequestId>
  </ResponseMetadata>
</ListVirtualMFADevicesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### PutAccountProperties

Sets account-level properties for the caller's Amazon Web Services account.

```http
POST / HTTP/1.1

Action=PutAccountProperties
&Version=2010-05-08
&Properties.entry.1.key=key
&Properties.entry.1.value=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Property key must be in format Namespace/PropertyName: key</Message>
  </Error>
  <RequestId>54f63a3a-5ca6-4456-83a5-438f18aa0e2e</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutGroupPolicy

Adds or updates an inline policy document that is embedded in the specified IAM group.

```http
POST / HTTP/1.1

Action=PutGroupPolicy
&Version=2010-05-08
&GroupName=example
&PolicyName=example
&PolicyDocument=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The group with name example cannot be found.</Message>
  </Error>
  <RequestId>65d4ff01-b299-4d07-a2b6-daa9cf9476fb</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PutRolePermissionsBoundary

Adds or updates the policy that is specified as the IAM role's permissions boundary.

```http
POST / HTTP/1.1

Action=PutRolePermissionsBoundary
&Version=2010-05-08
&RoleName=example
&PermissionsBoundary=example0000000000000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>ARN example0000000000000 is not valid.</Message>
  </Error>
  <RequestId>5a1c9cfa-b0f2-4f61-aaf9-aa898ac08bdc</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutRolePolicy

Adds or updates an inline policy document that is embedded in the specified IAM role.

```http
POST / HTTP/1.1

Action=PutRolePolicy
&Version=2010-05-08
&RoleName=example
&PolicyName=example
&PolicyDocument=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>ba4f0e54-0282-4c3d-bf98-95d852b45b53</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PutUserPermissionsBoundary

Adds or updates the policy that is specified as the IAM user's permissions boundary.

```http
POST / HTTP/1.1

Action=PutUserPermissionsBoundary
&Version=2010-05-08
&UserName=example
&PermissionsBoundary=example0000000000000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>ARN example0000000000000 is not valid.</Message>
  </Error>
  <RequestId>3fa7096c-3b7b-4cdd-9a12-69fc1ce20ff2</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutUserPolicy

Adds or updates an inline policy document that is embedded in the specified IAM user.

```http
POST / HTTP/1.1

Action=PutUserPolicy
&Version=2010-05-08
&UserName=example
&PolicyName=example
&PolicyDocument=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>dcdc0a0e-0526-497e-8564-c3051d5f5310</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RejectDelegationRequest

Rejects a delegation request, denying the requested temporary access.

```http
POST / HTTP/1.1

Action=RejectDelegationRequest
&Version=2010-05-08
&DelegationRequestId=example000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Delegation request not found: example000000000</Message>
  </Error>
  <RequestId>3286df51-5bad-4ea7-bf00-64bb02e10da5</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RemoveClientIDFromOpenIDConnectProvider

Removes the specified client ID (also known as audience) from the list of client IDs registered for the specified IAM OpenID Connect (OIDC) provider resource object.

```http
POST / HTTP/1.1

Action=RemoveClientIDFromOpenIDConnectProvider
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&ClientID=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenIDConnect Provider not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>beea8fd9-800f-467c-9aa0-6b32f7fa0098</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RemoveRoleFromInstanceProfile

Removes the specified IAM role from the specified Amazon EC2 instance profile.

```http
POST / HTTP/1.1

Action=RemoveRoleFromInstanceProfile
&Version=2010-05-08
&InstanceProfileName=example
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Instance Profile example cannot be found.</Message>
  </Error>
  <RequestId>58e8924b-2136-4a5d-9246-24b74b94fc01</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RemoveUserFromGroup

Removes the specified user from the specified group.

```http
POST / HTTP/1.1

Action=RemoveUserFromGroup
&Version=2010-05-08
&GroupName=example
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>4c628015-7048-4c6d-ada4-0bd2e7e1b244</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ResetServiceSpecificCredential

Resets the password for a service-specific credential.

```http
POST / HTTP/1.1

Action=ResetServiceSpecificCredential
&Version=2010-05-08
&ServiceSpecificCredentialId=example0000000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>No such credential example0000000000000 exists</Message>
  </Error>
  <RequestId>360a1143-9413-41ad-962b-06d60181e6de</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ResyncMFADevice

Synchronizes the specified MFA device with its IAM resource object on the Amazon Web Services servers.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=ResyncMFADevice
&Version=2010-05-08
&UserName=example
&SerialNumber=example00
&AuthenticationCode1=100000
&AuthenticationCode2=100000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>2d2fd6a2-3575-4c7a-831c-fcfec86fb0fe</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### SendDelegationToken

Sends the exchange token for an accepted delegation request.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=SendDelegationToken
&Version=2010-05-08
&DelegationRequestId=example000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Delegation request not found: example000000000</Message>
  </Error>
  <RequestId>835b65dc-f96e-4560-aea8-d63319f780ab</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### SetDefaultPolicyVersion

Sets the specified version of the specified policy as the policy's default (operative) version.

```http
POST / HTTP/1.1

Action=SetDefaultPolicyVersion
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&VersionId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value at 'versionId' failed to satisfy constraint: Member must satisfy regular expression pattern: v[1-9][0-9]*(\.[A-Za-z0-9-]*)?</Message>
  </Error>
  <RequestId>3c18c889-2ce9-47ae-99ce-57699a28e01c</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### SetSecurityTokenServicePreferences

Sets the specified version of the global endpoint token as the token version used for the Amazon Web Services account.

```http
POST / HTTP/1.1

Action=SetSecurityTokenServicePreferences
&Version=2010-05-08
&GlobalEndpointTokenVersion=v1Token
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<SetSecurityTokenServicePreferencesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <SetSecurityTokenServicePreferencesResult/>
  <ResponseMetadata>
    <RequestId>cdc087f5-b487-4e89-991e-1b8a78976fcf</RequestId>
  </ResponseMetadata>
</SetSecurityTokenServicePreferencesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### SimulateCustomPolicy

Simulate how a set of IAM policies and optionally a resource-based policy works with a list of API operations and Amazon Web Services resources to determine the policies' effective permissions.

```http
POST / HTTP/1.1

Action=SimulateCustomPolicy
&Version=2010-05-08
&PolicyInputList.member.1=example
&ActionNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Policy input list item 1 has invalid content</Message>
  </Error>
  <RequestId>4c026916-7ec3-4cf2-baf5-c50cd4fed7c4</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### SimulatePrincipalPolicy

Simulate how a set of IAM policies attached to an IAM entity works with a list of API operations and Amazon Web Services resources to determine the policies' effective permissions.

```http
POST / HTTP/1.1

Action=SimulatePrincipalPolicy
&Version=2010-05-08
&PolicySourceArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&ActionNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Invalid Action name: example must name a service and an action, as in iam:CreateUser</Message>
  </Error>
  <RequestId>63824e60-a399-4be1-9881-da3bd0e89b86</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### TagInstanceProfile

Adds one or more tags to an IAM instance profile.

```http
POST / HTTP/1.1

Action=TagInstanceProfile
&Version=2010-05-08
&InstanceProfileName=example
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Instance Profile example cannot be found.</Message>
  </Error>
  <RequestId>0d8d9189-e30b-4302-afcf-f276e1b93585</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagMFADevice

Adds one or more tags to an IAM virtual multi-factor authentication (MFA) device.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=TagMFADevice
&Version=2010-05-08
&SerialNumber=example00
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>MFA Device with serial number example00 does not exist.</Message>
  </Error>
  <RequestId>29772c58-61b8-4d4d-87b0-e3920b45d5a2</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagOpenIDConnectProvider

Adds one or more tags to an OpenID Connect (OIDC)-compatible identity provider.

```http
POST / HTTP/1.1

Action=TagOpenIDConnectProvider
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenId connect Provider arn:aws:iam:us-east-1:000000000000:example/example cannot be found.</Message>
  </Error>
  <RequestId>10e03cef-8312-4088-858b-fb8a9b5a8c42</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagPolicy

Adds one or more tags to an IAM customer managed policy.

```http
POST / HTTP/1.1

Action=TagPolicy
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>46baa267-8057-4af5-a959-9c4ce3c57a86</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### TagRole

Adds one or more tags to an IAM role.

```http
POST / HTTP/1.1

Action=TagRole
&Version=2010-05-08
&RoleName=example
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>07110304-cac5-40dd-98cf-ce1d07214d9e</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagSAMLProvider

Adds one or more tags to a Security Assertion Markup Language (SAML) identity provider.

```http
POST / HTTP/1.1

Action=TagSAMLProvider
&Version=2010-05-08
&SAMLProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Manifest not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>d4a58ca1-ff28-4847-86d9-51706b533b31</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagServerCertificate

Adds one or more tags to an IAM server certificate.

```http
POST / HTTP/1.1

Action=TagServerCertificate
&Version=2010-05-08
&ServerCertificateName=example
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Server Certificate with name example cannot be found.</Message>
  </Error>
  <RequestId>0abcf8d7-5db8-4ee1-a7df-03ce0082061c</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagUser

Adds one or more tags to an IAM user.

```http
POST / HTTP/1.1

Action=TagUser
&Version=2010-05-08
&UserName=example
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>e84264de-b0f8-4974-bb60-95baa6b6ec64</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagInstanceProfile

Removes the specified tags from the IAM instance profile.

```http
POST / HTTP/1.1

Action=UntagInstanceProfile
&Version=2010-05-08
&InstanceProfileName=example
&TagKeys.member.1=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Instance Profile example cannot be found.</Message>
  </Error>
  <RequestId>5daa9884-e9cd-47b6-a14f-01a3002b40ba</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagMFADevice

Removes the specified tags from the IAM virtual multi-factor authentication (MFA) device.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=UntagMFADevice
&Version=2010-05-08
&SerialNumber=example00
&TagKeys.member.1=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>MFA Device with serial number example00 does not exist.</Message>
  </Error>
  <RequestId>214d160d-6a95-4a06-bb36-cf0404260075</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagOpenIDConnectProvider

Removes the specified tags from the specified OpenID Connect (OIDC)-compatible identity provider in IAM.

```http
POST / HTTP/1.1

Action=UntagOpenIDConnectProvider
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&TagKeys.member.1=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenId connect Provider arn:aws:iam:us-east-1:000000000000:example/example cannot be found.</Message>
  </Error>
  <RequestId>05aeeeeb-792c-42bf-9d42-971cffc94bf0</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagPolicy

Removes the specified tags from the customer managed policy.

```http
POST / HTTP/1.1

Action=UntagPolicy
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&TagKeys.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>285fe013-e4af-4132-bdb6-0f39fd572c2e</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### UntagRole

Removes the specified tags from the role.

```http
POST / HTTP/1.1

Action=UntagRole
&Version=2010-05-08
&RoleName=example
&TagKeys.member.1=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>566e40bb-7463-409d-ac09-87c9153d8776</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagSAMLProvider

Removes the specified tags from the specified Security Assertion Markup Language (SAML) identity provider in IAM.

```http
POST / HTTP/1.1

Action=UntagSAMLProvider
&Version=2010-05-08
&SAMLProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&TagKeys.member.1=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Manifest not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>2f4e3d20-2db4-4d28-b131-58ddce9e1482</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagServerCertificate

Removes the specified tags from the IAM server certificate.

```http
POST / HTTP/1.1

Action=UntagServerCertificate
&Version=2010-05-08
&ServerCertificateName=example
&TagKeys.member.1=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Server Certificate with name example cannot be found.</Message>
  </Error>
  <RequestId>fcff6610-a6cf-40cd-99c2-bf412ab8cabc</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagUser

Removes the specified tags from the user.

```http
POST / HTTP/1.1

Action=UntagUser
&Version=2010-05-08
&UserName=example
&TagKeys.member.1=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>5db5c419-9bbf-40f1-9bcf-253b7626422d</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateAccessKey

Changes the status of the specified access key from Active to Inactive, or vice versa.

```http
POST / HTTP/1.1

Action=UpdateAccessKey
&Version=2010-05-08
&AccessKeyId=example000000000
&Status=Active
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Access Key with id example000000000 cannot be found.</Message>
  </Error>
  <RequestId>df72ed6f-651e-436a-81a3-87bc3ad60147</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateAccountPasswordPolicy

Updates the password policy settings for the Amazon Web Services account.

```http
POST / HTTP/1.1

Action=UpdateAccountPasswordPolicy
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<UpdateAccountPasswordPolicyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <UpdateAccountPasswordPolicyResult/>
  <ResponseMetadata>
    <RequestId>4de5f60a-3216-4a84-849c-92e5030547c6</RequestId>
  </ResponseMetadata>
</UpdateAccountPasswordPolicyResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### UpdateAssumeRolePolicy

Updates the policy that grants an IAM entity permission to assume a role.

```http
POST / HTTP/1.1

Action=UpdateAssumeRolePolicy
&Version=2010-05-08
&RoleName=example
&PolicyDocument=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>cb7529d8-c58d-475d-af4f-bbc1016c1488</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateDelegationRequest

Updates an existing delegation request with additional information.

```http
POST / HTTP/1.1

Action=UpdateDelegationRequest
&Version=2010-05-08
&DelegationRequestId=example000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Delegation request not found: example000000000</Message>
  </Error>
  <RequestId>b7f3a4f6-1c4b-44cf-bb1f-e4bfd57d66b0</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateGroup

Updates the name and/or the path of the specified IAM group.

```http
POST / HTTP/1.1

Action=UpdateGroup
&Version=2010-05-08
&GroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The group with name example cannot be found.</Message>
  </Error>
  <RequestId>b81e0db8-06f4-4566-9774-a74f11e759fc</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateLoginProfile

Changes the password for the specified IAM user.

```http
POST / HTTP/1.1

Action=UpdateLoginProfile
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>767653bf-7c2b-4a90-88ab-fda87ce4f26e</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateOpenIDConnectProviderThumbprint

Replaces the existing list of server certificate thumbprints associated with an OpenID Connect (OIDC) provider resource object with a new list of thumbprints.

```http
POST / HTTP/1.1

Action=UpdateOpenIDConnectProviderThumbprint
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&ThumbprintList.member.1=example000000000000000000000000000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenIDConnect Provider not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>4c685c12-2b95-4bfd-9e35-37883311c832</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateRole

Updates the description or maximum session duration setting of a role.

```http
POST / HTTP/1.1

Action=UpdateRole
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>5e2ac9d2-c2ea-4b0e-9c54-6aaf0c27faf4</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateRoleDescription

Use UpdateRole instead.

```http
POST / HTTP/1.1

Action=UpdateRoleDescription
&Version=2010-05-08
&RoleName=example
&Description=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>c64c1dd8-dda4-4db6-bf38-ba4affec74ea</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateSAMLProvider

Updates the metadata document, SAML encryption settings, and private keys for an existing SAML provider.

```http
POST / HTTP/1.1

Action=UpdateSAMLProvider
&Version=2010-05-08
&SAMLProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Manifest not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>5b412a7a-84a5-41a1-89e3-3c6bf77e8640</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateSSHPublicKey

Sets the status of an IAM user's SSH public key to active or inactive.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=UpdateSSHPublicKey
&Version=2010-05-08
&UserName=example
&SSHPublicKeyId=example0000000000000
&Status=Active
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>2e92069a-46cc-4481-afe8-301cfad2a2a9</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateServerCertificate

Updates the name and/or the path of the specified server certificate stored in IAM.

```http
POST / HTTP/1.1

Action=UpdateServerCertificate
&Version=2010-05-08
&ServerCertificateName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Server Certificate with name example cannot be found.</Message>
  </Error>
  <RequestId>bdce2e5e-39bb-4506-b9cf-ac2d06630115</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateServiceSpecificCredential

Sets the status of a service-specific credential to Active or Inactive .

```http
POST / HTTP/1.1

Action=UpdateServiceSpecificCredential
&Version=2010-05-08
&ServiceSpecificCredentialId=example0000000000000
&Status=Active
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>No such credential example0000000000000 exists</Message>
  </Error>
  <RequestId>7ce2f04c-7ded-430b-9981-e7df48991f32</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateSigningCertificate

Changes the status of the specified user signing certificate from active to disabled, or vice versa.

```http
POST / HTTP/1.1

Action=UpdateSigningCertificate
&Version=2010-05-08
&CertificateId=example00000000000000000
&Status=Active
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Certificate with id example00000000000000000 cannot be found.</Message>
  </Error>
  <RequestId>291c5265-798a-4912-bf11-bbe8223ba36c</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateUser

Updates the name and/or the path of the specified IAM user.

```http
POST / HTTP/1.1

Action=UpdateUser
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>96746e2f-ece1-4e6b-a5f6-5e34d1245e47</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UploadSSHPublicKey

Uploads an SSH public key and associates it with the specified IAM user.

```http
POST / HTTP/1.1

Action=UploadSSHPublicKey
&Version=2010-05-08
&UserName=example
&SSHPublicKeyBody=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>UnrecognizedPublicKeyEncoding</Code>
    <Message>The public key encoding format is unsupported or unrecognized.</Message>
  </Error>
  <RequestId>420bf7b4-ea34-484c-95f7-e00326738366</RequestId>
</ErrorResponse>
```

`400` `UnrecognizedPublicKeyEncoding`.

### UploadServerCertificate

Uploads a server certificate entity for the Amazon Web Services account.

```http
POST / HTTP/1.1

Action=UploadServerCertificate
&Version=2010-05-08
&ServerCertificateName=example
&CertificateBody=example
&PrivateKey=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>MalformedCertificate</Code>
    <Message>Unable to parse certificate. Please ensure the certificate is in PEM format.</Message>
  </Error>
  <RequestId>467f525f-19e3-4830-8f3a-9dff2fb45676</RequestId>
</ErrorResponse>
```

`400` `MalformedCertificate`: the placeholder values failed the operation's input validation.

### UploadSigningCertificate

Uploads an X.509 signing certificate and associates it with the specified IAM user.

```http
POST / HTTP/1.1

Action=UploadSigningCertificate
&Version=2010-05-08
&CertificateBody=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>MalformedCertificate</Code>
    <Message>Certificate example is malformed.</Message>
  </Error>
  <RequestId>266fc008-514c-416a-a9dc-abe1371c134e</RequestId>
</ErrorResponse>
```

`400` `MalformedCertificate`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/iam/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with a form-encoded body: `Action` names the operation, `Version` the API version, and every input member is a flattened key such as `Tags.member.1.Key`.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way AWS Identity and Access Management answers in AWS: XML wrapped in `<OperationResponse><OperationResult>`. Errors captured on this page are an XML document rooted at `<ErrorResponse>`.
