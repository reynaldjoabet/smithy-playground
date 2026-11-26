# Amazon Managed Streaming for Apache Kafka

## Wire protocol

- **Protocol:** `restJson1`
- **SDK id:** `Kafka`
- **sigv4 signing name:** `kafka`
- **Endpoint prefix:** `kafka`
- **ARN namespace:** `kafka`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `kafka`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListClustersV2`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:kafka" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  'https://127.0.0.1:9297/api/v2/clusters'
```

## End-to-end walkthrough

Create an MSK configuration and a provisioned cluster, inspect them and delete them. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateConfiguration

Server properties are sent as base64 in the JSON body.

**Request**

```http
POST /v1/configurations HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183218Z
Content-Length: 140
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "kafkaVersions": [
    "3.9.x"
  ],
  "name": "docs-demo",
  "serverProperties": "YXV0by5jcmVhdGUudG9waWNzLmVuYWJsZT10cnVlCmxvZy5yZXRlbnRpb24uaG91cnM9MjQK"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 267
date: Thu, 26 Nov 2026 18:32:18 GMT

{
  "arn": "arn:aws:kafka:us-east-1:000000000000:configuration/docs-demo/981502e4-1d54-46e4-8769-25c093886117-1",
  "creationTime": "2026-11-26T18:32:18.934109Z",
  "latestRevision": {
    "creationTime": "2026-11-26T18:32:18.934109Z",
    "revision": 1
  },
  "name": "docs-demo",
  "state": "ACTIVE"
}
```

### 2. CreateCluster

The broker count must be a multiple of the number of subnets.

**Request**

```http
POST /v1/clusters HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183218Z
Content-Length: 345
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "brokerNodeGroupInfo": {
    "clientSubnets": [
      "subnet-00000000000000007",
      "subnet-00000000000000009"
    ],
    "instanceType": "kafka.t3.small"
  },
  "clusterName": "docs-demo",
  "configurationInfo": {
    "arn": "arn:aws:kafka:us-east-1:000000000000:configuration/docs-demo/981502e4-1d54-46e4-8769-25c093886117-1",
    "revision": 1
  },
  "kafkaVersion": "3.9.x",
  "numberOfBrokerNodes": 2
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 155
date: Thu, 26 Nov 2026 18:32:18 GMT

{
  "clusterArn": "arn:aws:kafka:us-east-1:000000000000:cluster/docs-demo/811b4fb5-6c23-45b1-af78-82334e27ae03-1",
  "clusterName": "docs-demo",
  "state": "CREATING"
}
```

### 3. DescribeCluster

Poll until `ACTIVE`.

*The emulator was asked 2 times, about two seconds apart; the last exchange is shown.*

**Request**

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Acluster%2Fdocs-demo%2F811b4fb5-6c23-45b1-af78-82334e27ae03-1 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183218Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 1474
date: Thu, 26 Nov 2026 18:32:18 GMT

{
  "clusterInfo": {
    "brokerNodeGroupInfo": {
      "clientSubnets": [
        "subnet-00000000000000007",
        "subnet-00000000000000009"
      ],
      "instanceType": "kafka.t3.small",
      "securityGroups": [
        "sg-00000000000000013"
      ]
    },
    "clusterArn": "arn:aws:kafka:us-east-1:000000000000:cluster/docs-demo/811b4fb5-6c23-45b1-af78-82334e27ae03-1",
    "clusterName": "docs-demo",
    "creationTime": "2026-11-26T18:32:18.939718Z",
    "currentBrokerSoftwareInfo": {
      "configurationArn": "arn:aws:kafka:us-east-1:000000000000:configuration/docs-demo/981502e4-1d54-46e4-8769-25c093886117-1",
      "configurationRevision": 1,
      "kafkaVersion": "3.9.x"
    },
    "currentVersion": "KXKVZQKAT51QKE",
    "encryptionInfo": {
      "encryptionAtRest": {
        "dataVolumeKMSKeyId": "arn:aws:kms:us-east-1:000000000000:key/b4887068-c73a-4b59-bce3-dc3360ced13a"
      },
      "encryptionInTransit": {
        "clientBroker": "TLS_PLAINTEXT",
        "inCluster": true
      }
    },
    "enhancedMonitoring": "DEFAULT",
    "openMonitoring": {
      "prometheus": {
        "jmxExporter": {
          "enabledInBroker": false
        },
        "nodeExporter": {
          "enabledInBroker": false
        }
      }
    },
    "numberOfBrokerNodes": 2,
    "state": "CREATING",
    "tags": {},
    "zookeeperConnectString": "z-1.docs-demo.77b02e.c4.kafka.us-east-1.amazonaws.com:2181,z-2.docs-demo.77b02e.c4.kafka.us-east-1.amazonaws.com:2181,z-3.docs-demo.77b02e.c4.kafka.us-east-1.amazonaws.com:2181",
    "zookeeperConnectStringTls": "z-1.docs-demo.77b02e.c4.kafka.us-east-1.amazonaws.com:2182,z-2.docs-demo.77b02e.c4.kafka.us-east-1.amazonaws.com:2182,z-3.docs-demo.77b02e.c4.kafka.us-east-1.amazonaws.com:2182",
    "storageMode": "LOCAL",
    "customerActionStatus": "NONE"
  }
}
```

### 4. GetBootstrapBrokers

The broker addresses a Kafka client connects to, in MSK's hostname format.

**Request**

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Acluster%2Fdocs-demo%2F811b4fb5-6c23-45b1-af78-82334e27ae03-1/bootstrap-brokers HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 120
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "bootstrapBrokerString": "127.0.0.1:44349,127.0.0.1:44349",
  "bootstrapBrokerStringTls": "127.0.0.1:36041,127.0.0.1:36041"
}
```

### 5. DeleteCluster

Deletion is asynchronous.

**Request**

```http
DELETE /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Acluster%2Fdocs-demo%2F811b4fb5-6c23-45b1-af78-82334e27ae03-1 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 129
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "clusterArn": "arn:aws:kafka:us-east-1:000000000000:cluster/docs-demo/811b4fb5-6c23-45b1-af78-82334e27ae03-1",
  "state": "DELETING"
}
```

### 6. DescribeCluster

Poll until it is gone.

**Request**

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Acluster%2Fdocs-demo%2F811b4fb5-6c23-45b1-af78-82334e27ae03-1 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 114
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "NotFoundException",
  "invalidParameter": "clusterArn",
  "message": "The requested resource doesn’t exist."
}
```

### 7. DeleteConfiguration

A configuration no cluster uses can be deleted.

**Request**

```http
DELETE /v1/configurations/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aconfiguration%2Fdocs-demo%2F981502e4-1d54-46e4-8769-25c093886117-1 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 128
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "arn": "arn:aws:kafka:us-east-1:000000000000:configuration/docs-demo/981502e4-1d54-46e4-8769-25c093886117-1",
  "state": "DELETING"
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 64 operations, 7 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### BatchAssociateScramSecret

Associates one or more Scram Secrets with an Amazon MSK cluster.

```http
POST /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/scram-secrets HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 29
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "secretArnList": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 161
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "The specified cluster ARN isn’t valid. Specify a valid cluster ARN, and try again."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### BatchDisassociateScramSecret

Disassociates one or more Scram Secrets from an Amazon MSK cluster.

```http
PATCH /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/scram-secrets HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 29
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "secretArnList": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 161
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "The specified cluster ARN isn’t valid. Specify a valid cluster ARN, and try again."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### CreateChannel

Creates a Channel that streams records from an Amazon MSK Express cluster topic to Amazon S3 or Apache Iceberg.

```http
POST /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/channels HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 170
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "channelName": "example",
  "topicConfigurationList": [
    {
      "recordConverter": {
        "valueConverter": "BYTE_ARRAY"
      },
      "topicArn": "arn:aws:kafka:us-east-1:000000000000:example/example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### CreateCluster

Creates a new MSK cluster.

```http
POST /v1/clusters HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 151
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "brokerNodeGroupInfo": {
    "clientSubnets": [
      "example"
    ],
    "instanceType": "example"
  },
  "clusterName": "example",
  "kafkaVersion": "example",
  "numberOfBrokerNodes": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 228
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "kafkaVersion",
  "message": "Standard instance types are not supported for Kafka version example. Valid values: [3.8.x, 3.8.x.kraft, 3.9.x, 3.9.x.kraft, 4.0.x.kraft, 4.1.x.kraft]"
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-createcluster) for a successful call.

### CreateClusterV2

Creates a new MSK cluster.

```http
POST /api/v2/clusters HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 25
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "clusterName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 144
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterType",
  "message": "You must specify a cluster type, either provisioned or serverless."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### CreateConfiguration

Creates a new MSK configuration.

```http
POST /v1/configurations HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "name": "example",
  "serverProperties": "ZXhhbXBsZQ=="
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 565
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "serverProperties",
  "message": "[ {\n  \"name\" : \"example\",\n  \"value\" : \"\",\n  \"errorMessages\" : [ \"Key 'example' is not supported by at least one Apache Kafka version. Key checked against versions: [1.1.1, 2.1.0, 2.2.1, 2.3.1, 2.4.1, 2.4.1.1, 2.5.1, 2.6.0, 2.6.1, 2.6.2, 2.6.3, 2.7.0, 2.7.1, 2.7.2, 2.8.0, 2.8.1, 2.8.2.tiered, 3.1.1, 3.2.0, 3.3.1, 3.3.2, 3.4.0, 3.5.1, 3.6.0, 3.6.0.1, 3.7.x, 3.7.x.kraft, 3.8.x, 3.8.x.kraft, 3.8.link, 3.9.x, 3.9.x.kraft, 4.0.x.kraft, 4.1.x.kraft, 4.2.x.kraft]\" ]\n} ]"
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-createconfiguration) for a successful call.

### CreateReplicator

Creates the replicator.

```http
POST /replication/v1/replicators HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 378
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "kafkaClusters": [
    {}
  ],
  "replicationInfoList": [
    {
      "consumerGroupReplication": {
        "consumerGroupsToReplicate": [
          "example"
        ]
      },
      "targetCompressionType": "NONE",
      "topicReplication": {
        "topicsToReplicate": [
          "example"
        ]
      }
    }
  ],
  "replicatorName": "example",
… (1 more lines)
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 127
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "ValidationException",
  "message": "json parse error: Error at offset 360: unexpected token 'â'. Expected one of '\"'"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateTopic

Creates a topic in the specified MSK cluster.

```http
POST /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/topics HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 64
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "topicName": "example",
  "partitionCount": 1,
  "replicationFactor": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### CreateVpcConnection

Creates a new MSK VPC connection.

```http
POST /v1/vpc-connection HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 177
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "targetClusterArn": "arn:aws:kafka:us-east-1:000000000000:example/example",
  "authentication": "example",
  "vpcId": "example",
  "clientSubnets": [
    "example"
  ],
  "securityGroups": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 127
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "targetClusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DeleteChannel

Deletes the channel specified by channelArn from the cluster specified by clusterArn.

```http
DELETE /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/channels/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DeleteCluster

Deletes the MSK cluster specified by the Amazon Resource Name (ARN) in the request.

```http
DELETE /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#5-deletecluster) for a successful call.

### DeleteClusterPolicy

Deletes the MSK cluster policy specified by the Amazon Resource Name (ARN) in the request.

```http
DELETE /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/policy HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DeleteConfiguration

Deletes an MSK Configuration.

```http
DELETE /v1/configurations/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 103
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "arn",
  "message": "Configuration ARN does not exist."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#7-deleteconfiguration) for a successful call.

### DeleteReplicator

Deletes a replicator.

```http
DELETE /replication/v1/replicators/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 112
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "replicatorArn",
  "message": "The provided value is not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DeleteTopic

Deletes a topic in the specified MSK cluster.

```http
DELETE /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/topics/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DeleteVpcConnection

Deletes a MSK VPC connection.

```http
DELETE /v1/vpc-connection/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 127
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "vpcConnectionArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DescribeChannel

Returns the current configuration and state of a channel.

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/channels/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DescribeCluster

Returns a description of the MSK cluster whose Amazon Resource Name (ARN) is specified in the request.

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 114
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "NotFoundException",
  "invalidParameter": "clusterArn",
  "message": "The requested resource doesn’t exist."
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#3-describecluster) for a successful call.

### DescribeClusterOperation

Returns a description of the cluster operation specified by the ARN.

```http
GET /v1/operations/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 130
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterOperationArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DescribeClusterOperationV2

Returns a description of the cluster operation specified by the ARN.

```http
GET /api/v2/operations/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 130
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterOperationArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DescribeClusterV2

Returns a description of the MSK cluster whose Amazon Resource Name (ARN) is specified in the request.

```http
GET /api/v2/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 114
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "NotFoundException",
  "invalidParameter": "clusterArn",
  "message": "The requested resource doesn’t exist."
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeConfiguration

Returns a description of this MSK configuration.

```http
GET /v1/configurations/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 103
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "arn",
  "message": "Configuration ARN does not exist."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DescribeConfigurationRevision

Returns a description of this revision of the configuration.

```http
GET /v1/configurations/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/revisions/1 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 103
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "arn",
  "message": "Configuration ARN does not exist."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DescribeReplicator

Describes a replicator.

```http
GET /replication/v1/replicators/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 112
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "replicatorArn",
  "message": "The provided value is not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DescribeTopic

Returns topic details of this topic on a MSK cluster.

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/topics/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DescribeTopicPartitions

Returns partition details of this topic on a MSK cluster.

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/topics/example/partitions HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DescribeVpcConnection

Returns a description of this MSK VPC connection.

```http
GET /v1/vpc-connection/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 127
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "vpcConnectionArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### GetBootstrapBrokers

A list of brokers that a client application can use to bootstrap.

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/bootstrap-brokers HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 114
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "NotFoundException",
  "invalidParameter": "clusterArn",
  "message": "The requested resource doesn’t exist."
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#4-getbootstrapbrokers) for a successful call.

### GetClusterPolicy

Get the MSK cluster policy specified by the Amazon Resource Name (ARN) in the request.

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/policy HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### GetCompatibleKafkaVersions

Gets the Apache Kafka versions to which you can update the MSK cluster.

```http
GET /v1/compatible-kafka-versions HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 2799
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "compatibleKafkaVersions": [
    {
      "sourceVersion": "1.1.1",
      "targetVersions": [
        "3.6.0",
        "3.7.x",
        "3.8.x",
        "3.9.x"
      ]
    },
    {
      "sourceVersion": "2.1.0",
      "targetVersions": [
        "3.6.0",
        "3.7.x",
        "3.8.x",
        "3.9.x"
      ]
    },
… (284 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListChannels

Returns the list of channels in a cluster.

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/channels HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### ListClientVpcConnections

Returns a list of all the VPC connections in this Region.

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/client-vpc-connections HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 161
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "The specified cluster ARN isn’t valid. Specify a valid cluster ARN, and try again."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### ListClusterOperations

Returns a list of all the operations that have been performed on the specified MSK cluster.

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/operations HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### ListClusterOperationsV2

Returns a list of all the operations that have been performed on the specified MSK cluster.

```http
GET /api/v2/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/operations HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### ListClusters

Returns a list of all the MSK clusters in the current Region.

```http
GET /v1/clusters HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 22
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "clusterInfoList": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListClustersV2

Returns a list of all the MSK clusters in the current Region.

```http
GET /api/v2/clusters HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 22
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "clusterInfoList": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListConfigurationRevisions

Returns a list of all the MSK configurations in this Region.

```http
GET /v1/configurations/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/revisions HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 103
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "arn",
  "message": "Configuration ARN does not exist."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### ListConfigurations

Returns a list of all the MSK configurations in this Region.

```http
GET /v1/configurations HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 333
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "configurations": [
    {
      "arn": "arn:aws:kafka:us-east-1:000000000000:configuration/docs-demo/981502e4-1d54-46e4-8769-25c093886117-1",
      "creationTime": "2026-11-26T18:32:18.934109Z",
      "description": "",
      "kafkaVersions": [
        "3.9.x"
      ],
      "latestRevision": {
        "creationTime": "2026-11-26T18:32:18.934109Z",
        "revision": 1
      },
      "name": "docs-demo",
      "state": "DELETING"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListKafkaVersions

Returns a list of Apache Kafka versions.

```http
GET /v1/kafka-versions HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 1589
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "kafkaVersions": [
    {
      "version": "1.1.1",
      "status": "DEPRECATED"
    },
    {
      "version": "2.1.0",
      "status": "DEPRECATED"
    },
    {
      "version": "2.2.1",
      "status": "DEPRECATED"
    },
    {
      "version": "2.3.1",
      "status": "DEPRECATED"
    },
    {
      "version": "2.4.1",
… (132 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListNodes

Returns a list of the broker nodes in the cluster.

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/nodes HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### ListReplicators

Lists the replicators.

```http
GET /replication/v1/replicators HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 18
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "replicators": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListScramSecrets

Returns a list of the Scram Secrets associated with an Amazon MSK cluster.

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/scram-secrets HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### ListTagsForResource

Returns a list of the tags associated with the specified resource.

```http
GET /v1/tags/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 71
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "message": "Invalid input resource arn"
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### ListTopics

List topics in a MSK cluster.

```http
GET /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/topics HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### ListVpcConnections

Returns a list of all the VPC connections in this Region.

```http
GET /v1/vpc-connections HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 21
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "vpcConnections": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### PutClusterPolicy

Creates or updates the MSK cluster policy specified by the cluster Amazon Resource Name (ARN) in the request.

```http
PUT /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/policy HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 20
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "policy": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### RebootBroker

Reboots brokers.

```http
PUT /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/reboot-broker HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 25
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "brokerIds": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### RejectClientVpcConnection

Returns empty response.

```http
PUT /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/client-vpc-connection HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "vpcConnectionArn": "arn:aws:kafka:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 161
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "The specified cluster ARN isn’t valid. Specify a valid cluster ARN, and try again."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### TagResource

Adds tags to the specified MSK resource.

```http
POST /v1/tags/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 26
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "tags": {
    "key": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 71
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "message": "Invalid input resource arn"
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UntagResource

Removes the tags associated with the keys that are provided in the query.

```http
DELETE /v1/tags/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample?tagKeys=example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 71
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "message": "Invalid input resource arn"
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateBrokerCount

Updates the number of broker nodes in the cluster.

```http
PUT /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/nodes/count HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 58
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "currentVersion": "example",
  "targetNumberOfBrokerNodes": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateBrokerStorage

Updates the EBS storage associated with MSK brokers.

```http
PUT /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/nodes/storage HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 90
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "currentVersion": "example",
  "targetBrokerEBSVolumeInfo": [
    {
      "kafkaBrokerNodeId": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateBrokerType

Updates EC2 instance type.

```http
PUT /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/nodes/type HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "currentVersion": "example",
  "targetInstanceType": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateChannel

Updates the destination configuration of an existing channel.

```http
PUT /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/channels/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateClusterConfiguration

Updates the cluster with the configuration that is specified in the request body.

```http
PUT /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/configuration HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 124
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "configurationInfo": {
    "arn": "arn:aws:kafka:us-east-1:000000000000:example/example",
    "revision": 1
  },
  "currentVersion": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateClusterKafkaVersion

Updates the Apache Kafka version for the cluster.

```http
PUT /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/version HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "currentVersion": "example",
  "targetKafkaVersion": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateConfiguration

Updates an MSK configuration.

```http
PUT /v1/configurations/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 35
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "serverProperties": "ZXhhbXBsZQ=="
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 103
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "arn",
  "message": "Configuration ARN does not exist."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateConnectivity

Updates the cluster's connectivity configuration.

```http
PUT /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/connectivity HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "currentVersion": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateMonitoring

Updates the monitoring settings for the cluster.

```http
PUT /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/monitoring HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "currentVersion": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateRebalancing

Use this resource to update the intelligent rebalancing status of an Amazon MSK Provisioned cluster with Express brokers.

```http
PUT /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/rebalancing HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "currentVersion": "example",
  "rebalancing": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateReplicationInfo

Updates replication info of a replicator.

```http
PUT /replication/v1/replicators/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/replication-info HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "currentVersion": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 112
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "replicatorArn",
  "message": "The provided value is not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateSecurity

Updates the security settings for the cluster.

```http
PATCH /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/security HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "currentVersion": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateStorage

Updates cluster broker volume size (or) sets cluster storage mode to TIERED.

```http
PUT /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/storage HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "currentVersion": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateTopic

Updates the configuration of the specified topic.

```http
PUT /v1/clusters/arn%3Aaws%3Akafka%3Aus-east-1%3A000000000000%3Aexample%2Fexample/topics/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183219Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/kafka/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 121
date: Thu, 26 Nov 2026 18:32:19 GMT

{
  "__type": "BadRequestException",
  "invalidParameter": "clusterArn",
  "message": "One or more of the parameters are not valid."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/kafka/aws4_request` in the `Authorization` header) and the operation from the request itself: an HTTP method and path per operation (the `HTTP binding`), with identifiers in the path, options in the query string or headers, and the rest of the input as a JSON body.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon Managed Streaming for Apache Kafka answers in AWS: a JSON body and an HTTP status chosen per operation. Errors captured on this page are a JSON body with `__type`, `message`, plus the `x-amzn-errortype` header.
