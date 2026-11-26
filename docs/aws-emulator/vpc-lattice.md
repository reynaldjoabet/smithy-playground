# Amazon VPC Lattice

## Wire protocol

- **Protocol:** `restJson1`
- **SDK id:** `VPC Lattice`
- **sigv4 signing name:** `vpc-lattice`
- **ARN namespace:** `vpc-lattice`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `vpc-lattice`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListServices`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:vpc-lattice" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  'https://127.0.0.1:9297/services'
```

## End-to-end walkthrough

Create a service network and a service, associate them, then remove everything. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateServiceNetwork

A service network groups services that can reach each other.

**Request**

```http
POST /servicenetworks HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 95
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "f3c5b99c-ce22-4f9e-9e81-8f51f99afb14",
  "name": "docs-demo-net",
  "authType": "NONE"
}
```

**Response**

```http
HTTP/1.1 201 Created
content-type: application/json
content-length: 190
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "id": "sn-00b00e9acc7d23dae",
  "name": "docs-demo-net",
  "arn": "arn:aws:vpc-lattice:us-east-1:000000000000:servicenetwork/sn-00b00e9acc7d23dae",
  "sharingConfig": {
    "enabled": true
  },
  "authType": "NONE"
}
```

### 2. CreateService

The service's DNS entry is the emulator's own address, `127.0.0.1:9297`, rather than a `vpc-lattice-svcs` hostname.

**Request**

```http
POST /services HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 95
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "fbb979e1-eb95-4f2e-982f-d4cd17687603",
  "name": "docs-demo-svc",
  "authType": "NONE"
}
```

**Response**

```http
HTTP/1.1 201 Created
content-type: application/json
content-length: 282
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "id": "svc-09acc9f6690b7d5fc",
  "arn": "arn:aws:vpc-lattice:us-east-1:000000000000:service/svc-09acc9f6690b7d5fc",
  "name": "docs-demo-svc",
  "status": "CREATE_IN_PROGRESS",
  "authType": "NONE",
  "idleTimeoutSeconds": 60,
  "dnsEntry": {
    "domainName": "127.0.0.1",
    "hostedZoneId": "Z75XN9V3URQW92UXEHSC"
  }
}
```

### 3. CreateServiceNetworkServiceAssociation

Associating places the service in the network; the association starts in `CREATE_IN_PROGRESS`.

**Request**

```http
POST /servicenetworkserviceassociations HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 148
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "31c048b4-855e-4c9c-b3d1-8d676ad9af3b",
  "serviceIdentifier": "svc-09acc9f6690b7d5fc",
  "serviceNetworkIdentifier": "sn-00b00e9acc7d23dae"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 271
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "id": "snsa-0536714c13de2d461",
  "status": "CREATE_IN_PROGRESS",
  "arn": "arn:aws:vpc-lattice:us-east-1:000000000000:servicenetworkserviceassociation/snsa-0536714c13de2d461",
  "createdBy": "000000000000",
  "dnsEntry": {
    "domainName": "127.0.0.1",
    "hostedZoneId": "Z75XN9V3URQW92UXEHSC"
  }
}
```

### 4. ListServiceNetworkServiceAssociations

The association and its status.

**Request**

```http
GET /servicenetworkserviceassociations?serviceNetworkIdentifier=sn-00b00e9acc7d23dae HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 659
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "items": [
    {
      "id": "snsa-0536714c13de2d461",
      "status": "CREATE_IN_PROGRESS",
      "arn": "arn:aws:vpc-lattice:us-east-1:000000000000:servicenetworkserviceassociation/snsa-0536714c13de2d461",
      "createdBy": "000000000000",
      "createdAt": "2026-11-26T18:30:52.213163Z",
      "serviceId": "svc-09acc9f6690b7d5fc",
      "serviceName": "docs-demo-svc",
      "serviceArn": "arn:aws:vpc-lattice:us-east-1:000000000000:service/svc-09acc9f6690b7d5fc",
      "serviceNetworkId": "sn-00b00e9acc7d23dae",
      "serviceNetworkName": "docs-demo-net",
      "serviceNetworkArn": "arn:aws:vpc-lattice:us-east-1:000000000000:servicenetwork/sn-00b00e9acc7d23dae",
      "dnsEntry": {
        "domainName": "127.0.0.1",
        "hostedZoneId": "Z75XN9V3URQW92UXEHSC"
      }
    }
  ]
}
```

### 5. DeleteService

A service that is still associated cannot be deleted: 409 `ConflictException`.

**Request**

```http
DELETE /services/svc-09acc9f6690b7d5fc HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 409 Conflict
content-type: application/json
x-amzn-errortype: ConflictException
content-length: 179
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ConflictException",
  "message": "Service svc-09acc9f6690b7d5fc has one or more associated service networks.",
  "resourceId": "svc-09acc9f6690b7d5fc",
  "resourceType": "SERVICE"
}
```

### 6. DeleteServiceNetworkServiceAssociation

Remove the association ...

**Request**

```http
DELETE /servicenetworkserviceassociations/snsa-0536714c13de2d461 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 168
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "id": "snsa-0536714c13de2d461",
  "status": "DELETE_IN_PROGRESS",
  "arn": "arn:aws:vpc-lattice:us-east-1:000000000000:servicenetworkserviceassociation/snsa-0536714c13de2d461"
}
```

### 7. DeleteService

... the service ...

**Request**

```http
DELETE /services/svc-09acc9f6690b7d5fc HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 164
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "id": "svc-09acc9f6690b7d5fc",
  "arn": "arn:aws:vpc-lattice:us-east-1:000000000000:service/svc-09acc9f6690b7d5fc",
  "name": "docs-demo-svc",
  "status": "DELETE_IN_PROGRESS"
}
```

### 8. DeleteServiceNetwork

... and the network.

**Request**

```http
DELETE /servicenetworks/sn-00b00e9acc7d23dae HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 204 No Content
content-type: application/json
content-length: 2
date: Thu, 26 Nov 2026 18:30:52 GMT
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 73 operations, 9 accepted the placeholder call, 3 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.629* answered `501` in 1.0.595. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### BatchUpdateRule

Updates the listener rules in a batch.

```http
PATCH /services/example0000000000/listeners/example0000000000000/rules HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "rules": [
    {
      "ruleIdentifier": "example0000000000000"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 874
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "3 validation errors detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17})); Value 'example0000000000000' at 'listenerIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((listener-[0-9a-z]{17})|(^arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}/listener/listener-[0-9a-z]{17}$)); Value 'example0000000000000' at 'rules.1.member.ruleIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((rule-[0-9a-z]{17})|(^arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}/listener/listener-[0-9a-z]{17}/rule/rule-[0-9a-z]{17}$))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateAccessLogSubscription

Enables access logs to be sent to Amazon CloudWatch, Amazon S3, and Amazon Kinesis Data Firehose.

```http
POST /accesslogsubscriptions HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 172
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "bd8fbd89-5d44-4754-bf22-70c8eaff07a7",
  "resourceIdentifier": "example0000000000",
  "destinationArn": "arn:aws:vpclattice:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 387
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'resourceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((((sn)|(svc)|(rcfg))-[0-9a-z]{17})|(arn(:[a-z0-9]+([.-][a-z0-9]+)*){2}(:([a-z0-9]+([.-][a-z0-9]+)*)?){2}:((servicenetwork/sn)|(resourceconfiguration/rcfg)|(service/svc))-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateListener

Creates a listener for a service.

```http
POST /services/example0000000000/listeners HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 182
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "name": "example",
  "protocol": "HTTP",
  "defaultAction": {
    "forward": {
      "targetGroups": [
        {
          "targetGroupIdentifier": "example0000000000"
        }
      ]
    }
  },
  "clientToken": "c9c52a84-c725-4661-8a6c-4d534bdada47"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 581
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17})); Value 'example0000000000' at 'defaultAction.forward.targetGroups.1.member.targetGroupIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((tg-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:targetgroup/tg-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateResourceConfiguration

Creates a resource configuration.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST /resourceconfigurations HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 86
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "name": "example",
  "type": "GROUP",
  "clientToken": "34b15189-598d-48c0-996c-cdb07923df63"
}
```

```http
HTTP/1.1 201 Created
content-type: application/json
content-length: 289
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "id": "rcfg-09e157269bdc6458c",
  "name": "example",
  "arn": "arn:aws:vpc-lattice:us-east-1:000000000000:resourceconfiguration/rcfg-09e157269bdc6458c",
  "type": "GROUP",
  "protocol": "TCP",
  "status": "CREATE_IN_PROGRESS",
  "allowAssociationToShareableServiceNetwork": false,
  "createdAt": 1795717852.225236
}
```

Accepted: the emulator answered `201 Created` even with placeholder values.

### CreateResourceGateway

A resource gateway is a point of ingress into the VPC where a resource resides.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST /resourcegateways HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "c7cb806d-2bf4-4c74-9c1d-850ef12233be",
  "name": "example"
}
```

```http
HTTP/1.1 201 Created
content-type: application/json
content-length: 166
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "name": "example",
  "id": "rgw-0807a147116c8d0bc",
  "arn": "arn:aws:vpc-lattice:us-east-1:000000000000:resourcegateway/rgw-0807a147116c8d0bc",
  "status": "CREATE_IN_PROGRESS"
}
```

Accepted: the emulator answered `201 Created` even with placeholder values.

### CreateRule

Creates a listener rule.

```http
POST /services/example0000000000/listeners/example0000000000000/rules HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 195
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "name": "example",
  "match": {
    "httpMatch": {}
  },
  "priority": 1,
  "action": {
    "forward": {
      "targetGroups": [
        {
          "targetGroupIdentifier": "example0000000000"
        }
      ]
    }
  },
  "clientToken": "6e724a4d-cc99-4440-9610-798e2031f213"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 845
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "3 validation errors detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17})); Value 'example0000000000000' at 'listenerIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((listener-[0-9a-z]{17})|(^arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}/listener/listener-[0-9a-z]{17}$)); Value 'example0000000000' at 'action.forward.targetGroups.1.member.targetGroupIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((tg-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:targetgroup/tg-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateService

Creates a service.

```http
POST /services HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "3d2ea71c-16d1-42aa-a381-10497ca20fa3",
  "name": "example"
}
```

```http
HTTP/1.1 201 Created
content-type: application/json
content-length: 276
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "id": "svc-0b60181ef23d09d25",
  "arn": "arn:aws:vpc-lattice:us-east-1:000000000000:service/svc-0b60181ef23d09d25",
  "name": "example",
  "status": "CREATE_IN_PROGRESS",
  "authType": "NONE",
  "idleTimeoutSeconds": 60,
  "dnsEntry": {
    "domainName": "127.0.0.1",
    "hostedZoneId": "ZO1967O49886DAG26BSV"
  }
}
```

Accepted: the emulator answered `201 Created` even with placeholder values. See [the walkthrough](#2-createservice) for a successful call.

### CreateServiceNetwork

Creates a service network.

```http
POST /servicenetworks HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "71a3ffb6-92b3-4eac-aa48-cfe7ba7ef630",
  "name": "example"
}
```

```http
HTTP/1.1 201 Created
content-type: application/json
content-length: 184
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "id": "sn-06af534787b315aac",
  "name": "example",
  "arn": "arn:aws:vpc-lattice:us-east-1:000000000000:servicenetwork/sn-06af534787b315aac",
  "sharingConfig": {
    "enabled": true
  },
  "authType": "NONE"
}
```

Accepted: the emulator answered `201 Created` even with placeholder values. See [the walkthrough](#1-createservicenetwork) for a successful call.

### CreateServiceNetworkResourceAssociation

Associates the specified service network with the specified resource configuration.

```http
POST /servicenetworkresourceassociations HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 148
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "00cd4e92-d522-4dd3-a548-40ffbb399d3c",
  "resourceConfigurationIdentifier": "example0000000000000",
  "serviceNetworkIdentifier": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 334
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000000' at 'resourceConfigurationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((rcfg-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:resourceconfiguration/rcfg-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateServiceNetworkServiceAssociation

Associates the specified service with the specified service network.

```http
POST /servicenetworkserviceassociations HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 131
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "f9d41755-b730-4642-903b-6725dd6d5058",
  "serviceIdentifier": "example0000000000",
  "serviceNetworkIdentifier": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 533
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17})); Value 'example' at 'serviceNetworkIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((sn-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:servicenetwork/sn-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-createservicenetworkserviceassociation) for a successful call.

### CreateServiceNetworkVpcAssociation

Associates a VPC with a service network.

```http
POST /servicenetworkvpcassociations HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 117
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "a2a0fa95-b8f8-43ae-b5cd-6612fdd64d66",
  "serviceNetworkIdentifier": "example",
  "vpcIdentifier": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 221
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'vpcIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: vpc-(([0-9a-z]{8})|([0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateTargetGroup

Creates a target group.

```http
POST /targetgroups HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 83
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "name": "example",
  "type": "IP",
  "clientToken": "9a6f9a75-aa75-4307-8720-57aca952e1a2"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 279
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "Config fields port|protocol|vpcIdentifier must not be null for IP Target Group",
  "reason": "fieldValidationFailed",
  "fieldList": [
    {
      "name": "config",
      "message": "Config fields port|protocol|vpcIdentifier must not be null for IP Target Group"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteAccessLogSubscription

Deletes the specified access log subscription.

```http
DELETE /accesslogsubscriptions/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 329
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'accessLogSubscriptionIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((als-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:accesslogsubscription/als-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteAuthPolicy

Deletes the specified auth policy.

```http
DELETE /authpolicy/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 387
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'resourceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((((sn)|(svc)|(rcfg))-[0-9a-z]{17})|(arn(:[a-z0-9]+([.-][a-z0-9]+)*){2}(:([a-z0-9]+([.-][a-z0-9]+)*)?){2}:((servicenetwork/sn)|(resourceconfiguration/rcfg)|(service/svc))-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteDomainVerification

Deletes the specified domain verification.

```http
DELETE /domainverifications/example0000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 327
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000000' at 'domainVerificationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((dv-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:domainverification/dv-[a-fA-F0-9]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteListener

Deletes the specified listener.

```http
DELETE /services/example0000000000/listeners/example0000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 573
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17})); Value 'example0000000000000' at 'listenerIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((listener-[0-9a-z]{17})|(^arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}/listener/listener-[0-9a-z]{17}$))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteResourceConfiguration

Deletes the specified resource configuration.

```http
DELETE /resourceconfigurations/example0000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 334
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000000' at 'resourceConfigurationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((rcfg-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:resourceconfiguration/rcfg-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteResourceEndpointAssociation

Disassociates the resource configuration from the resource VPC endpoint.

```http
DELETE /resourceendpointassociations/example00000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 345
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example00000000000000' at 'resourceEndpointAssociationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((rea-[0-9a-f]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:resourceendpointassociation/rea-[0-9a-f]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteResourceGateway

Deletes the specified resource gateway.

```http
DELETE /resourcegateways/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 317
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'resourceGatewayIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((rgw-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:resourcegateway/rgw-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteResourcePolicy

Deletes the specified resource policy.

```http
DELETE /resourcepolicy/example0000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 344
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000000' at 'resourceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn(:[a-z0-9]+([.-][a-z0-9]+)*){2}(:([a-z0-9]+([.-][a-z0-9]+)*)?){2}:((servicenetwork/sn)|(service/svc)|(resourceconfiguration/rcfg))-[0-9a-z]{17}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteRule

Deletes a listener rule.

```http
DELETE /services/example0000000000/listeners/example0000000000000/rules/example0000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 859
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "3 validation errors detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17})); Value 'example0000000000000' at 'listenerIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((listener-[0-9a-z]{17})|(^arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}/listener/listener-[0-9a-z]{17}$)); Value 'example0000000000000' at 'ruleIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((rule-[0-9a-z]{17})|(^arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}/listener/listener-[0-9a-z]{17}/rule/rule-[0-9a-z]{17}$))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteService

Deletes a service.

```http
DELETE /services/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 301
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#7-deleteservice) for a successful call.

### DeleteServiceNetwork

Deletes a service network.

```http
DELETE /servicenetworks/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 303
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'serviceNetworkIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((sn-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:servicenetwork/sn-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#8-deleteservicenetwork) for a successful call.

### DeleteServiceNetworkResourceAssociation

Deletes the association between a service network and a resource configuration.

```http
DELETE /servicenetworkresourceassociations/example000000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 360
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example000000000000000' at 'serviceNetworkResourceAssociationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((snra-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:servicenetworkresourceassociation/snra-[0-9a-f]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteServiceNetworkServiceAssociation

Deletes the association between a service and a service network.

```http
DELETE /servicenetworkserviceassociations/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 353
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'serviceNetworkServiceAssociationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((snsa-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:servicenetworkserviceassociation/snsa-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#6-deleteservicenetworkserviceassociation) for a successful call.

### DeleteServiceNetworkVpcAssociation

Disassociates the VPC from the service network.

```http
DELETE /servicenetworkvpcassociations/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 345
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'serviceNetworkVpcAssociationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((snva-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:servicenetworkvpcassociation/snva-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteTargetGroup

Deletes a target group.

```http
DELETE /targetgroups/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 307
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'targetGroupIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((tg-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:targetgroup/tg-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeregisterTargets

Deregisters the specified targets from the specified target group.

```http
POST /targetgroups/example0000000000/deregistertargets HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 30
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "targets": [
    {
      "id": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 307
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'targetGroupIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((tg-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:targetgroup/tg-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetAccessLogSubscription

Retrieves information about the specified access log subscription.

```http
GET /accesslogsubscriptions/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 329
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'accessLogSubscriptionIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((als-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:accesslogsubscription/als-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetAuthPolicy

Retrieves information about the auth policy for the specified service or service network.

```http
GET /authpolicy/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 387
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'resourceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((((sn)|(svc)|(rcfg))-[0-9a-z]{17})|(arn(:[a-z0-9]+([.-][a-z0-9]+)*){2}(:([a-z0-9]+([.-][a-z0-9]+)*)?){2}:((servicenetwork/sn)|(resourceconfiguration/rcfg)|(service/svc))-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetDomainVerification

Retrieves information about a domain verification.ß

```http
GET /domainverifications/example0000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 327
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000000' at 'domainVerificationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((dv-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:domainverification/dv-[a-fA-F0-9]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetListener

Retrieves information about the specified listener for the specified service.

```http
GET /services/example0000000000/listeners/example0000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 573
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17})); Value 'example0000000000000' at 'listenerIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((listener-[0-9a-z]{17})|(^arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}/listener/listener-[0-9a-z]{17}$))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetResourceConfiguration

Retrieves information about the specified resource configuration.

```http
GET /resourceconfigurations/example0000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 334
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000000' at 'resourceConfigurationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((rcfg-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:resourceconfiguration/rcfg-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetResourceGateway

Retrieves information about the specified resource gateway.

```http
GET /resourcegateways/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 317
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'resourceGatewayIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((rgw-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:resourcegateway/rgw-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetResourcePolicy

Retrieves information about the specified resource policy.

```http
GET /resourcepolicy/example0000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 344
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000000' at 'resourceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn(:[a-z0-9]+([.-][a-z0-9]+)*){2}(:([a-z0-9]+([.-][a-z0-9]+)*)?){2}:((servicenetwork/sn)|(service/svc)|(resourceconfiguration/rcfg))-[0-9a-z]{17}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetRule

Retrieves information about the specified listener rules.

```http
GET /services/example0000000000/listeners/example0000000000000/rules/example0000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 859
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "3 validation errors detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17})); Value 'example0000000000000' at 'listenerIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((listener-[0-9a-z]{17})|(^arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}/listener/listener-[0-9a-z]{17}$)); Value 'example0000000000000' at 'ruleIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((rule-[0-9a-z]{17})|(^arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}/listener/listener-[0-9a-z]{17}/rule/rule-[0-9a-z]{17}$))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetService

Retrieves information about the specified service.

```http
GET /services/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 301
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetServiceNetwork

Retrieves information about the specified service network.

```http
GET /servicenetworks/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 303
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'serviceNetworkIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((sn-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:servicenetwork/sn-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetServiceNetworkResourceAssociation

Retrieves information about the specified association between a service network and a resource configuration.

```http
GET /servicenetworkresourceassociations/example000000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 360
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example000000000000000' at 'serviceNetworkResourceAssociationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((snra-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:servicenetworkresourceassociation/snra-[0-9a-f]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetServiceNetworkServiceAssociation

Retrieves information about the specified association between a service network and a service.

```http
GET /servicenetworkserviceassociations/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 353
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'serviceNetworkServiceAssociationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((snsa-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:servicenetworkserviceassociation/snsa-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetServiceNetworkVpcAssociation

Retrieves information about the specified association between a service network and a VPC.

```http
GET /servicenetworkvpcassociations/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 345
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'serviceNetworkVpcAssociationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((snva-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:servicenetworkvpcassociation/snva-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetTargetGroup

Retrieves information about the specified target group.

```http
GET /targetgroups/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 307
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'targetGroupIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((tg-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:targetgroup/tg-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListAccessLogSubscriptions

Lists the access log subscriptions for the specified service network or service.

```http
GET /accesslogsubscriptions?resourceIdentifier=example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 387
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'resourceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((((sn)|(svc)|(rcfg))-[0-9a-z]{17})|(arn(:[a-z0-9]+([.-][a-z0-9]+)*){2}(:([a-z0-9]+([.-][a-z0-9]+)*)?){2}:((servicenetwork/sn)|(resourceconfiguration/rcfg)|(service/svc))-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListDomainVerifications

Lists the domain verifications.

```http
GET /domainverifications HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 124
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListDomainVerifications is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListListeners

Lists the listeners for the specified service.

```http
GET /services/example0000000000/listeners HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 301
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListResourceConfigurations

Lists the resource configurations owned by or shared with this account.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
GET /resourceconfigurations HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 293
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "items": [
    {
      "id": "rcfg-09e157269bdc6458c",
      "name": "example",
      "arn": "arn:aws:vpc-lattice:us-east-1:000000000000:resourceconfiguration/rcfg-09e157269bdc6458c",
      "type": "GROUP",
      "status": "CREATE_IN_PROGRESS",
      "amazonManaged": false,
      "createdAt": 1795717852.225236,
      "lastUpdatedAt": 1795717852.225236
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListResourceEndpointAssociations

Lists the associations for the specified VPC endpoint.

```http
GET /resourceendpointassociations?resourceConfigurationIdentifier=example0000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 334
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000000' at 'resourceConfigurationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((rcfg-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:resourceconfiguration/rcfg-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListResourceGateways

Lists the resource gateways that you own or that were shared with you.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
GET /resourcegateways HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 248
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "items": [
    {
      "name": "example",
      "id": "rgw-0807a147116c8d0bc",
      "arn": "arn:aws:vpc-lattice:us-east-1:000000000000:resourcegateway/rgw-0807a147116c8d0bc",
      "status": "CREATE_IN_PROGRESS",
      "createdAt": 1795717852.2265832,
      "lastUpdatedAt": 1795717852.2265832
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListRules

Lists the rules for the specified listener.

```http
GET /services/example0000000000/listeners/example0000000000000/rules HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 573
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17})); Value 'example0000000000000' at 'listenerIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((listener-[0-9a-z]{17})|(^arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}/listener/listener-[0-9a-z]{17}$))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListServiceNetworkResourceAssociations

Lists the associations between a service network and a resource configuration.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
GET /servicenetworkresourceassociations HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 12
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "items": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListServiceNetworkServiceAssociations

Lists the associations between a service network and a service.

```http
GET /servicenetworkserviceassociations HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 325
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "either serviceNetworkIdentifier or serviceIdentifier must be provided",
  "reason": "fieldValidationFailed",
  "fieldList": [
    {
      "name": "serviceNetworkIdentifier",
      "message": "provide this or serviceIdentifier"
    },
    {
      "name": "serviceIdentifier",
      "message": "provide this or serviceNetworkIdentifier"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#4-listservicenetworkserviceassociations) for a successful call.

### ListServiceNetworkVpcAssociations

Lists the associations between a service network and a VPC.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
GET /servicenetworkvpcassociations HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 235
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "either serviceNetworkIdentifier or vpcIdentifier must be provided",
  "reason": "fieldValidationFailed",
  "fieldList": [
    {
      "name": "serviceNetworkIdentifier",
      "message": "provide this or vpcIdentifier"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListServiceNetworkVpcEndpointAssociations

Lists the associations between a service network and a VPC endpoint.

```http
GET /servicenetworkvpcendpointassociations?serviceNetworkIdentifier=example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 303
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'serviceNetworkIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((sn-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:servicenetwork/sn-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListServiceNetworks

Lists the service networks owned by or shared with this account.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /servicenetworks HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 336
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "items": [
    {
      "id": "sn-06af534787b315aac",
      "name": "example",
      "arn": "arn:aws:vpc-lattice:us-east-1:000000000000:servicenetwork/sn-06af534787b315aac",
      "createdAt": "2026-11-26T18:30:52.229488Z",
      "lastUpdatedAt": "2026-11-26T18:30:52.229488Z",
      "numberOfAssociatedVPCs": 0,
      "numberOfAssociatedServices": 0,
      "numberOfAssociatedResourceConfigurations": 0
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListServices

Lists the services owned by the caller account or shared with the caller account.

```http
GET /services HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 334
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "items": [
    {
      "id": "svc-0b60181ef23d09d25",
      "name": "example",
      "arn": "arn:aws:vpc-lattice:us-east-1:000000000000:service/svc-0b60181ef23d09d25",
      "createdAt": "2026-11-26T18:30:52.228656Z",
      "lastUpdatedAt": "2026-11-26T18:30:52.228656Z",
      "dnsEntry": {
        "domainName": "127.0.0.1",
        "hostedZoneId": "ZO1967O49886DAG26BSV"
      },
      "status": "CREATE_IN_PROGRESS"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTagsForResource

Lists the tags for the specified resource.

```http
GET /tags/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 276
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'resourceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:[a-z0-9][-.a-z0-9]{0,62}:vpc-lattice:([a-z0-9][-.a-z0-9]{0,62})?:\\d{12}?:[^/].{0,1023}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListTargetGroups

Lists your target groups.

```http
GET /targetgroups HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 117
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListTargetGroups is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListTargets

Lists the targets for the target group.

```http
POST /targetgroups/example0000000000/listtargets HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 307
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'targetGroupIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((tg-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:targetgroup/tg-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutAuthPolicy

Creates or updates the auth policy.

```http
PUT /authpolicy/example0000000000 HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 20
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "policy": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 387
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'resourceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((((sn)|(svc)|(rcfg))-[0-9a-z]{17})|(arn(:[a-z0-9]+([.-][a-z0-9]+)*){2}(:([a-z0-9]+([.-][a-z0-9]+)*)?){2}:((servicenetwork/sn)|(resourceconfiguration/rcfg)|(service/svc))-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutResourcePolicy

Attaches a resource-based permission policy to a service or service network.

```http
PUT /resourcepolicy/example0000000000000 HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 20
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "policy": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 344
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000000' at 'resourceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn(:[a-z0-9]+([.-][a-z0-9]+)*){2}(:([a-z0-9]+([.-][a-z0-9]+)*)?){2}:((servicenetwork/sn)|(service/svc)|(resourceconfiguration/rcfg))-[0-9a-z]{17}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### RegisterTargets

Registers the targets with the target group.

```http
POST /targetgroups/example0000000000/registertargets HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 30
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "targets": [
    {
      "id": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 307
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'targetGroupIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((tg-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:targetgroup/tg-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### StartDomainVerification

Starts the domain verification process for a custom domain name.

```http
POST /domainverifications HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clientToken": "3e5a001c-8cdd-4fa8-81d9-8114ff2c5d25",
  "domainName": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 124
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "NotImplemented",
  "message": "operation StartDomainVerification is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### TagResource

Adds the specified tags to the specified resource.

```http
POST /tags/example HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 26
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "tags": {
    "key": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 276
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'resourceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:[a-z0-9][-.a-z0-9]{0,62}:vpc-lattice:([a-z0-9][-.a-z0-9]{0,62})?:\\d{12}?:[^/].{0,1023}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UntagResource

Removes the specified tags from the specified resource.

```http
DELETE /tags/example?tagKeys=example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 276
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'resourceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:[a-z0-9][-.a-z0-9]{0,62}:vpc-lattice:([a-z0-9][-.a-z0-9]{0,62})?:\\d{12}?:[^/].{0,1023}"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateAccessLogSubscription

Updates the specified access log subscription.

```http
PATCH /accesslogsubscriptions/example0000000000 HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 78
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "destinationArn": "arn:aws:vpclattice:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 329
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'accessLogSubscriptionIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((als-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:accesslogsubscription/als-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateListener

Updates the specified listener for the specified service.

```http
PATCH /services/example0000000000/listeners/example0000000000000 HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 94
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "defaultAction": {
    "forward": {
      "targetGroups": [
        {
          "targetGroupIdentifier": "example0000000000"
        }
      ]
    }
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 852
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "3 validation errors detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17})); Value 'example0000000000000' at 'listenerIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((listener-[0-9a-z]{17})|(^arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}/listener/listener-[0-9a-z]{17}$)); Value 'example0000000000' at 'defaultAction.forward.targetGroups.1.member.targetGroupIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((tg-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:targetgroup/tg-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateResourceConfiguration

Updates the specified resource configuration.

```http
PATCH /resourceconfigurations/example0000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 334
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000000' at 'resourceConfigurationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((rcfg-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:resourceconfiguration/rcfg-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateResourceGateway

Updates the specified resource gateway.

```http
PATCH /resourcegateways/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 317
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'resourceGatewayIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((rgw-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:resourcegateway/rgw-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateRule

Updates a specified rule for the listener.

```http
PATCH /services/example0000000000/listeners/example0000000000000/rules/example0000000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 859
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "3 validation errors detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17})); Value 'example0000000000000' at 'listenerIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((listener-[0-9a-z]{17})|(^arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}/listener/listener-[0-9a-z]{17}$)); Value 'example0000000000000' at 'ruleIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((rule-[0-9a-z]{17})|(^arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}/listener/listener-[0-9a-z]{17}/rule/rule-[0-9a-z]{17}$))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateService

Updates the specified service.

```http
PATCH /services/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 301
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'serviceIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((svc-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:service/svc-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateServiceNetwork

Updates the specified service network.

```http
PATCH /servicenetworks/example HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 19
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "authType": "NONE"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 303
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'serviceNetworkIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((sn-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:servicenetwork/sn-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateServiceNetworkVpcAssociation

Updates the service network and VPC association.

```http
PATCH /servicenetworkvpcassociations/example0000000000 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 345
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'serviceNetworkVpcAssociationIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((snva-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:servicenetworkvpcassociation/snva-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateTargetGroup

Updates the specified target group.

```http
PATCH /targetgroups/example0000000000 HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183052Z
Content-Length: 18
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/vpc-lattice/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "healthCheck": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 307
date: Thu, 26 Nov 2026 18:30:52 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example0000000000' at 'targetGroupIdentifier' failed to satisfy constraint: Member must satisfy regular expression pattern: ((tg-[0-9a-z]{17})|(arn:[a-z0-9\\-]+:vpc-lattice:[a-zA-Z0-9\\-]+:\\d{12}:targetgroup/tg-[0-9a-z]{17}))"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/vpc-lattice/aws4_request` in the `Authorization` header) and the operation from the request itself: an HTTP method and path per operation (the `HTTP binding`), with identifiers in the path, options in the query string or headers, and the rest of the input as a JSON body.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon VPC Lattice answers in AWS: a JSON body and an HTTP status chosen per operation. Errors captured on this page are a JSON body with `__type`, `message`, plus the `x-amzn-errortype` header.
