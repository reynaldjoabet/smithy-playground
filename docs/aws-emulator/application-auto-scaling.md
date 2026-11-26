# Application Auto Scaling

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: AnyScaleFrontendService.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `Application Auto Scaling`
- **sigv4 signing name:** `application-autoscaling`
- **Endpoint prefix:** `application-autoscaling`
- **ARN namespace:** `application-autoscaling`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `application-autoscaling`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `DescribeScalableTargets`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:application-autoscaling" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.1' \
  -H 'X-Amz-Target: AnyScaleFrontendService.DescribeScalableTargets' \
  --data-binary '{"ServiceNamespace": "dynamodb"}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Let Application Auto Scaling scale a DynamoDB table's read capacity with target tracking. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. Amazon DynamoDB: CreateTable

A provisioned-capacity DynamoDB table to scale.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.CreateTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 253
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AttributeDefinitions": [
    {
      "AttributeName": "id",
      "AttributeType": "S"
    }
  ],
  "TableName": "docs-demo-scaled",
  "KeySchema": [
    {
      "AttributeName": "id",
      "KeyType": "HASH"
    }
  ],
  "BillingMode": "PROVISIONED",
  "ProvisionedThroughput": {
    "ReadCapacityUnits": 5,
    "WriteCapacityUnits": 5
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 59OHC6B4EMONYQFS5722JAFIG1IV0A7QGRGCU4XKN9B4FSD7HKIV
x-amz-crc32: 2525605252
content-length: 675
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "TableDescription": {
    "AttributeDefinitions": [
      {
        "AttributeName": "id",
        "AttributeType": "S"
      }
    ],
    "TableName": "docs-demo-scaled",
    "KeySchema": [
      {
        "AttributeName": "id",
        "KeyType": "HASH"
      }
    ],
    "TableStatus": "CREATING",
    "CreationDateTime": 1795717646.268,
    "ProvisionedThroughput": {
      "NumberOfDecreasesToday": 0,
      "ReadCapacityUnits": 5,
      "WriteCapacityUnits": 5
    },
    "TableSizeBytes": 0,
    "ItemCount": 0,
    "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:table/docs-demo-scaled",
    "TableId": "62b8f726-8be2-4c40-9980-7ac42bbaafb8",
    "DeletionProtectionEnabled": false,
    "TableThroughputModeSummary": {
      "TableThroughputMode": "PROVISIONED"
    },
    "WarmThroughput": {
      "ReadUnitsPerSecond": 5,
      "WriteUnitsPerSecond": 5,
      "Status": "ACTIVE"
    }
  }
}
```

### 2. Amazon DynamoDB: DescribeTable

Wait until the table is `ACTIVE`; a table still being created can't be registered or deleted.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 32
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "docs-demo-scaled"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 9OKAKVQXAM2V5L2YJ236Y5QC19EQ8ZS2LFNR66TYVETGD0MBKFX4
x-amz-crc32: 3738453875
content-length: 662
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "Table": {
    "AttributeDefinitions": [
      {
        "AttributeName": "id",
        "AttributeType": "S"
      }
    ],
    "TableName": "docs-demo-scaled",
    "KeySchema": [
      {
        "AttributeName": "id",
        "KeyType": "HASH"
      }
    ],
    "TableStatus": "ACTIVE",
    "CreationDateTime": 1795717646.268,
    "ProvisionedThroughput": {
      "NumberOfDecreasesToday": 0,
      "ReadCapacityUnits": 5,
      "WriteCapacityUnits": 5
    },
    "TableSizeBytes": 0,
    "ItemCount": 0,
    "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:table/docs-demo-scaled",
    "TableId": "62b8f726-8be2-4c40-9980-7ac42bbaafb8",
    "DeletionProtectionEnabled": false,
    "TableThroughputModeSummary": {
      "TableThroughputMode": "PROVISIONED"
    },
    "WarmThroughput": {
      "ReadUnitsPerSecond": 5,
      "WriteUnitsPerSecond": 5,
      "Status": "ACTIVE"
    }
  }
}
```

### 3. RegisterScalableTarget

A scalable target is identified by service namespace, resource id and dimension.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AnyScaleFrontendService.RegisterScalableTarget
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 157
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ServiceNamespace": "dynamodb",
  "ResourceId": "table/docs-demo-scaled",
  "ScalableDimension": "dynamodb:table:ReadCapacityUnits",
  "MinCapacity": 5,
  "MaxCapacity": 50
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 131
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "ScalableTargetARN": "arn:aws:application-autoscaling:us-east-1:000000000000:scalable-target/2cc3db9bdbd3a7cd49c4971fab2e610732a4"
}
```

### 4. PutScalingPolicy

Target tracking keeps utilization near 70%; the response names the CloudWatch alarms it created.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AnyScaleFrontendService.PutScalingPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 347
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "PolicyName": "docs-demo-reads",
  "ServiceNamespace": "dynamodb",
  "ResourceId": "table/docs-demo-scaled",
  "ScalableDimension": "dynamodb:table:ReadCapacityUnits",
  "PolicyType": "TargetTrackingScaling",
  "TargetTrackingScalingPolicyConfiguration": {
    "TargetValue": 70.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "DynamoDBReadCapacityUtilization"
    }
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 1228
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "PolicyARN": "arn:aws:autoscaling:us-east-1:000000000000:scalingPolicy:fb01e57c-a9bc-a3b2-2afb-f5125641478e:resource/dynamodb/table/docs-demo-scaled:policyName/docs-demo-reads",
  "Alarms": [
    {
      "AlarmName": "TargetTracking-table/docs-demo-scaled-AlarmHigh-9cf66fc2-2371-2720-6a68-c15a6f1df1d3",
      "AlarmARN": "arn:aws:cloudwatch:us-east-1:000000000000:alarm:TargetTracking-table/docs-demo-scaled-AlarmHigh-9cf66fc2-2371-2720-6a68-c15a6f1df1d3"
    },
    {
      "AlarmName": "TargetTracking-table/docs-demo-scaled-AlarmLow-2392f8b4-92f1-2c31-f722-5150fbc41581",
      "AlarmARN": "arn:aws:cloudwatch:us-east-1:000000000000:alarm:TargetTracking-table/docs-demo-scaled-AlarmLow-2392f8b4-92f1-2c31-f722-5150fbc41581"
    },
    {
      "AlarmName": "TargetTracking-table/docs-demo-scaled-ProvisionedCapacityHigh-331969b8-4b6f-72c2-02f2-8b684d342086",
      "AlarmARN": "arn:aws:cloudwatch:us-east-1:000000000000:alarm:TargetTracking-table/docs-demo-scaled-ProvisionedCapacityHigh-331969b8-4b6f-72c2-02f2-8b684d342086"
    },
    {
      "AlarmName": "TargetTracking-table/docs-demo-scaled-ProvisionedCapacityLow-876e0ffe-987a-f8c4-56b3-5aeb1abd6c87",
      "AlarmARN": "arn:aws:cloudwatch:us-east-1:000000000000:alarm:TargetTracking-table/docs-demo-scaled-ProvisionedCapacityLow-876e0ffe-987a-f8c4-56b3-5aeb1abd6c87"
    }
  ]
}
```

### 5. DescribeScalingPolicies

The policy and its alarms.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AnyScaleFrontendService.DescribeScalingPolicies
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 69
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ServiceNamespace": "dynamodb",
  "ResourceId": "table/docs-demo-scaled"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 1632
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "ScalingPolicies": [
    {
      "PolicyARN": "arn:aws:autoscaling:us-east-1:000000000000:scalingPolicy:fb01e57c-a9bc-a3b2-2afb-f5125641478e:resource/dynamodb/table/docs-demo-scaled:policyName/docs-demo-reads",
      "PolicyName": "docs-demo-reads",
      "ServiceNamespace": "dynamodb",
      "ResourceId": "table/docs-demo-scaled",
      "ScalableDimension": "dynamodb:table:ReadCapacityUnits",
      "PolicyType": "TargetTrackingScaling",
      "TargetTrackingScalingPolicyConfiguration": {
        "TargetValue": 70.0,
        "PredefinedMetricSpecification": {
          "PredefinedMetricType": "DynamoDBReadCapacityUtilization"
        }
      },
      "Alarms": [
        {
          "AlarmName": "TargetTracking-table/docs-demo-scaled-AlarmHigh-9cf66fc2-2371-2720-6a68-c15a6f1df1d3",
          "AlarmARN": "arn:aws:cloudwatch:us-east-1:000000000000:alarm:TargetTracking-table/docs-demo-scaled-AlarmHigh-9cf66fc2-2371-2720-6a68-c15a6f1df1d3"
        },
        {
          "AlarmName": "TargetTracking-table/docs-demo-scaled-AlarmLow-2392f8b4-92f1-2c31-f722-5150fbc41581",
          "AlarmARN": "arn:aws:cloudwatch:us-east-1:000000000000:alarm:TargetTracking-table/docs-demo-scaled-AlarmLow-2392f8b4-92f1-2c31-f722-5150fbc41581"
        },
        {
          "AlarmName": "TargetTracking-table/docs-demo-scaled-ProvisionedCapacityHigh-331969b8-4b6f-72c2-02f2-8b684d342086",
          "AlarmARN": "arn:aws:cloudwatch:us-east-1:000000000000:alarm:TargetTracking-table/docs-demo-scaled-ProvisionedCapacityHigh-331969b8-4b6f-72c2-02f2-8b684d342086"
        },
        {
          "AlarmName": "TargetTracking-table/docs-demo-scaled-ProvisionedCapacityLow-876e0ffe-987a-f8c4-56b3-5aeb1abd6c87",
          "AlarmARN": "arn:aws:cloudwatch:us-east-1:000000000000:alarm:TargetTracking-table/docs-demo-scaled-ProvisionedCapacityLow-876e0ffe-987a-f8c4-56b3-5aeb1abd6c87"
        }
      ],
      "CreationTime": 1795717649.2855077
    }
  ]
}
```

### 6. DeleteScalingPolicy

Remove the policy ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AnyScaleFrontendService.DeleteScalingPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 155
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "PolicyName": "docs-demo-reads",
  "ServiceNamespace": "dynamodb",
  "ResourceId": "table/docs-demo-scaled",
  "ScalableDimension": "dynamodb:table:ReadCapacityUnits"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 2
date: Thu, 26 Nov 2026 18:27:29 GMT

{}
```

### 7. DeregisterScalableTarget

... and the target.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AnyScaleFrontendService.DeregisterScalableTarget
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 124
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ServiceNamespace": "dynamodb",
  "ResourceId": "table/docs-demo-scaled",
  "ScalableDimension": "dynamodb:table:ReadCapacityUnits"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 2
date: Thu, 26 Nov 2026 18:27:29 GMT

{}
```

### 8. DeregisterScalableTarget

Deregistering again answers `ObjectNotFoundException`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AnyScaleFrontendService.DeregisterScalableTarget
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 124
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ServiceNamespace": "dynamodb",
  "ResourceId": "table/docs-demo-scaled",
  "ScalableDimension": "dynamodb:table:ReadCapacityUnits"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 196
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "__type": "ObjectNotFoundException",
  "Message": "No scalable target found for service namespace: dynamodb, resource ID: table/docs-demo-scaled, scalable dimension: dynamodb:table:ReadCapacityUnits"
}
```

### 9. Amazon DynamoDB: DescribeTable

Deregistering may still be updating the table; wait for `ACTIVE` before deleting it.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 32
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "docs-demo-scaled"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: GKSJAA7LWUFJ6WRK0QHG9QM7O58H1NX3ZSE1DD3NL4Q2NMZS1RPQ
x-amz-crc32: 3738453875
content-length: 662
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "Table": {
    "AttributeDefinitions": [
      {
        "AttributeName": "id",
        "AttributeType": "S"
      }
    ],
    "TableName": "docs-demo-scaled",
    "KeySchema": [
      {
        "AttributeName": "id",
        "KeyType": "HASH"
      }
    ],
    "TableStatus": "ACTIVE",
    "CreationDateTime": 1795717646.268,
    "ProvisionedThroughput": {
      "NumberOfDecreasesToday": 0,
      "ReadCapacityUnits": 5,
      "WriteCapacityUnits": 5
    },
    "TableSizeBytes": 0,
    "ItemCount": 0,
    "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:table/docs-demo-scaled",
    "TableId": "62b8f726-8be2-4c40-9980-7ac42bbaafb8",
    "DeletionProtectionEnabled": false,
    "TableThroughputModeSummary": {
      "TableThroughputMode": "PROVISIONED"
    },
    "WarmThroughput": {
      "ReadUnitsPerSecond": 5,
      "WriteUnitsPerSecond": 5,
      "Status": "ACTIVE"
    }
  }
}
```

### 10. Amazon DynamoDB: DeleteTable

Clean up the table.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DeleteTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 32
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "docs-demo-scaled"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: BC9TNOKTR6TY53HGSN9PBF933H1Y800CF9LYQPWOWAPVDVRYBGRV
x-amz-crc32: 3308961359
content-length: 675
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "TableDescription": {
    "AttributeDefinitions": [
      {
        "AttributeName": "id",
        "AttributeType": "S"
      }
    ],
    "TableName": "docs-demo-scaled",
    "KeySchema": [
      {
        "AttributeName": "id",
        "KeyType": "HASH"
      }
    ],
    "TableStatus": "DELETING",
    "CreationDateTime": 1795717646.268,
    "ProvisionedThroughput": {
      "NumberOfDecreasesToday": 0,
      "ReadCapacityUnits": 5,
      "WriteCapacityUnits": 5
    },
    "TableSizeBytes": 0,
    "ItemCount": 0,
    "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:table/docs-demo-scaled",
    "TableId": "62b8f726-8be2-4c40-9980-7ac42bbaafb8",
    "DeletionProtectionEnabled": false,
    "TableThroughputModeSummary": {
      "TableThroughputMode": "PROVISIONED"
    },
    "WarmThroughput": {
      "ReadUnitsPerSecond": 5,
      "WriteUnitsPerSecond": 5,
      "Status": "ACTIVE"
    }
  }
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 14 operations, 4 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### DeleteScalingPolicy

Deletes the specified scaling policy for an Application Auto Scaling scalable target.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleFrontendService.DeleteScalingPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 119
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "PolicyName": "example",
  "ServiceNamespace": "ecs",
  "ResourceId": "example",
  "ScalableDimension": "ecs:service:DesiredCount"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 189
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "__type": "ObjectNotFoundException",
  "Message": "No scaling policy found for service namespace: ecs, resource ID: example, scalable dimension: ecs:service:DesiredCount, policy name: example"
}
```

`400` `ObjectNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#6-deletescalingpolicy) for a successful call.

### DeleteScheduledAction

Deletes the specified scheduled action for an Application Auto Scaling scalable target.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleFrontendService.DeleteScheduledAction
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 128
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ServiceNamespace": "ecs",
  "ScheduledActionName": "example",
  "ResourceId": "example",
  "ScalableDimension": "ecs:service:DesiredCount"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 173
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "__type": "ObjectNotFoundException",
  "Message": "No scalable target registered for service namespace: ecs, resource ID: example, scalable dimension: ecs:service:DesiredCount"
}
```

`400` `ObjectNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeregisterScalableTarget

Deregisters an Application Auto Scaling scalable target when you have finished using it.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleFrontendService.DeregisterScalableTarget
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 96
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ServiceNamespace": "ecs",
  "ResourceId": "example",
  "ScalableDimension": "ecs:service:DesiredCount"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 168
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "__type": "ObjectNotFoundException",
  "Message": "No scalable target found for service namespace: ecs, resource ID: example, scalable dimension: ecs:service:DesiredCount"
}
```

`400` `ObjectNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#7-deregisterscalabletarget) for a successful call.

### DescribeScalableTargets

Gets information about the scalable targets in the specified namespace.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleFrontendService.DescribeScalableTargets
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 26
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ServiceNamespace": "ecs"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 22
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "ScalableTargets": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeScalingActivities

Provides descriptive information about the scaling activities in the specified namespace from the previous six weeks.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleFrontendService.DescribeScalingActivities
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 26
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ServiceNamespace": "ecs"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 24
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "ScalingActivities": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeScalingPolicies

Describes the Application Auto Scaling scaling policies for the specified service namespace.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleFrontendService.DescribeScalingPolicies
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 26
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ServiceNamespace": "ecs"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 22
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "ScalingPolicies": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#5-describescalingpolicies) for a successful call.

### DescribeScheduledActions

Describes the Application Auto Scaling scheduled actions for the specified service namespace.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleFrontendService.DescribeScheduledActions
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 26
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ServiceNamespace": "ecs"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 23
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "ScheduledActions": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetPredictiveScalingForecast

Retrieves the forecast data for a predictive scaling policy.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleFrontendService.GetPredictiveScalingForecast
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 187
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ServiceNamespace": "ecs",
  "ResourceId": "example",
  "ScalableDimension": "ecs:service:DesiredCount",
  "PolicyName": "example",
  "StartTime": "2026-11-16T12:11:23Z",
  "EndTime": "2026-11-16T12:11:23Z"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 83
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "__type": "ValidationException",
  "message": "invalid timestamp: failed to parse int"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListTagsForResource

Returns all the tags on the specified Application Auto Scaling scalable target.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleFrontendService.ListTagsForResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 25
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceARN": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 257
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'resourceARN' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:.+:application-autoscaling:.+:[0-9]+:scalable-target\\/[a-zA-Z0-9-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutScalingPolicy

Creates or updates a scaling policy for an Application Auto Scaling scalable target.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleFrontendService.PutScalingPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 119
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "PolicyName": "example",
  "ServiceNamespace": "ecs",
  "ResourceId": "example",
  "ScalableDimension": "ecs:service:DesiredCount"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 173
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "__type": "ObjectNotFoundException",
  "Message": "No scalable target registered for service namespace: ecs, resource ID: example, scalable dimension: ecs:service:DesiredCount"
}
```

`400` `ObjectNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#4-putscalingpolicy) for a successful call.

### PutScheduledAction

Creates or updates a scheduled action for an Application Auto Scaling scalable target.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleFrontendService.PutScheduledAction
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 128
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ServiceNamespace": "ecs",
  "ScheduledActionName": "example",
  "ResourceId": "example",
  "ScalableDimension": "ecs:service:DesiredCount"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 111
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "__type": "ValidationException",
  "Message": "Unsupported service namespace, resource type or scalable dimension"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### RegisterScalableTarget

Registers or updates a scalable target, which is the resource that you want to scale.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleFrontendService.RegisterScalableTarget
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 96
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ServiceNamespace": "ecs",
  "ResourceId": "example",
  "ScalableDimension": "ecs:service:DesiredCount"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 111
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "__type": "ValidationException",
  "Message": "Unsupported service namespace, resource type or scalable dimension"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-registerscalabletarget) for a successful call.

### TagResource

Adds or edits tags on an Application Auto Scaling scalable target.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleFrontendService.TagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceARN": "example",
  "Tags": {
    "key": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 257
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'resourceARN' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:.+:application-autoscaling:.+:[0-9]+:scalable-target\\/[a-zA-Z0-9-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UntagResource

Deletes tags from an Application Auto Scaling scalable target.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleFrontendService.UntagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/application-autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceARN": "example",
  "TagKeys": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 257
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'resourceARN' failed to satisfy constraint: Member must satisfy regular expression pattern: arn:.+:application-autoscaling:.+:[0-9]+:scalable-target\\/[a-zA-Z0-9-]+"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/application-autoscaling/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Application Auto Scaling answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `Message`, `message`.

### Other services involved

The walkthrough also called [Amazon DynamoDB](dynamodb.md). They are separate services behind the same endpoint, sharing one account and one in-memory engine, so a resource created through one is immediately visible to the other.
