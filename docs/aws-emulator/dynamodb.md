# Amazon DynamoDB

## Wire protocol

- **Protocol:** `awsJson1_0`
- **Request routing:** `POST /` with header `X-Amz-Target: DynamoDB_20120810.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `DynamoDB`
- **sigv4 signing name:** `dynamodb`
- **Endpoint prefix:** `dynamodb`
- **ARN namespace:** `dynamodb`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `dynamodb`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed.

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
X-Amz-Date: 20261116T120453Z
Content-Length: 307
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 3PF9DA3IKXUC1G2WUFFST7QBO7DS72GYYI9NQLZT5B7CC4AQT03U
x-amz-crc32: 1383107083
content-length: 1001
date: Mon, 16 Nov 2026 12:04:53 GMT

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
    "CreationDateTime": 1794830693.1,
    "ProvisionedThroughput": {
      "NumberOfDecreasesToday": 0,
      "ReadCapacityUnits": 0,
      "WriteCapacityUnits": 0
    },
    "TableSizeBytes": 0,
    "ItemCount": 0,
    "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:table/Orders",
    "TableId": "45f46f05-7c04-4e0e-9617-d1e62251a194",
    "BillingModeSummary": {
      "BillingMode": "PAY_PER_REQUEST",
      "LastUpdateToPayPerRequestDateTime": 1794830693.1
    },
    "DeletionProtectionEnabled": false,
    "OnDemandThroughput": {
      "MaxReadRequestUnits": -1,
      "MaxWriteRequestUnits": -1
    },
    "TableThroughputModeSummary": {
      "TableThroughputMode": "PAY_PER_REQUEST",
      "LastUpdateToPayPerRequestDateTime": 1794830693.1
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
X-Amz-Date: 20261116T120453Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "Orders"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: HKCSR9IB4F09NZ92ZMWJBXOO8ARBSMEFVDOQXVUKD7QLVH1QADP1
x-amz-crc32: 3348757818
content-length: 988
date: Mon, 16 Nov 2026 12:04:56 GMT

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
    "CreationDateTime": 1794830693.1,
    "ProvisionedThroughput": {
      "NumberOfDecreasesToday": 0,
      "ReadCapacityUnits": 0,
      "WriteCapacityUnits": 0
    },
    "TableSizeBytes": 0,
    "ItemCount": 0,
    "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:table/Orders",
    "TableId": "45f46f05-7c04-4e0e-9617-d1e62251a194",
    "BillingModeSummary": {
      "BillingMode": "PAY_PER_REQUEST",
      "LastUpdateToPayPerRequestDateTime": 1794830693.1
    },
    "DeletionProtectionEnabled": false,
    "OnDemandThroughput": {
      "MaxReadRequestUnits": -1,
      "MaxWriteRequestUnits": -1
    },
    "TableThroughputModeSummary": {
      "TableThroughputMode": "PAY_PER_REQUEST",
      "LastUpdateToPayPerRequestDateTime": 1794830693.1
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
X-Amz-Date: 20261116T120456Z
Content-Length: 135
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: B88EWPMQ21SOJW773N4N5SF9GW0KQEC97WK7LI3FMLX1CJ7POFP9
x-amz-crc32: 2745614147
content-length: 2
date: Mon, 16 Nov 2026 12:04:56 GMT

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
X-Amz-Date: 20261116T120456Z
Content-Length: 109
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: OV0CDISTJWJMSR73KQ6SRW2WXP9PJMJBXUPL94YMG8LI6D0FK7PH
x-amz-crc32: 2258453908
content-length: 99
date: Mon, 16 Nov 2026 12:04:56 GMT

{
  "Item": {
    "CustomerId": {
      "S": "c-1"
    },
    "Status": {
      "S": "NEW"
    },
    "Total": {
      "N": "42.5"
    },
    "OrderId": {
      "S": "o-1"
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
X-Amz-Date: 20261116T120456Z
Content-Length: 248
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: DOYX160F02TO6B4MUK2UY90KSR6AT97OMJEQH07UREMNB29SFJCL
x-amz-crc32: 205197486
content-length: 109
date: Mon, 16 Nov 2026 12:04:56 GMT

{
  "Attributes": {
    "CustomerId": {
      "S": "c-1"
    },
    "Status": {
      "S": "SHIPPED"
    },
    "Total": {
      "N": "42.5"
    },
    "OrderId": {
      "S": "o-1"
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
X-Amz-Date: 20261116T120456Z
Content-Length: 119
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: IL46VDW8TLG21U7OV8FKAANQWP70A07Q2QYNUBBD8J1L7OJH0VSC
x-amz-crc32: 3617416326
content-length: 133
date: Mon, 16 Nov 2026 12:04:56 GMT

{
  "Items": [
    {
      "CustomerId": {
        "S": "c-1"
      },
      "Status": {
        "S": "SHIPPED"
      },
      "Total": {
        "N": "42.5"
      },
      "OrderId": {
        "S": "o-1"
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
X-Amz-Date: 20261116T120456Z
Content-Length: 142
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: V5I9DXXL7PK3QZE8G5ZK7SLQBS1YBJDYSQWB3UQXKIJ9DGV0V8Z0
x-amz-crc32: 396270901
content-length: 120
date: Mon, 16 Nov 2026 12:04:56 GMT

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
X-Amz-Date: 20261116T120456Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TableName": "Orders"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 08J3DVA5NAXZ7FMEQ8JW07FCIRH9JACVJZZILWJ6WM1XKXRFSW8K
x-amz-crc32: 1389551042
content-length: 1001
date: Mon, 16 Nov 2026 12:04:56 GMT

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
    "CreationDateTime": 1794830693.1,
    "ProvisionedThroughput": {
      "NumberOfDecreasesToday": 0,
      "ReadCapacityUnits": 0,
      "WriteCapacityUnits": 0
    },
    "TableSizeBytes": 0,
    "ItemCount": 0,
    "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:table/Orders",
    "TableId": "45f46f05-7c04-4e0e-9617-d1e62251a194",
    "BillingModeSummary": {
      "BillingMode": "PAY_PER_REQUEST",
      "LastUpdateToPayPerRequestDateTime": 1794830693.1
    },
    "DeletionProtectionEnabled": false,
    "OnDemandThroughput": {
      "MaxReadRequestUnits": -1,
      "MaxWriteRequestUnits": -1
    },
    "TableThroughputModeSummary": {
      "TableThroughputMode": "PAY_PER_REQUEST",
      "LastUpdateToPayPerRequestDateTime": 1794830693.1
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

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 60 operations, 10 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error. `ListBulkOperations` and `StartBulkPutItems` are new in the service model as of emulator 1.0.595 and were captured on 2026-11-24.

### BatchExecuteStatement

This operation allows you to perform batch reads or writes on data stored in DynamoDB, using PartiQL.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.BatchExecuteStatement

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
X-Amz-Target: DynamoDB_20120810.BatchGetItem

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
X-Amz-Target: DynamoDB_20120810.BatchWriteItem

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
X-Amz-Target: DynamoDB_20120810.CreateBackup

{
  "TableName": "example",
  "BackupName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: DynamoDB_20120810.CreateGlobalTable

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

{
  "__type": "ValidationException",
  "message": "Global tables version 2017.11.29 (Legacy) is no longer supported. Use global tables version 2019.11.21 (Current) instead."
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateTable

The CreateTable operation adds a new table to your account.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.CreateTable

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "KeySchema is required and must declare a HASH key attribute"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-createtable) for a successful call.

### DeleteBackup

Deletes an existing backup of a table.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.DeleteBackup

{
  "BackupArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "Invalid Backup ARN"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteItem

Deletes a single item in a table by primary key.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.DeleteItem

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
X-Amz-Target: DynamoDB_20120810.DeleteResourcePolicy

{
  "ResourceArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "One or more parameter values were invalid: Invalid resource arn provided, only table or stream is accepted. Provided resource arn: arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteTable

The DeleteTable operation deletes a table and all of its items.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.DeleteTable

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: DynamoDB_20120810.DescribeBackup

{
  "BackupArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "Invalid Backup ARN"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeContinuousBackups

Checks the status of continuous backups and point in time recovery on the specified table.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.DescribeContinuousBackups

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: DynamoDB_20120810.DescribeContributorInsights

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: DynamoDB_20120810.DescribeEndpoints

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

{
  "Endpoints": [
    {
      "Address": "dynamodb.us-east-1.localhost:9297",
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
X-Amz-Target: DynamoDB_20120810.DescribeExport

{
  "ExportArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "Invalid ExportArn: Invalid ExportArn provided as input arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeGlobalTable

Returns information about the specified global table.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.DescribeGlobalTable

{
  "GlobalTableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "com.amazonaws.dynamodb.v20120810#GlobalTableNotFoundException",
  "message": "Global table not found: example"
}
```

`400` `GlobalTableNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeGlobalTableSettings

Describes Region-specific settings for a global table.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.DescribeGlobalTableSettings

{
  "GlobalTableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "com.amazonaws.dynamodb.v20120810#GlobalTableNotFoundException",
  "message": "Global table not found: example"
}
```

`400` `GlobalTableNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeImport

Represents the properties of the import.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.DescribeImport

{
  "ImportArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "Invalid ImportArn: Invalid ImportArn provided as input arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeKinesisStreamingDestination

Returns information about the status of Kinesis streaming.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.DescribeKinesisStreamingDestination

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: DynamoDB_20120810.DescribeLimits

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

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
X-Amz-Target: DynamoDB_20120810.DescribeTable

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: DynamoDB_20120810.DescribeTableReplicaAutoScaling

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found: Table: example not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeTimeToLive

Gives a description of the Time to Live (TTL) status on the specified table.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.DescribeTimeToLive

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: DynamoDB_20120810.DisableKinesisStreamingDestination

{
  "TableName": "example",
  "StreamArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: DynamoDB_20120810.EnableKinesisStreamingDestination

{
  "TableName": "example",
  "StreamArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: DynamoDB_20120810.ExecuteStatement

{
  "Statement": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "Statement wasn't well formed, can't be processed: Expected data manipulation"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ExecuteTransaction

This operation allows you to perform transactional reads or writes on data stored in DynamoDB, using PartiQL.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.ExecuteTransaction

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

{
  "__type": "ValidationException",
  "message": "Validation failed in TransactStatements[0]: Statement wasn't well formed, can't be processed: Expected data manipulation"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ExportTableToPointInTime

Exports table data to an S3 bucket.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.ExportTableToPointInTime

{
  "TableArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example",
  "ClientToken": "7fce3ed0-7599-42b3-be6f-04dce385c640",
  "S3Bucket": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "Invalid TableArn: Invalid TableArn provided as input arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetItem

The GetItem operation returns a set of attributes for the item with the given primary key.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.GetItem

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
X-Amz-Target: DynamoDB_20120810.GetResourcePolicy

{
  "ResourceArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "One or more parameter values were invalid: Invalid resource arn provided, only table or stream is accepted. Provided resource arn: arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ImportTable

Imports table data from an S3 bucket.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.ImportTable

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
… (2 more lines)
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "One or more parameter values were invalid: ProvisionedThroughput must be specified when BillingMode is PROVISIONED"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListBackups

List DynamoDB backups that are associated with an Amazon Web Services account and weren't made with Amazon Web Services Backup.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.ListBackups

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

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
X-Amz-Target: DynamoDB_20120810.ListBulkOperations

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

{
  "BulkOperationsSummaryList": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListContributorInsights

Returns a list of ContributorInsightsSummary for a table and all its global secondary indexes.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.ListContributorInsights

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

{
  "ContributorInsightsSummaries": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListExports

Lists completed exports within the past 90 days, in reverse alphanumeric order of ExportArn .

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.ListExports

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

{
  "ExportSummaries": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListGlobalTables

Lists all global tables that have a replica in the specified Region.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.ListGlobalTables

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

{
  "GlobalTables": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListImports

Lists completed imports within the past 90 days.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.ListImports

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

{
  "ImportSummaryList": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTables

Returns an array of table names associated with the current account and endpoint.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.ListTables

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

{
  "TableNames": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTagsOfResource

List all tags on an Amazon DynamoDB resource.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.ListTagsOfResource

{
  "ResourceArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "Invalid TableArn: Invalid ResourceArn provided as input arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutItem

Creates a new item, or replaces an old item with a new item.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.PutItem

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
X-Amz-Target: DynamoDB_20120810.PutResourcePolicy

{
  "ResourceArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example",
  "Policy": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "One or more parameter values were invalid: Invalid resource arn provided, only table or stream is accepted. Provided resource arn: arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### Query

You must provide the name of the partition key attribute and a single value for that attribute.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.Query

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "Either the KeyConditions or KeyConditionExpression parameter must be specified in the request."
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#6-query) for a successful call.

### RestoreTableFromBackup

Creates a new table from an existing backup.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.RestoreTableFromBackup

{
  "TargetTableName": "example",
  "BackupArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "Invalid Backup ARN"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### RestoreTableToPointInTime

Restores the specified table to the specified point in time within EarliestRestorableDateTime and LatestRestorableDateTime .

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.RestoreTableToPointInTime

{
  "TargetTableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "One or more parameter values were invalid: either SourceTableName or SourceTableArn must be specified"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### Scan

The Scan operation returns one or more items and item attributes by accessing every item in a table or a secondary index.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.Scan

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: DynamoDB_20120810.SearchVectors

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

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'searchVector' failed to satisfy constraint: Member must hold only numbers, each a 32-bit IEEE-754 floating point value"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### StartBulkPutItems

Starts a bulk operation that writes the items of the request into an existing table.

*New in the service model as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.StartBulkPutItems

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

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found: Table: example not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagResource

Associate a set of tags with an Amazon DynamoDB resource.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.TagResource

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

{
  "__type": "ValidationException",
  "message": "Invalid TableArn: Invalid ResourceArn provided as input arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### TransactGetItems

TransactGetItems is a synchronous operation that atomically retrieves multiple items from one or more tables (but not from indexes) in a single account and Region.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.TransactGetItems

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
X-Amz-Target: DynamoDB_20120810.TransactWriteItems

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

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'transactItems' failed to satisfy constraint: Member must contain exactly one of ConditionCheck, Put, Delete or Update"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UntagResource

Removes the association of tags from an Amazon DynamoDB resource.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.UntagResource

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

{
  "__type": "ValidationException",
  "message": "Invalid TableArn: Invalid ResourceArn provided as input arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateContinuousBackups

UpdateContinuousBackups enables or disables point in time recovery for the specified table.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.UpdateContinuousBackups

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
X-Amz-Target: DynamoDB_20120810.UpdateContributorInsights

{
  "TableName": "example",
  "ContributorInsightsAction": "ENABLE"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: DynamoDB_20120810.UpdateGlobalTable

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

{
  "__type": "com.amazonaws.dynamodb.v20120810#GlobalTableNotFoundException",
  "message": "Global table not found: example"
}
```

`400` `GlobalTableNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateGlobalTableSettings

Updates settings for a global table.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.UpdateGlobalTableSettings

{
  "GlobalTableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "com.amazonaws.dynamodb.v20120810#GlobalTableNotFoundException",
  "message": "Global table not found: example"
}
```

`400` `GlobalTableNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateItem

Edits an existing item's attributes, or adds a new item to the table if it does not already exist.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.UpdateItem

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
X-Amz-Target: DynamoDB_20120810.UpdateKinesisStreamingDestination

{
  "TableName": "example",
  "StreamArn": "arn:aws:dynamodb:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: DynamoDB_20120810.UpdateTable

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "At least one of ProvisionedThroughput, BillingMode, UpdateStreamEnabled, GlobalSecondaryIndexUpdates, SSESpecification, ReplicaUpdates, MultiAccountReplicaReady, ReplicaTransitRoleArn, MultiRegionConsistency, DeletionProtectionEnabled, OnDemandThroughput, WarmThroughput or TableClass is required"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateTableReplicaAutoScaling

Updates auto scaling settings on your global tables at once.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.UpdateTableReplicaAutoScaling

{
  "TableName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException",
  "message": "Requested resource not found: Table: example not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateTimeToLive

The UpdateTimeToLive method enables or disables Time to Live (TTL) for the specified table.

```http
POST / HTTP/1.1
X-Amz-Target: DynamoDB_20120810.UpdateTimeToLive

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
