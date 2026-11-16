# AWS CloudHSM Classic

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: CloudHsmFrontendService.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `CloudHSM`
- **sigv4 signing name:** `cloudhsm`
- **Endpoint prefix:** `cloudhsm`
- **ARN namespace:** `cloudhsm`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `cloudhsm`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListHsms`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:cloudhsm" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.1' \
  -H 'X-Amz-Target: CloudHsmFrontendService.ListHsms' \
  --data-binary '{}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create, describe and delete an HSM through the legacy CloudHSM Classic API. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateHsm

The Classic model only accepts the old 8-hex-digit subnet ids (`^subnet-[0-9a-f]{8}$`), so a current subnet id, like the default VPC's, is refused before anything else is checked. The SSH key is redacted on this page.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: CloudHsmFrontendService.CreateHsm
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124456Z
Content-Length: 209
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SubnetId": "subnet-00000000000000007",
  "SshKey": "<redacted>",
  "IamRoleArn": "arn:aws:iam::000000000000:role/CloudHSMRole",
  "SubscriptionType": "PRODUCTION"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 219
date: Mon, 16 Nov 2026 12:44:56 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'subnet-00000000000000007' at 'subnetId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^subnet-[0-9a-f]{8}$"
}
```

### 2. CreateHsm

An id in the legacy format is accepted; the emulator does not look this subnet up in EC2.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: CloudHsmFrontendService.CreateHsm
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124456Z
Content-Length: 200
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SubnetId": "subnet-0a1b2c3d",
  "SshKey": "<redacted>",
  "IamRoleArn": "arn:aws:iam::000000000000:role/CloudHSMRole",
  "SubscriptionType": "PRODUCTION"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 65
date: Mon, 16 Nov 2026 12:44:56 GMT

{
  "HsmArn": "arn:aws:cloudhsm:us-east-1:000000000000:hsm-00000002"
}
```

### 3. DescribeHsm

The HSM's status, subnet, IP address and serial number.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: CloudHsmFrontendService.DescribeHsm
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124456Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "HsmArn": "arn:aws:cloudhsm:us-east-1:000000000000:hsm-00000002"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 583
date: Mon, 16 Nov 2026 12:44:56 GMT

{
  "HsmArn": "arn:aws:cloudhsm:us-east-1:000000000000:hsm-00000002",
  "Status": "PENDING",
  "AvailabilityZone": "us-east-1f",
  "EniId": "eni-00000002",
  "EniIp": "10.0.0.3",
  "SubscriptionType": "PRODUCTION",
  "SubscriptionStartDate": "1794833096",
  "VpcId": "vpc-6e347ebc",
  "SubnetId": "subnet-0a1b2c3d",
  "IamRoleArn": "arn:aws:iam::000000000000:role/CloudHSMRole",
  "SerialNumber": "100000000002",
  "VendorName": "SafeNet Inc.",
  "HsmType": "safenet-luna-sa-7000",
  "SoftwareVersion": "5.3.13",
  "SshPublicKey": "<redacted>",
  "SshKeyLastUpdated": "1794833096",
  "Partitions": []
}
```

### 4. ListHsms

HSM ARNs of the account.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: CloudHsmFrontendService.ListHsms
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124456Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 68
date: Mon, 16 Nov 2026 12:44:56 GMT

{
  "HsmList": [
    "arn:aws:cloudhsm:us-east-1:000000000000:hsm-00000002"
  ]
}
```

### 5. DeleteHsm

Delete it.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: CloudHsmFrontendService.DeleteHsm
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124456Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "HsmArn": "arn:aws:cloudhsm:us-east-1:000000000000:hsm-00000002"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 20
date: Mon, 16 Nov 2026 12:44:56 GMT

{
  "Status": "DELETED"
}
```

### 6. DescribeHsm

Describing it again answers `CloudHsmServiceException`: no HSM matches the ARN any more.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: CloudHsmFrontendService.DescribeHsm
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T124456Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "HsmArn": "arn:aws:cloudhsm:us-east-1:000000000000:hsm-00000002"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 112
date: Mon, 16 Nov 2026 12:44:56 GMT

{
  "__type": "CloudHsmServiceException",
  "message": "no HSM matches the identifier in the request",
  "retryable": false
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 20 operations, 1 accepted the placeholder call, 9 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AddTagsToResource

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.AddTagsToResource

{
  "ResourceArn": "arn:aws:cloudhsm:us-east-1:000000000000:example/example",
  "TagList": [
    {
      "Key": "example",
      "Value": "example"
    }
  ]
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation AddTagsToResource is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateHapg

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.CreateHapg

{
  "Label": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation CreateHapg is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateHsm

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.CreateHsm

{
  "SubnetId": "example",
  "SshKey": "<redacted>",
  "IamRoleArn": "example",
  "SubscriptionType": "PRODUCTION"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'subnetId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^subnet-[0-9a-f]{8}$; Value 'example' at 'iamRoleArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:aws(-iso)?:iam::[0-9]{12}:role/[a-zA-Z0-9_\\+=,\\.\\-@]{1,64}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-createhsm) for a successful call.

### CreateLunaClient

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.CreateLunaClient

{
  "Certificate": "example00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation CreateLunaClient is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteHapg

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.DeleteHapg

{
  "HapgArn": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'hapgArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:aws(-iso)?:cloudhsm:[a-zA-Z0-9\\-]*:[0-9]{12}:hapg-[0-9a-f]{8}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteHsm

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.DeleteHsm

{
  "HsmArn": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'hsmArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:aws(-iso)?:cloudhsm:[a-zA-Z0-9\\-]*:[0-9]{12}:hsm-[0-9a-f]{8}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#5-deletehsm) for a successful call.

### DeleteLunaClient

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.DeleteLunaClient

{
  "ClientArn": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'clientArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:aws(-iso)?:cloudhsm:[a-zA-Z0-9\\-]*:[0-9]{12}:client-[0-9a-f]{8}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeHapg

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.DescribeHapg

{
  "HapgArn": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'hapgArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:aws(-iso)?:cloudhsm:[a-zA-Z0-9\\-]*:[0-9]{12}:hapg-[0-9a-f]{8}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeHsm

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.DescribeHsm

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidRequestException",
  "message": "specify either HsmArn or HsmSerialNumber to identify the HSM",
  "retryable": false
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-describehsm) for a successful call.

### DescribeLunaClient

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.DescribeLunaClient

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation DescribeLunaClient is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetConfig

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.GetConfig

{
  "ClientArn": "example",
  "ClientVersion": "5.1",
  "HapgList": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'clientArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:aws(-iso)?:cloudhsm:[a-zA-Z0-9\\-]*:[0-9]{12}:client-[0-9a-f]{8}$; Value 'example' at 'hapgList.1.member' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:aws(-iso)?:cloudhsm:[a-zA-Z0-9\\-]*:[0-9]{12}:hapg-[0-9a-f]{8}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListAvailableZones

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.ListAvailableZones

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation ListAvailableZones is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListHapgs

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.ListHapgs

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation ListHapgs is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListHsms

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.ListHsms

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "HsmList": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#4-listhsms) for a successful call.

### ListLunaClients

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.ListLunaClients

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation ListLunaClients is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListTagsForResource

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.ListTagsForResource

{
  "ResourceArn": "arn:aws:cloudhsm:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation ListTagsForResource is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyHapg

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.ModifyHapg

{
  "HapgArn": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'hapgArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:aws(-iso)?:cloudhsm:[a-zA-Z0-9\\-]*:[0-9]{12}:hapg-[0-9a-f]{8}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ModifyHsm

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.ModifyHsm

{
  "HsmArn": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'hsmArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:aws(-iso)?:cloudhsm:[a-zA-Z0-9\\-]*:[0-9]{12}:hsm-[0-9a-f]{8}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ModifyLunaClient

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.ModifyLunaClient

{
  "ClientArn": "example",
  "Certificate": "example00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'clientArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:aws(-iso)?:cloudhsm:[a-zA-Z0-9\\-]*:[0-9]{12}:client-[0-9a-f]{8}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### RemoveTagsFromResource

This is documentation for AWS CloudHSM Classic .

```http
POST / HTTP/1.1
X-Amz-Target: CloudHsmFrontendService.RemoveTagsFromResource

{
  "ResourceArn": "arn:aws:cloudhsm:us-east-1:000000000000:example/example",
  "TagKeyList": [
    "example"
  ]
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation RemoveTagsFromResource is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/cloudhsm/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way AWS CloudHSM Classic answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `message`.
