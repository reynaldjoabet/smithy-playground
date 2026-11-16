# Amazon Simple Notification Service

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `SNS`
- **sigv4 signing name:** `sns`
- **Endpoint prefix:** `sns`
- **ARN namespace:** `sns`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `sns`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

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
X-Amz-Date: 20261116T120521Z
Content-Length: 29
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueName": "order-events"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 4aa8cc81-2276-4377-8e8e-a2493c841d47
content-length: 76
date: Mon, 16 Nov 2026 12:05:21 GMT

{
  "QueueUrl": "https://sqs.us-east-1.amazonaws.com/000000000000/order-events"
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
X-Amz-Date: 20261116T120521Z
Content-Length: 109
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://sqs.us-east-1.amazonaws.com/000000000000/order-events",
  "AttributeNames": [
    "QueueArn"
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: b1853262-6d5f-4ce7-b9f0-5e4cbbf70102
content-length: 77
date: Mon, 16 Nov 2026 12:05:21 GMT

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
X-Amz-Date: 20261116T120521Z
Content-Length: 55
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateTopic
&Version=2010-03-31
&Name=order-events
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 295
date: Mon, 16 Nov 2026 12:05:21 GMT

<CreateTopicResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <CreateTopicResult>
    <TopicArn>arn:aws:sns:us-east-1:000000000000:order-events</TopicArn>
  </CreateTopicResult>
  <ResponseMetadata>
    <RequestId>325450d9-eefd-4abf-bb8a-b8e877fe204b</RequestId>
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
X-Amz-Date: 20261116T120521Z
Content-Length: 437
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://sqs.us-east-1.amazonaws.com/000000000000/order-events",
  "Attributes": {
    "Policy": "{\"Version\": \"2012-10-17\", \"Statement\": [{\"Effect\": \"Allow\", \"Principal\": {\"Service\": \"sns.amazonaws.com\"}, \"Action\": \"sqs:SendMessage\", \"Resource\": \"arn:aws:sqs:us-east-1:000000000000:order-events\", \"Condition\": {\"ArnEquals\": {\"aws:SourceArn\": \"arn:aws:sns:us-east-1:000000000000:order-events\"}}}]}"
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: a09fef60-e7d3-45af-a12e-40d0bde38bb3
content-length: 2
date: Mon, 16 Nov 2026 12:05:21 GMT

{}
```

### 5. Subscribe

An SQS subscription is confirmed immediately, so the response returns a real subscription ARN rather than `pending confirmation`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120521Z
Content-Length: 209
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
date: Mon, 16 Nov 2026 12:05:21 GMT

<SubscribeResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <SubscribeResult>
    <SubscriptionArn>arn:aws:sns:us-east-1:000000000000:order-events:24404949-7f8b-4991-a87f-f3e740c2aa88</SubscriptionArn>
  </SubscribeResult>
  <ResponseMetadata>
    <RequestId>9d80edaf-9bf0-464d-887b-94898d53c150</RequestId>
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
X-Amz-Date: 20261116T120521Z
Content-Length: 187
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
date: Mon, 16 Nov 2026 12:05:21 GMT

<PublishResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <PublishResult>
    <MessageId>1f624d5b-3ac8-4434-a561-670e0a4e3e81</MessageId>
  </PublishResult>
  <ResponseMetadata>
    <RequestId>ce8e50ed-1c90-4392-a5e8-9657db3a2346</RequestId>
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
X-Amz-Date: 20261116T120521Z
Content-Length: 99
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://sqs.us-east-1.amazonaws.com/000000000000/order-events",
  "WaitTimeSeconds": 2
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 1601
date: Mon, 16 Nov 2026 12:05:21 GMT

{
  "Messages": [
    {
      "MessageId": "e339293b-4556-4994-b596-acfc51ee392b",
      "ReceiptHandle": "MjE3ZmMxMGQtZDlkYy00MDFiLWIyYjYtYzIwMDNmZjU2YmJmIGYxN2RjNjVhLTE5MjYtNGVhNC05MjZhLTRmNzNlNTdjNmQ2OSBhcm46YXdzOnNxczp1cy1lYXN0LTE6MDAwMDAwMDAwMDAwOm9yZGVyLWV2ZW50cw==",
      "MD5OfBody": "7333bbf93ace088d0ffc556614485f87",
      "Body": "{\n  \"Type\" : \"Notification\",\n  \"MessageId\" : \"1f624d5b-3ac8-4434-a561-670e0a4e3e81\",\n  \"TopicArn\" : \"arn:aws:sns:us-east-1:000000000000:order-events\",\n  \"Subject\" : \"order\",\n  \"Message\" : \"{\\\"orderId\\\": 42, \\\"status\\\": \\\"shipped\\\"}\",\n  \"Timestamp\" : \"2026-11-16T12:05:21.880Z\",\n  \"SignatureVersion\" : \"1\",\n  \"Signature\" : \"MAD2bkkzKTJIR9rbh97ByTHlP4OWFS5pBD/RI//OBSbIwllm9+hABj6bVErntt8nz7+fdw3ebWR0uID+QmYff4+YvH5wC+oN9duL9S4HV2fSPVKgYPpJs2K0xBpw6Y7A/nPxQfF6C5wiVs23ALyrwV3p6xXAOoPDykJWRShSLDE6m0YQSQepUI3MTkug0C9CEjJa541G2YofP7T4ZtlD9pQZA6DODC5TlG8wjILUa06jEYByvVyirD59QsXfgMYu8xImZ6EZmKkcX5M0+xi2841lhU59EtLdhSLEeIQtImfarkcvAbcMvRIya5SJRJLYeqmndt7uLWw62e2yK9Jj0w==\",\n  \"SigningCertURL\" : \"https://127.0.0.1:9297/SimpleNotificationService-c94da7065ae6d4a2abc628726979687e.pem?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=<access-key-id>%2F20261116%2Fus-east-1%2Fsns%2Faws4_request&X-Amz-Date=20261116T120521Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=<signature>\",\n  \"UnsubscribeURL\" : \"https://sns.us-east-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-east-1:000000000000:order-events:24404949-7f8b-4991-a87f-f3e740c2aa88\"\n}"
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
X-Amz-Date: 20261116T120521Z
Content-Length: 150
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=Unsubscribe
&Version=2010-03-31
&SubscriptionArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aorder-events%3A24404949-7f8b-4991-a87f-f3e740c2aa88
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 227
date: Mon, 16 Nov 2026 12:05:21 GMT

<UnsubscribeResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <UnsubscribeResult/>
  <ResponseMetadata>
    <RequestId>1e293b6c-8839-4827-bb3b-aff3728966e8</RequestId>
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
X-Amz-Date: 20261116T120521Z
Content-Length: 104
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sns/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteTopic
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aorder-events
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 227
date: Mon, 16 Nov 2026 12:05:21 GMT

<DeleteTopicResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <DeleteTopicResult/>
  <ResponseMetadata>
    <RequestId>53dcd156-4f08-4823-8d0d-3c7caebd6ba2</RequestId>
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
X-Amz-Date: 20261116T120521Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueUrl": "https://sqs.us-east-1.amazonaws.com/000000000000/order-events"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 5c556d88-8b6a-4e2c-8bf4-ea89e4e7e6d1
content-length: 2
date: Mon, 16 Nov 2026 12:05:21 GMT

{}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 42 operations, 11 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AddPermission

Adds a statement to a topic's access control policy, granting access for the specified Amazon Web Services accounts to the specified actions.

```http
POST / HTTP/1.1

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

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: Topic Name</Message>
  </Error>
  <RequestId>ed37ced5-5713-4ff6-a70b-1cb8f400936c</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### CheckIfPhoneNumberIsOptedOut

Accepts a phone number and indicates whether the phone holder has opted out of receiving SMS messages from your Amazon Web Services account.

```http
POST / HTTP/1.1

Action=CheckIfPhoneNumberIsOptedOut
&Version=2010-03-31
&phoneNumber=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: PhoneNumber Reason: input incorrectly formatted</Message>
  </Error>
  <RequestId>0bbdbea1-b351-4867-841e-d755918e1c6b</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### ConfirmSubscription

Verifies an endpoint owner's intent to receive messages by validating the token sent to the endpoint by an earlier Subscribe action.

```http
POST / HTTP/1.1

Action=ConfirmSubscription
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Token=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: Topic Name</Message>
  </Error>
  <RequestId>390f123c-46c5-44a2-bd48-367d4a34ef68</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### CreatePlatformApplication

Creates a platform application object for one of the supported push notification services, such as APNS and GCM (Firebase Cloud Messaging), to which devices and mobile apps may register.

```http
POST / HTTP/1.1

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

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: Platform Reason: example is not supported</Message>
  </Error>
  <RequestId>fd5dac38-f793-4d80-a944-2ae20142901d</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### CreatePlatformEndpoint

Creates an endpoint for a device and mobile app on one of the supported push notification services, such as GCM (Firebase Cloud Messaging) and APNS.

```http
POST / HTTP/1.1

Action=CreatePlatformEndpoint
&Version=2010-03-31
&PlatformApplicationArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Token=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>NotFound</Code>
    <Message>PlatformApplication does not exist</Message>
  </Error>
  <RequestId>f06ae49e-0117-4fbf-a925-c66eb11ec24f</RequestId>
</ErrorResponse>
```

`404` `NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateSMSSandboxPhoneNumber

Adds a destination phone number to an Amazon Web Services account in the SMS sandbox and sends a one-time password (OTP) to that phone number.

```http
POST / HTTP/1.1

Action=CreateSMSSandboxPhoneNumber
&Version=2010-03-31
&PhoneNumber=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: PhoneNumber Reason: input incorrectly formatted</Message>
  </Error>
  <RequestId>bfe77044-aa1b-4eaf-af5e-9e78b12cc5a5</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### CreateTopic

Creates a topic to which notifications can be published.

```http
POST / HTTP/1.1

Action=CreateTopic
&Version=2010-03-31
&Name=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<CreateTopicResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <CreateTopicResult>
    <TopicArn>arn:aws:sns:us-east-1:000000000000:example</TopicArn>
  </CreateTopicResult>
  <ResponseMetadata>
    <RequestId>5fb2daa7-e645-42a5-aa28-db8c0923eaeb</RequestId>
  </ResponseMetadata>
</CreateTopicResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#3-createtopic) for a successful call.

### DeleteEndpoint

Deletes the endpoint for a device and mobile app from Amazon SNS.

```http
POST / HTTP/1.1

Action=DeleteEndpoint
&Version=2010-03-31
&EndpointArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DeleteEndpointResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <DeleteEndpointResult/>
  <ResponseMetadata>
    <RequestId>a3dc9e02-3280-4cd2-a07a-c7a5b72b4fb5</RequestId>
  </ResponseMetadata>
</DeleteEndpointResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeletePlatformApplication

Deletes a platform application object for one of the supported push notification services, such as APNS and GCM (Firebase Cloud Messaging).

```http
POST / HTTP/1.1

Action=DeletePlatformApplication
&Version=2010-03-31
&PlatformApplicationArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DeletePlatformApplicationResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <DeletePlatformApplicationResult/>
  <ResponseMetadata>
    <RequestId>8addfe44-2154-4c41-9c39-9aa6bdd8619e</RequestId>
  </ResponseMetadata>
</DeletePlatformApplicationResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteSMSSandboxPhoneNumber

Deletes an Amazon Web Services account's verified or pending phone number from the SMS sandbox.

```http
POST / HTTP/1.1

Action=DeleteSMSSandboxPhoneNumber
&Version=2010-03-31
&PhoneNumber=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: PhoneNumber Reason: input incorrectly formatted</Message>
  </Error>
  <RequestId>fab32207-9c9f-4ba7-9f8e-374c34c16853</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### DeleteTopic

Deletes a topic and all its subscriptions.

```http
POST / HTTP/1.1

Action=DeleteTopic
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: Topic Name</Message>
  </Error>
  <RequestId>abb79a80-9105-46f8-a16f-ddb411d4a002</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation. See [the walkthrough](#9-deletetopic) for a successful call.

### GetDataProtectionPolicy

Amazon SNS message data protection is no longer available to new customers.

```http
POST / HTTP/1.1

Action=GetDataProtectionPolicy
&Version=2010-03-31
&ResourceArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>NotFound</Code>
    <Message>Topic does not exist</Message>
  </Error>
  <RequestId>808a3f95-2305-464e-8aee-45114db32de4</RequestId>
</ErrorResponse>
```

`404` `NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetEndpointAttributes

Retrieves the endpoint attributes for a device on one of the supported push notification services, such as GCM (Firebase Cloud Messaging) and APNS.

```http
POST / HTTP/1.1

Action=GetEndpointAttributes
&Version=2010-03-31
&EndpointArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>NotFound</Code>
    <Message>Endpoint does not exist</Message>
  </Error>
  <RequestId>f1282bce-9093-4efc-a5c4-58cd61463754</RequestId>
</ErrorResponse>
```

`404` `NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetPlatformApplicationAttributes

Retrieves the attributes of the platform application object for the supported push notification services, such as APNS and GCM (Firebase Cloud Messaging).

```http
POST / HTTP/1.1

Action=GetPlatformApplicationAttributes
&Version=2010-03-31
&PlatformApplicationArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>NotFound</Code>
    <Message>PlatformApplication does not exist</Message>
  </Error>
  <RequestId>853da1ee-3e03-495c-ba6c-f0f82dee4a79</RequestId>
</ErrorResponse>
```

`404` `NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetSMSAttributes

Returns the settings for sending SMS messages from your Amazon Web Services account.

```http
POST / HTTP/1.1

Action=GetSMSAttributes
&Version=2010-03-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<GetSMSAttributesResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <GetSMSAttributesResult>
    <attributes>
      <entry>
        <key>MonthlySpendLimit</key>
        <value>1</value>
      </entry>
    </attributes>
  </GetSMSAttributesResult>
  <ResponseMetadata>
    <RequestId>3befa456-b85a-4351-be8d-f28d6bb790c0</RequestId>
  </ResponseMetadata>
</GetSMSAttributesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetSMSSandboxAccountStatus

Retrieves the SMS sandbox status for the calling Amazon Web Services account in the target Amazon Web Services Region.

```http
POST / HTTP/1.1

Action=GetSMSSandboxAccountStatus
&Version=2010-03-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<GetSMSSandboxAccountStatusResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <GetSMSSandboxAccountStatusResult>
    <IsInSandbox>true</IsInSandbox>
  </GetSMSSandboxAccountStatusResult>
  <ResponseMetadata>
    <RequestId>ae74eddf-96a0-49dc-99b1-c87eff2ddf93</RequestId>
  </ResponseMetadata>
</GetSMSSandboxAccountStatusResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetSubscriptionAttributes

Returns all of the properties of a subscription.

```http
POST / HTTP/1.1

Action=GetSubscriptionAttributes
&Version=2010-03-31
&SubscriptionArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: SubscriptionId</Message>
  </Error>
  <RequestId>f63125d6-d60a-4a78-be2b-5db4461c6d3c</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### GetTopicAttributes

Returns all of the properties of a topic.

```http
POST / HTTP/1.1

Action=GetTopicAttributes
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: Topic Name</Message>
  </Error>
  <RequestId>a3995e74-1a62-45f5-a1b2-b4b6e6300f23</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### ListEndpointsByPlatformApplication

Lists the endpoints and endpoint attributes for devices in a supported push notification service, such as GCM (Firebase Cloud Messaging) and APNS.

```http
POST / HTTP/1.1

Action=ListEndpointsByPlatformApplication
&Version=2010-03-31
&PlatformApplicationArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>NotFound</Code>
    <Message>PlatformApplication does not exist</Message>
  </Error>
  <RequestId>4822ddab-9efa-43c2-8f92-299756a2fafd</RequestId>
</ErrorResponse>
```

`404` `NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListOriginationNumbers

Lists the calling Amazon Web Services account's dedicated origination numbers and their metadata.

```http
POST / HTTP/1.1

Action=ListOriginationNumbers
&Version=2010-03-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListOriginationNumbersResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <ListOriginationNumbersResult>
    <PhoneNumbers/>
  </ListOriginationNumbersResult>
  <ResponseMetadata>
    <RequestId>50910303-cb82-4ad0-9083-4840d5b2981d</RequestId>
  </ResponseMetadata>
</ListOriginationNumbersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListPhoneNumbersOptedOut

Returns a list of phone numbers that are opted out, meaning you cannot send SMS messages to them.

```http
POST / HTTP/1.1

Action=ListPhoneNumbersOptedOut
&Version=2010-03-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListPhoneNumbersOptedOutResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <ListPhoneNumbersOptedOutResult>
    <phoneNumbers/>
  </ListPhoneNumbersOptedOutResult>
  <ResponseMetadata>
    <RequestId>dba747de-7568-487d-a278-0cfd3171a6d0</RequestId>
  </ResponseMetadata>
</ListPhoneNumbersOptedOutResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListPlatformApplications

Lists the platform application objects for the supported push notification services, such as APNS and GCM (Firebase Cloud Messaging).

```http
POST / HTTP/1.1

Action=ListPlatformApplications
&Version=2010-03-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListPlatformApplicationsResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <ListPlatformApplicationsResult>
    <PlatformApplications/>
  </ListPlatformApplicationsResult>
  <ResponseMetadata>
    <RequestId>35543231-d0b9-4597-8812-32a4017ba99f</RequestId>
  </ResponseMetadata>
</ListPlatformApplicationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListSMSSandboxPhoneNumbers

Lists the calling Amazon Web Services account's current verified and pending destination phone numbers in the SMS sandbox.

```http
POST / HTTP/1.1

Action=ListSMSSandboxPhoneNumbers
&Version=2010-03-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListSMSSandboxPhoneNumbersResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <ListSMSSandboxPhoneNumbersResult>
    <PhoneNumbers/>
  </ListSMSSandboxPhoneNumbersResult>
  <ResponseMetadata>
    <RequestId>d1195f8f-26ae-4c0e-a4a0-8bd85c4e5c81</RequestId>
  </ResponseMetadata>
</ListSMSSandboxPhoneNumbersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListSubscriptions

Returns a list of the requester's subscriptions.

```http
POST / HTTP/1.1

Action=ListSubscriptions
&Version=2010-03-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListSubscriptionsResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <ListSubscriptionsResult>
    <Subscriptions/>
  </ListSubscriptionsResult>
  <ResponseMetadata>
    <RequestId>7c84a39d-b991-4ea7-8754-a4b2f834ab1d</RequestId>
  </ResponseMetadata>
</ListSubscriptionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListSubscriptionsByTopic

Returns a list of the subscriptions to a specific topic.

```http
POST / HTTP/1.1

Action=ListSubscriptionsByTopic
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: Topic Name</Message>
  </Error>
  <RequestId>ea76251f-8a69-47b2-b71b-b49e3d953eac</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### ListTagsForResource

List all tags added to the specified Amazon SNS topic.

```http
POST / HTTP/1.1

Action=ListTagsForResource
&Version=2010-03-31
&ResourceArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: ResourceArn Reason: An SNS tag is held by a topic, so the ARN must name an Amazon SNS topic</Message>
  </Error>
  <RequestId>e81abc75-e931-4fef-a03b-f2c7f4e83644</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### ListTopics

Returns a list of the requester's topics.

```http
POST / HTTP/1.1

Action=ListTopics
&Version=2010-03-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ListTopicsResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <ListTopicsResult>
    <Topics>
      <member xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
        <TopicArn>arn:aws:sns:us-east-1:000000000000:example</TopicArn>
      </member>
    </Topics>
  </ListTopicsResult>
  <ResponseMetadata>
    <RequestId>92bb67e9-5bf6-4e4b-98bf-00d7e7edd9f7</RequestId>
  </ResponseMetadata>
</ListTopicsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### OptInPhoneNumber

Use this request to opt in a phone number that is opted out, which enables you to resume sending SMS messages to the number.

```http
POST / HTTP/1.1

Action=OptInPhoneNumber
&Version=2010-03-31
&phoneNumber=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: PhoneNumber Reason: input incorrectly formatted</Message>
  </Error>
  <RequestId>ca4cba69-39f1-4e16-8bcf-72c5c82f46bc</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### Publish

Sends a message to an Amazon SNS topic, a text message (SMS message) directly to a phone number, or a message to a mobile platform endpoint (when you specify the TargetArn ).

```http
POST / HTTP/1.1

Action=Publish
&Version=2010-03-31
&Message=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: TopicArn or TargetArn Reason: no value for required parameter</Message>
  </Error>
  <RequestId>fba5eee5-fdae-4cdd-8184-a15cda871fbc</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation. See [the walkthrough](#6-publish) for a successful call.

### PublishBatch

Publishes up to 10 messages to the specified topic in a single batch.

```http
POST / HTTP/1.1

Action=PublishBatch
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&PublishBatchRequestEntries.member.1.Id=example
&PublishBatchRequestEntries.member.1.Message=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: Topic Name</Message>
  </Error>
  <RequestId>8aeb5fb3-3230-4f88-b549-7a4d96536796</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### PutDataProtectionPolicy

Amazon SNS message data protection is no longer available to new customers.

```http
POST / HTTP/1.1

Action=PutDataProtectionPolicy
&Version=2010-03-31
&ResourceArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&DataProtectionPolicy=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidAction</Code>
    <Message>Operation (PutDataProtectionPolicy) is not supported in this region</Message>
  </Error>
  <RequestId>78758bf3-2075-436a-9c94-917a024e7ef3</RequestId>
</ErrorResponse>
```

`400` `InvalidAction`.

### RemovePermission

Removes a statement from a topic's access control policy.

```http
POST / HTTP/1.1

Action=RemovePermission
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Label=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: Topic Name</Message>
  </Error>
  <RequestId>a6094d03-be5e-49ad-8b98-a002bc75b2a5</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### SetEndpointAttributes

Sets the attributes for an endpoint for a device on one of the supported push notification services, such as GCM (Firebase Cloud Messaging) and APNS.

```http
POST / HTTP/1.1

Action=SetEndpointAttributes
&Version=2010-03-31
&EndpointArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Attributes.entry.1.key=key
&Attributes.entry.1.value=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>NotFound</Code>
    <Message>Endpoint does not exist</Message>
  </Error>
  <RequestId>75a8b15e-809b-410c-8b49-bb31cbb5415c</RequestId>
</ErrorResponse>
```

`404` `NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### SetPlatformApplicationAttributes

Sets the attributes of the platform application object for the supported push notification services, such as APNS and GCM (Firebase Cloud Messaging).

```http
POST / HTTP/1.1

Action=SetPlatformApplicationAttributes
&Version=2010-03-31
&PlatformApplicationArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Attributes.entry.1.key=key
&Attributes.entry.1.value=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: Attributes Reason: Invalid attribute name: key</Message>
  </Error>
  <RequestId>efe65cd9-df27-4cd5-aa95-ea8516b0dc2f</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### SetSMSAttributes

Use this request to set the default settings for sending SMS messages and receiving daily SMS usage reports.

```http
POST / HTTP/1.1

Action=SetSMSAttributes
&Version=2010-03-31
&attributes.entry.1.key=key
&attributes.entry.1.value=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>key is not a valid attribute</Message>
  </Error>
  <RequestId>33688b61-6384-4b78-b110-d00e4b62c3d7</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### SetSubscriptionAttributes

Allows a subscription owner to set an attribute of the subscription to a new value.

```http
POST / HTTP/1.1

Action=SetSubscriptionAttributes
&Version=2010-03-31
&SubscriptionArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&AttributeName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: SubscriptionId</Message>
  </Error>
  <RequestId>ecf4f241-fb82-4f20-853b-50c62119a5d7</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### SetTopicAttributes

Allows a topic owner to set an attribute of the topic to a new value.

```http
POST / HTTP/1.1

Action=SetTopicAttributes
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&AttributeName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: Topic Name</Message>
  </Error>
  <RequestId>afe981b4-2d10-481f-966e-54d1505cd682</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### Subscribe

Subscribes an endpoint to an Amazon SNS topic.

```http
POST / HTTP/1.1

Action=Subscribe
&Version=2010-03-31
&TopicArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Protocol=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: Topic Name</Message>
  </Error>
  <RequestId>b00a8801-7d4f-4e66-81fe-55c8af923120</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation. See [the walkthrough](#5-subscribe) for a successful call.

### TagResource

Add tags to the specified Amazon SNS topic.

```http
POST / HTTP/1.1

Action=TagResource
&Version=2010-03-31
&ResourceArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Tags.member.1.Key=example
&Tags.member.1.Value=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: ResourceArn Reason: An SNS tag is held by a topic, so the ARN must name an Amazon SNS topic</Message>
  </Error>
  <RequestId>36e69e93-47d6-4d96-b70d-39a3e95b8916</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### Unsubscribe

Deletes a subscription.

```http
POST / HTTP/1.1

Action=Unsubscribe
&Version=2010-03-31
&SubscriptionArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: SubscriptionId</Message>
  </Error>
  <RequestId>573e0495-9401-4670-9ec4-8be86d2e71f7</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation. See [the walkthrough](#8-unsubscribe) for a successful call.

### UntagResource

Remove tags from the specified Amazon SNS topic.

```http
POST / HTTP/1.1

Action=UntagResource
&Version=2010-03-31
&ResourceArn=arn%3Aaws%3Asns%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&TagKeys.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: ResourceArn Reason: An SNS tag is held by a topic, so the ARN must name an Amazon SNS topic</Message>
  </Error>
  <RequestId>c3f7d68c-58db-4b2a-9d9f-78fe68725796</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### VerifySMSSandboxPhoneNumber

Verifies a destination phone number with a one-time password (OTP) for the calling Amazon Web Services account.

```http
POST / HTTP/1.1

Action=VerifySMSSandboxPhoneNumber
&Version=2010-03-31
&PhoneNumber=1
&OneTimePassword=10000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameter</Code>
    <Message>Invalid parameter: PhoneNumber Reason: input incorrectly formatted</Message>
  </Error>
  <RequestId>d1ccfc96-e56c-467a-8cd5-df4056ce564a</RequestId>
</ErrorResponse>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

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
