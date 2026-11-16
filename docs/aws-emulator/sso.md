# AWS Single Sign-On

## Wire protocol

- **Protocol:** `restJson1`
- **SDK id:** `SSO`
- **sigv4 signing name:** `awsssoportal`
- **Endpoint prefix:** `portal.sso`
- **ARN namespace:** `awsssoportal`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `awsssoportal`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListAccounts`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:awsssoportal" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -H 'x-amz-sso_bearer_token: <token from sso-oidc CreateToken>' \
  'https://127.0.0.1:9297/assignment/accounts'
```

This one answers `401` until you substitute a real token, as the walkthrough below shows.

## End-to-end walkthrough

List the accounts a portal session can reach. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. ListAccounts

The access token comes from `sso-oidc` `CreateToken` after a person approved the device; a token no session holds is refused with 401 `UnauthorizedException`.

**Request**

```http
GET /assignment/accounts HTTP/1.1
x-amz-sso_bearer_token: not-a-real-session-token
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121017Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/awsssoportal/aws4_request, SignedHeaders=host;x-amz-date;x-amz-sso_bearer_token, Signature=<signature>
```

**Response**

```http
HTTP/1.1 401 Unauthorized
content-type: application/json
x-amzn-errortype: UnauthorizedException
content-length: 81
date: Mon, 16 Nov 2026 12:10:17 GMT

{
  "__type": "UnauthorizedException",
  "message": "Session token not found or invalid"
}
```

### 2. GetRoleCredentials

The same token gates role credentials.

**Request**

```http
GET /federation/credentials?account_id=000000000000&role_name=ReadOnly HTTP/1.1
x-amz-sso_bearer_token: not-a-real-session-token
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121017Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/awsssoportal/aws4_request, SignedHeaders=host;x-amz-date;x-amz-sso_bearer_token, Signature=<signature>
```

**Response**

```http
HTTP/1.1 401 Unauthorized
content-type: application/json
x-amzn-errortype: UnauthorizedException
content-length: 81
date: Mon, 16 Nov 2026 12:10:17 GMT

{
  "__type": "UnauthorizedException",
  "message": "Session token not found or invalid"
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`) are left out here; the walkthrough shows them in full.

Of 4 operations, 0 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### GetRoleCredentials

Returns the STS short-term credentials for a given role name that is assigned to the user.

```http
GET /federation/credentials?account_id=example&role_name=example HTTP/1.1
x-amz-sso_bearer_token: example
```

```http
HTTP/1.1 401 Unauthorized
content-type: application/json
x-amzn-errortype: UnauthorizedException

{
  "__type": "UnauthorizedException",
  "message": "Session token not found or invalid"
}
```

`401` `UnauthorizedException`: the request was refused by authorization.

### ListAccountRoles

Lists all roles that are assigned to the user for a given AWS account.

```http
GET /assignment/roles?account_id=example HTTP/1.1
x-amz-sso_bearer_token: example
```

```http
HTTP/1.1 401 Unauthorized
content-type: application/json
x-amzn-errortype: UnauthorizedException

{
  "__type": "UnauthorizedException",
  "message": "Session token not found or invalid"
}
```

`401` `UnauthorizedException`: the request was refused by authorization.

### ListAccounts

Lists all AWS accounts assigned to the user.

```http
GET /assignment/accounts HTTP/1.1
x-amz-sso_bearer_token: example
```

```http
HTTP/1.1 401 Unauthorized
content-type: application/json
x-amzn-errortype: UnauthorizedException

{
  "__type": "UnauthorizedException",
  "message": "Session token not found or invalid"
}
```

`401` `UnauthorizedException`: the request was refused by authorization.

### Logout

Removes the locally stored SSO tokens from the client-side cache and sends an API call to the IAM Identity Center service to invalidate the corresponding server-side IAM Identity Center sign in session.

```http
POST /logout HTTP/1.1
x-amz-sso_bearer_token: example
```

```http
HTTP/1.1 401 Unauthorized
content-type: application/json
x-amzn-errortype: UnauthorizedException

{
  "__type": "UnauthorizedException",
  "message": "Session token not found or invalid"
}
```

`401` `UnauthorizedException`: the request was refused by authorization.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/awsssoportal/aws4_request` in the `Authorization` header) and the operation from the request itself: an HTTP method and path per operation (the `HTTP binding`), with identifiers in the path, options in the query string or headers, and the rest of the input as a JSON body.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way AWS Single Sign-On answers in AWS: a JSON body and an HTTP status chosen per operation. Errors captured on this page are a JSON body with `__type`, `message`, plus the `x-amzn-errortype` header.
