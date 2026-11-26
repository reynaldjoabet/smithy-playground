# AWS Secrets Manager

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: secretsmanager.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `Secrets Manager`
- **sigv4 signing name:** `secretsmanager`
- **Endpoint prefix:** `secretsmanager`
- **ARN namespace:** `secretsmanager`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `secretsmanager`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListSecrets`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:secretsmanager" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.1' \
  -H 'X-Amz-Target: secretsmanager.ListSecrets' \
  --data-binary '{}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Store a secret, read it, rotate its value by hand, then delete it. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateSecret

The ARN ends in the name plus six random characters; the version id is the `ClientRequestToken` the request supplied.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.CreateSecret
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 121
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "prod/db/password",
  "ClientRequestToken": "11111111-2222-3333-4444-555555555555",
  "SecretString": "<redacted>"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 163
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "ARN": "arn:aws:secretsmanager:us-east-1:000000000000:secret:prod/db/password-rVzQGu",
  "Name": "prod/db/password",
  "VersionId": "11111111-2222-3333-4444-555555555555"
}
```

### 2. GetSecretValue

The current value comes back with the `AWSCURRENT` staging label. Reads and writes go through the secret's KMS key (the `aws/secretsmanager` key by default).

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.GetSecretValue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "prod/db/password"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 257
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "ARN": "arn:aws:secretsmanager:us-east-1:000000000000:secret:prod/db/password-rVzQGu",
  "Name": "prod/db/password",
  "VersionId": "11111111-2222-3333-4444-555555555555",
  "SecretString": "<redacted>",
  "VersionStages": [
    "AWSCURRENT"
  ],
  "CreatedDate": 1795717851.456
}
```

### 3. PutSecretValue

A new version takes `AWSCURRENT`; the old one moves to `AWSPREVIOUS`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.PutSecretValue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 125
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "prod/db/password",
  "ClientRequestToken": "fda5fc4f-3168-415f-b711-a7f73fb034a7",
  "SecretString": "<redacted>"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 194
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "ARN": "arn:aws:secretsmanager:us-east-1:000000000000:secret:prod/db/password-rVzQGu",
  "Name": "prod/db/password",
  "VersionId": "fda5fc4f-3168-415f-b711-a7f73fb034a7",
  "VersionStages": [
    "AWSCURRENT"
  ]
}
```

### 4. ListSecretVersionIds

Both versions are listed with the labels they now carry.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.ListSecretVersionIds
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "prod/db/password"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 455
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "Versions": [
    {
      "VersionId": "11111111-2222-3333-4444-555555555555",
      "VersionStages": [
        "AWSPREVIOUS"
      ],
      "LastAccessedDate": 1795651200,
      "CreatedDate": 1795717851.456,
      "KmsKeyIds": [
        "DefaultEncryptionKey"
      ]
    },
    {
      "VersionId": "fda5fc4f-3168-415f-b711-a7f73fb034a7",
      "VersionStages": [
        "AWSCURRENT"
      ],
      "CreatedDate": 1795717851.46,
      "KmsKeyIds": [
        "DefaultEncryptionKey"
      ]
    }
  ],
  "ARN": "arn:aws:secretsmanager:us-east-1:000000000000:secret:prod/db/password-rVzQGu",
  "Name": "prod/db/password"
}
```

### 5. DescribeSecret

Metadata only, never the value. Any `SecretId` can be the name or the ARN.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.DescribeSecret
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 91
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "arn:aws:secretsmanager:us-east-1:000000000000:secret:prod/db/password-rVzQGu"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 335
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "ARN": "arn:aws:secretsmanager:us-east-1:000000000000:secret:prod/db/password-rVzQGu",
  "Name": "prod/db/password",
  "LastChangedDate": 1795717851.46,
  "LastAccessedDate": 1795651200,
  "VersionIdsToStages": {
    "fda5fc4f-3168-415f-b711-a7f73fb034a7": [
      "AWSCURRENT"
    ],
    "11111111-2222-3333-4444-555555555555": [
      "AWSPREVIOUS"
    ]
  },
  "CreatedDate": 1795717851.456
}
```

### 6. DeleteSecret

Forced deletion skips the 7–30 day recovery window.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.DeleteSecret
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 65
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "prod/db/password",
  "ForceDeleteWithoutRecovery": true
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 142
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "ARN": "arn:aws:secretsmanager:us-east-1:000000000000:secret:prod/db/password-rVzQGu",
  "Name": "prod/db/password",
  "DeletionDate": 1795717851.463
}
```

### 7. GetSecretValue

Deletion completes in the background, so a read right after it is refused: the secret is marked for deletion.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.GetSecretValue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "prod/db/password"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 131
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "InvalidRequestException",
  "Message": "You can't perform this operation on the secret because it was marked for deletion."
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 23 operations, 10 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### BatchGetSecretValue

Retrieves the contents of the encrypted fields SecretString or SecretBinary for up to 20 secrets.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.BatchGetSecretValue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 107
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "InvalidParameterException",
  "Message": "You must specify a list of Secret IDs or a filter value."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CancelRotateSecret

Turns off automatic rotation, and if a rotation is currently in progress, cancels the rotation.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.CancelRotateSecret
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 99
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Secrets Manager can't find the specified secret."
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateSecret

Creates a new secret.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.CreateSecret
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 78
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Name": "example",
  "ClientRequestToken": "fab5ace6-4b3f-4939-a6fc-94d01af1caac"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 94
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "ARN": "arn:aws:secretsmanager:us-east-1:000000000000:secret:example-D5ytA8",
  "Name": "example"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#1-createsecret) for a successful call.

### DeleteResourcePolicy

Deletes the resource-based permission policy attached to the secret.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.DeleteResourcePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 94
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "ARN": "arn:aws:secretsmanager:us-east-1:000000000000:secret:example-D5ytA8",
  "Name": "example"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteSecret

Deletes a secret and all of its versions.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.DeleteSecret
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 124
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "ARN": "arn:aws:secretsmanager:us-east-1:000000000000:secret:example-D5ytA8",
  "Name": "example",
  "DeletionDate": 1798309851.468
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#6-deletesecret) for a successful call.

### DescribeSecret

Retrieves the details of a secret.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.DescribeSecret
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 185
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "ARN": "arn:aws:secretsmanager:us-east-1:000000000000:secret:example-D5ytA8",
  "Name": "example",
  "LastChangedDate": 1795717851.468,
  "DeletedDate": 1795717851.468,
  "CreatedDate": 1795717851.466
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#5-describesecret) for a successful call.

### GetRandomPassword

Generates a random password.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.GetRandomPassword
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 53
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "RandomPassword": "<redacted>"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetResourcePolicy

Retrieves the JSON text of the resource-based policy document attached to the secret.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.GetResourcePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 131
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "InvalidRequestException",
  "Message": "You can't perform this operation on the secret because it was marked for deletion."
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### GetSecretValue

Retrieves the contents of the encrypted fields SecretString or SecretBinary from the specified version of a secret, whichever contains content.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.GetSecretValue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 131
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "InvalidRequestException",
  "Message": "You can't perform this operation on the secret because it was marked for deletion."
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-getsecretvalue) for a successful call.

### ListSecretVersionIds

Lists the versions of a secret.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.ListSecretVersionIds
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 108
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "Versions": [],
  "ARN": "arn:aws:secretsmanager:us-east-1:000000000000:secret:example-D5ytA8",
  "Name": "example"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#4-listsecretversionids) for a successful call.

### ListSecrets

Lists the secrets that are stored by Secrets Manager in the Amazon Web Services account, not including secrets that are marked for deletion.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.ListSecrets
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 17
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "SecretList": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### PutResourcePolicy

Attaches a resource-based permission policy to a secret.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.PutResourcePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example",
  "ResourcePolicy": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 103
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "MalformedPolicyDocumentException",
  "Message": "This resource policy contains a syntax error."
}
```

`400` `MalformedPolicyDocumentException`: the placeholder values failed the operation's input validation.

### PutSecretValue

Creates a new version of your secret by creating a new encrypted value and attaching it to the secret. version can contain a new SecretString value or a new SecretBinary value.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.PutSecretValue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 82
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example",
  "ClientRequestToken": "a4e43738-3c56-4200-97cb-f8e5bd22cbc0"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 102
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "InvalidRequestException",
  "Message": "You must provide either SecretString or SecretBinary."
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-putsecretvalue) for a successful call.

### RemoveRegionsFromReplication

For a secret that is replicated to other Regions, deletes the secret replicas from the Regions you specify.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.RemoveRegionsFromReplication
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example",
  "RemoveReplicaRegions": [
    "example-1"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 131
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "InvalidRequestException",
  "Message": "You can't perform this operation on the secret because it was marked for deletion."
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### ReplicateSecretToRegions

Replicates the secret to a new Regions.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.ReplicateSecretToRegions
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example",
  "AddReplicaRegions": [
    {}
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 131
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "InvalidRequestException",
  "Message": "You can't perform this operation on the secret because it was marked for deletion."
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### RestoreSecret

Cancels the scheduled deletion of a secret by removing the DeletedDate time stamp.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.RestoreSecret
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 94
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "ARN": "arn:aws:secretsmanager:us-east-1:000000000000:secret:example-D5ytA8",
  "Name": "example"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### RotateSecret

Configures and starts the asynchronous process of rotating the secret.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.RotateSecret
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 82
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example",
  "ClientRequestToken": "cd6e38ed-a160-4072-92d3-5c9afc87eed8"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 112
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "InvalidRequestException",
  "Message": "No Lambda rotation function ARN is associated with this secret."
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### StopReplicationToReplica

Removes the link between the replica secret and the primary secret and promotes the replica to a primary secret in the replica Region.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.StopReplicationToReplica
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 106
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "InvalidParameterException",
  "Message": "Operation not permitted on a primary/standalone secret."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### TagResource

Attaches tags to a secret.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.TagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 34
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example",
  "Tags": [
    {}
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 91
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "InvalidParameterException",
  "Message": "Invalid Tags. Every tag must have a Key."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UntagResource

Removes specific tags from a secret.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.UntagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example",
  "TagKeys": [
    "example"
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 2
date: Thu, 26 Nov 2026 18:30:51 GMT

{}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### UpdateSecret

Modifies the details of a secret, including metadata and the secret value.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.UpdateSecret
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 82
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example",
  "ClientRequestToken": "c22d96e5-910e-4261-a5b5-1fa449d77a85"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 94
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "ARN": "arn:aws:secretsmanager:us-east-1:000000000000:secret:example-D5ytA8",
  "Name": "example"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### UpdateSecretVersionStage

Modifies the staging labels attached to a version of a secret.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.UpdateSecretVersionStage
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SecretId": "example",
  "VersionStage": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 114
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "InvalidParameterException",
  "Message": "You must specify either MoveToVersionId or RemoveFromVersionId."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ValidateResourcePolicy

Validates that a resource policy does not grant a wide range of principals access to your secret.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: secretsmanager.ValidateResourcePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/secretsmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourcePolicy": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 103
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "MalformedPolicyDocumentException",
  "Message": "This resource policy contains a syntax error."
}
```

`400` `MalformedPolicyDocumentException`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/secretsmanager/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way AWS Secrets Manager answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `Message`.
