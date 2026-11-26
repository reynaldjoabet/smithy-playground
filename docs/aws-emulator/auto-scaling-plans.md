# AWS Auto Scaling Plans

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: AnyScaleScalingPlannerFrontendService.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `Auto Scaling Plans`
- **sigv4 signing name:** `autoscaling-plans`
- **Endpoint prefix:** `autoscaling-plans`
- **ARN namespace:** `autoscaling-plans`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `autoscaling-plans`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `DescribeScalingPlans`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:autoscaling-plans" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.1' \
  -H 'X-Amz-Target: AnyScaleScalingPlannerFrontendService.DescribeScalingPlans' \
  --data-binary '{}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create a scaling plan for a tagged DynamoDB table, inspect it, and delete it. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. Amazon DynamoDB: CreateTable

A tagged table for the plan to discover.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.CreateTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182729Z
Content-Length: 294
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AttributeDefinitions": [
    {
      "AttributeName": "id",
      "AttributeType": "S"
    }
  ],
  "TableName": "docs-demo-plan",
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
  },
  "Tags": [
    {
      "Key": "app",
      "Value": "docs-demo"
    }
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 9QX6YHQN1973DC4ZB77AYKFS2A0VGR07CPHGJR5B69703AYIN0FY
x-amz-crc32: 3192097194
content-length: 671
date: Thu, 26 Nov 2026 18:27:29 GMT

{
  "TableDescription": {
    "AttributeDefinitions": [
      {
        "AttributeName": "id",
        "AttributeType": "S"
      }
    ],
    "TableName": "docs-demo-plan",
    "KeySchema": [
      {
        "AttributeName": "id",
        "KeyType": "HASH"
      }
    ],
    "TableStatus": "CREATING",
    "CreationDateTime": 1795717649.363,
    "ProvisionedThroughput": {
      "NumberOfDecreasesToday": 0,
      "ReadCapacityUnits": 5,
      "WriteCapacityUnits": 5
    },
    "TableSizeBytes": 0,
    "ItemCount": 0,
    "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:table/docs-demo-plan",
    "TableId": "cb4422fc-f0e0-4d63-b5a0-1423e6c2313f",
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
X-Amz-Date: 20261126T182729Z
Content-Length: 30
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "docs-demo-plan"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: A5SQEB623B6NEZQ966GTFY4NIXP9SPXA8SVQS9QDNNOXOCYIEHOZ
x-amz-crc32: 3486182907
content-length: 658
date: Thu, 26 Nov 2026 18:27:32 GMT

{
  "Table": {
    "AttributeDefinitions": [
      {
        "AttributeName": "id",
        "AttributeType": "S"
      }
    ],
    "TableName": "docs-demo-plan",
    "KeySchema": [
      {
        "AttributeName": "id",
        "KeyType": "HASH"
      }
    ],
    "TableStatus": "ACTIVE",
    "CreationDateTime": 1795717649.363,
    "ProvisionedThroughput": {
      "NumberOfDecreasesToday": 0,
      "ReadCapacityUnits": 5,
      "WriteCapacityUnits": 5
    },
    "TableSizeBytes": 0,
    "ItemCount": 0,
    "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:table/docs-demo-plan",
    "TableId": "cb4422fc-f0e0-4d63-b5a0-1423e6c2313f",
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

### 3. CreateScalingPlan

A plan is versioned; the first is version 1.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AnyScaleScalingPlannerFrontendService.CreateScalingPlan
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 444
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling-plans/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ScalingPlanName": "docs-demo",
  "ApplicationSource": {
    "TagFilters": [
      {
        "Key": "app",
        "Values": [
          "docs-demo"
        ]
      }
    ]
  },
  "ScalingInstructions": [
    {
      "ServiceNamespace": "dynamodb",
      "ResourceId": "table/docs-demo-plan",
      "ScalableDimension": "dynamodb:table:ReadCapacityUnits",
      "MinCapacity": 5,
      "MaxCapacity": 20,
      "TargetTrackingConfigurations": [
        {
          "PredefinedScalingMetricSpecification": {
            "PredefinedScalingMetricType": "DynamoDBReadCapacityUtilization"
          },
          "TargetValue": 70.0
        }
      ]
    }
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 24
date: Thu, 26 Nov 2026 18:27:32 GMT

{
  "ScalingPlanVersion": 1
}
```

### 4. DescribeScalingPlans

Poll until the plan's status moves from `CreationInProgress` to `Active`; a plan still being created can't be deleted.

*The emulator was asked 2 times, about two seconds apart; the last exchange is shown.*

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AnyScaleScalingPlannerFrontendService.DescribeScalingPlans
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 34
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling-plans/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ScalingPlanNames": [
    "docs-demo"
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 691
date: Thu, 26 Nov 2026 18:27:32 GMT

{
  "ScalingPlans": [
    {
      "ScalingPlanName": "docs-demo",
      "ScalingPlanVersion": 1,
      "ApplicationSource": {
        "TagFilters": [
          {
            "Key": "app",
            "Values": [
              "docs-demo"
            ]
          }
        ]
      },
      "ScalingInstructions": [
        {
          "ServiceNamespace": "dynamodb",
          "ResourceId": "table/docs-demo-plan",
          "ScalableDimension": "dynamodb:table:ReadCapacityUnits",
          "MinCapacity": 5,
          "MaxCapacity": 20,
          "TargetTrackingConfigurations": [
            {
              "PredefinedScalingMetricSpecification": {
                "PredefinedScalingMetricType": "DynamoDBReadCapacityUtilization"
              },
              "TargetValue": 70.0
            }
          ]
        }
      ],
      "StatusCode": "CreationInProgress",
      "StatusMessage": "Scaling plan is being created.",
      "StatusStartTime": 1795717652.3751066,
      "CreationTime": 1795717652.3751066,
      "LastMutatingRequestTime": 1795717652.3751066
    }
  ]
}
```

### 5. DescribeScalingPlanResources

The resources the plan scales and their scaling status.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AnyScaleScalingPlannerFrontendService.DescribeScalingPlanResources
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling-plans/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ScalingPlanName": "docs-demo",
  "ScalingPlanVersion": 1
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 376
date: Thu, 26 Nov 2026 18:27:32 GMT

{
  "ScalingPlanResources": [
    {
      "ScalingPlanName": "docs-demo",
      "ScalingPlanVersion": 1,
      "ServiceNamespace": "dynamodb",
      "ResourceId": "table/docs-demo-plan",
      "ScalableDimension": "dynamodb:table:ReadCapacityUnits",
      "ScalingStatusCode": "Inactive",
      "ScalingStatusMessage": "Target tracking scaling policies have not been applied to the resource yet because the scaling plan is being created."
    }
  ]
}
```

### 6. DeleteScalingPlan

Delete the plan.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AnyScaleScalingPlannerFrontendService.DeleteScalingPlan
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling-plans/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ScalingPlanName": "docs-demo",
  "ScalingPlanVersion": 1
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 2
date: Thu, 26 Nov 2026 18:27:32 GMT

{}
```

### 7. Amazon DynamoDB: DescribeTable

Wait until the table is idle again.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 30
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "docs-demo-plan"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: FQ0NI2P9P6TKIQKZPFX1S2PUY5BYOXQPBO0FLD3AD7YLFQ4BOJ3X
x-amz-crc32: 3486182907
content-length: 658
date: Thu, 26 Nov 2026 18:27:32 GMT

{
  "Table": {
    "AttributeDefinitions": [
      {
        "AttributeName": "id",
        "AttributeType": "S"
      }
    ],
    "TableName": "docs-demo-plan",
    "KeySchema": [
      {
        "AttributeName": "id",
        "KeyType": "HASH"
      }
    ],
    "TableStatus": "ACTIVE",
    "CreationDateTime": 1795717649.363,
    "ProvisionedThroughput": {
      "NumberOfDecreasesToday": 0,
      "ReadCapacityUnits": 5,
      "WriteCapacityUnits": 5
    },
    "TableSizeBytes": 0,
    "ItemCount": 0,
    "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:table/docs-demo-plan",
    "TableId": "cb4422fc-f0e0-4d63-b5a0-1423e6c2313f",
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

### 8. Amazon DynamoDB: DeleteTable

Clean up.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DeleteTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 30
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "docs-demo-plan"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: VR6Z2HY4JQ7WWCLD4PWTKAP5LTZMSPE4FSABRWYZ5I2PTL99Q84K
x-amz-crc32: 1052277674
content-length: 671
date: Thu, 26 Nov 2026 18:27:32 GMT

{
  "TableDescription": {
    "AttributeDefinitions": [
      {
        "AttributeName": "id",
        "AttributeType": "S"
      }
    ],
    "TableName": "docs-demo-plan",
    "KeySchema": [
      {
        "AttributeName": "id",
        "KeyType": "HASH"
      }
    ],
    "TableStatus": "DELETING",
    "CreationDateTime": 1795717649.363,
    "ProvisionedThroughput": {
      "NumberOfDecreasesToday": 0,
      "ReadCapacityUnits": 5,
      "WriteCapacityUnits": 5
    },
    "TableSizeBytes": 0,
    "ItemCount": 0,
    "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:table/docs-demo-plan",
    "TableId": "cb4422fc-f0e0-4d63-b5a0-1423e6c2313f",
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

Of 6 operations, 2 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### CreateScalingPlan

Creates a scaling plan.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleScalingPlannerFrontendService.CreateScalingPlan
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 284
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling-plans/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ScalingPlanName": "example",
  "ApplicationSource": {},
  "ScalingInstructions": [
    {
      "ServiceNamespace": "autoscaling",
      "ResourceId": "example",
      "ScalableDimension": "autoscaling:autoScalingGroup:DesiredCapacity",
      "MinCapacity": 1,
      "MaxCapacity": 1,
      "TargetTrackingConfigurations": [
        {
          "TargetValue": 1
        }
      ]
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 69
date: Thu, 26 Nov 2026 18:27:32 GMT

{
  "__type": "ValidationException",
  "Message": "Empty application source"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-createscalingplan) for a successful call.

### DeleteScalingPlan

Deletes the specified scaling plan.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleScalingPlannerFrontendService.DeleteScalingPlan
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling-plans/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ScalingPlanName": "example",
  "ScalingPlanVersion": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 113
date: Thu, 26 Nov 2026 18:27:32 GMT

{
  "__type": "ObjectNotFoundException",
  "Message": "No scaling plan found for scaling plan name: example, version: 1"
}
```

`400` `ObjectNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#6-deletescalingplan) for a successful call.

### DescribeScalingPlanResources

Describes the scalable resources in the specified scaling plan.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleScalingPlannerFrontendService.DescribeScalingPlanResources
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling-plans/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ScalingPlanName": "example",
  "ScalingPlanVersion": 1
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 27
date: Thu, 26 Nov 2026 18:27:32 GMT

{
  "ScalingPlanResources": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#5-describescalingplanresources) for a successful call.

### DescribeScalingPlans

Describes one or more of your scaling plans.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleScalingPlannerFrontendService.DescribeScalingPlans
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling-plans/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 691
date: Thu, 26 Nov 2026 18:27:32 GMT

{
  "ScalingPlans": [
    {
      "ScalingPlanName": "docs-demo",
      "ScalingPlanVersion": 1,
      "ApplicationSource": {
        "TagFilters": [
          {
            "Key": "app",
            "Values": [
              "docs-demo"
            ]
          }
        ]
      },
      "ScalingInstructions": [
        {
          "ServiceNamespace": "dynamodb",
          "ResourceId": "table/docs-demo-plan",
          "ScalableDimension": "dynamodb:table:ReadCapacityUnits",
… (20 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#4-describescalingplans) for a successful call.

### GetScalingPlanResourceForecastData

Retrieves the forecast data for a scalable resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleScalingPlannerFrontendService.GetScalingPlanResourceForecastData
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 281
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling-plans/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ScalingPlanName": "example",
  "ScalingPlanVersion": 1,
  "ServiceNamespace": "autoscaling",
  "ResourceId": "example",
  "ScalableDimension": "autoscaling:autoScalingGroup:DesiredCapacity",
  "ForecastDataType": "CapacityForecast",
  "StartTime": "2026-11-16T12:11:23Z",
  "EndTime": "2026-11-16T12:11:23Z"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 83
date: Thu, 26 Nov 2026 18:27:32 GMT

{
  "__type": "ValidationException",
  "message": "invalid timestamp: failed to parse int"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateScalingPlan

Updates the specified scaling plan.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AnyScaleScalingPlannerFrontendService.UpdateScalingPlan
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling-plans/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ScalingPlanName": "example",
  "ScalingPlanVersion": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 113
date: Thu, 26 Nov 2026 18:27:32 GMT

{
  "__type": "ObjectNotFoundException",
  "Message": "No scaling plan found for scaling plan name: example, version: 1"
}
```

`400` `ObjectNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/autoscaling-plans/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way AWS Auto Scaling Plans answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `Message`, `message`.

### Other services involved

The walkthrough also called [Amazon DynamoDB](dynamodb.md). They are separate services behind the same endpoint, sharing one account and one in-memory engine, so a resource created through one is immediately visible to the other.
