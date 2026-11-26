# Amazon Simple Queue Service

## Wire protocol

- **Protocol:** `awsJson1_0` (also `awsQueryCompatible`)
- **Request routing:** `POST /` with header `X-Amz-Target: AmazonSQS.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `SQS`
- **sigv4 signing name:** `sqs`
- **Endpoint prefix:** `sqs`
- **ARN namespace:** `sqs`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `sqs`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListQueues`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:sqs" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.0' \
  -H 'X-Amz-Target: AmazonSQS.ListQueues' \
  --data-binary '{}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create a queue, send a message, receive it, delete it, then delete the queue. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateQueue

The queue is created and addressed from now on by the URL the response returns, not by its name.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.CreateQueue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 62
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueName": "orders",
  "Attributes": {
    "VisibilityTimeout": "30"
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 29babfc6-1dfd-495d-abaf-69bb2c2a6eb5
content-length: 57
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "QueueUrl": "https://127.0.0.1:9297/000000000000/orders"
}
```

### 2. SendMessage

The response carries the message id and the MD5 of the body, which a client compares against its own to detect corruption.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.SendMessage
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 91
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://127.0.0.1:9297/000000000000/orders",
  "MessageBody": "{\"orderId\": 42}"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: f83e4318-d291-4cc8-96c1-8ad177cc4116
content-length: 106
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "MD5OfMessageBody": "654fc194a6077496f7f45fa26024c821",
  "MessageId": "ff3e73f6-7d5e-432b-a170-5ca4db683eed"
}
```

### 3. ReceiveMessage

Receiving does not remove the message: it becomes invisible for the visibility timeout and comes with a receipt handle, the token a delete needs.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.ReceiveMessage
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 101
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://127.0.0.1:9297/000000000000/orders",
  "MaxNumberOfMessages": 1,
  "WaitTimeSeconds": 1
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: fb1115e6-0191-4cb9-bfa4-1ad2cf04025a
content-length: 316
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "Messages": [
    {
      "MessageId": "ff3e73f6-7d5e-432b-a170-5ca4db683eed",
      "ReceiptHandle": "ZmMzYWRjYzctN2Q0NC00MjNhLWJjYzgtYTZiZTdiMjQ1MTBkIDNlNTI2YzQzLWI5MWItNGI2Ni05OTQzLTlmNDMxNDZhMWRmMyBhcm46YXdzOnNxczp1cy1lYXN0LTE6MDAwMDAwMDAwMDAwOm9yZGVycw==",
      "MD5OfBody": "654fc194a6077496f7f45fa26024c821",
      "Body": "{\"orderId\": 42}"
    }
  ]
}
```

### 4. DeleteMessage

Deleting with the receipt handle acknowledges the message; an empty 200 is success.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.DeleteMessage
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 232
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://127.0.0.1:9297/000000000000/orders",
  "ReceiptHandle": "ZmMzYWRjYzctN2Q0NC00MjNhLWJjYzgtYTZiZTdiMjQ1MTBkIDNlNTI2YzQzLWI5MWItNGI2Ni05OTQzLTlmNDMxNDZhMWRmMyBhcm46YXdzOnNxczp1cy1lYXN0LTE6MDAwMDAwMDAwMDAwOm9yZGVycw=="
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: c0480ec0-901e-4120-aa6d-e9b5197e7bbc
content-length: 2
date: Thu, 26 Nov 2026 18:30:51 GMT

{}
```

### 5. GetQueueAttributes

The attributes report the queue's ARN, its configuration and its live message counts.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.GetQueueAttributes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 82
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://127.0.0.1:9297/000000000000/orders",
  "AttributeNames": [
    "All"
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 58e09dd9-e102-4c10-af02-4d754b748271
content-length: 434
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "Attributes": {
    "SqsManagedSseEnabled": "true",
    "LastModifiedTimestamp": "1795717851",
    "CreatedTimestamp": "1795717851",
    "MaximumMessageSize": "1048576",
    "MessageRetentionPeriod": "345600",
    "QueueArn": "arn:aws:sqs:us-east-1:000000000000:orders",
    "DelaySeconds": "0",
    "ReceiveMessageWaitTimeSeconds": "0",
    "VisibilityTimeout": "30",
    "ApproximateNumberOfMessagesNotVisible": "0",
    "ApproximateNumberOfMessagesDelayed": "0",
    "ApproximateNumberOfMessages": "0"
  }
}
```

### 6. DeleteQueue

The queue and any messages left in it are removed.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.DeleteQueue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 57
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://127.0.0.1:9297/000000000000/orders"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: e5464e80-f7b9-4121-a859-2890b26635ab
content-length: 2
date: Thu, 26 Nov 2026 18:30:51 GMT

{}
```

### 7. GetQueueUrl

Looking the deleted queue up again fails with `QueueDoesNotExist`; `__type` carries the error's fully qualified shape id.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.GetQueueUrl
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueName": "orders"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 0cb9d2e4-6b21-4663-bdb5-944153dbd22a
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 23 operations, 4 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AddPermission

Adds a permission to a queue for a specific principal .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.AddPermission
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 90
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example",
  "Label": "example",
  "AWSAccountIds": [
    "example"
  ],
  "Actions": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 7edbf66b-4101-46f3-8048-02f389ce3807
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CancelMessageMoveTask

Cancels a specified message movement task.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.CancelMessageMoveTask
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TaskHandle": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 84980a2a-3992-44d1-bb52-84682a869672
x-amzn-query-error: InvalidParameterValue;Sender
content-length: 123
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazon.coral.service#InvalidParameterValueException",
  "message": "Value for parameter TaskHandle is invalid."
}
```

`400` `InvalidParameterValueException`: the placeholder values failed the operation's input validation.

### ChangeMessageVisibility

Changes the visibility timeout of a specified message in a queue to a new value.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.ChangeMessageVisibility
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example",
  "ReceiptHandle": "example",
  "VisibilityTimeout": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
x-amzn-requestid: aef4e0e0-f86d-4c89-a10b-a7e7635a1f32
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ChangeMessageVisibilityBatch

Changes the visibility timeout of multiple messages.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.ChangeMessageVisibilityBatch
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example",
  "Entries": [
    {
      "Id": "example",
      "ReceiptHandle": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
x-amzn-requestid: c25b1775-d1fb-4aae-9504-164d47e3e068
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateQueue

Creates a new standard or FIFO queue.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.CreateQueue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueName": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: b3e741ba-f780-4fa6-8f8f-06d9ac8c9b91
content-length: 58
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "QueueUrl": "https://127.0.0.1:9297/000000000000/example"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#1-createqueue) for a successful call.

### DeleteMessage

Deletes the specified message from the specified queue.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.DeleteMessage
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 48
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example",
  "ReceiptHandle": "example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/x-amz-json-1.0
x-amzn-query-error: ReceiptHandleIsInvalid;Sender
x-amzn-requestid: a1c0fbce-d277-4c79-bc8c-1e79bedeaec9
content-length: 102
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#ReceiptHandleIsInvalid",
  "message": "The input receipt handle is invalid."
}
```

`404` `ReceiptHandleIsInvalid`: the placeholder values failed the operation's input validation. See [the walkthrough](#4-deletemessage) for a successful call.

### DeleteMessageBatch

Deletes up to ten messages from the specified queue.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.DeleteMessageBatch
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example",
  "Entries": [
    {
      "Id": "example",
      "ReceiptHandle": "example"
    }
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 44ed229f-de58-41c2-8ad7-6ca12de8dd7d
content-length: 145
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "Successful": [],
  "Failed": [
    {
      "Id": "example",
      "SenderFault": true,
      "Code": "ReceiptHandleIsInvalid",
      "Message": "The input receipt handle is invalid."
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteQueue

Deletes the queue specified by the QueueUrl , regardless of the queue's contents.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.DeleteQueue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 3848d42c-1792-4380-a0eb-20d9ad9ec593
content-length: 2
date: Thu, 26 Nov 2026 18:30:51 GMT

{}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#6-deletequeue) for a successful call.

### GetQueueAttributes

Gets attributes for the specified queue.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.GetQueueAttributes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: d2d326c0-3d33-443b-b937-6a2aa2caa00c
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#5-getqueueattributes) for a successful call.

### GetQueueUrl

The GetQueueUrl API returns the URL of an existing Amazon SQS queue.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.GetQueueUrl
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: aa40b1bd-1331-4365-929b-342f05230931
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListDeadLetterSourceQueues

Returns a list of your queues that have the RedrivePolicy queue attribute configured with a dead-letter queue.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.ListDeadLetterSourceQueues
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: d476c8a0-f531-4888-8a8a-181e3f4a611d
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListMessageMoveTasks

Gets the most recent message movement tasks (up to 10) under a specific source queue.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.ListMessageMoveTasks
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SourceArn": "arn:aws:sqs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/x-amz-json-1.0
x-amzn-requestid: 4c222bb8-d5c8-4066-88ea-410fcc99e303
x-amzn-query-error: ResourceNotFoundException;Sender
content-length: 143
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#ResourceNotFoundException",
  "message": "The resource that you specified for the SourceArn parameter doesn't exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListQueueTags

List all cost allocation tags added to the specified Amazon SQS queue.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.ListQueueTags
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 00a5278b-ef93-4285-ae93-f6157f843f9b
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListQueues

Returns a list of your queues in the current region.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.ListQueues
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 442da32f-0240-4743-9720-82c53a5deea2
content-length: 2
date: Thu, 26 Nov 2026 18:30:51 GMT

{}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### PurgeQueue

Deletes available messages in a queue (including in-flight messages) specified by the QueueURL parameter.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.PurgeQueue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 5eb384fa-f798-4fe5-8b8e-f32275928443
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ReceiveMessage

Retrieves one or more messages (up to 10), from the specified queue.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.ReceiveMessage
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
x-amzn-requestid: 4ab8507c-757b-434d-b5ce-5c9c105e19ae
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#3-receivemessage) for a successful call.

### RemovePermission

Revokes any permissions in the queue policy that matches the specified Label parameter.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.RemovePermission
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 40
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example",
  "Label": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: a055355e-a762-4ed5-919f-fc91b6dc5cc1
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### SendMessage

Delivers a message to the specified queue.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.SendMessage
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example",
  "MessageBody": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
x-amzn-requestid: 5890e2be-4ec1-476d-b466-fb5c3605a1db
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#2-sendmessage) for a successful call.

### SendMessageBatch

You can use SendMessageBatch to send up to 10 messages to the specified queue by assigning either identical or different values to each message (or by not assigning values at all).

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.SendMessageBatch
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example",
  "Entries": [
    {
      "Id": "example",
      "MessageBody": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
x-amzn-requestid: 6ba44906-aa7d-4a41-ae12-0a31e37b7e47
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### SetQueueAttributes

Sets the value of one or more queue attributes, like a policy.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.SetQueueAttributes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example",
  "Attributes": {
    "key": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 8bba09b7-0657-41aa-8c46-b0f648e42b15
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StartMessageMoveTask

Starts an asynchronous task to move messages from a specified source queue to a specified destination queue.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.StartMessageMoveTask
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SourceArn": "arn:aws:sqs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/x-amz-json-1.0
x-amzn-requestid: 18a52047-947e-4825-8b77-649c3740e2ff
x-amzn-query-error: ResourceNotFoundException;Sender
content-length: 143
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#ResourceNotFoundException",
  "message": "The resource that you specified for the SourceArn parameter doesn't exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagQueue

Add cost allocation tags to the specified Amazon SQS queue.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.TagQueue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example",
  "Tags": {
    "key": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 51f0662b-1717-4797-874e-6badabd2d0e7
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagQueue

Remove cost allocation tags from the specified Amazon SQS queue.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.UntagQueue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "example",
  "TagKeys": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: c95d60c4-fedc-47a9-84e9-97dd7afedc33
x-amzn-query-error: AWS.SimpleQueueService.NonExistentQueue;Sender
content-length: 96
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/sqs/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon Simple Queue Service answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `message`.
