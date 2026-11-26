# Amazon Simple Notification Service

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `SNS`
- **sigv4 signing name:** `sns`
- **Endpoint prefix:** `sns`
- **ARN namespace:** `sns`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `sns`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListTopics`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:sns" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-www-form-urlencoded; charset=utf-8' \
  --data-binary 'Action=ListTopics&Version=2010-03-31' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Fan a message out from an SNS topic to an SQS queue, then read it from the queue. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. Amazon Simple Queue Service: CreateQueue

An SQS queue to subscribe to the topic.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.CreateQueue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueName": "order-events"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 03ed68fe-a0b0-4f06-a6ab-aab184281ce5
content-length: 63
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "QueueUrl": "https://127.0.0.1:9297/000000000000/order-events"
}
```

### 2. Amazon Simple Queue Service: GetQueueAttributes

SNS addresses the queue by ARN, so read it back.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.GetQueueAttributes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 93
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://127.0.0.1:9297/000000000000/order-events",
  "AttributeNames": [
    "QueueArn"
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 25cc3793-c6f3-4754-a03c-6f32f2bdeb3d
content-length: 77
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "Attributes": {
    "QueueArn": "arn:aws:sqs:us-east-1:000000000000:order-events"
  }
}
```

### 3. CreateTopic

The topic's ARN is the handle every later call uses.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 55
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateTopic
&Version=2010-03-31
&Name=order-events
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 295
date: Thu, 26 Nov 2026 18:30:51 GMT

<CreateTopicResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <CreateTopicResult>
    <TopicArn>arn:aws:sns:us-east-1:000000000000:order-events</TopicArn>
  </CreateTopicResult>
  <ResponseMetadata>
    <RequestId>cb72a9d2-8d6d-4cec-a63f-1d9a2ce337c3</RequestId>
  </ResponseMetadata>
</CreateTopicResponse>
```

### 4. Amazon Simple Queue Service: SetQueueAttributes

As in AWS, SNS may only deliver into a queue whose access policy allows the topic to `sqs:SendMessage`. Without this step the publish succeeds but nothing reaches the queue.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.SetQueueAttributes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 420
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://127.0.0.1:9297/000000000000/order-events",
  "Attributes": {
    "Policy": "{\"Version\": \"2012-10-17\", \"Statement\": [{\"Effect\": \"Allow\", \"Principal\": {\"Service\": \"sns.amazonaws.com\"}, \"Action\": \"sqs:SendMessage\", \"Resource\": \"arn:aws:sqs:us-east-1:000000000000:order-events\", \"Condition\": {\"ArnEquals\": {\"aws:SourceArn\": \"arn:aws:sns:us-east-1:000000000000:order-events\"}}}]}"
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: cd351a33-381c-4e16-9c69-a17f1f80b9e6
content-length: 2
date: Thu, 26 Nov 2026 18:30:51 GMT

{}
```

### 5. Subscribe

An SQS subscription is confirmed immediately, so the response returns a real subscription ARN rather than `pending confirmation`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 209
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=Subscribe
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aorder-events
&Protocol=sqs
&Endpoint=arn%3Aaws%3Asqs%3Aus-east-1%3A000000000000%3Aorder-events
&ReturnSubscriptionArn=true
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 338
date: Thu, 26 Nov 2026 18:30:51 GMT

<SubscribeResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <SubscribeResult>
    <SubscriptionArn>arn:aws:sns:us-east-1:000000000000:order-events:0428581c-ac2f-475c-9011-2ad90c0f16cf</SubscriptionArn>
  </SubscribeResult>
  <ResponseMetadata>
    <RequestId>f5f779fb-4592-47d4-8efe-6247d244ce74</RequestId>
  </ResponseMetadata>
</SubscribeResponse>
```

### 6. Publish

Publishing returns a message id; delivery to each subscriber happens after the call returns.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 187
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=Publish
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aorder-events
&Message=%7B%22orderId%22%3A%2042%2C%20%22status%22%3A%20%22shipped%22%7D
&Subject=order
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 270
date: Thu, 26 Nov 2026 18:30:51 GMT

<PublishResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <PublishResult>
    <MessageId>726bd21d-077e-4d64-a582-736b6d8e0bab</MessageId>
  </PublishResult>
  <ResponseMetadata>
    <RequestId>13f4e3d2-51bd-4c45-ab3c-f77e9082b120</RequestId>
  </ResponseMetadata>
</PublishResponse>
```

### 7. Amazon Simple Queue Service: ReceiveMessage

The message arrives in the queue wrapped in an SNS notification envelope (`Type`, `TopicArn`, `Message`, signature fields).

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.ReceiveMessage
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 83
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://127.0.0.1:9297/000000000000/order-events",
  "WaitTimeSeconds": 2
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 798d60d7-24e6-41e4-8542-5031da45effd
content-length: 1318
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "Messages": [
    {
      "MessageId": "5f7c31ae-39ac-49c1-be0d-3b87ba79528f",
      "ReceiptHandle": "MjhhNzk5ZWMtNmFhYi00YmZhLTliYzktOWU4Yjk4YmYyNjY1IGY4ZDI0MmI1LTMwMGItNDZhMy1iMWY1LTFjYWZlZmFmZDJkNCBhcm46YXdzOnNxczp1cy1lYXN0LTE6MDAwMDAwMDAwMDAwOm9yZGVyLWV2ZW50cw==",
      "MD5OfBody": "00bc1bb8d9d20e6bceabd51b8b0aea5c",
      "Body": "{\n  \"Type\" : \"Notification\",\n  \"MessageId\" : \"726bd21d-077e-4d64-a582-736b6d8e0bab\",\n  \"TopicArn\" : \"arn:aws:sns:us-east-1:000000000000:order-events\",\n  \"Subject\" : \"order\",\n  \"Message\" : \"{\\\"orderId\\\": 42, \\\"status\\\": \\\"shipped\\\"}\",\n  \"Timestamp\" : \"2026-11-26T18:30:51.551Z\",\n  \"SignatureVersion\" : \"1\",\n  \"Signature\" : \"EeYWhxcwaD5/V4iGtGTU4w/uPnCNMbF1+RMPbUHpEApb1B9f79Njk83+TgMEsBqKK7AfT5LG5VsdXWIjKzD1g09TYpEwz/F0lsmrOJ8sXcGPMuKQU84Ng/JwF7ThIdfzb8TS8Kndw738tbpboRYfykkfFMcvjy4VlpvRfDSLweJMdzoGhbI549b2ZW04fXeW15i6hSJu499le35lmhg6zBg+JdRZSadhFmiiZCzOJZMNw7h94igjY4CpxDtMLJ8flBHlatknyLcWxTqgqm4EY7G24wyoZ0XMs84W5pcXOJMj9fwgl7pfM7Az2ykqiUEjeqrkasPXVxSVXdVQcCfwUg==\",\n  \"SigningCertURL\" : \"https://127.0.0.1:9297/SimpleNotificationService-76da023b8817678ece030582bc80e0b5.pem\",\n  \"UnsubscribeURL\" : \"https://127.0.0.1:9297/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-east-1:000000000000:order-events:0428581c-ac2f-475c-9011-2ad90c0f16cf\"\n}"
    }
  ]
}
```

### 8. Unsubscribe

Removes the subscription.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 150
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=Unsubscribe
&Version=2010-03-31
&SubscriptionArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aorder-events%3A24404949-7f8b-4991-a87f-f3e740c2aa88
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 227
date: Thu, 26 Nov 2026 18:30:51 GMT

<UnsubscribeResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <UnsubscribeResult></UnsubscribeResult>
  <ResponseMetadata>
    <RequestId>0f4576a5-98d7-47d5-bdeb-3aeb8f6f46ea</RequestId>
  </ResponseMetadata>
</UnsubscribeResponse>
```

### 9. DeleteTopic

Deletes the topic.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 104
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteTopic
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aorder-events
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 227
date: Thu, 26 Nov 2026 18:30:51 GMT

<DeleteTopicResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <DeleteTopicResult></DeleteTopicResult>
  <ResponseMetadata>
    <RequestId>a9d209bd-b33a-4e4e-b6f3-7ba93a7deb23</RequestId>
  </ResponseMetadata>
</DeleteTopicResponse>
```

### 10. Amazon Simple Queue Service: DeleteQueue

Cleans up the queue.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.DeleteQueue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 63
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://127.0.0.1:9297/000000000000/order-events"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 97bebc8e-a74d-49d7-a718-cfc17b8e8c24
content-length: 2
date: Thu, 26 Nov 2026 18:30:51 GMT

{}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 42 operations, 0 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AddPermission

Adds a statement to a topic's access control policy, granting access for the specified Amazon Web Services accounts to the specified actions.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 188
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AddPermission
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Label=example
&AWSAccountId.member.1=example
&ActionName.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 9d43fbf6-0e95-4840-a5f4-410131ba6b0e
content-length: 224
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: AddPermission&#xA;</Message>
  </Error>
  <RequestId>9d43fbf6-0e95-4840-a5f4-410131ba6b0e</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### CheckIfPhoneNumberIsOptedOut

Accepts a phone number and indicates whether the phone holder has opted out of receiving SMS messages from your Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CheckIfPhoneNumberIsOptedOut
&Version=2010-03-31
&phoneNumber=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 66363435-0d6b-49a4-b41a-2b9f7c491ab9
content-length: 239
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: CheckIfPhoneNumberIsOptedOut&#xA;</Message>
  </Error>
  <RequestId>66363435-0d6b-49a4-b41a-2b9f7c491ab9</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ConfirmSubscription

Verifies an endpoint owner's intent to receive messages by validating the token sent to the endpoint by an earlier Subscribe action.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 134
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ConfirmSubscription
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Token=<redacted>
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 58e68e79-66b4-4de2-bd54-9fb094c44a74
content-length: 230
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ConfirmSubscription&#xA;</Message>
  </Error>
  <RequestId>58e68e79-66b4-4de2-bd54-9fb094c44a74</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### CreatePlatformApplication

Creates a platform application object for one of the supported push notification services, such as APNS and GCM (Firebase Cloud Messaging), to which devices and mobile apps may register.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 146
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreatePlatformApplication
&Version=2010-03-31
&Name=example
&Platform=example
&Attributes.entry.1.key=key
&Attributes.entry.1.value=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 3c2b77b4-0236-4f60-84af-9b69f95dae9e
content-length: 236
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: CreatePlatformApplication&#xA;</Message>
  </Error>
  <RequestId>3c2b77b4-0236-4f60-84af-9b69f95dae9e</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### CreatePlatformEndpoint

Creates an endpoint for a device and mobile app on one of the supported push notification services, such as GCM (Firebase Cloud Messaging) and APNS.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 151
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreatePlatformEndpoint
&Version=2010-03-31
&PlatformApplicationArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Token=<redacted>
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 8bf56715-280a-4261-92a5-d5cf40325f89
content-length: 233
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: CreatePlatformEndpoint&#xA;</Message>
  </Error>
  <RequestId>8bf56715-280a-4261-92a5-d5cf40325f89</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### CreateSMSSandboxPhoneNumber

Adds a destination phone number to an Amazon Web Services account in the SMS sandbox and sends a one-time password (OTP) to that phone number.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 69
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateSMSSandboxPhoneNumber
&Version=2010-03-31
&PhoneNumber=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 05469283-810d-4eca-921d-35e4f7a98076
content-length: 238
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: CreateSMSSandboxPhoneNumber&#xA;</Message>
  </Error>
  <RequestId>05469283-810d-4eca-921d-35e4f7a98076</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### CreateTopic

Creates a topic to which notifications can be published.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateTopic
&Version=2010-03-31
&Name=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 875a8fdb-649e-4f1f-b483-c2b0006b0833
content-length: 222
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: CreateTopic&#xA;</Message>
  </Error>
  <RequestId>875a8fdb-649e-4f1f-b483-c2b0006b0833</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`. See [the walkthrough](#3-createtopic) for a successful call.

### DeleteEndpoint

Deletes the endpoint for a device and mobile app from Amazon SNS.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 117
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteEndpoint
&Version=2010-03-31
&EndpointArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: b3f922ce-3a07-427c-9e4c-e2266f3b59ca
content-length: 225
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: DeleteEndpoint&#xA;</Message>
  </Error>
  <RequestId>b3f922ce-3a07-427c-9e4c-e2266f3b59ca</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### DeletePlatformApplication

Deletes a platform application object for one of the supported push notification services, such as APNS and GCM (Firebase Cloud Messaging).

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 139
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeletePlatformApplication
&Version=2010-03-31
&PlatformApplicationArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: ae9fd888-2e5b-4eef-b45f-2fe9041e70b5
content-length: 236
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: DeletePlatformApplication&#xA;</Message>
  </Error>
  <RequestId>ae9fd888-2e5b-4eef-b45f-2fe9041e70b5</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### DeleteSMSSandboxPhoneNumber

Deletes an Amazon Web Services account's verified or pending phone number from the SMS sandbox.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 69
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteSMSSandboxPhoneNumber
&Version=2010-03-31
&PhoneNumber=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 1d1651d0-a064-4a9b-8d05-8009d31b67c7
content-length: 238
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: DeleteSMSSandboxPhoneNumber&#xA;</Message>
  </Error>
  <RequestId>1d1651d0-a064-4a9b-8d05-8009d31b67c7</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### DeleteTopic

Deletes a topic and all its subscriptions.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 111
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteTopic
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: bfd2f96f-8884-4bd6-9e5b-5d0c6743fe3d
content-length: 222
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: DeleteTopic&#xA;</Message>
  </Error>
  <RequestId>bfd2f96f-8884-4bd6-9e5b-5d0c6743fe3d</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`. See [the walkthrough](#9-deletetopic) for a successful call.

### GetDataProtectionPolicy

Amazon SNS message data protection is no longer available to new customers.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 126
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetDataProtectionPolicy
&Version=2010-03-31
&ResourceArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 90897381-7bf9-4ff2-8b0c-80ebd8c9105a
content-length: 234
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: GetDataProtectionPolicy&#xA;</Message>
  </Error>
  <RequestId>90897381-7bf9-4ff2-8b0c-80ebd8c9105a</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### GetEndpointAttributes

Retrieves the endpoint attributes for a device on one of the supported push notification services, such as GCM (Firebase Cloud Messaging) and APNS.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 124
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetEndpointAttributes
&Version=2010-03-31
&EndpointArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 4e01001d-687a-443b-9017-bbda56d62dbd
content-length: 232
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: GetEndpointAttributes&#xA;</Message>
  </Error>
  <RequestId>4e01001d-687a-443b-9017-bbda56d62dbd</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### GetPlatformApplicationAttributes

Retrieves the attributes of the platform application object for the supported push notification services, such as APNS and GCM (Firebase Cloud Messaging).

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 146
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetPlatformApplicationAttributes
&Version=2010-03-31
&PlatformApplicationArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: f1b4e816-2f77-4f25-96ca-f5e2b76bfa2a
content-length: 243
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: GetPlatformApplicationAttributes&#xA;</Message>
  </Error>
  <RequestId>f1b4e816-2f77-4f25-96ca-f5e2b76bfa2a</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### GetSMSAttributes

Returns the settings for sending SMS messages from your Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetSMSAttributes
&Version=2010-03-31
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 28ac00bf-0a4f-4c5a-9d35-8d37e9d291dc
content-length: 227
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: GetSMSAttributes&#xA;</Message>
  </Error>
  <RequestId>28ac00bf-0a4f-4c5a-9d35-8d37e9d291dc</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### GetSMSSandboxAccountStatus

Retrieves the SMS sandbox status for the calling Amazon Web Services account in the target Amazon Web Services Region.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetSMSSandboxAccountStatus
&Version=2010-03-31
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 4046174b-7d91-46fc-a117-7a4d3603af22
content-length: 237
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: GetSMSSandboxAccountStatus&#xA;</Message>
  </Error>
  <RequestId>4046174b-7d91-46fc-a117-7a4d3603af22</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### GetSubscriptionAttributes

Returns all of the properties of a subscription.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 132
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetSubscriptionAttributes
&Version=2010-03-31
&SubscriptionArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 33d20741-a514-4ee7-a6f4-0176160a0866
content-length: 236
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: GetSubscriptionAttributes&#xA;</Message>
  </Error>
  <RequestId>33d20741-a514-4ee7-a6f4-0176160a0866</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### GetTopicAttributes

Returns all of the properties of a topic.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 118
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetTopicAttributes
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 0074ccf4-d8fa-4fd2-aa9c-f3b21b9494d1
content-length: 229
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: GetTopicAttributes&#xA;</Message>
  </Error>
  <RequestId>0074ccf4-d8fa-4fd2-aa9c-f3b21b9494d1</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListEndpointsByPlatformApplication

Lists the endpoints and endpoint attributes for devices in a supported push notification service, such as GCM (Firebase Cloud Messaging) and APNS.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 148
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListEndpointsByPlatformApplication
&Version=2010-03-31
&PlatformApplicationArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: f7c0fbc9-36bf-44a7-b6eb-fab0cfd6ae01
content-length: 245
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListEndpointsByPlatformApplication&#xA;</Message>
  </Error>
  <RequestId>f7c0fbc9-36bf-44a7-b6eb-fab0cfd6ae01</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListOriginationNumbers

Lists the calling Amazon Web Services account's dedicated origination numbers and their metadata.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListOriginationNumbers
&Version=2010-03-31
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 797353bc-30b2-4c2d-83aa-2989e7f1d640
content-length: 233
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListOriginationNumbers&#xA;</Message>
  </Error>
  <RequestId>797353bc-30b2-4c2d-83aa-2989e7f1d640</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListPhoneNumbersOptedOut

Returns a list of phone numbers that are opted out, meaning you cannot send SMS messages to them.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListPhoneNumbersOptedOut
&Version=2010-03-31
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 4e64fb59-d33a-469a-b13e-75bb74fd28c7
content-length: 235
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListPhoneNumbersOptedOut&#xA;</Message>
  </Error>
  <RequestId>4e64fb59-d33a-469a-b13e-75bb74fd28c7</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListPlatformApplications

Lists the platform application objects for the supported push notification services, such as APNS and GCM (Firebase Cloud Messaging).

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListPlatformApplications
&Version=2010-03-31
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: dd51fee5-07bd-4319-ba2d-78ba3990dc07
content-length: 235
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListPlatformApplications&#xA;</Message>
  </Error>
  <RequestId>dd51fee5-07bd-4319-ba2d-78ba3990dc07</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListSMSSandboxPhoneNumbers

Lists the calling Amazon Web Services account's current verified and pending destination phone numbers in the SMS sandbox.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListSMSSandboxPhoneNumbers
&Version=2010-03-31
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: ad83d41a-c02b-4ab5-a467-d043f08c8000
content-length: 237
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListSMSSandboxPhoneNumbers&#xA;</Message>
  </Error>
  <RequestId>ad83d41a-c02b-4ab5-a467-d043f08c8000</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListSubscriptions

Returns a list of the requester's subscriptions.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListSubscriptions
&Version=2010-03-31
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: d7fb0c30-6ae3-4d45-8342-22d45d586507
content-length: 228
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListSubscriptions&#xA;</Message>
  </Error>
  <RequestId>d7fb0c30-6ae3-4d45-8342-22d45d586507</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListSubscriptionsByTopic

Returns a list of the subscriptions to a specific topic.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 124
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListSubscriptionsByTopic
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: e2d0b19f-f356-455d-b56e-7cd3ec46a72b
content-length: 235
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListSubscriptionsByTopic&#xA;</Message>
  </Error>
  <RequestId>e2d0b19f-f356-455d-b56e-7cd3ec46a72b</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListTagsForResource

List all tags added to the specified Amazon SNS topic.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 122
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListTagsForResource
&Version=2010-03-31
&ResourceArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: de458cbf-45ce-4e10-aa4f-632b860cf997
content-length: 230
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListTagsForResource&#xA;</Message>
  </Error>
  <RequestId>de458cbf-45ce-4e10-aa4f-632b860cf997</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### ListTopics

Returns a list of the requester's topics.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 37
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListTopics
&Version=2010-03-31
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 9b69ad88-e47b-4539-856c-efaf332ea3d0
content-length: 221
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: ListTopics&#xA;</Message>
  </Error>
  <RequestId>9b69ad88-e47b-4539-856c-efaf332ea3d0</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### OptInPhoneNumber

Use this request to opt in a phone number that is opted out, which enables you to resume sending SMS messages to the number.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 64
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=OptInPhoneNumber
&Version=2010-03-31
&phoneNumber=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 675f2bc1-0545-4cd9-b107-dc4acf4d2023
content-length: 227
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: OptInPhoneNumber&#xA;</Message>
  </Error>
  <RequestId>675f2bc1-0545-4cd9-b107-dc4acf4d2023</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### Publish

Sends a message to an Amazon SNS topic, a text message (SMS message) directly to a phone number, or a message to a mobile platform endpoint (when you specify the TargetArn ).

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=Publish
&Version=2010-03-31
&Message=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: d302383c-98b2-4be5-8a2c-905e402f764e
content-length: 218
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: Publish&#xA;</Message>
  </Error>
  <RequestId>d302383c-98b2-4be5-8a2c-905e402f764e</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`. See [the walkthrough](#6-publish) for a successful call.

### PublishBatch

Publishes up to 10 messages to the specified topic in a single batch.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 213
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PublishBatch
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&PublishBatchRequestEntries.member.1.Id=example
&PublishBatchRequestEntries.member.1.Message=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 9bc5b5b7-19bb-4664-bec1-e9830b6b4662
content-length: 223
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: PublishBatch&#xA;</Message>
  </Error>
  <RequestId>9bc5b5b7-19bb-4664-bec1-e9830b6b4662</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### PutDataProtectionPolicy

Amazon SNS message data protection is no longer available to new customers.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 156
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PutDataProtectionPolicy
&Version=2010-03-31
&ResourceArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&DataProtectionPolicy=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 2117c3df-9df6-48f7-b86e-03fc135967c4
content-length: 234
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: PutDataProtectionPolicy&#xA;</Message>
  </Error>
  <RequestId>2117c3df-9df6-48f7-b86e-03fc135967c4</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### RemovePermission

Removes a statement from a topic's access control policy.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 131
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RemovePermission
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Label=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: fea9502b-8488-4ec0-b38d-81b95f5740e1
content-length: 227
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: RemovePermission&#xA;</Message>
  </Error>
  <RequestId>fea9502b-8488-4ec0-b38d-81b95f5740e1</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### SetEndpointAttributes

Sets the attributes for an endpoint for a device on one of the supported push notification services, such as GCM (Firebase Cloud Messaging) and APNS.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 186
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetEndpointAttributes
&Version=2010-03-31
&EndpointArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Attributes.entry.1.key=key
&Attributes.entry.1.value=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: c97ba0d9-5e47-45ae-95ac-a4eedaa3f26b
content-length: 232
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: SetEndpointAttributes&#xA;</Message>
  </Error>
  <RequestId>c97ba0d9-5e47-45ae-95ac-a4eedaa3f26b</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### SetPlatformApplicationAttributes

Sets the attributes of the platform application object for the supported push notification services, such as APNS and GCM (Firebase Cloud Messaging).

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 208
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetPlatformApplicationAttributes
&Version=2010-03-31
&PlatformApplicationArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Attributes.entry.1.key=key
&Attributes.entry.1.value=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 624952c5-9c6f-402f-a12f-916e1fc241e2
content-length: 243
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: SetPlatformApplicationAttributes&#xA;</Message>
  </Error>
  <RequestId>624952c5-9c6f-402f-a12f-916e1fc241e2</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### SetSMSAttributes

Use this request to set the default settings for sending SMS messages and receiving daily SMS usage reports.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 105
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetSMSAttributes
&Version=2010-03-31
&attributes.entry.1.key=key
&attributes.entry.1.value=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: c94d3fee-cec9-4544-83ad-c545ceb7f2ec
content-length: 227
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: SetSMSAttributes&#xA;</Message>
  </Error>
  <RequestId>c94d3fee-cec9-4544-83ad-c545ceb7f2ec</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### SetSubscriptionAttributes

Allows a subscription owner to set an attribute of the subscription to a new value.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 155
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetSubscriptionAttributes
&Version=2010-03-31
&SubscriptionArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&AttributeName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 1421e433-c022-4d89-aa0a-930b958eed3f
content-length: 236
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: SetSubscriptionAttributes&#xA;</Message>
  </Error>
  <RequestId>1421e433-c022-4d89-aa0a-930b958eed3f</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### SetTopicAttributes

Allows a topic owner to set an attribute of the topic to a new value.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 141
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetTopicAttributes
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&AttributeName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 42e30727-ae21-4492-800c-8df67a299eb4
content-length: 229
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: SetTopicAttributes&#xA;</Message>
  </Error>
  <RequestId>42e30727-ae21-4492-800c-8df67a299eb4</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### Subscribe

Subscribes an endpoint to an Amazon SNS topic.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 127
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=Subscribe
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Protocol=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: b261f77d-8816-481f-9335-2ac9b300217b
content-length: 220
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: Subscribe&#xA;</Message>
  </Error>
  <RequestId>b261f77d-8816-481f-9335-2ac9b300217b</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`. See [the walkthrough](#5-subscribe) for a successful call.

### TagResource

Add tags to the specified Amazon SNS topic.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 170
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=TagResource
&Version=2010-03-31
&ResourceArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 693d1b58-1dfb-4c35-b284-d6c619057af1
content-length: 222
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: TagResource&#xA;</Message>
  </Error>
  <RequestId>693d1b58-1dfb-4c35-b284-d6c619057af1</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### Unsubscribe

Deletes a subscription.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 118
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=Unsubscribe
&Version=2010-03-31
&SubscriptionArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 774514be-8657-4574-bd14-4b1737d06df3
content-length: 222
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: Unsubscribe&#xA;</Message>
  </Error>
  <RequestId>774514be-8657-4574-bd14-4b1737d06df3</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`. See [the walkthrough](#8-unsubscribe) for a successful call.

### UntagResource

Remove tags from the specified Amazon SNS topic.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 142
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UntagResource
&Version=2010-03-31
&ResourceArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&TagKeys.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: bcdef34f-19ef-440d-b2d9-5aa2c26d4b12
content-length: 224
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: UntagResource&#xA;</Message>
  </Error>
  <RequestId>bcdef34f-19ef-440d-b2d9-5aa2c26d4b12</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### VerifySMSSandboxPhoneNumber

Verifies a destination phone number with a one-time password (OTP) for the calling Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 92
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=VerifySMSSandboxPhoneNumber
&Version=2010-03-31
&PhoneNumber=1
&OneTimePassword=10000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: ce6cb60a-ffaf-404f-8376-3ba9a7e498d5
content-length: 238
date: Thu, 26 Nov 2026 18:30:51 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>unknown action: VerifySMSSandboxPhoneNumber&#xA;</Message>
  </Error>
  <RequestId>ce6cb60a-ffaf-404f-8376-3ba9a7e498d5</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/sns/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with a form-encoded body: `Action` names the operation, `Version` the API version, and every input member is a flattened key such as `Tags.member.1.Key`.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon Simple Notification Service answers in AWS: XML wrapped in `<OperationResponse><OperationResult>`. Errors captured on this page are an XML document rooted at `<ErrorResponse>`.

### Other services involved

The walkthrough also called [Amazon Simple Queue Service](sqs.md). They are separate services behind the same endpoint, sharing one account and one in-memory engine, so a resource created through one is immediately visible to the other.
