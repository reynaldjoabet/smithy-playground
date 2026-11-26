# AWS Security Token Service

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `STS`
- **sigv4 signing name:** `sts`
- **Endpoint prefix:** `sts`
- **ARN namespace:** `sts`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `sts`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `GetCallerIdentity`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:sts" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-www-form-urlencoded; charset=utf-8' \
  --data-binary 'Action=GetCallerIdentity&Version=2011-06-15' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Find out who the credentials belong to, get temporary credentials, and assume an IAM role. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. GetCallerIdentity

Every request is signed with the emulator's credentials, which belong to IAM user `emulator` in account `000000000000`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetCallerIdentity
&Version=2011-06-15
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 371
date: Thu, 26 Nov 2026 18:30:52 GMT

<GetCallerIdentityResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <GetCallerIdentityResult>
    <UserId>AIDAH5I7GEMO7ADXNFAZU</UserId>
    <Account>000000000000</Account>
    <Arn>arn:aws:iam::000000000000:user/emulator</Arn>
  </GetCallerIdentityResult>
  <ResponseMetadata>
    <RequestId>fdf8c6b8-9c78-4950-9f16-a948176fadde</RequestId>
  </ResponseMetadata>
</GetCallerIdentityResponse>
```

### 2. GetSessionToken

Temporary credentials for the same user. The secret key and session token are redacted on this page.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetSessionToken
&Version=2011-06-15
&DurationSeconds=900
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1322
date: Thu, 26 Nov 2026 18:30:52 GMT

<GetSessionTokenResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <GetSessionTokenResult>
    <Credentials xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
      <AccessKeyId>EMULATORAS73WNJGKPIB</AccessKeyId>
      <SecretAccessKey><redacted></SecretAccessKey>
      <SessionToken><redacted></SessionToken>
      <Expiration>2026-11-26T18:45:52Z</Expiration>
    </Credentials>
    <SessionTokenUtilization>17</SessionTokenUtilization>
    <SessionTokenSize>712</SessionTokenSize>
  </GetSessionTokenResult>
  <ResponseMetadata>
    <RequestId>4a9891b4-d8a1-4ab1-a6de-aeae9d9dd32d</RequestId>
  </ResponseMetadata>
</GetSessionTokenResponse>
```

### 3. AWS Identity and Access Management: CreateRole

An IAM role whose trust policy lets principals of this account assume it.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 335
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateRole
&Version=2010-05-08
&RoleName=docs-demo-role
&AssumeRolePolicyDocument=%7B%22Version%22%3A%20%222012-10-17%22%2C%20%22Statement%22%3A%20%5B%7B%22Effect%22%3A%20%22Allow%22%2C%20%22Principal%22%3A%20%7B%22AWS%22%3A%20%22arn%3Aaws%3Aiam%3A%3A000000000000%3Aroot%22%7D%2C%20%22Action%22%3A%20%22sts%3AAssumeRole%22%7D%5D%7D
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 841
date: Thu, 26 Nov 2026 18:30:52 GMT

<CreateRoleResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateRoleResult>
    <Role xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <RoleName>docs-demo-role</RoleName>
      <RoleId>AROAKWCHDRLOD744GNFOK</RoleId>
      <Arn>arn:aws:iam::000000000000:role/docs-demo-role</Arn>
      <CreateDate>2026-11-26T18:30:52.133Z</CreateDate>
      <AssumeRolePolicyDocument>%7B%22Version%22%3A%222012-10-17%22%2C%22Statement%22%3A%5B%7B%22Effect%22%3A%22Allow%22%2C%22Principal%22%3A%7B%22AWS%22%3A%22arn%3Aaws%3Aiam%3A%3A000000000000%3Aroot%22%7D%2C%22Action%22%3A%22sts%3AAssumeRole%22%7D%5D%7D</AssumeRolePolicyDocument>
      <MaxSessionDuration>3600</MaxSessionDuration>
      <Tags></Tags>
      <RoleLastUsed></RoleLastUsed>
    </Role>
  </CreateRoleResult>
  <ResponseMetadata>
    <RequestId>8178d9a3-9a39-4ffe-8585-e8b6045dfe9f</RequestId>
  </ResponseMetadata>
</CreateRoleResponse>
```

### 4. AssumeRole

STS checks the role's trust policy and returns credentials for the role session, identified by `AssumedRoleUser`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 148
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AssumeRole
&Version=2011-06-15
&RoleArn=arn%3Aaws%3Aiam%3A%3A000000000000%3Arole%2Fdocs-demo-role
&RoleSessionName=docs-demo
&DurationSeconds=900
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1511
date: Thu, 26 Nov 2026 18:30:52 GMT

<AssumeRoleResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <AssumeRoleResult>
    <Credentials xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
      <AccessKeyId>EMULATORASVYYK2CIMMO</AccessKeyId>
      <SecretAccessKey><redacted></SecretAccessKey>
      <SessionToken><redacted></SessionToken>
      <Expiration>2026-11-26T18:45:52Z</Expiration>
    </Credentials>
    <AssumedRoleUser>
      <AssumedRoleId>AROAKWCHDRLOD744GNFOK:docs-demo</AssumedRoleId>
      <Arn>arn:aws:sts::000000000000:assumed-role/docs-demo-role/docs-demo</Arn>
    </AssumedRoleUser>
    <PackedPolicySize>1</PackedPolicySize>
    <SessionTokenUtilization>17</SessionTokenUtilization>
    <SessionTokenSize>712</SessionTokenSize>
  </AssumeRoleResult>
  <ResponseMetadata>
    <RequestId>d18f9403-d732-4385-9cd1-b6cc8ea935d1</RequestId>
  </ResponseMetadata>
</AssumeRoleResponse>
```

### 5. AssumeRole

A role that does not exist is refused with 403 `AccessDenied`, as AWS does, so a caller cannot probe which roles exist.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 128
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AssumeRole
&Version=2011-06-15
&RoleArn=arn%3Aaws%3Aiam%3A%3A000000000000%3Arole%2Fdoes-not-exist
&RoleSessionName=docs-demo
```

**Response**

```http
HTTP/1.1 403 Forbidden
content-type: text/xml
x-amzn-requestid: 748cb7a4-4c60-4a93-89cf-bc9fb542886c
content-length: 367
date: Thu, 26 Nov 2026 18:30:52 GMT

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>AccessDenied</Code>
    <Message>User: arn:aws:iam::000000000000:user/emulator is not authorized to perform: sts:AssumeRole on resource: arn:aws:iam::000000000000:role/does-not-exist</Message>
  </Error>
  <RequestId>748cb7a4-4c60-4a93-89cf-bc9fb542886c</RequestId>
</ErrorResponse>
```

### 6. AWS Identity and Access Management: DeleteRole

Clean up.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteRole
&Version=2010-05-08
&RoleName=docs-demo-role
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 224
date: Thu, 26 Nov 2026 18:30:52 GMT

<DeleteRoleResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteRoleResult></DeleteRoleResult>
  <ResponseMetadata>
    <RequestId>9015dc6d-5374-4414-9402-6b5a363f8b89</RequestId>
  </ResponseMetadata>
</DeleteRoleResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 11 operations, 3 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AssumeRole

Returns a set of temporary security credentials that you can use to access Amazon Web Services resources.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 131
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AssumeRole
&Version=2011-06-15
&RoleArn=arn%3Aaws%3Asts%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&RoleSessionName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: cfe515ae-edc6-4413-920a-abc6a7e472c6
content-length: 368
date: Thu, 26 Nov 2026 18:30:52 GMT

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>RoleArn 'arn:aws:sts:us-east-1:000000000000:example/example' is not an IAM role ARN of the form arn:&lt;partition>:iam::&lt;account>:role/&lt;name></Message>
  </Error>
  <RequestId>cfe515ae-edc6-4413-920a-abc6a7e472c6</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#4-assumerole) for a successful call.

### AssumeRoleWithSAML

Returns a set of temporary security credentials for users who have been authenticated via a SAML authentication response.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 213
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AssumeRoleWithSAML
&Version=2011-06-15
&RoleArn=arn%3Aaws%3Asts%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&PrincipalArn=arn%3Aaws%3Asts%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&SAMLAssertion=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 09c4ae08-512e-4945-81b8-168a529d59e5
content-length: 368
date: Thu, 26 Nov 2026 18:30:52 GMT

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>RoleArn 'arn:aws:sts:us-east-1:000000000000:example/example' is not an IAM role ARN of the form arn:&lt;partition>:iam::&lt;account>:role/&lt;name></Message>
  </Error>
  <RequestId>09c4ae08-512e-4945-81b8-168a529d59e5</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### AssumeRoleWithWebIdentity

Returns a set of temporary security credentials for users who have been authenticated in a mobile or web application with a web identity provider.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 171
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AssumeRoleWithWebIdentity
&Version=2011-06-15
&RoleArn=arn%3Aaws%3Asts%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&RoleSessionName=example
&WebIdentityToken=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 96342659-1d45-46ce-952a-2d718cac74e6
content-length: 320
date: Thu, 26 Nov 2026 18:30:52 GMT

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidIdentityToken</Code>
    <Message>The ID Token provided is not a valid JWT. (You may see this error if you sent an Access Token)</Message>
  </Error>
  <RequestId>96342659-1d45-46ce-952a-2d718cac74e6</RequestId>
</ErrorResponse>
```

`400` `InvalidIdentityToken`.

### AssumeRoot

Returns a set of short term credentials you can use to perform privileged tasks on a member account in your organization.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 65
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AssumeRoot
&Version=2011-06-15
&TargetPrincipal=example00000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 67800246-55ce-40b8-8c6b-2cbbeced00dd
content-length: 304
date: Thu, 26 Nov 2026 18:30:52 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at 'taskPolicyArn' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>67800246-55ce-40b8-8c6b-2cbbeced00dd</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DecodeAuthorizationMessage

Decodes additional information about the authorization status of a request from an encoded message returned in response to an Amazon Web Services request.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DecodeAuthorizationMessage
&Version=2011-06-15
&EncodedMessage=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: b4eab68e-bf68-45ce-9917-2db7f0421030
content-length: 287
date: Thu, 26 Nov 2026 18:30:52 GMT

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAuthorizationMessageException</Code>
    <Message>Message is not valid: [123, 22, -90, -90, 87]</Message>
  </Error>
  <RequestId>b4eab68e-bf68-45ce-9917-2db7f0421030</RequestId>
</ErrorResponse>
```

`400` `InvalidAuthorizationMessageException`.

### GetAccessKeyInfo

Returns the account identifier for the specified access key ID.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetAccessKeyInfo
&Version=2011-06-15
&AccessKeyId=example000000000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 7250c79f-c594-4809-b6f5-0b54b9f8c60b
content-length: 248
date: Thu, 26 Nov 2026 18:30:52 GMT

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Access key ID is not valid.</Message>
  </Error>
  <RequestId>7250c79f-c594-4809-b6f5-0b54b9f8c60b</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetCallerIdentity

Returns details about the IAM user or role whose credentials are used to call the operation.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetCallerIdentity
&Version=2011-06-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 371
date: Thu, 26 Nov 2026 18:30:52 GMT

<GetCallerIdentityResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <GetCallerIdentityResult>
    <UserId>AIDAH5I7GEMO7ADXNFAZU</UserId>
    <Account>000000000000</Account>
    <Arn>arn:aws:iam::000000000000:user/emulator</Arn>
  </GetCallerIdentityResult>
  <ResponseMetadata>
    <RequestId>1f89ffd3-0fab-4f93-a365-8739f095d1c9</RequestId>
  </ResponseMetadata>
</GetCallerIdentityResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#1-getcalleridentity) for a successful call.

### GetDelegatedAccessToken

Exchanges a trade-in token for temporary Amazon Web Services credentials with the permissions associated with the assumed principal.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetDelegatedAccessToken
&Version=2011-06-15
&TradeInToken=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: e8a57fe7-5ce1-4cb0-afa2-558046fd0ab9
content-length: 243
date: Thu, 26 Nov 2026 18:30:52 GMT

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Invalid trade-in token</Message>
  </Error>
  <RequestId>e8a57fe7-5ce1-4cb0-afa2-558046fd0ab9</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetFederationToken

Returns a set of temporary security credentials (consisting of an access key ID, a secret access key, and a security token) for a user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 57
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetFederationToken
&Version=2011-06-15
&Name=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1517
date: Thu, 26 Nov 2026 18:30:52 GMT

<GetFederationTokenResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <GetFederationTokenResult>
    <Credentials xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
      <AccessKeyId>EMULATORASXRXCDPOB5C</AccessKeyId>
      <SecretAccessKey><redacted></SecretAccessKey>
      <SessionToken><redacted></SessionToken>
      <Expiration>2026-11-27T06:30:52Z</Expiration>
    </Credentials>
    <FederatedUser>
      <FederatedUserId>000000000000:example</FederatedUserId>
      <Arn>arn:aws:sts::000000000000:federated-user/example</Arn>
    </FederatedUser>
    <PackedPolicySize>1</PackedPolicySize>
    <SessionTokenUtilization>17</SessionTokenUtilization>
    <SessionTokenSize>712</SessionTokenSize>
  </GetFederationTokenResult>
  <ResponseMetadata>
    <RequestId>f5fbb70e-1116-402a-a316-41f70ce9a364</RequestId>
  </ResponseMetadata>
</GetFederationTokenResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetSessionToken

Returns a set of temporary credentials for an Amazon Web Services account or IAM user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 41
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetSessionToken
&Version=2011-06-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1322
date: Thu, 26 Nov 2026 18:30:52 GMT

<GetSessionTokenResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <GetSessionTokenResult>
    <Credentials xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
      <AccessKeyId>EMULATORASKRE5UZWO7V</AccessKeyId>
      <SecretAccessKey><redacted></SecretAccessKey>
      <SessionToken><redacted></SessionToken>
      <Expiration>2026-11-27T06:30:52Z</Expiration>
    </Credentials>
    <SessionTokenUtilization>17</SessionTokenUtilization>
    <SessionTokenSize>712</SessionTokenSize>
  </GetSessionTokenResult>
  <ResponseMetadata>
    <RequestId>63a66d8c-68b5-420e-ad52-4c14dea38dbf</RequestId>
  </ResponseMetadata>
</GetSessionTokenResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#2-getsessiontoken) for a successful call.

### GetWebIdentityToken

Returns a signed JSON Web Token (JWT) that represents the calling Amazon Web Services identity.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 94
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetWebIdentityToken
&Version=2011-06-15
&Audience.member.1=example
&SigningAlgorithm=examp
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: a8d0f7ec-ca85-47e3-9faf-74801a07bc0b
content-length: 332
date: Thu, 26 Nov 2026 18:30:52 GMT

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Requested SigningAlgorithm is not supported. See the GetWebIdentityToken documentation for supported algorithms</Message>
  </Error>
  <RequestId>a8d0f7ec-ca85-47e3-9faf-74801a07bc0b</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/sts/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with a form-encoded body: `Action` names the operation, `Version` the API version, and every input member is a flattened key such as `Tags.member.1.Key`.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way AWS Security Token Service answers in AWS: XML wrapped in `<OperationResponse><OperationResult>`. Errors captured on this page are an XML document rooted at `<ErrorResponse>`.

### Other services involved

The walkthrough also called [AWS Identity and Access Management](iam.md). They are separate services behind the same endpoint, sharing one account and one in-memory engine, so a resource created through one is immediately visible to the other.
