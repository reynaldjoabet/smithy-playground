# AWS Identity and Access Management

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `IAM`
- **sigv4 signing name:** `iam`
- **Endpoint prefix:** `iam`
- **ARN namespace:** `iam`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `iam`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

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
X-Amz-Date: 20261126T183005Z
Content-Length: 108
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
content-length: 542
date: Thu, 26 Nov 2026 18:30:05 GMT

<CreateUserResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateUserResult>
    <User xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <UserName>docs-demo-user</UserName>
      <UserId>AIDABGXEQES5NRCCJJWAN</UserId>
      <Arn>arn:aws:iam::000000000000:user/docs-demo-user</Arn>
      <CreateDate>2026-11-26T18:30:05.49Z</CreateDate>
      <Tags>
        <member>
          <Key>team</Key>
          <Value>docs</Value>
        </member>
      </Tags>
    </User>
  </CreateUserResult>
  <ResponseMetadata>
    <RequestId>ae14c80b-631e-43a3-9629-db98e193ce4c</RequestId>
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
X-Amz-Date: 20261126T183005Z
Content-Length: 301
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreatePolicy
&Version=2010-05-08
&PolicyName=docs-demo-read
&PolicyDocument=%7B%22Version%22%3A%20%222012-10-17%22%2C%20%22Statement%22%3A%20%5B%7B%22Effect%22%3A%20%22Allow%22%2C%20%22Action%22%3A%20%5B%22s3%3AGetObject%22%2C%20%22s3%3AListBucket%22%5D%2C%20%22Resource%22%3A%20%22%2A%22%7D%5D%7D
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 735
date: Thu, 26 Nov 2026 18:30:05 GMT

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
      <CreateDate>2026-11-26T18:30:05.492Z</CreateDate>
      <UpdateDate>2026-11-26T18:30:05.492Z</UpdateDate>
      <Tags></Tags>
    </Policy>
  </CreatePolicyResult>
  <ResponseMetadata>
    <RequestId>333f0390-af6c-4f65-b604-0aee843769b9</RequestId>
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
X-Amz-Date: 20261126T183005Z
Content-Length: 136
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
date: Thu, 26 Nov 2026 18:30:05 GMT

<AttachUserPolicyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <AttachUserPolicyResult></AttachUserPolicyResult>
  <ResponseMetadata>
    <RequestId>0e29dffc-4da5-4468-a438-a70c01e3d90b</RequestId>
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
X-Amz-Date: 20261126T183005Z
Content-Length: 74
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListAttachedUserPolicies
&Version=2010-05-08
&UserName=docs-demo-user
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 525
date: Thu, 26 Nov 2026 18:30:05 GMT

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
    <RequestId>5eb5a028-8ec5-4ebe-be82-86f070d198ef</RequestId>
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
X-Amz-Date: 20261126T183005Z
Content-Length: 65
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateAccessKey
&Version=2010-05-08
&UserName=docs-demo-user
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 546
date: Thu, 26 Nov 2026 18:30:05 GMT

<CreateAccessKeyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateAccessKeyResult>
    <AccessKey xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <UserName>docs-demo-user</UserName>
      <AccessKeyId>EMULATORAKXOBG24MQ4E</AccessKeyId>
      <Status>Active</Status>
      <SecretAccessKey><redacted></SecretAccessKey>
      <CreateDate>2026-11-26T18:30:05.495Z</CreateDate>
    </AccessKey>
  </CreateAccessKeyResult>
  <ResponseMetadata>
    <RequestId>a4dc0dbe-9e2c-4406-a598-78206465b9f8</RequestId>
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
X-Amz-Date: 20261126T183005Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteUser
&Version=2010-05-08
&UserName=docs-demo-user
```

**Response**

```http
HTTP/1.1 409 Conflict
content-type: text/xml
x-amzn-requestid: e633372b-a70f-4010-8d50-2b4aa660deac
content-length: 273
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>DeleteConflict</Code>
    <Message>Cannot delete entity, must detach all policies first.</Message>
  </Error>
  <RequestId>e633372b-a70f-4010-8d50-2b4aa660deac</RequestId>
</ErrorResponse>
```

### 7. DeleteAccessKey

Remove the key ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 98
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteAccessKey
&Version=2010-05-08
&UserName=docs-demo-user
&AccessKeyId=EMULATORAKXOBG24MQ4E
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 244
date: Thu, 26 Nov 2026 18:30:05 GMT

<DeleteAccessKeyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteAccessKeyResult></DeleteAccessKeyResult>
  <ResponseMetadata>
    <RequestId>9fa2dc3d-a42a-4dca-a77d-9fd6507c9003</RequestId>
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
X-Amz-Date: 20261126T183005Z
Content-Length: 136
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
date: Thu, 26 Nov 2026 18:30:05 GMT

<DetachUserPolicyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DetachUserPolicyResult></DetachUserPolicyResult>
  <ResponseMetadata>
    <RequestId>c6db1519-da52-4927-b443-7967fe9910e5</RequestId>
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
X-Amz-Date: 20261126T183005Z
Content-Length: 108
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeletePolicy
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3A%3A000000000000%3Apolicy%2Fdocs-demo-read
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 232
date: Thu, 26 Nov 2026 18:30:05 GMT

<DeletePolicyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeletePolicyResult></DeletePolicyResult>
  <ResponseMetadata>
    <RequestId>45cd48e1-8d12-4524-b8ae-3c104f9464df</RequestId>
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
X-Amz-Date: 20261126T183005Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteUser
&Version=2010-05-08
&UserName=docs-demo-user
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 224
date: Thu, 26 Nov 2026 18:30:05 GMT

<DeleteUserResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteUserResult></DeleteUserResult>
  <ResponseMetadata>
    <RequestId>031358a4-498a-48e6-ba12-d1230cfdbbb1</RequestId>
  </ResponseMetadata>
</DeleteUserResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 180 operations, 34 accepted the placeholder call, 4 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### AcceptDelegationRequest

Accepts a delegation request, granting the requested temporary access.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 86
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AcceptDelegationRequest
&Version=2010-05-08
&DelegationRequestId=example000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: f7e308c9-6e13-4e37-81e5-8db865292801
content-length: 263
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>DelegationRequest not found: example000000000</Message>
  </Error>
  <RequestId>f7e308c9-6e13-4e37-81e5-8db865292801</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AcquireRole

Creates an IAM role from the specified role template.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 112
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AcquireRole
&Version=2010-05-08
&TemplateArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 7feede69-0f0a-4591-beaf-01a16c15211b
content-length: 293
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Role template not found: arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>7feede69-0f0a-4591-beaf-01a16c15211b</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AddClientIDToOpenIDConnectProvider

Adds a new client ID (also known as audience) to the list of client IDs already registered for the specified IAM OpenID Connect (OIDC) provider resource.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 165
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AddClientIDToOpenIDConnectProvider
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&ClientID=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 5f338f33-5c8f-46e6-8e8d-43549ab5a795
content-length: 309
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenIDConnect Provider not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>5f338f33-5c8f-46e6-8e8d-43549ab5a795</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AddRoleToInstanceProfile

Adds the specified IAM role to the specified instance profile.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 95
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AddRoleToInstanceProfile
&Version=2010-05-08
&InstanceProfileName=example
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 387ba8b7-44ab-4504-b499-f1ca4f609a0f
content-length: 259
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Instance Profile example cannot be found.</Message>
  </Error>
  <RequestId>387ba8b7-44ab-4504-b499-f1ca4f609a0f</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AddUserToGroup

Adds the specified user to the specified group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AddUserToGroup
&Version=2010-05-08
&GroupName=example
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 3d46b931-c57f-4ea3-b296-bf962dd9ef48
content-length: 262
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The group with name example cannot be found.</Message>
  </Error>
  <RequestId>3d46b931-c57f-4ea3-b296-bf962dd9ef48</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AssociateDelegationRequest

Associates a delegation request with the current identity.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 89
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AssociateDelegationRequest
&Version=2010-05-08
&DelegationRequestId=example000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 9be304a4-a6c4-4698-826b-3b9f8ff95967
content-length: 264
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Delegation request not found: example000000000</Message>
  </Error>
  <RequestId>9be304a4-a6c4-4698-826b-3b9f8ff95967</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AttachGroupPolicy

Attaches the specified managed policy to the specified IAM group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 134
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AttachGroupPolicy
&Version=2010-05-08
&GroupName=example
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 07bd2a97-b228-4e0d-ae87-7f033dba16fe
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>07bd2a97-b228-4e0d-ae87-7f033dba16fe</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### AttachRolePolicy

Attaches the specified managed policy to the specified IAM role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 132
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AttachRolePolicy
&Version=2010-05-08
&RoleName=example
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: bd18e98e-c2c4-4191-8b04-a2669c943dc0
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>bd18e98e-c2c4-4191-8b04-a2669c943dc0</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### AttachUserPolicy

Attaches the specified managed policy to the specified user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 132
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AttachUserPolicy
&Version=2010-05-08
&UserName=example
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 5bd71575-2e92-47a1-9205-6d882f35efa6
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>5bd71575-2e92-47a1-9205-6d882f35efa6</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-attachuserpolicy) for a successful call.

### ChangePassword

Changes the password of the IAM user who is calling this operation.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 80
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ChangePassword
&Version=2010-05-08
&OldPassword=<redacted>
&NewPassword=<redacted>
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: fb6cc254-4cdc-4ade-9780-e20e8b5a6b1e
content-length: 266
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Login Profile for User emulator cannot be found.</Message>
  </Error>
  <RequestId>fb6cc254-4cdc-4ade-9780-e20e8b5a6b1e</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateAccessKey

Creates a new Amazon Web Services secret access key and corresponding Amazon Web Services access key ID for the specified user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 41
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateAccessKey
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 539
date: Thu, 26 Nov 2026 18:30:05 GMT

<CreateAccessKeyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateAccessKeyResult>
    <AccessKey xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <UserName>emulator</UserName>
      <AccessKeyId>EMULATORAKNGDWTSXFHM</AccessKeyId>
      <Status>Active</Status>
      <SecretAccessKey><redacted></SecretAccessKey>
      <CreateDate>2026-11-26T18:30:05.51Z</CreateDate>
    </AccessKey>
  </CreateAccessKeyResult>
  <ResponseMetadata>
    <RequestId>9f9191ae-6359-4f3f-b048-8bae1228d378</RequestId>
  </ResponseMetadata>
</CreateAccessKeyResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#5-createaccesskey) for a successful call.

### CreateAccountAlias

Creates an alias for your Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 65
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateAccountAlias
&Version=2010-05-08
&AccountAlias=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 256
date: Thu, 26 Nov 2026 18:30:05 GMT

<CreateAccountAliasResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateAccountAliasResult></CreateAccountAliasResult>
  <ResponseMetadata>
    <RequestId>a7d67242-460a-4552-9e6d-baf3e105b4b9</RequestId>
  </ResponseMetadata>
</CreateAccountAliasResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateDelegationRequest

Creates an IAM delegation request for temporary access delegation.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 145
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
x-amzn-requestid: 6fdf2d83-cfb4-4e89-bfca-de4fb34dcbaa
content-length: 307
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value at &apos;permissions&apos; failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>6fdf2d83-cfb4-4e89-bfca-de4fb34dcbaa</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CreateGroup

Creates a new group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 55
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateGroup
&Version=2010-05-08
&GroupName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 476
date: Thu, 26 Nov 2026 18:30:05 GMT

<CreateGroupResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateGroupResult>
    <Group xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <GroupName>example</GroupName>
      <GroupId>AGPABGXEQES5NRCCJJWAN</GroupId>
      <Arn>arn:aws:iam::000000000000:group/example</Arn>
      <CreateDate>2026-11-26T18:30:05.513Z</CreateDate>
    </Group>
  </CreateGroupResult>
  <ResponseMetadata>
    <RequestId>1aca7bf8-e3d4-4f82-b564-e85524a9c5e4</RequestId>
  </ResponseMetadata>
</CreateGroupResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateInstanceProfile

Creates a new instance profile.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateInstanceProfile
&Version=2010-05-08
&InstanceProfileName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 615
date: Thu, 26 Nov 2026 18:30:05 GMT

<CreateInstanceProfileResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateInstanceProfileResult>
    <InstanceProfile xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <InstanceProfileName>example</InstanceProfileName>
      <InstanceProfileId>AIPABGXEQES5NRCCJJWAN</InstanceProfileId>
      <Arn>arn:aws:iam::000000000000:instance-profile/example</Arn>
      <CreateDate>2026-11-26T18:30:05.514Z</CreateDate>
      <Roles></Roles>
      <Tags></Tags>
    </InstanceProfile>
  </CreateInstanceProfileResult>
  <ResponseMetadata>
    <RequestId>c3796a0b-d1ba-4dd7-883d-4610f2b7c4aa</RequestId>
  </ResponseMetadata>
</CreateInstanceProfileResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateLoginProfile

Creates a password for the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateLoginProfile
&Version=2010-05-08
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: a18c9a1a-d93c-4c8b-bfa5-711a44ffbb5b
content-length: 301
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Password must be specified unless the request is made with an AssumeRoot session</Message>
  </Error>
  <RequestId>a18c9a1a-d93c-4c8b-bfa5-711a44ffbb5b</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CreateOpenIDConnectProvider

Creates an IAM entity to describe an identity provider (IdP) that supports OpenID Connect (OIDC) .

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 65
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateOpenIDConnectProvider
&Version=2010-05-08
&Url=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 1ad00b57-2ee4-4861-beef-fd3bd3d017a1
content-length: 199
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
  </Error>
  <RequestId>1ad00b57-2ee4-4861-beef-fd3bd3d017a1</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### CreatePolicy

Creates a new managed policy for your Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 80
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreatePolicy
&Version=2010-05-08
&PolicyName=example
&PolicyDocument=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 823752a9-2ddb-4533-ab23-1cce88567947
content-length: 253
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>MalformedPolicyDocument</Code>
    <Message>Syntax errors in policy.</Message>
  </Error>
  <RequestId>823752a9-2ddb-4533-ab23-1cce88567947</RequestId>
</ErrorResponse>
```

`400` `MalformedPolicyDocument`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-createpolicy) for a successful call.

### CreatePolicyVersion

Creates a new version of the specified managed policy.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 141
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreatePolicyVersion
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&PolicyDocument=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 770cde9e-11b1-4731-8a6d-f7d2ad2cc643
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>770cde9e-11b1-4731-8a6d-f7d2ad2cc643</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### CreateRole

Creates a new role for your Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 86
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateRole
&Version=2010-05-08
&RoleName=example
&AssumeRolePolicyDocument=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: c761b718-8b4e-4acd-9774-69759a12fe69
content-length: 262
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>MalformedPolicyDocument</Code>
    <Message>This policy contains invalid Json</Message>
  </Error>
  <RequestId>c761b718-8b4e-4acd-9774-69759a12fe69</RequestId>
</ErrorResponse>
```

`400` `MalformedPolicyDocument`: the placeholder values failed the operation's input validation.

### CreateSAMLProvider

Creates an IAM resource that describes an identity provider (IdP) that supports SAML 2.0.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 1079
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateSAMLProvider
&Version=2010-05-08
&SAMLMetadataDocument=example000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
&Name=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 668dd01c-58b6-4a03-919c-bbe9565073df
content-length: 248
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Invalid SAML metadata document</Message>
  </Error>
  <RequestId>668dd01c-58b6-4a03-919c-bbe9565073df</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### CreateServiceLinkedRole

Creates an IAM role that is linked to a specific Amazon Web Services service.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 72
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateServiceLinkedRole
&Version=2010-05-08
&AWSServiceName=example
```

```http
HTTP/1.1 403 Forbidden
content-type: text/xml
x-amzn-requestid: 249a8c1f-2a79-4be5-98e7-dfdaf4d24503
content-length: 277
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>AccessDenied</Code>
    <Message>example is not a valid AWS service name (Service Principal)</Message>
  </Error>
  <RequestId>249a8c1f-2a79-4be5-98e7-dfdaf4d24503</RequestId>
</ErrorResponse>
```

`403` `AccessDenied`: the request was refused by authorization.

### CreateServiceSpecificCredential

Generates a set of credentials consisting of a user name and password that can be used to access the service specified in the request.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 94
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateServiceSpecificCredential
&Version=2010-05-08
&UserName=example
&ServiceName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: bd30cfdb-33f3-4b18-b991-b748ef38e1c4
content-length: 287
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>No such service example is supported for Service Specific Credentials</Message>
  </Error>
  <RequestId>bd30cfdb-33f3-4b18-b991-b748ef38e1c4</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateUser

Creates a new IAM user for your Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateUser
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 478
date: Thu, 26 Nov 2026 18:30:05 GMT

<CreateUserResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateUserResult>
    <User xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <UserName>example</UserName>
      <UserId>AIDAOWVOJOY3VBWOJCSHY</UserId>
      <Arn>arn:aws:iam::000000000000:user/example</Arn>
      <CreateDate>2026-11-26T18:30:05.521Z</CreateDate>
      <Tags></Tags>
    </User>
  </CreateUserResult>
  <ResponseMetadata>
    <RequestId>0d2d71e4-808c-4b0f-809e-30fb07a3896e</RequestId>
  </ResponseMetadata>
</CreateUserResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#1-createuser) for a successful call.

### CreateVirtualMFADevice

Creates a new virtual MFA device for the Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateVirtualMFADevice
&Version=2010-05-08
&VirtualMFADeviceName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1826
date: Thu, 26 Nov 2026 18:30:05 GMT

<CreateVirtualMFADeviceResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateVirtualMFADeviceResult>
    <VirtualMFADevice xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <SerialNumber>arn:aws:iam::000000000000:mfa/example</SerialNumber>
      <Base32StringSeed>SUZKVU1TRE9HWlhGTTJMR0pSTFVNWkJZSEJCRVFUU0k=</Base32StringSeed>
      <QRCodePNG><redacted></QRCodePNG>
      <Tags></Tags>
    </VirtualMFADevice>
  </CreateVirtualMFADeviceResult>
  <ResponseMetadata>
    <RequestId>06c9d216-4e9e-4f3f-9265-f410817a7b32</RequestId>
  </ResponseMetadata>
</CreateVirtualMFADeviceResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeactivateMFADevice

Deactivates the specified MFA device and removes it from association with the user name for which it was originally enabled.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeactivateMFADevice
&Version=2010-05-08
&SerialNumber=example00
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: d4e8087e-2607-4b69-84f7-fd95124a5669
content-length: 273
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>MFA Device with serial number example00 does not exist.</Message>
  </Error>
  <RequestId>d4e8087e-2607-4b69-84f7-fd95124a5669</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteAccessKey

Deletes the access key pair associated with the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteAccessKey
&Version=2010-05-08
&AccessKeyId=example000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: b3b89230-efb1-4e91-84e1-7f0b8cddc376
content-length: 274
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Access Key with id example000000000 cannot be found.</Message>
  </Error>
  <RequestId>b3b89230-efb1-4e91-84e1-7f0b8cddc376</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#7-deleteaccesskey) for a successful call.

### DeleteAccountAlias

Deletes the specified Amazon Web Services account alias.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 65
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteAccountAlias
&Version=2010-05-08
&AccountAlias=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 256
date: Thu, 26 Nov 2026 18:30:05 GMT

<DeleteAccountAliasResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteAccountAliasResult></DeleteAccountAliasResult>
  <ResponseMetadata>
    <RequestId>99595f52-f111-4f22-ad70-d1300c719b39</RequestId>
  </ResponseMetadata>
</DeleteAccountAliasResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteAccountPasswordPolicy

Deletes the password policy for the Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteAccountPasswordPolicy
&Version=2010-05-08
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 8995e220-9288-4f18-a521-f8b973381f8a
content-length: 278
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The account policy with name PasswordPolicy cannot be found.</Message>
  </Error>
  <RequestId>8995e220-9288-4f18-a521-f8b973381f8a</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteGroup

Deletes the specified IAM group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 55
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteGroup
&Version=2010-05-08
&GroupName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 228
date: Thu, 26 Nov 2026 18:30:05 GMT

<DeleteGroupResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteGroupResult></DeleteGroupResult>
  <ResponseMetadata>
    <RequestId>f7e84dc7-a641-4069-81ce-647ac103570e</RequestId>
  </ResponseMetadata>
</DeleteGroupResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteGroupPolicy

Deletes the specified inline policy that is embedded in the specified IAM group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 80
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteGroupPolicy
&Version=2010-05-08
&GroupName=example
&PolicyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 0df24ae7-51e6-441b-9b6f-f97aa91ccb77
content-length: 262
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The group with name example cannot be found.</Message>
  </Error>
  <RequestId>0df24ae7-51e6-441b-9b6f-f97aa91ccb77</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteInstanceProfile

Deletes the specified instance profile.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteInstanceProfile
&Version=2010-05-08
&InstanceProfileName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 268
date: Thu, 26 Nov 2026 18:30:05 GMT

<DeleteInstanceProfileResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteInstanceProfileResult></DeleteInstanceProfileResult>
  <ResponseMetadata>
    <RequestId>a130163c-1730-4c3a-80fa-9a0eb9818696</RequestId>
  </ResponseMetadata>
</DeleteInstanceProfileResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteLoginProfile

Deletes the password for the specified IAM user or root user, For more information, see Managing passwords for IAM users .

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteLoginProfile
&Version=2010-05-08
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: c2e41420-d3a1-4863-a3d6-c2cf3c427f9f
content-length: 266
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Login Profile for User emulator cannot be found.</Message>
  </Error>
  <RequestId>c2e41420-d3a1-4863-a3d6-c2cf3c427f9f</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteOpenIDConnectProvider

Deletes an OpenID Connect identity provider (IdP) resource object in IAM.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 141
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteOpenIDConnectProvider
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 7258e7a8-70e4-4ad7-b752-38ef9ae59be2
content-length: 309
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenId connect Provider arn:aws:iam:us-east-1:000000000000:example/example cannot be found.</Message>
  </Error>
  <RequestId>7258e7a8-70e4-4ad7-b752-38ef9ae59be2</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeletePolicy

Deletes the specified managed policy.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 111
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeletePolicy
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 01d34a1a-4425-4a2a-b03b-45f8b6484148
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>01d34a1a-4425-4a2a-b03b-45f8b6484148</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation. See [the walkthrough](#9-deletepolicy) for a successful call.

### DeletePolicyVersion

Deletes the specified version from the specified managed policy.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 136
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeletePolicyVersion
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&VersionId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: cd45ce39-d033-45d2-9e70-1d7bb01c7c5c
content-length: 360
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value at &apos;versionId&apos; failed to satisfy constraint: Member must satisfy regular expression pattern: v[1-9][0-9]*(\.[A-Za-z0-9-]*)?</Message>
  </Error>
  <RequestId>cd45ce39-d033-45d2-9e70-1d7bb01c7c5c</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteRole

Deletes the specified role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteRole
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 3a0039e6-1dd4-45e1-b21e-004b622ae078
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>3a0039e6-1dd4-45e1-b21e-004b622ae078</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteRolePermissionsBoundary

Deletes the permissions boundary for the specified IAM role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 72
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteRolePermissionsBoundary
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: b6340c70-4e7c-41fc-a05d-d24708034e8f
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>b6340c70-4e7c-41fc-a05d-d24708034e8f</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteRolePolicy

Deletes the specified inline policy that is embedded in the specified IAM role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 78
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteRolePolicy
&Version=2010-05-08
&RoleName=example
&PolicyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: c72460bc-f1a6-425d-9e6a-a90e622709ae
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>c72460bc-f1a6-425d-9e6a-a90e622709ae</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteSAMLProvider

Deletes a SAML provider resource in IAM.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 123
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteSAMLProvider
&Version=2010-05-08
&SAMLProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: f763c4f5-8eb9-4eaa-9e2f-0a3140d59dbb
content-length: 249
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Invalid resource type in ARN</Message>
  </Error>
  <RequestId>f763c4f5-8eb9-4eaa-9e2f-0a3140d59dbb</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteSSHPublicKey

Deletes the specified SSH public key.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 97
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteSSHPublicKey
&Version=2010-05-08
&UserName=example
&SSHPublicKeyId=example0000000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: ee809424-b650-4172-852d-7ca8ee5031ff
content-length: 278
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Public Key with id example0000000000000 cannot be found.</Message>
  </Error>
  <RequestId>ee809424-b650-4172-852d-7ca8ee5031ff</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteServerCertificate

Deletes the specified server certificate.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 79
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteServerCertificate
&Version=2010-05-08
&ServerCertificateName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 101e2628-ccf4-425d-bd02-b1db47a4713b
content-length: 275
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Server Certificate with name example cannot be found.</Message>
  </Error>
  <RequestId>101e2628-ccf4-425d-bd02-b1db47a4713b</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteServiceLinkedRole

Submits a service-linked role deletion request and returns a DeletionTaskId , which you can use to check the status of the deletion.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteServiceLinkedRole
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 7275439e-9ff0-4f8d-9f9e-640fa63ed3ce
content-length: 257
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Cannot find the service role to delete.</Message>
  </Error>
  <RequestId>7275439e-9ff0-4f8d-9f9e-640fa63ed3ce</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteServiceSpecificCredential

Deletes the specified service-specific credential.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 106
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteServiceSpecificCredential
&Version=2010-05-08
&ServiceSpecificCredentialId=example0000000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 4cbd9262-9eef-4785-959e-e06aecf3cc87
content-length: 264
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>No such credential example0000000000000 exists</Message>
  </Error>
  <RequestId>4cbd9262-9eef-4785-959e-e06aecf3cc87</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteSigningCertificate

Deletes a signing certificate associated with the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 89
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteSigningCertificate
&Version=2010-05-08
&CertificateId=example00000000000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 91070b7a-eb31-47c8-a3c5-376138599475
content-length: 283
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Certificate with id example00000000000000000 cannot be found.</Message>
  </Error>
  <RequestId>91070b7a-eb31-47c8-a3c5-376138599475</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteUser

Deletes the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteUser
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 224
date: Thu, 26 Nov 2026 18:30:05 GMT

<DeleteUserResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteUserResult></DeleteUserResult>
  <ResponseMetadata>
    <RequestId>6b4f7623-1b7b-458d-8f38-6a581945569c</RequestId>
  </ResponseMetadata>
</DeleteUserResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#10-deleteuser) for a successful call.

### DeleteUserPermissionsBoundary

Deletes the permissions boundary for the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 72
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteUserPermissionsBoundary
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 8729b64c-b026-4a7a-b0e4-28b0534239e5
content-length: 245
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>User example was not found.</Message>
  </Error>
  <RequestId>8729b64c-b026-4a7a-b0e4-28b0534239e5</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteUserPolicy

Deletes the specified inline policy that is embedded in the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 78
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteUserPolicy
&Version=2010-05-08
&UserName=example
&PolicyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: be34a367-d13a-4329-9dd6-6cc7751e8ecb
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>be34a367-d13a-4329-9dd6-6cc7751e8ecb</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteVirtualMFADevice

Deletes a virtual MFA device.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteVirtualMFADevice
&Version=2010-05-08
&SerialNumber=example00
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 9f3f2f78-17b4-4d55-8ce8-7455a739ea70
content-length: 273
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>MFA Device with serial number example00 does not exist.</Message>
  </Error>
  <RequestId>9f3f2f78-17b4-4d55-8ce8-7455a739ea70</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DetachGroupPolicy

Removes the specified managed policy from the specified IAM group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 134
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DetachGroupPolicy
&Version=2010-05-08
&GroupName=example
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: aaec8584-7a02-48d0-9d54-b98260c0487d
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>aaec8584-7a02-48d0-9d54-b98260c0487d</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### DetachRolePolicy

Removes the specified managed policy from the specified role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 132
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DetachRolePolicy
&Version=2010-05-08
&RoleName=example
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 2d510047-f211-41a9-90c4-3fc972801b09
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>2d510047-f211-41a9-90c4-3fc972801b09</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### DetachUserPolicy

Removes the specified managed policy from the specified user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 132
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DetachUserPolicy
&Version=2010-05-08
&UserName=example
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 8c79f901-d331-43c9-8459-240c5b6ee9d0
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>8c79f901-d331-43c9-8459-240c5b6ee9d0</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation. See [the walkthrough](#8-detachuserpolicy) for a successful call.

### DisableOrganizationsRootCredentialsManagement

Disables the management of privileged root user credentials across member accounts in your organization.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DisableOrganizationsRootCredentialsManagement
&Version=2010-05-08
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: 6e1d6219-2629-4c13-971f-582ab050a281
content-length: 328
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DisableOrganizationsRootCredentialsManagement is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>6e1d6219-2629-4c13-971f-582ab050a281</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisableOrganizationsRootSessions

Disables root user sessions for privileged tasks across member accounts in your organization.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 58
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DisableOrganizationsRootSessions
&Version=2010-05-08
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: 7ea48d12-f5b8-4cdc-8d9f-76e720707d77
content-length: 315
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DisableOrganizationsRootSessions is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>7ea48d12-f5b8-4cdc-8d9f-76e720707d77</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisableOutboundWebIdentityFederation

Disables the outbound identity federation feature for your Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 62
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DisableOutboundWebIdentityFederation
&Version=2010-05-08
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: cc355538-b55d-4693-a1c0-1a396a91fdaa
content-length: 294
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>FeatureDisabled</Code>
    <Message>Outbound identity federation is already disabled for account 000000000000</Message>
  </Error>
  <RequestId>cc355538-b55d-4693-a1c0-1a396a91fdaa</RequestId>
</ErrorResponse>
```

`404` `FeatureDisabled`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### EnableMFADevice

Enables the specified MFA device and associates it with the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 135
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
x-amzn-requestid: 9aa780fe-d381-45c8-930d-e992ee9e8e1c
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>9aa780fe-d381-45c8-930d-e992ee9e8e1c</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### EnableOrganizationsRootCredentialsManagement

Enables the management of privileged root user credentials across member accounts in your organization.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=EnableOrganizationsRootCredentialsManagement
&Version=2010-05-08
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: 6389b778-6e20-4c85-aafa-e27ac964a90c
content-length: 327
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation EnableOrganizationsRootCredentialsManagement is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>6389b778-6e20-4c85-aafa-e27ac964a90c</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableOrganizationsRootSessions

Allows the management account or delegated administrator to perform privileged tasks on member accounts in your organization.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 57
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=EnableOrganizationsRootSessions
&Version=2010-05-08
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: a9941b0c-cde3-4a87-976d-54c443c9136b
content-length: 314
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation EnableOrganizationsRootSessions is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>a9941b0c-cde3-4a87-976d-54c443c9136b</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableOutboundWebIdentityFederation

Enables the outbound identity federation feature for your Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=EnableOutboundWebIdentityFederation
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 416
date: Thu, 26 Nov 2026 18:30:05 GMT

<EnableOutboundWebIdentityFederationResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <EnableOutboundWebIdentityFederationResult>
    <IssuerIdentifier>https://127.0.0.1:9297/F7B11509F4D675C3C44F0DD37CA830BB</IssuerIdentifier>
  </EnableOutboundWebIdentityFederationResult>
  <ResponseMetadata>
    <RequestId>d6805995-45e1-4905-b41b-01977d2b6d01</RequestId>
  </ResponseMetadata>
</EnableOutboundWebIdentityFederationResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GenerateCredentialReport

Generates a credential report for the Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GenerateCredentialReport
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 384
date: Thu, 26 Nov 2026 18:30:05 GMT

<GenerateCredentialReportResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <GenerateCredentialReportResult>
    <State>STARTED</State>
    <Description>No report exists. Starting a new report generation task</Description>
  </GenerateCredentialReportResult>
  <ResponseMetadata>
    <RequestId>cb277cf9-4afb-492a-af62-0b9221b7252d</RequestId>
  </ResponseMetadata>
</GenerateCredentialReportResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GenerateOrganizationsAccessReport

Generates a report for service last accessed data for Organizations.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 90
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GenerateOrganizationsAccessReport
&Version=2010-05-08
&EntityPath=example000000000000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 1d89ef3a-6a4f-4ca7-b047-b67a79d0f023
content-length: 378
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value at &apos;entityPath&apos; failed to satisfy constraint: Member must satisfy regular expression pattern: o-[0-9a-z]{10,32}\/r-[0-9a-z]{4,32}[0-9a-z-\/]*</Message>
  </Error>
  <RequestId>1d89ef3a-6a4f-4ca7-b047-b67a79d0f023</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GenerateServiceLastAccessedDetails

Generates a report that includes details about when an IAM resource (user, group, role, or policy) was last used in an attempt to access Amazon Web Services services.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 127
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GenerateServiceLastAccessedDetails
&Version=2010-05-08
&Arn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: e261ef5c-afe4-4c5e-ae92-3f8cbd30ed7b
content-length: 352
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid: Arn must be the ARN of an IAM user, group, role or managed policy</Message>
  </Error>
  <RequestId>e261ef5c-afe4-4c5e-ae92-3f8cbd30ed7b</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### GetAccessKeyLastUsed

Retrieves information about when the specified access key was last used.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetAccessKeyLastUsed
&Version=2010-05-08
&AccessKeyId=example000000000
```

```http
HTTP/1.1 403 Forbidden
content-type: text/xml
x-amzn-requestid: 431db870-7c39-429d-bacb-01e4517ce88c
content-length: 358
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>AccessDenied</Code>
    <Message>User: arn:aws:iam::000000000000:user/emulator is not authorized to perform iam:GetAccessKeyLastUsed on resource: access key example000000000</Message>
  </Error>
  <RequestId>431db870-7c39-429d-bacb-01e4517ce88c</RequestId>
</ErrorResponse>
```

`403` `AccessDenied`: the request was refused by authorization.

### GetAccountAuthorizationDetails

Retrieves information about all IAM users, groups, roles, and policies in your Amazon Web Services account, including their relationships to one another.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 56
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetAccountAuthorizationDetails
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 7255
date: Thu, 26 Nov 2026 18:30:05 GMT

<GetAccountAuthorizationDetailsResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <GetAccountAuthorizationDetailsResult>
    <UserDetailList>
      <member xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
        <Path>/</Path>
        <UserName>emulator</UserName>
        <UserId>AIDAH5I7GEMO7ADXNFAZU</UserId>
        <Arn>arn:aws:iam::000000000000:user/emulator</Arn>
        <CreateDate>1970-01-01T00:00:00Z</CreateDate>
        <UserPolicyList></UserPolicyList>
        <GroupList></GroupList>
        <AttachedManagedPolicies></AttachedManagedPolicies>
        <Tags></Tags>
      </member>
    </UserDetailList>
    <GroupDetailList></GroupDetailList>
    <RoleDetailList>
      <member>
        <Path>/aws-service-role/dynamodb.application-autoscaling.amazonaws.com/</Path>
        <RoleName>AWSServiceRoleForApplicationAutoScaling_DynamoDBTable</RoleName>
… (120 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetAccountPasswordPolicy

Retrieves the password policy for the Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetAccountPasswordPolicy
&Version=2010-05-08
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 6ec3b347-1ea0-4546-8113-79b67cd98466
content-length: 284
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Password Policy with domain name 000000000000 cannot be found.</Message>
  </Error>
  <RequestId>6ec3b347-1ea0-4546-8113-79b67cd98466</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetAccountProperties

Retrieves the account-level properties for the caller's Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetAccountProperties
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 354
date: Thu, 26 Nov 2026 18:30:05 GMT

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
    <RequestId>ad4d31f1-1acf-48b4-b9ad-19ede456d089</RequestId>
  </ResponseMetadata>
</GetAccountPropertiesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetAccountSummary

Retrieves information about IAM entity usage and IAM quotas in the Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetAccountSummary
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 2365
date: Thu, 26 Nov 2026 18:30:05 GMT

<GetAccountSummaryResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <GetAccountSummaryResult>
    <SummaryMap>
      <entry>
        <key>VersionsPerPolicyQuota</key>
        <value>5</value>
      </entry>
      <entry>
        <key>MFADevices</key>
        <value>1</value>
      </entry>
      <entry>
        <key>Groups</key>
        <value>0</value>
      </entry>
      <entry>
        <key>SigningCertificatesPerUserQuota</key>
        <value>2</value>
      </entry>
      <entry>
… (125 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetContextKeysForCustomPolicy

Gets a list of all of the context keys referenced in the input policies.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 88
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetContextKeysForCustomPolicy
&Version=2010-05-08
&PolicyInputList.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: e7d6f20f-ec77-4778-ae6b-eeebc2b5ce90
content-length: 262
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Policy input list item 1 has invalid content</Message>
  </Error>
  <RequestId>e7d6f20f-ec77-4778-ae6b-eeebc2b5ce90</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### GetContextKeysForPrincipalPolicy

Gets a list of all of the context keys referenced in all the IAM policies that are attached to the specified IAM entity.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 137
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetContextKeysForPrincipalPolicy
&Version=2010-05-08
&PolicySourceArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 1048d480-0253-4062-bf23-e605a89fb0e2
content-length: 305
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Invalid ARN provided in the request: arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>1048d480-0253-4062-bf23-e605a89fb0e2</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### GetCredentialReport

Retrieves a credential report for the Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetCredentialReport
&Version=2010-05-08
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 5fceb580-895e-4978-a136-43143d567c75
content-length: 203
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ReportInProgress</Code>
  </Error>
  <RequestId>5fceb580-895e-4978-a136-43143d567c75</RequestId>
</ErrorResponse>
```

`404` `ReportInProgress`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetDelegationRequest

Retrieves information about a specific delegation request.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 83
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetDelegationRequest
&Version=2010-05-08
&DelegationRequestId=example000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 16c04dcc-c6b0-4ad7-bc80-ba5c6a9a00e9
content-length: 263
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>DelegationRequest not found: example000000000</Message>
  </Error>
  <RequestId>16c04dcc-c6b0-4ad7-bc80-ba5c6a9a00e9</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetGroup

Returns a list of IAM users that are in the specified IAM group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetGroup
&Version=2010-05-08
&GroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: cd0b86f2-9391-40b5-8827-c08fff8a459b
content-length: 262
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The group with name example cannot be found.</Message>
  </Error>
  <RequestId>cd0b86f2-9391-40b5-8827-c08fff8a459b</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetGroupPolicy

Retrieves the specified inline policy document that is embedded in the specified IAM group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetGroupPolicy
&Version=2010-05-08
&GroupName=example
&PolicyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: cd70c251-582d-4399-bdf3-a813ce8a7e1d
content-length: 262
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The group with name example cannot be found.</Message>
  </Error>
  <RequestId>cd70c251-582d-4399-bdf3-a813ce8a7e1d</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetHumanReadableSummary

Retrieves a human readable summary for a given entity.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 122
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetHumanReadableSummary
&Version=2010-05-08
&EntityArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 2456ef34-be37-4200-aa9f-37e0310e9353
content-length: 249
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Invalid resource type in ARN</Message>
  </Error>
  <RequestId>2456ef34-be37-4200-aa9f-37e0310e9353</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetInstanceProfile

Retrieves information about the specified instance profile, including the instance profile's path, GUID, ARN, and role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 72
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetInstanceProfile
&Version=2010-05-08
&InstanceProfileName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: de8e00ff-236e-44ad-87ee-daa4baf0162a
content-length: 259
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Instance Profile example cannot be found.</Message>
  </Error>
  <RequestId>de8e00ff-236e-44ad-87ee-daa4baf0162a</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetLoginProfile

Retrieves the user name for the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 41
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetLoginProfile
&Version=2010-05-08
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: de0ea91c-8ebf-423b-b704-13915d4fd895
content-length: 266
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Login Profile for User emulator cannot be found.</Message>
  </Error>
  <RequestId>de0ea91c-8ebf-423b-b704-13915d4fd895</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetMFADevice

Retrieves information about an MFA device for a specified user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetMFADevice
&Version=2010-05-08
&SerialNumber=example00
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 888ef4b9-41c6-4646-9c6c-469148508048
content-length: 410
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>The MFA device serial number sent in the request is invalid or not supported by this API. Currently, only hardware FIDO security keys are supported. Please use the compatible device or API.</Message>
  </Error>
  <RequestId>888ef4b9-41c6-4646-9c6c-469148508048</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetOpenIDConnectProvider

Returns information about the specified OpenID Connect (OIDC) provider resource object in IAM.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 138
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetOpenIDConnectProvider
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 91ad42ba-54d9-49f4-8044-701496f88b0a
content-length: 309
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenIDConnect Provider not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>91ad42ba-54d9-49f4-8044-701496f88b0a</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetOrganizationsAccessReport

Retrieves the service last accessed data report for Organizations that was previously generated using the GenerateOrganizationsAccessReport operation.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 97
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetOrganizationsAccessReport
&Version=2010-05-08
&JobId=example00000000000000000000000000000
```

```http
HTTP/1.1 403 Forbidden
content-type: text/xml
x-amzn-requestid: e4ac5dbc-aeb0-4d56-8cd3-61754c58a575
content-length: 267
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>AccessDenied</Code>
    <Message>You are not authorized to perform this operation.</Message>
  </Error>
  <RequestId>e4ac5dbc-aeb0-4d56-8cd3-61754c58a575</RequestId>
</ErrorResponse>
```

`403` `AccessDenied`: the request was refused by authorization.

### GetOutboundWebIdentityFederationInfo

Retrieves the configuration information for the outbound identity federation feature in your Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 62
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetOutboundWebIdentityFederationInfo
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 463
date: Thu, 26 Nov 2026 18:30:05 GMT

<GetOutboundWebIdentityFederationInfoResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <GetOutboundWebIdentityFederationInfoResult>
    <IssuerIdentifier>https://127.0.0.1:9297/F7B11509F4D675C3C44F0DD37CA830BB</IssuerIdentifier>
    <JwtVendingEnabled>true</JwtVendingEnabled>
  </GetOutboundWebIdentityFederationInfoResult>
  <ResponseMetadata>
    <RequestId>670d351f-4471-45e1-83e4-b8c58ce851c9</RequestId>
  </ResponseMetadata>
</GetOutboundWebIdentityFederationInfoResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetPolicy

Retrieves information about the specified managed policy, including the policy's default version and the total number of IAM users, groups, and roles to which the policy is attached.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 108
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetPolicy
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: aa382f20-96a3-4c2d-bc77-d2c4513d07af
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>aa382f20-96a3-4c2d-bc77-d2c4513d07af</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### GetPolicyVersion

Retrieves information about the specified version of the specified managed policy, including the policy document.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 133
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetPolicyVersion
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&VersionId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 36eaf6b9-e058-401a-a06e-e887d673cc80
content-length: 360
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value at &apos;versionId&apos; failed to satisfy constraint: Member must satisfy regular expression pattern: v[1-9][0-9]*(\.[A-Za-z0-9-]*)?</Message>
  </Error>
  <RequestId>36eaf6b9-e058-401a-a06e-e887d673cc80</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetRole

Retrieves information about the specified role, including the role's path, GUID, ARN, and the role's trust policy that grants permission to assume the role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetRole
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: d7b6a1e3-6b62-44da-88e2-73cc0f4064a0
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>d7b6a1e3-6b62-44da-88e2-73cc0f4064a0</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetRolePolicy

Retrieves the specified inline policy document that is embedded with the specified IAM role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetRolePolicy
&Version=2010-05-08
&RoleName=example
&PolicyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 70a7e708-b5e2-4feb-91dc-27ff260318e3
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>70a7e708-b5e2-4feb-91dc-27ff260318e3</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetRoleTemplateVersion

Retrieves information about a version of the specified role template.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 123
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetRoleTemplateVersion
&Version=2010-05-08
&TemplateArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 7e5d57c7-5f34-46d3-89ac-db8c8b60dcea
content-length: 439
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>The specified role template ARN is not valid. Expected format is: arn:{partition}:iam::aws:role-template/{servicePrincipal}/{templateName}:{majorVersion} (majorVersion must be a positive integer with no leading zeros).</Message>
  </Error>
  <RequestId>7e5d57c7-5f34-46d3-89ac-db8c8b60dcea</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetSAMLProvider

Returns the SAML provider metadocument that was uploaded when the IAM SAML provider resource object was created or updated.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 120
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetSAMLProvider
&Version=2010-05-08
&SAMLProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 933f9cb2-abc3-4e62-986a-6a5233085bc8
content-length: 249
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Invalid resource type in ARN</Message>
  </Error>
  <RequestId>933f9cb2-abc3-4e62-986a-6a5233085bc8</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetSSHPublicKey

Retrieves the specified SSH public key, including metadata about the key.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 107
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetSSHPublicKey
&Version=2010-05-08
&UserName=example
&SSHPublicKeyId=example0000000000000
&Encoding=SSH
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: a97922d6-afca-4242-9ea0-2fbc6c558bf4
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>a97922d6-afca-4242-9ea0-2fbc6c558bf4</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetServerCertificate

Retrieves information about the specified server certificate stored in IAM.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetServerCertificate
&Version=2010-05-08
&ServerCertificateName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 2f04181b-cbea-40ee-8d79-4c9d62d20399
content-length: 275
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Server Certificate with name example cannot be found.</Message>
  </Error>
  <RequestId>2f04181b-cbea-40ee-8d79-4c9d62d20399</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetServiceLastAccessedDetails

Retrieves a service last accessed report that was created using the GenerateServiceLastAccessedDetails operation.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 98
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetServiceLastAccessedDetails
&Version=2010-05-08
&JobId=example00000000000000000000000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: ec96b532-617d-42dc-adb6-d66a1719a68c
content-length: 274
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Job example00000000000000000000000000000 does not exist.</Message>
  </Error>
  <RequestId>ec96b532-617d-42dc-adb6-d66a1719a68c</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetServiceLastAccessedDetailsWithEntities

After you generate a group or policy report using the GenerateServiceLastAccessedDetails operation, you can use the JobId parameter in GetServiceLastAccessedDetailsWithEntities .

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 135
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetServiceLastAccessedDetailsWithEntities
&Version=2010-05-08
&JobId=example00000000000000000000000000000
&ServiceNamespace=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: b8574e9e-4f6c-4819-885c-4289c7c8e202
content-length: 274
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Job example00000000000000000000000000000 does not exist.</Message>
  </Error>
  <RequestId>b8574e9e-4f6c-4819-885c-4289c7c8e202</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetServiceLinkedRoleDeletionStatus

Retrieves the status of your service-linked role deletion.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 83
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetServiceLinkedRoleDeletionStatus
&Version=2010-05-08
&DeletionTaskId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 90f891d8-1c83-4fbf-8b30-358aa6bcd1eb
content-length: 386
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>The deletion task ID example is not valid: DeletionTaskId must be in the format task/aws-service-role/&lt;service-principal-name&gt;/&lt;role-name&gt;/&lt;task-uuid&gt;</Message>
  </Error>
  <RequestId>90f891d8-1c83-4fbf-8b30-358aa6bcd1eb</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### GetUser

Retrieves information about the specified IAM user, including the user's creation date, path, unique ID, and ARN.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 33
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetUser
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 464
date: Thu, 26 Nov 2026 18:30:05 GMT

<GetUserResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <GetUserResult>
    <User xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <UserName>emulator</UserName>
      <UserId>AIDAH5I7GEMO7ADXNFAZU</UserId>
      <Arn>arn:aws:iam::000000000000:user/emulator</Arn>
      <CreateDate>1970-01-01T00:00:00Z</CreateDate>
      <Tags></Tags>
    </User>
  </GetUserResult>
  <ResponseMetadata>
    <RequestId>6cd566a2-39b8-4f99-bc71-f50eccdc9e13</RequestId>
  </ResponseMetadata>
</GetUserResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetUserPolicy

Retrieves the specified inline policy document that is embedded in the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetUserPolicy
&Version=2010-05-08
&UserName=example
&PolicyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 1749a03e-e37b-4235-90c8-30bac9be266b
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>1749a03e-e37b-4235-90c8-30bac9be266b</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListAccessKeys

Returns information about the access key IDs associated with the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 40
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListAccessKeys
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 686
date: Thu, 26 Nov 2026 18:30:05 GMT

<ListAccessKeysResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListAccessKeysResult>
    <AccessKeyMetadata>
      <member xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
        <UserName>emulator</UserName>
        <AccessKeyId>EMULATORAKNGDWTSXFHM</AccessKeyId>
        <Status>Active</Status>
        <CreateDate>2026-11-26T18:30:05.51Z</CreateDate>
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
    <RequestId>c751d32e-c5a6-4933-98a1-af9220cac025</RequestId>
… (2 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListAccountAliases

Lists the account alias associated with the Amazon Web Services account (Note: you can have only one).

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListAccountAliases
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 321
date: Thu, 26 Nov 2026 18:30:05 GMT

<ListAccountAliasesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListAccountAliasesResult>
    <AccountAliases></AccountAliases>
    <IsTruncated>false</IsTruncated>
  </ListAccountAliasesResult>
  <ResponseMetadata>
    <RequestId>b899d419-5309-4366-9918-d50251ee2f5e</RequestId>
  </ResponseMetadata>
</ListAccountAliasesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListAttachedGroupPolicies

Lists all managed policies that are attached to the specified IAM group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListAttachedGroupMFADevices
&Version=2010-05-08
&GroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 4deef908-3040-4f4c-b734-97e2b1b9befd
content-length: 233
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListAttachedGroupMFADevices</Message>
  </Error>
  <RequestId>4deef908-3040-4f4c-b734-97e2b1b9befd</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListAttachedRolePolicies

Lists all managed policies that are attached to the specified IAM role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 69
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListAttachedRoleMFADevices
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 48d08668-205a-4eb2-93c0-121659ef2ff8
content-length: 232
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListAttachedRoleMFADevices</Message>
  </Error>
  <RequestId>48d08668-205a-4eb2-93c0-121659ef2ff8</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListAttachedUserPolicies

Lists all managed policies that are attached to the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 69
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListAttachedUserMFADevices
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 17c1c006-422c-4635-a522-7c71e843e702
content-length: 232
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListAttachedUserMFADevices</Message>
  </Error>
  <RequestId>17c1c006-422c-4635-a522-7c71e843e702</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`. See [the walkthrough](#4-listattacheduserpolicies) for a successful call.

### ListDelegationRequests

Lists delegation requests based on the specified criteria.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 48
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListDelegationRequests
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 345
date: Thu, 26 Nov 2026 18:30:05 GMT

<ListDelegationRequestsResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListDelegationRequestsResult>
    <DelegationRequests></DelegationRequests>
    <isTruncated>false</isTruncated>
  </ListDelegationRequestsResult>
  <ResponseMetadata>
    <RequestId>e46c8449-a103-4613-b45b-e9b90027afba</RequestId>
  </ResponseMetadata>
</ListDelegationRequestsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListEntitiesForPolicy

Lists all IAM users, groups, and roles that the specified managed policy is attached to.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 120
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListEntitiesForPolicy
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: f6966ba4-8c5c-4259-ac41-2fadb1ecbcf8
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>f6966ba4-8c5c-4259-ac41-2fadb1ecbcf8</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### ListGroupPolicies

Lists the names of the inline policies that are embedded in the specified IAM group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 63
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListGroupMFADevices
&Version=2010-05-08
&GroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 55b29485-b048-4df9-b072-bf8e0cdb08d9
content-length: 225
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListGroupMFADevices</Message>
  </Error>
  <RequestId>55b29485-b048-4df9-b072-bf8e0cdb08d9</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListGroups

Lists the IAM groups that have the specified path prefix.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 36
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListGroups
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 273
date: Thu, 26 Nov 2026 18:30:05 GMT

<ListGroupsResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListGroupsResult>
    <Groups></Groups>
    <IsTruncated>false</IsTruncated>
  </ListGroupsResult>
  <ResponseMetadata>
    <RequestId>c169b3c4-6e4c-4fd2-a9fd-6a4d71c55241</RequestId>
  </ResponseMetadata>
</ListGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListGroupsForUser

Lists the IAM groups that the specified IAM user belongs to.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListGroupsForUser
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: a89328e2-5062-429f-8f42-e0e459a12dd0
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>a89328e2-5062-429f-8f42-e0e459a12dd0</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListInstanceProfileTags

Lists the tags that are attached to the specified IAM instance profile.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListInstanceProfileTags
&Version=2010-05-08
&InstanceProfileName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 187c2d04-e9e0-45e6-a9f6-6f94392d0172
content-length: 259
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Instance Profile example cannot be found.</Message>
  </Error>
  <RequestId>187c2d04-e9e0-45e6-a9f6-6f94392d0172</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListInstanceProfiles

Lists the instance profiles that have the specified path prefix.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListInstanceProfiles
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 333
date: Thu, 26 Nov 2026 18:30:05 GMT

<ListInstanceProfilesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListInstanceProfilesResult>
    <InstanceProfiles></InstanceProfiles>
    <IsTruncated>false</IsTruncated>
  </ListInstanceProfilesResult>
  <ResponseMetadata>
    <RequestId>7a5dac64-a943-45dc-9635-490e10023aaa</RequestId>
  </ResponseMetadata>
</ListInstanceProfilesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListInstanceProfilesForRole

Lists the instance profiles that have the specified associated IAM role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListInstanceProfilesForRole
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: a4e84886-a926-4073-b60a-4ef68c01deb6
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>a4e84886-a926-4073-b60a-4ef68c01deb6</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListMFADeviceTags

Lists the tags that are attached to the specified IAM virtual multi-factor authentication (MFA) device.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListMFADeviceTags
&Version=2010-05-08
&SerialNumber=example00
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 8095ba4f-ee4f-4fdd-8e20-a059a130d93c
content-length: 273
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>MFA Device with serial number example00 does not exist.</Message>
  </Error>
  <RequestId>8095ba4f-ee4f-4fdd-8e20-a059a130d93c</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListMFADevices

Lists the MFA devices for an IAM user.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 40
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListMFADevices
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 297
date: Thu, 26 Nov 2026 18:30:05 GMT

<ListMFADevicesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListMFADevicesResult>
    <MFADevices></MFADevices>
    <IsTruncated>false</IsTruncated>
  </ListMFADevicesResult>
  <ResponseMetadata>
    <RequestId>c10ea78a-5d6d-420e-a746-bce63c9fe4e6</RequestId>
  </ResponseMetadata>
</ListMFADevicesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListOpenIDConnectProviderTags

Lists the tags that are attached to the specified OpenID Connect (OIDC)-compatible identity provider.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 143
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListOpenIDConnectProviderTags
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 6f7876aa-2520-4e11-bd1c-850a096f21bd
content-length: 309
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenIDConnect Provider not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>6f7876aa-2520-4e11-bd1c-850a096f21bd</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListOpenIDConnectProviders

Lists information about the IAM OpenID Connect (OIDC) provider resource objects defined in the Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListOpenIDConnectProviders
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 343
date: Thu, 26 Nov 2026 18:30:05 GMT

<ListOpenIDConnectProvidersResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListOpenIDConnectProvidersResult>
    <OpenIDConnectProviderList></OpenIDConnectProviderList>
  </ListOpenIDConnectProvidersResult>
  <ResponseMetadata>
    <RequestId>44e858c2-6ac2-45e9-9297-e2426a2d9746</RequestId>
  </ResponseMetadata>
</ListOpenIDConnectProvidersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListOrganizationsFeatures

Lists the centralized root access features enabled for your organization.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListOrganizationsFeatures
&Version=2010-05-08
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 42f31931-42a3-40ab-9580-d4caf49bef5d
content-length: 316
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>OrganizationNotFoundException</Code>
    <Message>The request was rejected because no organization is associated with your account.</Message>
  </Error>
  <RequestId>42f31931-42a3-40ab-9580-d4caf49bef5d</RequestId>
</ErrorResponse>
```

`400` `OrganizationNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListPolicies

Lists all the managed policies that are available in your Amazon Web Services account, including your own customer-defined managed policies and all Amazon Web Services managed policies.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 40
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListMFADevices
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 297
date: Thu, 26 Nov 2026 18:30:05 GMT

<ListMFADevicesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListMFADevicesResult>
    <MFADevices></MFADevices>
    <IsTruncated>false</IsTruncated>
  </ListMFADevicesResult>
  <ResponseMetadata>
    <RequestId>6cde18f4-d4d9-4e44-b6cc-b5903e8818ea</RequestId>
  </ResponseMetadata>
</ListMFADevicesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListPoliciesGrantingServiceAccess

Retrieves a list of policies that the IAM identity (user, group, or role) can use to access each specified service.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 163
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListMFADevicesGrantingServiceAccess
&Version=2010-05-08
&Arn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&ServiceNamespaces.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 743900d6-edfe-4744-80ba-803be738b423
content-length: 241
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListMFADevicesGrantingServiceAccess</Message>
  </Error>
  <RequestId>743900d6-edfe-4744-80ba-803be738b423</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListPolicyTags

Lists the tags that are attached to the specified IAM customer managed policy.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 113
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListPolicyTags
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 1fcaed96-e7e9-4730-a243-9ca1466cdaff
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>1fcaed96-e7e9-4730-a243-9ca1466cdaff</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### ListPolicyVersions

Lists information about the versions of the specified managed policy, including the version that is currently set as the policy's default version.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 117
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListPolicyVersions
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: daaa00c7-b6d4-4974-a01b-124e0467622c
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>daaa00c7-b6d4-4974-a01b-124e0467622c</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### ListRolePolicies

Lists the names of the inline policies that are embedded in the specified IAM role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListRoleMFADevices
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: fdac900d-ee19-4d71-9625-6120d88d6360
content-length: 224
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListRoleMFADevices</Message>
  </Error>
  <RequestId>fdac900d-ee19-4d71-9625-6120d88d6360</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListRoleTags

Lists the tags that are attached to the specified role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 55
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListRoleTags
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: f745cee0-d99b-4377-8f0b-6f26b8997778
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>f745cee0-d99b-4377-8f0b-6f26b8997778</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListRoles

Lists the IAM roles that have the specified path prefix.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 35
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListRoles
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 2331
date: Thu, 26 Nov 2026 18:30:05 GMT

<ListRolesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListRolesResult>
    <Roles>
      <member xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
        <Path>/aws-service-role/dynamodb.application-autoscaling.amazonaws.com/</Path>
        <RoleName>AWSServiceRoleForApplicationAutoScaling_DynamoDBTable</RoleName>
        <RoleId>AROABGXEQES5NRCCJJWAN</RoleId>
        <Arn>arn:aws:iam::000000000000:role/aws-service-role/dynamodb.application-autoscaling.amazonaws.com/AWSServiceRoleForApplicationAutoScaling_DynamoDBTable</Arn>
        <CreateDate>2026-11-26T18:27:29.281Z</CreateDate>
        <AssumeRolePolicyDocument>%7B%22Version%22%3A%222012-10-17%22%2C%22Statement%22%3A%5B%7B%22Effect%22%3A%22Allow%22%2C%22Principal%22%3A%7B%22Service%22%3A%22dynamodb.application-autoscaling.amazonaws.com%22%7D%2C%22Action%22%3A%5B%22sts%3AAssumeRole%22%5D%7D%5D%7D</AssumeRolePolicyDocument>
        <MaxSessionDuration>3600</MaxSessionDuration>
      </member>
      <member>
        <Path>/aws-service-role/support.amazonaws.com/</Path>
        <RoleName>AWSServiceRoleForSupport</RoleName>
        <RoleId>AROA65WPD4XO6IA2PEUOZ</RoleId>
        <Arn>arn:aws:iam::000000000000:role/aws-service-role/support.amazonaws.com/AWSServiceRoleForSupport</Arn>
        <CreateDate>1970-01-01T00:00:00Z</CreateDate>
        <AssumeRolePolicyDocument>%7B%22Version%22%3A%222012-10-17%22%2C%22Statement%22%3A%5B%7B%22Effect%22%3A%22Allow%22%2C%22Principal%22%3A%7B%22Service%22%3A%22support.amazonaws.com%22%7D%2C%22Action%22%3A%5B%22sts%3AAssumeRole%22%5D%7D%5D%7D</AssumeRolePolicyDocument>
        <MaxSessionDuration>3600</MaxSessionDuration>
… (17 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListSAMLProviderTags

Lists the tags that are attached to the specified Security Assertion Markup Language (SAML) identity provider.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 125
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListSAMLProviderTags
&Version=2010-05-08
&SAMLProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 6eca393e-ed74-44ac-8010-10d6745f527c
content-length: 249
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Invalid resource type in ARN</Message>
  </Error>
  <RequestId>6eca393e-ed74-44ac-8010-10d6745f527c</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ListSAMLProviders

Lists the SAML provider resource objects defined in IAM in the account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListSAMLProviders
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 289
date: Thu, 26 Nov 2026 18:30:05 GMT

<ListSAMLProvidersResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListSAMLProvidersResult>
    <SAMLProviderList></SAMLProviderList>
  </ListSAMLProvidersResult>
  <ResponseMetadata>
    <RequestId>c51e2548-ba1f-493d-afb3-7b0f0d6f1340</RequestId>
  </ResponseMetadata>
</ListSAMLProvidersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListSSHPublicKeys

Returns information about the SSH public keys associated with the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListSSHPublicKeys
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 315
date: Thu, 26 Nov 2026 18:30:05 GMT

<ListSSHPublicKeysResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListSSHPublicKeysResult>
    <SSHPublicKeys></SSHPublicKeys>
    <IsTruncated>false</IsTruncated>
  </ListSSHPublicKeysResult>
  <ResponseMetadata>
    <RequestId>2106f62b-7a30-418f-b290-6cebffbce1b0</RequestId>
  </ResponseMetadata>
</ListSSHPublicKeysResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListServerCertificateTags

Lists the tags that are attached to the specified IAM server certificate.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 81
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListServerCertificateTags
&Version=2010-05-08
&ServerCertificateName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 27758c36-7a3e-417d-b6cf-00eeb9c6be25
content-length: 275
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Server Certificate with name example cannot be found.</Message>
  </Error>
  <RequestId>27758c36-7a3e-417d-b6cf-00eeb9c6be25</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListServerCertificates

Lists the server certificates stored in IAM that have the specified path prefix.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 48
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListServerCertificates
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 367
date: Thu, 26 Nov 2026 18:30:05 GMT

<ListServerCertificatesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListServerCertificatesResult>
    <ServerCertificateMetadataList></ServerCertificateMetadataList>
    <IsTruncated>false</IsTruncated>
  </ListServerCertificatesResult>
  <ResponseMetadata>
    <RequestId>9263e363-cfd0-48e4-a638-79960d41c98f</RequestId>
  </ResponseMetadata>
</ListServerCertificatesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListServiceSpecificCredentials

Returns information about the service-specific credentials associated with the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 56
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListServiceSpecificCredentials
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 393
date: Thu, 26 Nov 2026 18:30:05 GMT

<ListServiceSpecificCredentialsResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListServiceSpecificCredentialsResult>
    <ServiceSpecificCredentials></ServiceSpecificCredentials>
    <IsTruncated>false</IsTruncated>
  </ListServiceSpecificCredentialsResult>
  <ResponseMetadata>
    <RequestId>9044afdc-aca9-4bb3-8edb-ae52f5c92fa8</RequestId>
  </ResponseMetadata>
</ListServiceSpecificCredentialsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListSigningCertificates

Returns information about the signing certificates associated with the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListSigningCertificates
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 337
date: Thu, 26 Nov 2026 18:30:05 GMT

<ListSigningCertificatesResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <ListSigningCertificatesResult>
    <Certificates></Certificates>
    <IsTruncated>false</IsTruncated>
  </ListSigningCertificatesResult>
  <ResponseMetadata>
    <RequestId>94ed494a-506d-4fad-9598-eb1347af0f30</RequestId>
  </ResponseMetadata>
</ListSigningCertificatesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListUserPolicies

Lists the names of the inline policies embedded in the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListUserMFADevices
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: a77cfd3c-7744-4383-8af9-ec30f8c11269
content-length: 224
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListUserMFADevices</Message>
  </Error>
  <RequestId>a77cfd3c-7744-4383-8af9-ec30f8c11269</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListUserTags

Lists the tags that are attached to the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 55
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListUserTags
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: d7d01dba-71ed-4db2-8136-8ea1b111e8f4
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>d7d01dba-71ed-4db2-8136-8ea1b111e8f4</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListUsers

Lists the IAM users that have the specified path prefix.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 35
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListUsers
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 510
date: Thu, 26 Nov 2026 18:30:05 GMT

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
    <RequestId>2a3375f7-1f19-47df-ba70-2c4245c88fa6</RequestId>
  </ResponseMetadata>
</ListUsersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListVirtualMFADevices

Lists the virtual MFA devices defined in the Amazon Web Services account by assignment status.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListVirtualMFADevices
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 472
date: Thu, 26 Nov 2026 18:30:05 GMT

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
    <RequestId>37ffb0db-a977-40ce-af00-7e2ad81fc263</RequestId>
  </ResponseMetadata>
</ListVirtualMFADevicesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### PutAccountProperties

Sets account-level properties for the caller's Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 106
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PutAccountProperties
&Version=2010-05-08
&Properties.entry.1.key=key
&Properties.entry.1.value=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 67a30d7f-482e-4068-87ed-4c1f4d03c695
content-length: 276
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Property key must be in format Namespace/PropertyName: key</Message>
  </Error>
  <RequestId>67a30d7f-482e-4068-87ed-4c1f4d03c695</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### PutGroupPolicy

Adds or updates an inline policy document that is embedded in the specified IAM group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 100
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PutGroupPolicy
&Version=2010-05-08
&GroupName=example
&PolicyName=example
&PolicyDocument=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: d29e0325-91fa-47e1-9ef4-a27e7192423e
content-length: 262
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The group with name example cannot be found.</Message>
  </Error>
  <RequestId>d29e0325-91fa-47e1-9ef4-a27e7192423e</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PutRolePermissionsBoundary

Adds or updates the policy that is specified as the IAM role's permissions boundary.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 110
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PutRolePermissionsBoundary
&Version=2010-05-08
&RoleName=example
&PermissionsBoundary=example0000000000000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 31084b5d-26bf-49f9-814b-c5af85dd45b0
content-length: 259
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>ARN example0000000000000 is not valid.</Message>
  </Error>
  <RequestId>31084b5d-26bf-49f9-814b-c5af85dd45b0</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutRolePolicy

Adds or updates an inline policy document that is embedded in the specified IAM role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 98
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PutRolePolicy
&Version=2010-05-08
&RoleName=example
&PolicyName=example
&PolicyDocument=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 4b5d80a7-71eb-4b0a-8261-93bc3b81a4d3
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>4b5d80a7-71eb-4b0a-8261-93bc3b81a4d3</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PutUserPermissionsBoundary

Adds or updates the policy that is specified as the IAM user's permissions boundary.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 110
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PutUserPermissionsBoundary
&Version=2010-05-08
&UserName=example
&PermissionsBoundary=example0000000000000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: d4b27313-0a90-4122-a406-9f1c13314422
content-length: 259
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>ARN example0000000000000 is not valid.</Message>
  </Error>
  <RequestId>d4b27313-0a90-4122-a406-9f1c13314422</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutUserPolicy

Adds or updates an inline policy document that is embedded in the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 98
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PutUserPolicy
&Version=2010-05-08
&UserName=example
&PolicyName=example
&PolicyDocument=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: fa348538-7013-42c0-9eb0-b6d6b7ac3178
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>fa348538-7013-42c0-9eb0-b6d6b7ac3178</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RejectDelegationRequest

Rejects a delegation request, denying the requested temporary access.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 86
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RejectDelegationRequest
&Version=2010-05-08
&DelegationRequestId=example000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 38560698-92a8-48ba-b9b8-343b1d23e641
content-length: 264
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Delegation request not found: example000000000</Message>
  </Error>
  <RequestId>38560698-92a8-48ba-b9b8-343b1d23e641</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RemoveClientIDFromOpenIDConnectProvider

Removes the specified client ID (also known as audience) from the list of client IDs registered for the specified IAM OpenID Connect (OIDC) provider resource object.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 170
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RemoveClientIDFromOpenIDConnectProvider
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&ClientID=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 590a164d-3f61-46a1-bf6f-a9e3e515754f
content-length: 309
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenIDConnect Provider not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>590a164d-3f61-46a1-bf6f-a9e3e515754f</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RemoveRoleFromInstanceProfile

Removes the specified IAM role from the specified Amazon EC2 instance profile.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 100
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RemoveRoleFromInstanceProfile
&Version=2010-05-08
&InstanceProfileName=example
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: ed394713-00bb-4d57-919e-e4f5f14201e1
content-length: 259
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Instance Profile example cannot be found.</Message>
  </Error>
  <RequestId>ed394713-00bb-4d57-919e-e4f5f14201e1</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RemoveUserFromGroup

Removes the specified user from the specified group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 80
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RemoveUserFromGroup
&Version=2010-05-08
&GroupName=example
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: a84fe92c-819b-46cc-bf7c-5b1ab4dcfc7b
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>a84fe92c-819b-46cc-bf7c-5b1ab4dcfc7b</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ResetServiceSpecificCredential

Resets the password for a service-specific credential.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 105
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ResetServiceSpecificCredential
&Version=2010-05-08
&ServiceSpecificCredentialId=example0000000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 5086532d-a6b4-4c7f-bebf-5a3c786156f1
content-length: 264
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>No such credential example0000000000000 exists</Message>
  </Error>
  <RequestId>5086532d-a6b4-4c7f-bebf-5a3c786156f1</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ResyncMFADevice

Synchronizes the specified MFA device with its IAM resource object on the Amazon Web Services servers.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 135
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
x-amzn-requestid: 1f40cecd-1231-465a-8908-411bc802e232
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>1f40cecd-1231-465a-8908-411bc802e232</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### SendDelegationToken

Sends the exchange token for an accepted delegation request.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 82
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SendDelegationToken
&Version=2010-05-08
&DelegationRequestId=example000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: d9a7f241-cb0e-482a-9495-e684b168d693
content-length: 264
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Delegation request not found: example000000000</Message>
  </Error>
  <RequestId>d9a7f241-cb0e-482a-9495-e684b168d693</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### SetDefaultPolicyVersion

Sets the specified version of the specified policy as the policy's default (operative) version.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 140
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetDefaultPolicyVersion
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&VersionId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 184647bc-b982-43bc-961f-9164e92ab08a
content-length: 360
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value at &apos;versionId&apos; failed to satisfy constraint: Member must satisfy regular expression pattern: v[1-9][0-9]*(\.[A-Za-z0-9-]*)?</Message>
  </Error>
  <RequestId>184647bc-b982-43bc-961f-9164e92ab08a</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### SetSecurityTokenServicePreferences

Sets the specified version of the global endpoint token as the token version used for the Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetSecurityTokenServicePreferences
&Version=2010-05-08
&Groups=v1Token
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 56e16aca-9b26-4a12-8e6a-8f71405ffaf5
content-length: 322
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value at &apos;globalEndpointTokenVersion&apos; failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>56e16aca-9b26-4a12-8e6a-8f71405ffaf5</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### SimulateCustomPolicy

Simulate how a set of IAM policies and optionally a resource-based policy works with a list of API operations and Amazon Web Services resources to determine the policies' effective permissions.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 108
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SimulateCustomPolicy
&Version=2010-05-08
&PolicyInputList.member.1=example
&ActionNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 9541dc22-fea6-4dc6-b616-deb59cbe017b
content-length: 262
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Policy input list item 1 has invalid content</Message>
  </Error>
  <RequestId>9541dc22-fea6-4dc6-b616-deb59cbe017b</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### SimulatePrincipalPolicy

Simulate how a set of IAM policies attached to an IAM entity works with a list of API operations and Amazon Web Services resources to determine the policies' effective permissions.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 157
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SimulatePrincipalPolicy
&Version=2010-05-08
&PolicySourceArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&ActionNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 343cad94-6ed6-4389-8969-f6388b3549bf
content-length: 265
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>The action is invalid since it is missing names</Message>
  </Error>
  <RequestId>343cad94-6ed6-4389-8969-f6388b3549bf</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### TagInstanceProfile

Adds one or more tags to an IAM instance profile.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 126
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=TagInstanceProfile
&Version=2010-05-08
&InstanceProfileName=example
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 4a2c640a-93e8-42b8-a023-672145024680
content-length: 259
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Instance Profile example cannot be found.</Message>
  </Error>
  <RequestId>4a2c640a-93e8-42b8-a023-672145024680</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagMFADevice

Adds one or more tags to an IAM virtual multi-factor authentication (MFA) device.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 115
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=TagMFADevice
&Version=2010-05-08
&SerialNumber=example00
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 4e062b81-ddb3-4e29-9c08-c451ab20c411
content-length: 273
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>MFA Device with serial number example00 does not exist.</Message>
  </Error>
  <RequestId>4e062b81-ddb3-4e29-9c08-c451ab20c411</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagOpenIDConnectProvider

Adds one or more tags to an OpenID Connect (OIDC)-compatible identity provider.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 192
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=TagOpenIDConnectProvider
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 24f12a60-99a3-4e53-b1b1-4009d81ed645
content-length: 309
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenId connect Provider arn:aws:iam:us-east-1:000000000000:example/example cannot be found.</Message>
  </Error>
  <RequestId>24f12a60-99a3-4e53-b1b1-4009d81ed645</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagPolicy

Adds one or more tags to an IAM customer managed policy.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 162
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=TagPolicy
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: b27861ca-6450-4985-ac8d-e89ad02c8d69
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>b27861ca-6450-4985-ac8d-e89ad02c8d69</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### TagRole

Adds one or more tags to an IAM role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 104
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=TagRole
&Version=2010-05-08
&RoleName=example
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 10868b19-b75e-4c20-aea6-6beaeb9bf8bc
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>10868b19-b75e-4c20-aea6-6beaeb9bf8bc</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagSAMLProvider

Adds one or more tags to a Security Assertion Markup Language (SAML) identity provider.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 174
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=TagSAMLProvider
&Version=2010-05-08
&SAMLProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: d29206eb-1ca8-49f4-bf89-1362a444e484
content-length: 249
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Invalid resource type in ARN</Message>
  </Error>
  <RequestId>d29206eb-1ca8-49f4-bf89-1362a444e484</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### TagServerCertificate

Adds one or more tags to an IAM server certificate.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 130
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=TagServerCertificate
&Version=2010-05-08
&ServerCertificateName=example
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 4d4e44cd-9ae7-496c-b3fe-a6f522877dd8
content-length: 275
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Server Certificate with name example cannot be found.</Message>
  </Error>
  <RequestId>4d4e44cd-9ae7-496c-b3fe-a6f522877dd8</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagUser

Adds one or more tags to an IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 104
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=TagUser
&Version=2010-05-08
&UserName=example
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: fa9adcc0-5d9a-428e-95f9-ef1e1f11aa3a
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>fa9adcc0-5d9a-428e-95f9-ef1e1f11aa3a</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagInstanceProfile

Removes the specified tags from the IAM instance profile.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 99
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UntagInstanceProfile
&Version=2010-05-08
&InstanceProfileName=example
&TagKeys.member.1=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 7829215c-1706-4ea2-a4c8-0cc270103479
content-length: 259
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Instance Profile example cannot be found.</Message>
  </Error>
  <RequestId>7829215c-1706-4ea2-a4c8-0cc270103479</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagMFADevice

Removes the specified tags from the IAM virtual multi-factor authentication (MFA) device.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 88
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UntagMFADevice
&Version=2010-05-08
&SerialNumber=example00
&TagKeys.member.1=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 876c3b32-34c6-4775-a790-f5a8fffea313
content-length: 273
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>MFA Device with serial number example00 does not exist.</Message>
  </Error>
  <RequestId>876c3b32-34c6-4775-a790-f5a8fffea313</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagOpenIDConnectProvider

Removes the specified tags from the specified OpenID Connect (OIDC)-compatible identity provider in IAM.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 165
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UntagOpenIDConnectProvider
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&TagKeys.member.1=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: ec06b50e-acc5-4efa-9f28-ea044535ff78
content-length: 309
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenId connect Provider arn:aws:iam:us-east-1:000000000000:example/example cannot be found.</Message>
  </Error>
  <RequestId>ec06b50e-acc5-4efa-9f28-ea044535ff78</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagPolicy

Removes the specified tags from the customer managed policy.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 135
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UntagPolicy
&Version=2010-05-08
&PolicyArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&TagKeys.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 60bdc59f-2f42-4aae-8406-bd633717dc4c
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>ARN arn:aws:iam:us-east-1:000000000000:example/example is not valid.</Message>
  </Error>
  <RequestId>60bdc59f-2f42-4aae-8406-bd633717dc4c</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### UntagRole

Removes the specified tags from the role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UntagRole
&Version=2010-05-08
&RoleName=example
&TagKeys.member.1=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 91fc185a-a8f9-41f2-8d9f-0bd3e1e454fc
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>91fc185a-a8f9-41f2-8d9f-0bd3e1e454fc</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagSAMLProvider

Removes the specified tags from the specified Security Assertion Markup Language (SAML) identity provider in IAM.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 147
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UntagSAMLProvider
&Version=2010-05-08
&SAMLProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&TagKeys.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: cd45a43f-afc9-4272-954a-ffb09a627fef
content-length: 249
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Invalid resource type in ARN</Message>
  </Error>
  <RequestId>cd45a43f-afc9-4272-954a-ffb09a627fef</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### UntagServerCertificate

Removes the specified tags from the IAM server certificate.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 103
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UntagServerCertificate
&Version=2010-05-08
&ServerCertificateName=example
&TagKeys.member.1=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: eee1076e-086d-4290-b9f0-4bf204595918
content-length: 275
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Server Certificate with name example cannot be found.</Message>
  </Error>
  <RequestId>eee1076e-086d-4290-b9f0-4bf204595918</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagUser

Removes the specified tags from the user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UntagUser
&Version=2010-05-08
&UserName=example
&TagKeys.member.1=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: d676243c-d252-428a-8b6f-50dbad1ca9fa
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>d676243c-d252-428a-8b6f-50dbad1ca9fa</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateAccessKey

Changes the status of the specified access key from Active to Inactive, or vice versa.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 84
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateAccessKey
&Version=2010-05-08
&AccessKeyId=example000000000
&Status=Active
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: cf1827df-fe82-4b42-96d8-df0263ec828e
content-length: 274
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Access Key with id example000000000 cannot be found.</Message>
  </Error>
  <RequestId>cf1827df-fe82-4b42-96d8-df0263ec828e</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateAccountPasswordPolicy

Updates the password policy settings for the Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateAccountPasswordPolicy
&Version=2010-05-08
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 292
date: Thu, 26 Nov 2026 18:30:05 GMT

<UpdateAccountPasswordPolicyResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <UpdateAccountPasswordPolicyResult></UpdateAccountPasswordPolicyResult>
  <ResponseMetadata>
    <RequestId>cdede138-47db-48c3-a19e-4ebae0f3a64d</RequestId>
  </ResponseMetadata>
</UpdateAccountPasswordPolicyResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### UpdateAssumeRolePolicy

Updates the policy that grants an IAM entity permission to assume a role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 88
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateAssumeRolePolicy
&Version=2010-05-08
&RoleName=example
&PolicyDocument=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 52b79d9e-4166-4cb3-809d-aaca4ce95b9a
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>52b79d9e-4166-4cb3-809d-aaca4ce95b9a</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateDelegationRequest

Updates an existing delegation request with additional information.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 86
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateDelegationRequest
&Version=2010-05-08
&DelegationRequestId=example000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 787ad1ee-e8a2-4d51-bade-4cd0fc706775
content-length: 264
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>Delegation request not found: example000000000</Message>
  </Error>
  <RequestId>787ad1ee-e8a2-4d51-bade-4cd0fc706775</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateGroup

Updates the name and/or the path of the specified IAM group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 55
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateGroup
&Version=2010-05-08
&GroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: e514cc27-e2e8-4674-9ae6-cdfc6248749c
content-length: 262
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The group with name example cannot be found.</Message>
  </Error>
  <RequestId>e514cc27-e2e8-4674-9ae6-cdfc6248749c</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateLoginProfile

Changes the password for the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateLoginProfile
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 34b9c8c0-9125-42a5-b3c7-769afc4c16cc
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>34b9c8c0-9125-42a5-b3c7-769afc4c16cc</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateOpenIDConnectProviderThumbprint

Replaces the existing list of server certificate thumbprints associated with an OpenID Connect (OIDC) provider resource object with a new list of thumbprints.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 216
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateOpenIDConnectProviderThumbprint
&Version=2010-05-08
&OpenIDConnectProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&ThumbprintList.member.1=example000000000000000000000000000000000
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: fbdfdd91-7de4-47c5-9daa-6e1f33d9ed40
content-length: 309
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>OpenIDConnect Provider not found for arn arn:aws:iam:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>fbdfdd91-7de4-47c5-9daa-6e1f33d9ed40</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateRole

Updates the description or maximum session duration setting of a role.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateRole
&Version=2010-05-08
&RoleName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: ff1d0849-68a5-405d-8815-75459f5730b2
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>ff1d0849-68a5-405d-8815-75459f5730b2</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateRoleDescription

Use UpdateRole instead.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 84
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateRoleDescription
&Version=2010-05-08
&RoleName=example
&Description=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 1e2743d2-c878-4395-9ce0-3f7840464b3c
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The role with name example cannot be found.</Message>
  </Error>
  <RequestId>1e2743d2-c878-4395-9ce0-3f7840464b3c</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateSAMLProvider

Updates the metadata document, SAML encryption settings, and private keys for an existing SAML provider.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 123
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateSAMLProvider
&Version=2010-05-08
&SAMLProviderArn=arn%3Aaws%3Aiam%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: c37a5b0f-9231-489b-b443-e68a018301ed
content-length: 249
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Invalid resource type in ARN</Message>
  </Error>
  <RequestId>c37a5b0f-9231-489b-b443-e68a018301ed</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### UpdateSSHPublicKey

Sets the status of an IAM user's SSH public key to active or inactive.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 111
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateSSHPublicKey
&Version=2010-05-08
&UserName=example
&SSHPublicKeyId=example0000000000000
&Status=Active
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: b6689e68-4ec3-45e5-bf22-cca08de56ad5
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>b6689e68-4ec3-45e5-bf22-cca08de56ad5</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateServerCertificate

Updates the name and/or the path of the specified server certificate stored in IAM.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 79
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateServerCertificate
&Version=2010-05-08
&ServerCertificateName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 9f3fbe67-6f22-40cf-b327-3b6dad314d08
content-length: 275
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Server Certificate with name example cannot be found.</Message>
  </Error>
  <RequestId>9f3fbe67-6f22-40cf-b327-3b6dad314d08</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateServiceSpecificCredential

Sets the status of a service-specific credential to Active or Inactive .

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 120
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateServiceSpecificCredential
&Version=2010-05-08
&ServiceSpecificCredentialId=example0000000000000
&Status=Active
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 9f93f3d8-6040-45d0-82ca-e3dc98a05e8d
content-length: 264
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>No such credential example0000000000000 exists</Message>
  </Error>
  <RequestId>9f93f3d8-6040-45d0-82ca-e3dc98a05e8d</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateSigningCertificate

Changes the status of the specified user signing certificate from active to disabled, or vice versa.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 103
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateSigningCertificate
&Version=2010-05-08
&CertificateId=example00000000000000000
&Status=Active
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 9b391f7b-0d6f-476c-bb57-581d3adf1698
content-length: 283
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The Certificate with id example00000000000000000 cannot be found.</Message>
  </Error>
  <RequestId>9b391f7b-0d6f-476c-bb57-581d3adf1698</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateUser

Updates the name and/or the path of the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateUser
&Version=2010-05-08
&UserName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 6e12a7d4-d090-4498-b1ca-a0c43206c72f
content-length: 261
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchEntity</Code>
    <Message>The user with name example cannot be found.</Message>
  </Error>
  <RequestId>6e12a7d4-d090-4498-b1ca-a0c43206c72f</RequestId>
</ErrorResponse>
```

`404` `NoSuchEntity`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UploadSSHPublicKey

Uploads an SSH public key and associates it with the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 86
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UploadSSHPublicKey
&Version=2010-05-08
&UserName=example
&SSHPublicKeyBody=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 16a9b77f-1c75-4e6c-9bf0-7c8f7013631f
content-length: 263
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>UnrecognizedPublicKeyEncoding</Code>
    <Message>Invalid public key uploaded.</Message>
  </Error>
  <RequestId>16a9b77f-1c75-4e6c-9bf0-7c8f7013631f</RequestId>
</ErrorResponse>
```

`400` `UnrecognizedPublicKeyEncoding`.

### UploadServerCertificate

Uploads a server certificate entity for the Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 122
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UploadServerCertificate
&Version=2010-05-08
&ServerCertificateName=example
&CertificateBody=example
&PrivateKey=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: a0746638-3f59-4c04-a53a-7553e0b1bd11
content-length: 302
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>MalformedCertificate</Code>
    <Message>Unable to parse certificate. Please ensure the certificate is in PEM format.</Message>
  </Error>
  <RequestId>a0746638-3f59-4c04-a53a-7553e0b1bd11</RequestId>
</ErrorResponse>
```

`400` `MalformedCertificate`: the placeholder values failed the operation's input validation.

### UploadSigningCertificate

Uploads an X.509 signing certificate and associates it with the specified IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 74
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UploadSigningCertificate
&Version=2010-05-08
&CertificateBody=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: a1e5e598-8729-4e6c-aabb-5fbd027bdb31
content-length: 259
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>MalformedCertificate</Code>
    <Message>Certificate example is malformed.</Message>
  </Error>
  <RequestId>a1e5e598-8729-4e6c-aabb-5fbd027bdb31</RequestId>
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
