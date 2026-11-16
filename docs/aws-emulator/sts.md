# AWS Security Token Service

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `STS`
- **sigv4 signing name:** `sts`
- **Endpoint prefix:** `sts`
- **ARN namespace:** `sts`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `sts`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

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
X-Amz-Date: 20261116T120456Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetCallerIdentity
&Version=2011-06-15
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 371
date: Mon, 16 Nov 2026 12:04:56 GMT

<GetCallerIdentityResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <GetCallerIdentityResult>
    <UserId>AIDAH5I7GEMO7ADXNFAZU</UserId>
    <Account>000000000000</Account>
    <Arn>arn:aws:iam::000000000000:user/emulator</Arn>
  </GetCallerIdentityResult>
  <ResponseMetadata>
    <RequestId>6eec8e9f-9fa3-43e0-b20c-c3c376087dbd</RequestId>
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
X-Amz-Date: 20261116T120456Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetSessionToken
&Version=2011-06-15
&DurationSeconds=900
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1322
date: Mon, 16 Nov 2026 12:04:56 GMT

<GetSessionTokenResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <GetSessionTokenResult>
    <Credentials xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
      <AccessKeyId>EMULATORAS3UHQ4Z7EQ5</AccessKeyId>
      <SecretAccessKey><redacted></SecretAccessKey>
      <SessionToken><redacted></SessionToken>
      <Expiration>2026-11-16T12:19:56Z</Expiration>
    </Credentials>
    <SessionTokenUtilization>17</SessionTokenUtilization>
    <SessionTokenSize>712</SessionTokenSize>
  </GetSessionTokenResult>
  <ResponseMetadata>
    <RequestId>605a1900-c0a6-469a-b3f4-538ea3374d25</RequestId>
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
X-Amz-Date: 20261116T120456Z
Content-Length: 335
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateRole
&Version=2010-05-08
&RoleName=docs-demo-role
&AssumeRolePolicyDocument=%7B%22Version%22%3A%20%222012-10-17%22%2C%20%22Statement%22%3A%20%5B%7B%22Effect%22%3A%20%22Allow%22%2C%20%22Principal%22%3A%20%7B%22AWS%22%3A%20%22arn%3Aaws%3Aiam%3A%3A000000000000%3Aroot%22%7D%2C%20%22Action%22%3A%20%22sts%3AAssumeRole%22%7D%5D%7D
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 799
date: Mon, 16 Nov 2026 12:04:56 GMT

<CreateRoleResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateRoleResult>
    <Role xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <RoleName>docs-demo-role</RoleName>
      <RoleId>AROABGXEQES5NRCCJJWAN</RoleId>
      <Arn>arn:aws:iam::000000000000:role/docs-demo-role</Arn>
      <CreateDate>2026-11-16T12:04:56.437Z</CreateDate>
      <AssumeRolePolicyDocument>%7B%22Version%22%3A%222012-10-17%22%2C%22Statement%22%3A%5B%7B%22Effect%22%3A%22Allow%22%2C%22Principal%22%3A%7B%22AWS%22%3A%22arn%3Aaws%3Aiam%3A%3A000000000000%3Aroot%22%7D%2C%22Action%22%3A%22sts%3AAssumeRole%22%7D%5D%7D</AssumeRolePolicyDocument>
      <MaxSessionDuration>3600</MaxSessionDuration>
    </Role>
  </CreateRoleResult>
  <ResponseMetadata>
    <RequestId>994a3320-a08c-45a9-a8a6-064de4318192</RequestId>
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
X-Amz-Date: 20261116T120456Z
Content-Length: 148
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
date: Mon, 16 Nov 2026 12:04:56 GMT

<AssumeRoleResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <AssumeRoleResult>
    <Credentials xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
      <AccessKeyId>EMULATORASUSRY23U66Z</AccessKeyId>
      <SecretAccessKey><redacted></SecretAccessKey>
      <SessionToken><redacted></SessionToken>
      <Expiration>2026-11-16T12:19:56Z</Expiration>
    </Credentials>
    <AssumedRoleUser>
      <AssumedRoleId>AROABGXEQES5NRCCJJWAN:docs-demo</AssumedRoleId>
      <Arn>arn:aws:sts::000000000000:assumed-role/docs-demo-role/docs-demo</Arn>
    </AssumedRoleUser>
    <PackedPolicySize>0</PackedPolicySize>
    <SessionTokenUtilization>17</SessionTokenUtilization>
    <SessionTokenSize>712</SessionTokenSize>
  </AssumeRoleResult>
  <ResponseMetadata>
    <RequestId>921a16c0-77eb-4981-8ae0-aaf7034f03bb</RequestId>
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
X-Amz-Date: 20261116T120456Z
Content-Length: 128
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AssumeRole
&Version=2011-06-15
&RoleArn=arn%3Aaws%3Aiam%3A%3A000000000000%3Arole%2Fdoes-not-exist
&RoleSessionName=docs-demo
```

**Response**

```http
HTTP/1.1 403 Forbidden
content-type: text/xml
x-amzn-requestid: 7895c9e2-d419-473c-907f-dd4fde3e2859
content-length: 367
date: Mon, 16 Nov 2026 12:04:56 GMT

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>AccessDenied</Code>
    <Message>User: arn:aws:iam::000000000000:user/emulator is not authorized to perform: sts:AssumeRole on resource: arn:aws:iam::000000000000:role/does-not-exist</Message>
  </Error>
  <RequestId>7895c9e2-d419-473c-907f-dd4fde3e2859</RequestId>
</ErrorResponse>
```

### 6. AWS Identity and Access Management: DeleteRole

Clean up.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120456Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteRole
&Version=2010-05-08
&RoleName=docs-demo-role
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 224
date: Mon, 16 Nov 2026 12:04:56 GMT

<DeleteRoleResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteRoleResult/>
  <ResponseMetadata>
    <RequestId>f6e77c26-d049-4be5-a7c1-47df19a08813</RequestId>
  </ResponseMetadata>
</DeleteRoleResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 11 operations, 3 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AssumeRole

Returns a set of temporary security credentials that you can use to access Amazon Web Services resources.

```http
POST / HTTP/1.1

Action=AssumeRole
&Version=2011-06-15
&RoleArn=arn%3Aaws%3Asts%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&RoleSessionName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>RoleArn 'arn:aws:sts:us-east-1:000000000000:example/example' is not an IAM role ARN of the form arn:&lt;partition&gt;:iam::&lt;account&gt;:role/&lt;name&gt;</Message>
  </Error>
  <RequestId>8e364c59-da9d-4e52-bf85-dc9add81eb91</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#4-assumerole) for a successful call.

### AssumeRoleWithSAML

Returns a set of temporary security credentials for users who have been authenticated via a SAML authentication response.

```http
POST / HTTP/1.1

Action=AssumeRoleWithSAML
&Version=2011-06-15
&RoleArn=arn%3Aaws%3Asts%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&PrincipalArn=arn%3Aaws%3Asts%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&SAMLAssertion=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>RoleArn 'arn:aws:sts:us-east-1:000000000000:example/example' is not an IAM role ARN of the form arn:&lt;partition&gt;:iam::&lt;account&gt;:role/&lt;name&gt;</Message>
  </Error>
  <RequestId>844a40fb-c295-4cd0-ba09-8027755c7aaa</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### AssumeRoleWithWebIdentity

Returns a set of temporary security credentials for users who have been authenticated in a mobile or web application with a web identity provider.

```http
POST / HTTP/1.1

Action=AssumeRoleWithWebIdentity
&Version=2011-06-15
&RoleArn=arn%3Aaws%3Asts%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&RoleSessionName=example
&WebIdentityToken=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidIdentityToken</Code>
    <Message>The ID Token provided is not a valid JWT. (You may see this error if you sent an Access Token)</Message>
  </Error>
  <RequestId>1d1418af-70ed-43b4-a70c-405f8d631bcf</RequestId>
</ErrorResponse>
```

`400` `InvalidIdentityToken`.

### AssumeRoot

Returns a set of short term credentials you can use to perform privileged tasks on a member account in your organization.

```http
POST / HTTP/1.1

Action=AssumeRoot
&Version=2011-06-15
&TargetPrincipal=example00000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at 'taskPolicyArn' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>58de53a8-776f-4d60-892a-5b703a948542</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DecodeAuthorizationMessage

Decodes additional information about the authorization status of a request from an encoded message returned in response to an Amazon Web Services request.

```http
POST / HTTP/1.1

Action=DecodeAuthorizationMessage
&Version=2011-06-15
&EncodedMessage=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAuthorizationMessageException</Code>
    <Message>Message is not valid: [123, 22, -90, -90, 87]</Message>
  </Error>
  <RequestId>d9ebb8ab-d4b9-4319-85b6-c6d061ab6172</RequestId>
</ErrorResponse>
```

`400` `InvalidAuthorizationMessageException`.

### GetAccessKeyInfo

Returns the account identifier for the specified access key ID.

```http
POST / HTTP/1.1

Action=GetAccessKeyInfo
&Version=2011-06-15
&AccessKeyId=example000000000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Access key ID is not valid.</Message>
  </Error>
  <RequestId>93ce01bc-ea95-4782-9635-949b741b2e1f</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetCallerIdentity

Returns details about the IAM user or role whose credentials are used to call the operation.

```http
POST / HTTP/1.1

Action=GetCallerIdentity
&Version=2011-06-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<GetCallerIdentityResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <GetCallerIdentityResult>
    <UserId>AIDAH5I7GEMO7ADXNFAZU</UserId>
    <Account>000000000000</Account>
    <Arn>arn:aws:iam::000000000000:user/emulator</Arn>
  </GetCallerIdentityResult>
  <ResponseMetadata>
    <RequestId>0d3d912e-319d-4c87-a262-06654f87efe4</RequestId>
  </ResponseMetadata>
</GetCallerIdentityResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#1-getcalleridentity) for a successful call.

### GetDelegatedAccessToken

Exchanges a trade-in token for temporary Amazon Web Services credentials with the permissions associated with the assumed principal.

```http
POST / HTTP/1.1

Action=GetDelegatedAccessToken
&Version=2011-06-15
&TradeInToken=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Invalid trade-in token</Message>
  </Error>
  <RequestId>5c315069-d1fd-49e0-b3ab-c82f74a2dfe0</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetFederationToken

Returns a set of temporary security credentials (consisting of an access key ID, a secret access key, and a security token) for a user.

```http
POST / HTTP/1.1

Action=GetFederationToken
&Version=2011-06-15
&Name=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<GetFederationTokenResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <GetFederationTokenResult>
    <Credentials xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
      <AccessKeyId>EMULATORASJQNPDGQHT7</AccessKeyId>
      <SecretAccessKey><redacted></SecretAccessKey>
      <SessionToken><redacted></SessionToken>
      <Expiration>2026-11-17T00:11:25Z</Expiration>
    </Credentials>
    <FederatedUser>
      <FederatedUserId>000000000000:example</FederatedUserId>
      <Arn>arn:aws:sts::000000000000:federated-user/example</Arn>
    </FederatedUser>
    <PackedPolicySize>0</PackedPolicySize>
    <SessionTokenUtilization>17</SessionTokenUtilization>
    <SessionTokenSize>712</SessionTokenSize>
  </GetFederationTokenResult>
  <ResponseMetadata>
    <RequestId>bdbafc8d-6458-4578-bc7f-90f1ee977cac</RequestId>
  </ResponseMetadata>
</GetFederationTokenResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetSessionToken

Returns a set of temporary credentials for an Amazon Web Services account or IAM user.

```http
POST / HTTP/1.1

Action=GetSessionToken
&Version=2011-06-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<GetSessionTokenResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <GetSessionTokenResult>
    <Credentials xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
      <AccessKeyId>EMULATORASKGAC7FLUTY</AccessKeyId>
      <SecretAccessKey><redacted></SecretAccessKey>
      <SessionToken><redacted></SessionToken>
      <Expiration>2026-11-17T00:11:25Z</Expiration>
    </Credentials>
    <SessionTokenUtilization>17</SessionTokenUtilization>
    <SessionTokenSize>712</SessionTokenSize>
  </GetSessionTokenResult>
  <ResponseMetadata>
    <RequestId>68ebc4bf-9727-48fc-a048-14267036b74d</RequestId>
  </ResponseMetadata>
</GetSessionTokenResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#2-getsessiontoken) for a successful call.

### GetWebIdentityToken

Returns a signed JSON Web Token (JWT) that represents the calling Amazon Web Services identity.

```http
POST / HTTP/1.1

Action=GetWebIdentityToken
&Version=2011-06-15
&Audience.member.1=example
&SigningAlgorithm=examp
```

```http
HTTP/1.1 403 Forbidden
content-type: text/xml

<ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <Error>
    <Type>Sender</Type>
    <Code>OutboundWebIdentityFederationDisabledException</Code>
    <Message>The outbound web identity federation feature is not enabled for this account. To use this feature, you must first enable it through the Amazon Web Services Management Console or API.</Message>
  </Error>
  <RequestId>f5b3d023-61be-4150-9a54-3d07c0fb58de</RequestId>
</ErrorResponse>
```

`403` `OutboundWebIdentityFederationDisabledException`: the request was refused by authorization.

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
