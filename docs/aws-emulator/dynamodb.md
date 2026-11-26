# Amazon DynamoDB

## Wire protocol

- **Protocol:** `awsJson1_0`
- **Request routing:** `POST /` with header `X-Amz-Target: DynamoDB_20120810.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `DynamoDB`
- **sigv4 signing name:** `dynamodb`
- **Endpoint prefix:** `dynamodb`
- **ARN namespace:** `dynamodb`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `dynamodb`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListTables`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:dynamodb" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.0' \
  -H 'X-Amz-Target: DynamoDB_20120810.ListTables' \
  --data-binary '{}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create a table, write, read, query and update an item, then delete the table. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateTable

The table starts in `CREATING`; only its key attributes are declared up front, every other attribute is schemaless.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.CreateTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 286
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AttributeDefinitions": [
    {
      "AttributeName": "CustomerId",
      "AttributeType": "S"
    },
    {
      "AttributeName": "OrderId",
      "AttributeType": "S"
    }
  ],
  "TableName": "Orders",
  "KeySchema": [
    {
      "AttributeName": "CustomerId",
      "KeyType": "HASH"
    },
    {
      "AttributeName": "OrderId",
      "KeyType": "RANGE"
    }
  ],
  "BillingMode": "PAY_PER_REQUEST"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 3YUN2XOZMK1ETOU1657C6HP917CQIB124332WH3LGY2A5M531M9V
x-amz-crc32: 4278653480
content-length: 933
date: Thu, 26 Nov 2026 18:27:55 GMT

{
  "TableDescription": {
    "AttributeDefinitions": [
      {
        "AttributeName": "CustomerId",
        "AttributeType": "S"
      },
      {
        "AttributeName": "OrderId",
        "AttributeType": "S"
      }
    ],
    "TableName": "Orders",
    "KeySchema": [
      {
        "AttributeName": "CustomerId",
        "KeyType": "HASH"
      },
      {
        "AttributeName": "OrderId",
        "KeyType": "RANGE"
      }
    ],
    "TableStatus": "CREATING",
    "CreationDateTime": 1795717675.695,
    "ProvisionedThroughput": {
      "NumberOfDecreasesToday": 0,
      "ReadCapacityUnits": 0,
      "WriteCapacityUnits": 0
    },
    "TableSizeBytes": 0,
    "ItemCount": 0,
    "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:table/Orders",
    "TableId": "4f45f341-c0fe-40a0-9ae8-0812a0a11007",
    "BillingModeSummary": {
      "BillingMode": "PAY_PER_REQUEST",
      "LastUpdateToPayPerRequestDateTime": 1795717675.695
    },
    "DeletionProtectionEnabled": false,
    "TableThroughputModeSummary": {
      "TableThroughputMode": "PAY_PER_REQUEST",
      "LastUpdateToPayPerRequestDateTime": 1795717675.695
    },
    "WarmThroughput": {
      "ReadUnitsPerSecond": 12000,
      "WriteUnitsPerSecond": 4000,
      "Status": "ACTIVE"
    }
  }
}
```

### 2. DescribeTable

Poll until the table reports `ACTIVE` before writing to it.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182755Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "Orders"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: ILOWONPD7ALGCFJKF4RDDDDT21BQWRYGKJT9LRSRFXOHHC1UNDMZ
x-amz-crc32: 985675484
content-length: 920
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "Table": {
    "AttributeDefinitions": [
      {
        "AttributeName": "CustomerId",
        "AttributeType": "S"
      },
      {
        "AttributeName": "OrderId",
        "AttributeType": "S"
      }
    ],
    "TableName": "Orders",
    "KeySchema": [
      {
        "AttributeName": "CustomerId",
        "KeyType": "HASH"
      },
      {
        "AttributeName": "OrderId",
        "KeyType": "RANGE"
      }
    ],
    "TableStatus": "ACTIVE",
    "CreationDateTime": 1795717675.695,
    "ProvisionedThroughput": {
      "NumberOfDecreasesToday": 0,
      "ReadCapacityUnits": 0,
      "WriteCapacityUnits": 0
    },
    "TableSizeBytes": 0,
    "ItemCount": 0,
    "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:table/Orders",
    "TableId": "4f45f341-c0fe-40a0-9ae8-0812a0a11007",
    "BillingModeSummary": {
      "BillingMode": "PAY_PER_REQUEST",
      "LastUpdateToPayPerRequestDateTime": 1795717675.695
    },
    "DeletionProtectionEnabled": false,
    "TableThroughputModeSummary": {
      "TableThroughputMode": "PAY_PER_REQUEST",
      "LastUpdateToPayPerRequestDateTime": 1795717675.695
    },
    "WarmThroughput": {
      "ReadUnitsPerSecond": 12000,
      "WriteUnitsPerSecond": 4000,
      "Status": "ACTIVE"
    }
  }
}
```

### 3. PutItem

Every value is tagged with its type (`S` string, `N` number). A successful put answers `{}`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.PutItem
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 121
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "Orders",
  "Item": {
    "CustomerId": {
      "S": "c-1"
    },
    "OrderId": {
      "S": "o-1"
    },
    "Total": {
      "N": "42.50"
    },
    "Status": {
      "S": "NEW"
    }
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: HAYAB4Y5KWQ8XHRAE5APDO4MQXB4NLWAR3O6F8BOBWVCB2H2I3XL
x-amz-crc32: 2745614147
content-length: 2
date: Thu, 26 Nov 2026 18:27:58 GMT

{}
```

### 4. GetItem

Reads the item back by its full primary key.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.GetItem
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 99
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "Orders",
  "Key": {
    "CustomerId": {
      "S": "c-1"
    },
    "OrderId": {
      "S": "o-1"
    }
  },
  "ConsistentRead": true
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: XBAS6CKY6JA88G0VAKRYT6GH8F0B22X1J2SOTN44H92EMTA7POGJ
x-amz-crc32: 159231099
content-length: 99
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "Item": {
    "CustomerId": {
      "S": "c-1"
    },
    "OrderId": {
      "S": "o-1"
    },
    "Total": {
      "N": "42.5"
    },
    "Status": {
      "S": "NEW"
    }
  }
}
```

### 5. UpdateItem

`Status` is a reserved word, so the expression names it through `#s`; `ALL_NEW` returns the item as it is after the update.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.UpdateItem
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 229
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "Orders",
  "Key": {
    "CustomerId": {
      "S": "c-1"
    },
    "OrderId": {
      "S": "o-1"
    }
  },
  "ReturnValues": "ALL_NEW",
  "UpdateExpression": "SET #s = :s",
  "ExpressionAttributeNames": {
    "#s": "Status"
  },
  "ExpressionAttributeValues": {
    ":s": {
      "S": "SHIPPED"
    }
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 7J2FRE92U0ZOH2B4Z1M23KA6T1A06ET4XQUY35DHLOTGZ0QUCVBW
x-amz-crc32: 3152424532
content-length: 109
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "Attributes": {
    "CustomerId": {
      "S": "c-1"
    },
    "OrderId": {
      "S": "o-1"
    },
    "Total": {
      "N": "42.5"
    },
    "Status": {
      "S": "SHIPPED"
    }
  }
}
```

### 6. Query

Query reads every item under one partition key, ordered by the sort key.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.Query
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 112
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "Orders",
  "KeyConditionExpression": "CustomerId = :c",
  "ExpressionAttributeValues": {
    ":c": {
      "S": "c-1"
    }
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: YK41UDSGC7ZDHWX8VHM5U1PEA5S5P5G4DHGPIED9PKX33TE1J5X5
x-amz-crc32: 4040226390
content-length: 133
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "Items": [
    {
      "CustomerId": {
        "S": "c-1"
      },
      "OrderId": {
        "S": "o-1"
      },
      "Total": {
        "N": "42.5"
      },
      "Status": {
        "S": "SHIPPED"
      }
    }
  ],
  "Count": 1,
  "ScannedCount": 1
}
```

### 7. PutItem

A conditional put that must not overwrite an existing item fails with `ConditionalCheckFailedException`, because o-1 exists.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.PutItem
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 132
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "Orders",
  "Item": {
    "CustomerId": {
      "S": "c-1"
    },
    "OrderId": {
      "S": "o-1"
    }
  },
  "ConditionExpression": "attribute_not_exists(OrderId)"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 8ZQQV9ZSN9H3JW386S086O8J7EX7X5WWL8IOH0OMGWYMY0BGN2NQ
x-amz-crc32: 396270901
content-length: 120
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ConditionalCheckFailedException",
  "message": "The conditional request failed"
}
```

### 8. DeleteTable

The table moves to `DELETING` and then disappears.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DeleteTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "Orders"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: X837H36IIQ38EJ955TI24ZIDPUA3QEBN5QMCM6J1LDYDA7UEJQVO
x-amz-crc32: 1356879560
content-length: 933
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "TableDescription": {
    "AttributeDefinitions": [
      {
        "AttributeName": "CustomerId",
        "AttributeType": "S"
      },
      {
        "AttributeName": "OrderId",
        "AttributeType": "S"
      }
    ],
    "TableName": "Orders",
    "KeySchema": [
      {
        "AttributeName": "CustomerId",
        "KeyType": "HASH"
      },
      {
        "AttributeName": "OrderId",
        "KeyType": "RANGE"
      }
    ],
    "TableStatus": "DELETING",
    "CreationDateTime": 1795717675.695,
    "ProvisionedThroughput": {
      "NumberOfDecreasesToday": 0,
      "ReadCapacityUnits": 0,
      "WriteCapacityUnits": 0
    },
    "TableSizeBytes": 0,
    "ItemCount": 0,
    "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:table/Orders",
    "TableId": "4f45f341-c0fe-40a0-9ae8-0812a0a11007",
    "BillingModeSummary": {
      "BillingMode": "PAY_PER_REQUEST",
      "LastUpdateToPayPerRequestDateTime": 1795717675.695
    },
    "DeletionProtectionEnabled": false,
    "TableThroughputModeSummary": {
      "TableThroughputMode": "PAY_PER_REQUEST",
      "LastUpdateToPayPerRequestDateTime": 1795717675.695
    },
    "WarmThroughput": {
      "ReadUnitsPerSecond": 12000,
      "WriteUnitsPerSecond": 4000,
      "Status": "ACTIVE"
    }
  }
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 60 operations, 10 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error. `ListBulkOperations` and `StartBulkPutItems` are new in the service model as of emulator 1.0.595 and were captured on 2026-11-24.

### BatchExecuteStatement

This operation allows you to perform batch reads or writes on data stored in DynamoDB, using PartiQL.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.BatchExecuteStatement
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 40
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Statements": [
    {
      "Statement": "example"
    }
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: FWSA38QA37HWV3RM55N743CY6DTJ8AW20L4EMMU9QQOX12URTG2I
x-amz-crc32: 2050133913
content-length: 141
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "Responses": [
    {
      "Error": {
        "Code": "ValidationError",
        "Message": "Statement wasn't well formed, can't be processed: Expected data manipulation"
      }
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### BatchGetItem

The BatchGetItem operation returns the attributes of one or more items from one or more tables.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.BatchGetItem
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "RequestItems": {
    "key": {
      "Keys": [
        {
          "key": {
            "S": "example"
          }
        }
      ]
    }
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: HKUJ5OYKBHIH41J6HCMCS7K4OCPJPY8B4GHR150XZJCXH890VZ7R
x-amz-crc32: 3737639027
content-length: 112
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### BatchWriteItem

The BatchWriteItem operation puts or deletes multiple items in one or more tables.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.BatchWriteItem
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 29
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "RequestItems": {
    "key": [
      {}
    ]
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 7W2L9OWJM66NOOQ7IIORC1LBRRSYK4JH1RQAK8XDXL7UQVY7ESLE
x-amz-crc32: 3737639027
content-length: 112
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateBackup

Creates a backup for an existing table.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.CreateBackup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example",
  "BackupName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: NCZUBIA0N2A71QWZQWAUBMLKGLUUPW0PCLDUK1CYOTCS48IT9RF0
x-amz-crc32: 79288739
content-length: 105
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#TableNotFoundException",
  "message": "Table not found: example"
}
```

`400` `TableNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateGlobalTable

Creates a global table from an existing table.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.CreateGlobalTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "GlobalTableName": "example",
  "ReplicationGroup": [
    {}
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 81F9LJB59MUCCBZNPM5GNJ658O4B0UWV7N3HHZOZPRUFQR0SSUT6
x-amz-crc32: 3046658788
content-length: 275
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "One or more parameter values were invalid: DynamoDB global tables version 2017.11.29 is not supported. We recommend using DynamoDB global tables version 2019.11.21, instead of version 2017.11.29 (Legacy)."
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateTable

The CreateTable operation adds a new table to your account.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.CreateTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: A3AHZ06DH7WXXP639C5WKNCGESROEQX7LCGYVO1N80NX1YCLN9CO
x-amz-crc32: 4251507980
content-length: 209
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "One or more parameter values were invalid: ReadCapacityUnits and WriteCapacityUnits must both be specified when BillingMode is PROVISIONED"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-createtable) for a successful call.

### DeleteBackup

Deletes an existing backup of a table.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DeleteBackup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "BackupArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 7IHPHIH6FCAGI5JQPRE1GLSE73JHGEVG06PDYEGQQQ987G4D59M9
x-amz-crc32: 2621702438
content-length: 89
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "Invalid Backup ARN"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteItem

Deletes a single item in a table by primary key.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DeleteItem
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example",
  "Key": {
    "key": {
      "S": "example"
    }
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: GD9I807VRT6K881NWBPN7EENXDLNL9E2852LHPM2S7MF5NAQZSY8
x-amz-crc32: 3737639027
content-length: 112
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteResourcePolicy

Deletes the resource-based policy attached to the resource, which can be a table or stream.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DeleteResourcePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 73
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: UR7AX4LGM8HMFFE3CPY3HBA4UZQYEHIIMDXOGSW2GYTS7AVHF2MZ
x-amz-crc32: 1287011363
content-length: 214
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "One or more parameter values were invalid: Provided Arn is not a DynamoDB resource arn: arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteTable

The DeleteTable operation deletes a table and all of its items.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DeleteTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: N6F0ITKVG84UPHBNTBA6USO6O6MXMB9DQDMH9TOYQRA5EZ64NKG8
x-amz-crc32: 203954860
content-length: 138
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found: Table: example not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#8-deletetable) for a successful call.

### DescribeBackup

Describes an existing backup of a table.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeBackup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "BackupArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: X9JMTRIS3NXVV1IFOL4B4Q98TAJYD2MWXD6FBZ8CP1JJ5T6JRIP7
x-amz-crc32: 2621702438
content-length: 89
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "Invalid Backup ARN"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeContinuousBackups

Checks the status of continuous backups and point in time recovery on the specified table.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeContinuousBackups
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: W3EG65521ZAW534RXDJPV77EE56B7YFHNNUI2EXC9MNBSLL3Q9O3
x-amz-crc32: 79288739
content-length: 105
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#TableNotFoundException",
  "message": "Table not found: example"
}
```

`400` `TableNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeContributorInsights

Returns information about contributor insights for a given table or global secondary index.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeContributorInsights
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 4OK610U26MGJMDVUXRCMPSJMU51UK4M5BZNZDHHNRH5N049J8D0W
x-amz-crc32: 203954860
content-length: 138
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found: Table: example not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeEndpoints

Returns the regional endpoint information.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeEndpoints
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 81B36I16MD9UWH2S9CJ47IIWW1XLGZ1BAK5155W1JP4JZJ2U09BI
x-amz-crc32: 338333978
content-length: 72
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "Endpoints": [
    {
      "Address": "127.0.0.1:9297",
      "CachePeriodInMinutes": 1440
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeExport

Describes an existing table export.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeExport
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ExportArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: SV3MRTTZSTYVNM04M452DH1X67V57L81WO6KEY3BTL3LD7YY2J6C
x-amz-crc32: 3214887545
content-length: 181
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "Invalid ExportArn: Invalid ExportArn provided as input arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeGlobalTable

Returns information about the specified global table.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeGlobalTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 29
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "GlobalTableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: PWEJW5HJ7EG94KQ6LQGVVUYFRQG0HV6EUGITT1S80P7UGWWUQGJM
x-amz-crc32: 1707832236
content-length: 160
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#GlobalTableNotFoundException",
  "message": "Global table not found: Global table with name: 'example' does not exist."
}
```

`400` `GlobalTableNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeGlobalTableSettings

Describes Region-specific settings for a global table.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeGlobalTableSettings
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 29
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "GlobalTableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 2BU7VUJD3PBNDEBDW8NLCHREDEGE0YEE2QY1UFUVNVUS85E2QZG0
x-amz-crc32: 2610105866
content-length: 136
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#GlobalTableNotFoundException",
  "message": "Global table with name: 'example' does not exist."
}
```

`400` `GlobalTableNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeImport

Represents the properties of the import.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeImport
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ImportArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: UDL990GWY6325O8HPL0M1GF0OGZXGH2VQWLKC0KN4BHGBRBSTXI5
x-amz-crc32: 1971534971
content-length: 181
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "Invalid ImportArn: Invalid ImportArn provided as input arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeKinesisStreamingDestination

Returns information about the status of Kinesis streaming.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeKinesisStreamingDestination
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: REMIB8R24E3GBG5L9KG7Q4F42UVCN3X2L55LVSTRI1SQUI3M5WP8
x-amz-crc32: 203954860
content-length: 138
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found: Table: example not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeLimits

Returns the current provisioned-capacity quotas for your Amazon Web Services account in a Region, both for the Region as a whole and for any one DynamoDB table that you create there.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeLimits
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: GMHGVSWO5DN2ETDGWEILBFC3PWQHH0TL7RL5B2F324BDP5N5FBRD
x-amz-crc32: 3062975651
content-length: 143
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "AccountMaxReadCapacityUnits": 80000,
  "AccountMaxWriteCapacityUnits": 80000,
  "TableMaxReadCapacityUnits": 40000,
  "TableMaxWriteCapacityUnits": 40000
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTable

Returns information about the table, including the current status of the table, when it was created, the primary key schema, and any indexes on the table.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: I0XXMC4QH4919BK6YLMYTBY3GJ649KZSQJWPDWWJIAVGZPB8Z7NZ
x-amz-crc32: 203954860
content-length: 138
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found: Table: example not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#2-describetable) for a successful call.

### DescribeTableReplicaAutoScaling

Describes auto scaling settings across replicas of the global table at once.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeTableReplicaAutoScaling
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: VFWGW8H92YB2AOVMM7KVB9OM3W80GXVQKTM8KF1ARTB9MP41X927
x-amz-crc32: 312258189
content-length: 133
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Global table with name: 'example' does not exist."
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeTimeToLive

Gives a description of the Time to Live (TTL) status on the specified table.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DescribeTimeToLive
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: ZAJXZ7TXV0Z6AKB3O98M94LYRE48W5RVK4V70SU9WLRCFC7IDGKF
x-amz-crc32: 203954860
content-length: 138
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found: Table: example not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisableKinesisStreamingDestination

Stops replication from the DynamoDB table to the Kinesis data stream.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.DisableKinesisStreamingDestination
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 93
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example",
  "StreamArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: F2XVTW9CCENQDGW8SRUFIFO8ZIDAKDDWS4DPWM0VKO66S73XDKYF
x-amz-crc32: 203954860
content-length: 138
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found: Table: example not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### EnableKinesisStreamingDestination

Starts table data replication to the specified Kinesis data stream at a timestamp chosen during the enable workflow.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.EnableKinesisStreamingDestination
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 93
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example",
  "StreamArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: RE0MLH5ECK3E5BOGCIR0H1C8D0UG5WAHEXT7M7YK7B4Z09JERGM6
x-amz-crc32: 203954860
content-length: 138
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found: Table: example not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ExecuteStatement

This operation allows you to perform reads and singleton writes on data stored in DynamoDB, using PartiQL.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.ExecuteStatement
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Statement": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: KJ3N0R29PL60GZ8ZL0JKD9WKMSFT0NH8ISO5WXZ4XMZS3SZHHNLN
x-amz-crc32: 4234718714
content-length: 147
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "Statement wasn't well formed, can't be processed: Expected data manipulation"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ExecuteTransaction

This operation allows you to perform transactional reads or writes on data stored in DynamoDB, using PartiQL.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.ExecuteTransaction
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 108
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TransactStatements": [
    {
      "Statement": "example"
    }
  ],
  "ClientRequestToken": "f5006e9f-84ac-4024-873c-85b6684991b5"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: P0HG9318TSNW109F7OG44BE5S53QZ6UBFD6UCT5AJMUXG9YFQG9S
x-amz-crc32: 3727717423
content-length: 191
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "Validation failed in TransactStatements[0]: Statement wasn't well formed, can't be processed: Expected data manipulation"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ExportTableToPointInTime

Exports table data to an S3 bucket.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.ExportTableToPointInTime
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 144
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example",
  "ClientToken": "7fce3ed0-7599-42b3-be6f-04dce385c640",
  "S3Bucket": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: YQ7P50IJSPLLF8KTADYS1SWH7OPZP0QYAA3KPW88Y1RVUEIB8TLG
x-amz-crc32: 4217040713
content-length: 179
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "Invalid TableArn: Invalid TableArn provided as input arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetItem

The GetItem operation returns a set of attributes for the item with the given primary key.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.GetItem
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example",
  "Key": {
    "key": {
      "S": "example"
    }
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: QVM3OOA4G87BT7V4ZCEJ6CURP2UOO5NZ5KEA6PMGZL9H1TCISR67
x-amz-crc32: 3737639027
content-length: 112
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#4-getitem) for a successful call.

### GetResourcePolicy

Returns the resource-based policy document attached to the resource, which can be a table or stream, in JSON format.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.GetResourcePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 73
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: CZ19KGTTHF65VOUSQ8XCC3NX2CO6PUBTUEEPVHAP1WOVZFJOGCCD
x-amz-crc32: 1287011363
content-length: 214
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "One or more parameter values were invalid: Provided Arn is not a DynamoDB resource arn: arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ImportTable

Imports table data from an S3 bucket.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.ImportTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 444
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClientToken": "091dc31e-9ae9-4182-9fbb-a9f9933d37bd",
  "S3BucketSource": {
    "S3Bucket": "example"
  },
  "InputFormat": "DYNAMODB_JSON",
  "TableCreationParameters": {
    "TableName": "example",
    "AttributeDefinitions": [
      {
        "AttributeName": "example",
        "AttributeType": "S"
      }
    ],
    "KeySchema": [
      {
        "AttributeName": "example",
        "KeyType": "HASH"
      }
    ]
… (1 more lines)
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: QT1PYD5ESXTM5IG70LJTULK2A3OO5YQSBXIZLO8D1SNBF7HBJMX2
x-amz-crc32: 3948637019
content-length: 60
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.service#SerializationException"
}
```

`400` `SerializationException`.

### ListBackups

List DynamoDB backups that are associated with an Amazon Web Services account and weren't made with Amazon Web Services Backup.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.ListBackups
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: Q5PQEJYYXS19FHRF4CI3JSYKC66EQ7N8JAQ8KTK6PII7U0OFQGDX
x-amz-crc32: 1411314939
content-length: 22
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "BackupSummaries": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListBulkOperations

Returns summary information about the bulk operations of the account and Region, most recently started first.

*New in the service model as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.ListBulkOperations
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: D01A9HZ01VTPRT1SLWT1JLNUJC8EL5S19P8OT78U8PGE8XY9VW61
x-amz-crc32: 3356066316
content-length: 32
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "BulkOperationsSummaryList": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListContributorInsights

Returns a list of ContributorInsightsSummary for a table and all its global secondary indexes.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.ListContributorInsights
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: T4FONXH2BXLWYAVQ5AWRC30GY7AI8M1PNTPG65D5X061QBWCV4SN
x-amz-crc32: 2686160312
content-length: 35
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "ContributorInsightsSummaries": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListExports

Lists completed exports within the past 90 days, in reverse alphanumeric order of ExportArn .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.ListExports
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: A9HQOWL2QLA4QNC6Y7KFTXMANPRNAUL1J6OA8JY8ILPZW34LKQS5
x-amz-crc32: 3102986012
content-length: 22
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "ExportSummaries": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListGlobalTables

Lists all global tables that have a replica in the specified Region.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.ListGlobalTables
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: QRCQMZQY4JWYRAIAV8X89V7UECXWJ5XY6LVL6A4ELI0VELBM514A
x-amz-crc32: 72925925
content-length: 19
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "GlobalTables": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListImports

Lists completed imports within the past 90 days.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.ListImports
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: Q7TU7N0X9XPA01IFQ1NT8C05AQG3YE0ZDEN2HXUTMYOFLH828QAI
x-amz-crc32: 2490712219
content-length: 24
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "ImportSummaryList": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTables

Returns an array of table names associated with the current account and endpoint.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.ListTables
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 0V6O3I5FZ5D52O75ZKA19552Q5R8ML57928GFJUZCLIUDQ47HN86
x-amz-crc32: 1848568431
content-length: 25
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "TableNames": [
    "Orders"
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTagsOfResource

List all tags on an Amazon DynamoDB resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.ListTagsOfResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 73
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: I8OT70R9UBOPH1Y6VZ50BVPSCKF2WNF4SV25MLMW6KV1P31L72ZE
x-amz-crc32: 3530764421
content-length: 182
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "Invalid TableArn: Invalid ResourceArn provided as input arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutItem

Creates a new item, or replaces an old item with a new item.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.PutItem
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example",
  "Item": {
    "key": {
      "S": "example"
    }
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: WC5KPLTLGJQW53CWHVLM7LCJ6ZDK3X89GYGDHB5PH7JTD8KISG0J
x-amz-crc32: 3737639027
content-length: 112
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#3-putitem) for a successful call.

### PutResourcePolicy

Attaches a resource-based policy document to the resource, which can be a table or stream.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.PutResourcePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 92
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example",
  "Policy": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 78R2SMPVYDAZYYE9EWDEW1ZHPRMP6IJEANLO0OTHSPXHFH6AG1T1
x-amz-crc32: 1287011363
content-length: 214
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "One or more parameter values were invalid: Provided Arn is not a DynamoDB resource arn: arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### Query

You must provide the name of the partition key attribute and a single value for that attribute.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.Query
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: F14FDRM5RFRBOQDHHJPAQMP1OKKJFZYTX8Y45TUP3K1DF6X39TM5
x-amz-crc32: 2570445832
content-length: 165
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "Either the KeyConditions or KeyConditionExpression parameter must be specified in the request."
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#6-query) for a successful call.

### RestoreTableFromBackup

Creates a new table from an existing backup.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.RestoreTableFromBackup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 99
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TargetTableName": "example",
  "BackupArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: RSIZEEGHVQ72KNJCD138VZO8W2H4Q3RMDAUCVBMS9S9UUH1EWVLQ
x-amz-crc32: 2621702438
content-length: 89
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "Invalid Backup ARN"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### RestoreTableToPointInTime

Restores the specified table to the specified point in time within EarliestRestorableDateTime and LatestRestorableDateTime .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.RestoreTableToPointInTime
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 29
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TargetTableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: P2TM48PAWL8GBU5YLQ01XJ2JHCXERWWBJJCHMOE95A1VYPUJU6V8
x-amz-crc32: 2258547525
content-length: 172
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "One or more parameter values were invalid: either SourceTableName or SourceTableArn must be specified"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### Scan

The Scan operation returns one or more items and item attributes by accessing every item in a table or a secondary index.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.Scan
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: Z3IVP5OR01LQGGJ6SUGRX3IHNEXIE84YY2FQLI2UGA3FFF958VQ0
x-amz-crc32: 3737639027
content-length: 112
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### SearchVectors

Performs a vector similarity search on a vector index associated with an Amazon DynamoDB table, and returns the most similar items sorted by similarity score based on the distance function configured for the index.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.SearchVectors
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 87
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example",
  "IndexName": "example",
  "SearchVector": [
    {
      "S": "example"
    }
  ],
  "TopK": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 4B4H5R4YT09X0M3YRM88VHN59XZ534YT8DXOUPY80P8AYNM2DQSR
x-amz-crc32: 1514290548
content-length: 190
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "Search vector contains invalid values. All values in the search vector must be a 32-bit floating-point number attribute"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### StartBulkPutItems

Starts a bulk operation that writes the items of the request into an existing table.

*New in the service model as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.StartBulkPutItems
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 122
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example",
  "InputS3Configuration": {
    "S3Bucket": "example"
  },
  "ClientToken": "c126dac7-919d-4483-91ed-d80f5d24b7c6"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: MV89QSHVDXS5W97RLUPMVYHZK2LXGT8RU5HVBLONPLWAAX40OPPC
x-amz-crc32: 3595763353
content-length: 191
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "1 validation error detected: Value null at 'outputS3Configuration' failed to satisfy constraint: Member must not be null"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### TagResource

Associate a set of tags with an Amazon DynamoDB resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.TagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 118
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example",
  "Tags": [
    {
      "Key": "example",
      "Value": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: KCU3SMAE6FXC8KCRSZPC0SA5SV0YEBLL0STQVDNWQDYZ0FVKUZWX
x-amz-crc32: 3530764421
content-length: 182
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "Invalid TableArn: Invalid ResourceArn provided as input arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### TransactGetItems

TransactGetItems is a synchronous operation that atomically retrieves multiple items from one or more tables (but not from indexes) in a single account and Region.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.TransactGetItems
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 81
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TransactItems": [
    {
      "Get": {
        "Key": {
          "key": {
            "S": "example"
          }
        },
        "TableName": "example"
      }
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: CQ3E149SM2N9GO320C4TB4WBSBXULLO1RNUZCSHXBDR1GV3IVF0O
x-amz-crc32: 3737639027
content-length: 112
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TransactWriteItems

TransactWriteItems is a synchronous write operation that groups up to 100 action requests.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.TransactWriteItems
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 82
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TransactItems": [
    {}
  ],
  "ClientRequestToken": "18a183e3-283b-4fda-a726-0281aa4a2c89"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: LHNO4Q21XNKMCM45AIENE5VBZNZ69N7UAO40IKO0BDI9MKR00AYK
x-amz-crc32: 3962908076
content-length: 235
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "1 validation error detected: Value '' at 'transactItems' failed to satisfy constraint: TransactWriteRequest should contain Delete or Put or Update or ConditionCheck"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UntagResource

Removes the association of tags from an Amazon DynamoDB resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.UntagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 95
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example",
  "TagKeys": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: PN1KX99N7TEL5BE7KESZOHKATKAZ6RARSYAZB7M9RUA7HBMQ41LG
x-amz-crc32: 3530764421
content-length: 182
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "Invalid TableArn: Invalid ResourceArn provided as input arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateContinuousBackups

UpdateContinuousBackups enables or disables point in time recovery for the specified table.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.UpdateContinuousBackups
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 95
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example",
  "PointInTimeRecoverySpecification": {
    "PointInTimeRecoveryEnabled": false
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: ZFM0N2G31GHNN33QPGOK0DIA7GKP1NXBH9A55JWC4DYRUT246GGK
x-amz-crc32: 79288739
content-length: 105
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#TableNotFoundException",
  "message": "Table not found: example"
}
```

`400` `TableNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateContributorInsights

Updates the status for contributor insights for a specific table or index.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.UpdateContributorInsights
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example",
  "ContributorInsightsAction": "ENABLE"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: SX026TVIXVTIIVINUY1C34Y4S5BKWGG9RV57L04XUB8HODCYUNNL
x-amz-crc32: 203954860
content-length: 138
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found: Table: example not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateGlobalTable

Adds or removes replicas in the specified global table.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.UpdateGlobalTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "GlobalTableName": "example",
  "ReplicaUpdates": [
    {}
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: YDWQU8KMDH81OVPYB54TW5MNIU8JNPL7FL5AAA01A19KQ11LWEXZ
x-amz-crc32: 1707832236
content-length: 160
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#GlobalTableNotFoundException",
  "message": "Global table not found: Global table with name: 'example' does not exist."
}
```

`400` `GlobalTableNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateGlobalTableSettings

Updates settings for a global table.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.UpdateGlobalTableSettings
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 29
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "GlobalTableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: CPAVCZER0D05UEU9JLEHDZN9XJVXJUH2LR9VRJ4J3I2FKEL5Z01Q
x-amz-crc32: 2610105866
content-length: 136
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#GlobalTableNotFoundException",
  "message": "Global table with name: 'example' does not exist."
}
```

`400` `GlobalTableNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateItem

Edits an existing item's attributes, or adds a new item to the table if it does not already exist.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.UpdateItem
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example",
  "Key": {
    "key": {
      "S": "example"
    }
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: Z0RVBD0FYV1QJEMV7AEGIG9V5O1U8YT1NH9TIX7AGIU70NKKQBV1
x-amz-crc32: 3737639027
content-length: 112
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#5-updateitem) for a successful call.

### UpdateKinesisStreamingDestination

The command to update the Kinesis stream destination.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.UpdateKinesisStreamingDestination
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 93
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example",
  "StreamArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 42PHJY30M4RC2NDP9AFZLB0DVOX2EOK0VCI4T5NXQL1UJSJWI5ZV
x-amz-crc32: 203954860
content-length: 138
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found: Table: example not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateTable

Modifies the provisioned throughput settings, global secondary indexes, DynamoDB Streams settings, or global table replica settings for a given table.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.UpdateTable
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: NQT2R06WYL0UBQSY4EPYUYKOIO06FASI977J6LBR2ZDSNG5679KY
x-amz-crc32: 1658647735
content-length: 367
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazon.coral.validate#ValidationException",
  "message": "At least one of ProvisionedThroughput, BillingMode, UpdateStreamEnabled, GlobalSecondaryIndexUpdates, SSESpecification, ReplicaUpdates, MultiAccountReplicaReady, ReplicaTransitRoleArn, MultiRegionConsistency, DeletionProtectionEnabled, OnDemandThroughput, WarmThroughput or TableClass is required"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateTableReplicaAutoScaling

Updates auto scaling settings on your global tables at once.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.UpdateTableReplicaAutoScaling
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: SOIUGE8FPOM61K99FCDV1NE33PT4AZ40U2Q395MBTDRBEYJW7BQ9
x-amz-crc32: 312258189
content-length: 133
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Global table with name: 'example' does not exist."
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateTimeToLive

The UpdateTimeToLive method enables or disables Time to Live (TTL) for the specified table.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.UpdateTimeToLive
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182758Z
Content-Length: 93
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "example",
  "TimeToLiveSpecification": {
    "Enabled": false,
    "AttributeName": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: FI1Z68JBQOX2RQ550QXI5NBLMZPW8TWFP4GZ1ZQOYK59F1W3APWX
x-amz-crc32: 203954860
content-length: 138
date: Thu, 26 Nov 2026 18:27:58 GMT

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found: Table: example not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/dynamodb/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon DynamoDB answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `message`.
