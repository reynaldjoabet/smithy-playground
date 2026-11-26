# AWS SSO OIDC

## Wire protocol

- **Protocol:** `restJson1`
- **SDK id:** `SSO OIDC`
- **sigv4 signing name:** `sso-oauth`
- **Endpoint prefix:** `oidc`
- **ARN namespace:** `sso-oauth`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `sso-oauth`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `RegisterClient`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:sso-oauth" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/json' \
  --data-binary '{"clientName": "my-cli", "clientType": "public"}' \
  'https://127.0.0.1:9297/client/register'
```

## End-to-end walkthrough

Register an OIDC client and start the device authorization flow the AWS CLI uses for `aws sso login`. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. RegisterClient

The client id and secret (redacted here) authenticate the later calls; this operation is not SigV4-signed in AWS.

**Request**

```http
POST /client/register HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 84
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso-oauth/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientName": "docs-demo-cli",
  "clientType": "public",
  "scopes": [
    "sso:account:access"
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 226
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "clientId": "3729f0b0462247e0a6f2cda4af75b6f896aa09dfdd124f93aae0161a8a5edbdb",
  "clientSecret": "<redacted>",
  "clientIdIssuedAt": 1795717852,
  "clientSecretExpiresAt": 1803493852
}
```

### 2. AWS Single Sign-On Admin: ListInstances

The device flow must start from the Identity Center instance's portal URL.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: SWBExternalService.ListInstances
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 624
date: Thu, 26 Nov 2026 18:30:52 GMT

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

### 3. StartDeviceAuthorization

Returns the device code, the user code a person types into the verification page, and the polling interval.

**Request**

```http
POST /device_authorization HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 213
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso-oauth/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientId": "3729f0b0462247e0a6f2cda4af75b6f896aa09dfdd124f93aae0161a8a5edbdb",
  "clientSecret": "<redacted>",
  "startUrl": "https://d-9067f3a1c2.awsapps.com/start"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 289
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "deviceCode": "<redacted>",
  "userCode": "<redacted>",
  "verificationUri": "https://device.sso.us-east-1.amazonaws.com/",
  "verificationUriComplete": "https://device.sso.us-east-1.amazonaws.com/?user_code=<redacted>",
  "expiresIn": 600,
  "interval": 5
}
```

### 4. CreateToken

Until a person approves the user code, polling answers `AuthorizationPendingException`, exactly as a CLI sees while it waits.

**Request**

```http
POST /token HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 300
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso-oauth/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientId": "3729f0b0462247e0a6f2cda4af75b6f896aa09dfdd124f93aae0161a8a5edbdb",
  "clientSecret": "<redacted>",
  "grantType": "urn:ietf:params:oauth:grant-type:device_code",
  "deviceCode": "<redacted>"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: AuthorizationPendingException
content-length: 127
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "AuthorizationPendingException",
  "error": "authorization_pending",
  "error_description": "Authorization is still pending"
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 4 operations, 0 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### CreateToken

Creates and returns access and refresh tokens for clients that are authenticated using client secrets.

```http
POST /token HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 126
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso-oauth/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientId": "example",
  "clientSecret": "<redacted>",
  "grantType": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: InvalidRequestException
content-length: 100
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "InvalidRequestException",
  "error": "invalid_request",
  "error_description": "Invalid request"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### CreateTokenWithIAM

Creates and returns access and refresh tokens for authorized client applications that are authenticated using any IAM entity, such as a service role or user.

```http
POST /token?aws_iam=t HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso-oauth/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientId": "example",
  "grantType": "example"
}
```

```http
HTTP/1.1 401 Unauthorized
content-type: application/json
x-amzn-errortype: InvalidClientException
content-length: 106
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "InvalidClientException",
  "error": "invalid_client",
  "error_description": "Invalid client provided"
}
```

`401` `InvalidClientException`: the request was refused by authorization.

### RegisterClient

Registers a public client with IAM Identity Center.

```http
POST /client/register HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso-oauth/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientName": "example",
  "clientType": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: InvalidClientMetadataException
content-length: 128
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "InvalidClientMetadataException",
  "error": "invalid_client_metadata",
  "error_description": "Invalid client type provided"
}
```

`400` `InvalidClientMetadataException`. See [the walkthrough](#1-registerclient) for a successful call.

### StartDeviceAuthorization

Initiates device authorization by requesting a pair of verification codes from the authorization service.

```http
POST /device_authorization HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 125
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sso-oauth/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientId": "example",
  "clientSecret": "<redacted>",
  "startUrl": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: InvalidRequestException
content-length: 100
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "InvalidRequestException",
  "error": "invalid_request",
  "error_description": "Invalid request"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-startdeviceauthorization) for a successful call.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/sso-oauth/aws4_request` in the `Authorization` header) and the operation from the request itself: an HTTP method and path per operation (the `HTTP binding`), with identifiers in the path, options in the query string or headers, and the rest of the input as a JSON body.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way AWS SSO OIDC answers in AWS: a JSON body and an HTTP status chosen per operation. Errors captured on this page are a JSON body with `__type`, plus the `x-amzn-errortype` header.

### Other services involved

The walkthrough also called [AWS Single Sign-On Admin](sso-admin.md). They are separate services behind the same endpoint, sharing one account and one in-memory engine, so a resource created through one is immediately visible to the other.
