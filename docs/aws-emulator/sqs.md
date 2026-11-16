# Amazon Simple Queue Service

## Wire protocol

- **Protocol:** `awsJson1_0` (also `awsQueryCompatible`)
- **Request routing:** `POST /` with header `X-Amz-Target: AmazonSQS.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `SQS`
- **sigv4 signing name:** `sqs`
- **Endpoint prefix:** `sqs`
- **ARN namespace:** `sqs`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `sqs`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed.

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
X-Amz-Date: 20261116T120450Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
x-amzn-requestid: 085d5456-66c5-4d05-9ee0-a713db63ccf5
content-length: 70
date: Mon, 16 Nov 2026 12:04:50 GMT

{
  "QueueUrl": "https://sqs.us-east-1.amazonaws.com/000000000000/orders"
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
X-Amz-Date: 20261116T120450Z
Content-Length: 107
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://sqs.us-east-1.amazonaws.com/000000000000/orders",
  "MessageBody": "{\"orderId\": 42}"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 106
date: Mon, 16 Nov 2026 12:04:50 GMT

{
  "MD5OfMessageBody": "654fc194a6077496f7f45fa26024c821",
  "MessageId": "e6667fb6-29d1-40e8-8773-ec0d5ad64c34"
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
X-Amz-Date: 20261116T120450Z
Content-Length: 119
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://sqs.us-east-1.amazonaws.com/000000000000/orders",
  "MaxNumberOfMessages": 1,
  "WaitTimeSeconds": 1
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 316
date: Mon, 16 Nov 2026 12:04:50 GMT

{
  "Messages": [
    {
      "MessageId": "e6667fb6-29d1-40e8-8773-ec0d5ad64c34",
      "ReceiptHandle": "MWJjNjdhMmQtOWJlMS00NjNiLWE2YmEtYTQzMzliYjZiYzVkIGI1YTYzY2VlLTllNTMtNDQ4My04OGE5LTE2ZjFjMGFiZjMzMCBhcm46YXdzOnNxczp1cy1lYXN0LTE6MDAwMDAwMDAwMDAwOm9yZGVycw==",
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
X-Amz-Date: 20261116T120450Z
Content-Length: 248
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://sqs.us-east-1.amazonaws.com/000000000000/orders",
  "ReceiptHandle": "MWJjNjdhMmQtOWJlMS00NjNiLWE2YmEtYTQzMzliYjZiYzVkIGI1YTYzY2VlLTllNTMtNDQ4My04OGE5LTE2ZjFjMGFiZjMzMCBhcm46YXdzOnNxczp1cy1lYXN0LTE6MDAwMDAwMDAwMDAwOm9yZGVycw=="
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 2
date: Mon, 16 Nov 2026 12:04:50 GMT

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
X-Amz-Date: 20261116T120450Z
Content-Length: 98
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://sqs.us-east-1.amazonaws.com/000000000000/orders",
  "AttributeNames": [
    "All"
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 1e56e09a-824e-491d-99ee-e93b738230d8
content-length: 434
date: Mon, 16 Nov 2026 12:04:50 GMT

{
  "Attributes": {
    "VisibilityTimeout": "30",
    "LastModifiedTimestamp": "1794830690",
    "ApproximateNumberOfMessagesDelayed": "0",
    "ApproximateNumberOfMessagesNotVisible": "0",
    "CreatedTimestamp": "1794830690",
    "ApproximateNumberOfMessages": "0",
    "MaximumMessageSize": "1048576",
    "QueueArn": "arn:aws:sqs:us-east-1:000000000000:orders",
    "ReceiveMessageWaitTimeSeconds": "0",
    "SqsManagedSseEnabled": "true",
    "DelaySeconds": "0",
    "MessageRetentionPeriod": "345600"
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
X-Amz-Date: 20261116T120450Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://sqs.us-east-1.amazonaws.com/000000000000/orders"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 38767638-4012-4b80-ba3a-a42ee886c3e3
content-length: 2
date: Mon, 16 Nov 2026 12:04:50 GMT

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
X-Amz-Date: 20261116T120450Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueName": "orders"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
x-amzn-requestid: 91f3b628-359d-4bf1-b973-4301f53aa2af
content-length: 96
date: Mon, 16 Nov 2026 12:04:50 GMT

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 23 operations, 3 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AddPermission

Adds a permission to a queue for a specific principal .

```http
POST / HTTP/1.1
X-Amz-Target: AmazonSQS.AddPermission

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
X-Amz-Target: AmazonSQS.CancelMessageMoveTask

{
  "TaskHandle": "example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/x-amz-json-1.0

{
  "__type": "com.amazonaws.sqs#ResourceNotFoundException",
  "message": "Task does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ChangeMessageVisibility

Changes the visibility timeout of a specified message in a queue to a new value.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonSQS.ChangeMessageVisibility

{
  "QueueUrl": "example",
  "ReceiptHandle": "example",
  "VisibilityTimeout": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: AmazonSQS.ChangeMessageVisibilityBatch

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
X-Amz-Target: AmazonSQS.CreateQueue

{
  "QueueName": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

{
  "QueueUrl": "https://sqs.us-east-1.amazonaws.com/000000000000/example"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#1-createqueue) for a successful call.

### DeleteMessage

Deletes the specified message from the specified queue.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonSQS.DeleteMessage

{
  "QueueUrl": "example",
  "ReceiptHandle": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#4-deletemessage) for a successful call.

### DeleteMessageBatch

Deletes up to ten messages from the specified queue.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonSQS.DeleteMessageBatch

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

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteQueue

Deletes the queue specified by the QueueUrl , regardless of the queue's contents.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonSQS.DeleteQueue

{
  "QueueUrl": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "com.amazonaws.sqs#QueueDoesNotExist",
  "message": "The specified queue does not exist."
}
```

`400` `QueueDoesNotExist`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#6-deletequeue) for a successful call.

### GetQueueAttributes

Gets attributes for the specified queue.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonSQS.GetQueueAttributes

{
  "QueueUrl": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: AmazonSQS.GetQueueUrl

{
  "QueueName": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

{
  "QueueUrl": "https://sqs.us-east-1.amazonaws.com/000000000000/example"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListDeadLetterSourceQueues

Returns a list of your queues that have the RedrivePolicy queue attribute configured with a dead-letter queue.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonSQS.ListDeadLetterSourceQueues

{
  "QueueUrl": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: AmazonSQS.ListMessageMoveTasks

{
  "SourceArn": "arn:aws:sqs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/x-amz-json-1.0

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
X-Amz-Target: AmazonSQS.ListQueueTags

{
  "QueueUrl": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: AmazonSQS.ListQueues

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

{
  "QueueUrls": [
    "https://sqs.us-east-1.amazonaws.com/000000000000/example"
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### PurgeQueue

Deletes available messages in a queue (including in-flight messages) specified by the QueueURL parameter.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonSQS.PurgeQueue

{
  "QueueUrl": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: AmazonSQS.ReceiveMessage

{
  "QueueUrl": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: AmazonSQS.RemovePermission

{
  "QueueUrl": "example",
  "Label": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: AmazonSQS.SendMessage

{
  "QueueUrl": "example",
  "MessageBody": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: AmazonSQS.SendMessageBatch

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
X-Amz-Target: AmazonSQS.SetQueueAttributes

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
X-Amz-Target: AmazonSQS.StartMessageMoveTask

{
  "SourceArn": "arn:aws:sqs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/x-amz-json-1.0

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
X-Amz-Target: AmazonSQS.TagQueue

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
X-Amz-Target: AmazonSQS.UntagQueue

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
