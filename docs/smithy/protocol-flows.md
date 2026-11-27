# One call through each protocol

This page follows one real operation through this repo's generator and runtime, once for each Smithy protocol AWS uses, and once for JSON-RPC 2.0. Every diagram has the same steps, so you can see exactly which pieces are shared and which ones a protocol needs of its own.

| Step | Piece | Where it lives |
|---|---|---|
| 1-4 | Load the model, walk the service closure, keep the runtime traits, write one file per shape | [client-codegen](../../modules/client-codegen) (`Main.loadModel`, `ScalaClientGenerator`) |
| 5 | Write the client file, which names the protocol | `ScalaClientGenerator.protocolExpr` |
| 6 | The call pipeline: endpoint, retries, order of steps | [Pipeline.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/Pipeline.scala) |
| 7 | The protocol: method, path, headers, where each member goes | [Protocols.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/Protocols.scala) |
| 8, 13 | The shape codec: walks the Scala value, or builds it | Generated code, derived by [Hearth](../../modules/hearth) |
| 9, 12 | The format codec: bytes in one format | [codecs/](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/codecs) |
| 10-11 | Auth (SigV4) and transport (JDK `HttpClient`) | [Auth.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/Auth.scala), [Http.scala](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/Http.scala) |
| 14 | Errors: from the response to a typed exception | The protocol, plus the operation's `TypeRegistry` |

`✓` means the step works for that protocol today, and `✗` means it's missing.

## Summary

| Protocol | Example | Client generated? | Protocol class | Format codec | Status |
|---|---|---|---|---|---|
| `awsJson1_0` / `awsJson1_1` | SQS `SendMessage` | ✓ | ✓ `AwsJsonProtocol` | ✓ `JsonCodec` | **Works** (tested, and run against the emulator) |
| `rpcv2Json` | Weather `GetForecast` | ✓ | ✓ `RpcV2JsonProtocol` | ✓ `JsonCodec` | **Works** (tested against a local server) |
| `rpcv2Cbor` | Weather with `@rpcv2Cbor` | ✗ | ✗ (small: rpcv2Json's routing) | ✗ `CborCodec` placeholder | Missing |
| `restJson1` | Lambda `GetFunction` | ✗ | ✗ HTTP bindings | ✓ `JsonCodec` (body only) | Missing |
| `restXml` | S3 `GetObject` | ✗ | ✗ HTTP bindings | ✗ `XmlCodec` placeholder | Missing, plus S3 customizations |
| `awsQuery` | IAM `CreateUser` | ✗ | ✗ | ✗ `FormUrlCodec`, `XmlCodec` placeholders | Missing |
| `ec2Query` | EC2 `CreateVpc` | ✗ | ✗ | ✗ `FormUrlCodec` (ec2 names), `XmlCodec` | Missing |
| JSON-RPC 2.0 (your own `@jsonRpc2` trait) | Weather `GetForecast` | ✗ | ✗ (small: an envelope and `id` matching) | ✓ `JsonCodec` | Missing; smallest runtime change |

In every diagram, steps 1-4, 6, 8, 11 and 13 are the same. Model loading, schemas, the Hearth shape codecs, the pipeline and the transport don't depend on the protocol. What changes is the protocol (steps 5, 7 and 14) and the format codec (steps 9 and 12), plus SigV4 for S3.

## `awsJson1_0` and `awsJson1_1`: SQS `SendMessage`

Every step exists. This is the path the [generated SQS and DynamoDB clients](scala-client.md) take, and what [GeneratedClientSpec](../../modules/client-example/src/test/scala/example/GeneratedClientSpec.scala) and the emulator demo exercise. `awsJson1_1` differs only in its `Content-Type` (`application/x-amz-json-1.1`).

```text
BUILD TIME
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 1-4 Load, closure, shapes  sqs.json → AmazonSQS (23 operations)                            │
│       ModelAssembler → Walker + TopDownIndex → one file per shape (protocol-independent)     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 3   Kept traits            core traits + the protocol's @protocolDefinition list:          │
│       timestampFormat, cors, endpoint, hostLabel                                             │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 5   Client file                                                                            │
│       protocolExpr → rt.ProtocolInfo.AwsJson("1.0")                                          │
│       SQSClient.scala with Operations.SendMessage                                            │
└──────────────────────────────────────────────────────────────────────────────────────────────┘

RUN TIME: sqs.sendMessage(SendMessageRequest(queueUrl, "hello"))
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 6   Pipeline             pipeline.invoke(Operations.SendMessage, input)                    │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 7   Protocol             AwsJsonProtocol: POST /                                           │
│       Content-Type: application/x-amz-json-1.0, X-Amz-Target: AmazonSQS.SendMessage          │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 8   Shape codec (Hearth) writeStruct(SendMessageRequest) { writeString(QueueUrl) … }       │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 9   Format codec         JsonCodec: member names, @timestampFormat honoured, no @jsonName  │
│       {"QueueUrl":"https://sqs.…/orders","MessageBody":"hello"}                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 10-11 Auth + transport   SigV4 (signing name sqs), JDK HttpClient → 200 + JSON             │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 12  Format codec         JsonCodec reads {"MessageId":"…","MD5OfMessageBody":"…"}          │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 13  Shape codec (Hearth) → SendMessageResult(messageId = Some("…"), …)                     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 14  Errors               X-Amzn-Errortype header, or __type / code in the body             │
│       → QueueDoesNotExist through the operation's TypeRegistry                               │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

## `rpcv2Json`: Weather `GetForecast`

Every step exists. The operation's name is in the URL path rather than a header, and timestamps are always epoch seconds, because `rpcv2Json`'s `@protocolDefinition` doesn't list `timestampFormat`. [RpcV2JsonClientSpec](../../modules/client-example/src/test/scala/example/RpcV2JsonClientSpec.scala) runs it.

```text
BUILD TIME
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 1-4 Load, closure, shapes  weather.smithy → Weather (GetForecast, Ping)                    │
│       ModelAssembler → Walker + TopDownIndex → one file per shape (protocol-independent)     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 3   Kept traits            core traits + the protocol's @protocolDefinition list:          │
│       cors, endpoint, hostLabel, httpError, requestCompression                               │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 5   Client file                                                                            │
│       protocolExpr → rt.ProtocolInfo.RpcV2Json                                               │
│       WeatherClient.scala with Operations.GetForecast                                        │
└──────────────────────────────────────────────────────────────────────────────────────────────┘

RUN TIME: weather.getForecast(GetForecastInput("Oslo"))
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 6   Pipeline             pipeline.invoke(Operations.GetForecast, input)                    │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 7   Protocol             RpcV2JsonProtocol: POST /service/Weather/operation/GetForecast    │
│       Smithy-Protocol: rpc-v2-json, Content-Type and Accept: application/json                │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 8   Shape codec (Hearth) writeStruct(GetForecastInput) { writeString(city) … }             │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 9   Format codec         JsonCodec: member names, timestamps always epoch seconds          │
│       {"city":"Oslo","units":"METRIC","detail":1}                                            │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 10-11 Auth + transport   no auth trait in this model, so unsigned; JDK HttpClient          │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 12  Format codec         JsonCodec reads {"days":[…],"type":{"rain":3}}                    │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 13  Shape codec (Hearth) → GetForecastOutput(days = Some(…), `type` = Kind.Rain(3))        │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 14  Errors               __type in the JSON body names the shape → Busy (retryable)        │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

## `rpcv2Cbor`: Weather, in CBOR

The closest missing protocol. It routes exactly like `rpcv2Json` and keeps the same traits, so the protocol class is a small change. The real work is a CBOR format codec ([RFC 8949](https://www.rfc-editor.org/rfc/rfc8949)).

```text
BUILD TIME
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 1-4 Load, closure, shapes  weather.smithy with @rpcv2Cbor instead of @rpcv2Json            │
│       ModelAssembler → Walker + TopDownIndex → one file per shape (protocol-independent)     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 3   Kept traits            core traits + the protocol's @protocolDefinition list:          │
│       cors, endpoint, hostLabel, httpError, requestCompression                               │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 5   Client file                                                                            │
│       protocolExpr has no Rpcv2CborTrait case → UnsupportedOperationException                │
│       the runtime's ProtocolInfo has no RpcV2Cbor case to emit                               │
└──────────────────────────────────────────────────────────────────────────────────────────────┘

RUN TIME: what weather.getForecast would need
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 6   Pipeline             unchanged                                                         │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 7   Protocol             RpcV2JsonProtocol's routing, with Smithy-Protocol: rpc-v2-cbor    │
│       and Content-Type / Accept: application/cbor (a small change)                           │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 8   Shape codec (Hearth) unchanged: the same writeStruct / writeString calls               │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 9   Format codec         CborCodec is a placeholder: CBOR maps, major types, tag 1 for     │
│       timestamps, byte strings for blobs                                                     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 10-11 Auth + transport   unchanged                                                         │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 12  Format codec         CborCodec reading                                                 │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 13  Shape codec (Hearth) unchanged                                                         │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 14  Errors               __type inside the CBOR body                                       │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

## `restJson1`: Lambda `GetFunction`

The format codec already exists: the body is JSON, honouring `@jsonName`. What's missing is the protocol. Each member goes wherever its binding trait says: path, query, header or body. See [HTTP and REST](http-and-formats.md#http-and-rest) for the bindings.

```text
BUILD TIME
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 1-4 Load, closure, shapes  lambda.json → Lambda                                            │
│       ModelAssembler → Walker + TopDownIndex → one file per shape (protocol-independent)     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 3   Kept traits            core traits + the protocol's @protocolDefinition list:          │
│       http*, httpError, httpChecksum*, jsonName, timestampFormat, …                          │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 5   Client file                                                                            │
│       protocolExpr has no RestJson1Trait case → UnsupportedOperationException                │
│       and no ProtocolInfo.RestJson1 to emit                                                  │
└──────────────────────────────────────────────────────────────────────────────────────────────┘

RUN TIME: what lambda.getFunction(GetFunctionRequest("my-fn", qualifier = Some("PROD"))) needs
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 6   Pipeline             unchanged                                                         │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 7   Protocol             RestJsonProtocol driven by HTTP bindings (HttpBindingIndex):      │
│       @http GET /2015-03-31/functions/{FunctionName}, @httpLabel, @httpQuery("Qualifier")    │
│       → GET /2015-03-31/functions/my-fn?Qualifier=PROD                                       │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 8   Shape codec (Hearth) unchanged; the protocol routes each member by its binding         │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 9   Format codec         JsonCodec with useJsonName = true, for unbound members only       │
│       (GetFunction has none, so no body)                                                     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 10-11 Auth + transport   SigV4 (signing name lambda), JDK HttpClient                       │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 12  Response             headers and @httpResponseCode into output members (protocol);     │
│       ✓ JsonCodec reads the body members                                                     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 13  Shape codec (Hearth) → GetFunctionResponse(configuration = Some(…), …)                 │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 14  Errors               X-Amzn-ErrorType header + status from @httpError(404)             │
│       → ResourceNotFoundException                                                            │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

## `restXml`: S3 `GetObject`

The same binding work as `restJson1`, plus an XML codec for operations with XML bodies. S3 also needs things outside the protocol: a signed payload hash, and virtual-hosted bucket addressing. Its errors aren't wrapped (`restXml(noErrorWrapping: true)`).

```text
BUILD TIME
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 1-4 Load, closure, shapes  s3.json → S3, restXml(noErrorWrapping: true)                    │
│       ModelAssembler → Walker + TopDownIndex → one file per shape (protocol-independent)     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 3   Kept traits            core traits + the protocol's @protocolDefinition list:          │
│       http*, httpError, httpChecksum*, xml*, timestampFormat, …                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 5   Client file                                                                            │
│       protocolExpr has no RestXmlTrait case → UnsupportedOperationException                  │
│       and no ProtocolInfo.RestXml to emit                                                    │
└──────────────────────────────────────────────────────────────────────────────────────────────┘

RUN TIME: what s3.getObject(GetObjectRequest("my-bucket", "photos/cat.jpg")) needs
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 6   Pipeline             unchanged                                                         │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 7   Protocol             RestXmlProtocol driven by HTTP bindings:                          │
│       @http GET /{Bucket}/{Key+}?x-id=GetObject, greedy {Key+}, Range → header               │
│       → GET /my-bucket/photos/cat.jpg?x-id=GetObject                                         │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 8   Shape codec (Hearth) unchanged                                                         │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 9   Format codec         XmlCodec is a placeholder (needed by ops with XML bodies;         │
│       GetObject sends none)                                                                  │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 10  Auth                 S3 needs x-amz-content-sha256 signed; this SigV4 doesn't add it   │
│       (plus S3 customizations: virtual-hosted bucket addressing)                             │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 11  Transport            JDK HttpClient                                                    │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 12  Response             Body is the @httpPayload blob (raw bytes, no codec);              │
│       ETag, Content-Type, Last-Modified from headers (protocol)                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 13  Shape codec (Hearth) → GetObjectOutput(body = Some(Blob(…)), eTag = Some(…), …)        │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 14  Errors               <Error><Code>NoSuchKey</Code>… (no <ErrorResponse> wrapper)       │
│       + @httpError(404) → NoSuchKey                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

## `awsQuery`: IAM `CreateUser`

Form-encoded requests and XML responses, both missing. The wire format is taken apart, with real captures, in [`awsQuery`: STS, IAM and Elastic Load Balancing v2](http-and-formats.md#a-third-style-awsquery-sts-iam-and-elastic-load-balancing-v2).

```text
BUILD TIME
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 1-4 Load, closure, shapes  iam.json → IAM (181 operations, 819 shapes)                     │
│       ModelAssembler → Walker + TopDownIndex → one file per shape (protocol-independent)     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 3   Kept traits            core traits + the protocol's @protocolDefinition list:          │
│       xmlName, xmlFlattened, xmlNamespace, awsQueryError, …                                  │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 5   Client file                                                                            │
│       protocolExpr has no AwsQueryTrait case → UnsupportedOperationException                 │
│       and no ProtocolInfo.AwsQuery to emit                                                   │
└──────────────────────────────────────────────────────────────────────────────────────────────┘

RUN TIME: what iam.createUser(CreateUserRequest("docs-demo-user", tags = …)) needs
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 6   Pipeline             unchanged                                                         │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 7   Protocol             AwsQueryProtocol: POST /, application/x-www-form-urlencoded,      │
│       body starts Action=CreateUser&Version=2010-05-08                                       │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 8   Shape codec (Hearth) writeStruct(CreateUserRequest) { writeString(UserName)            │
│                             writeList(Tags) { writeStruct(Tag) { Key, Value } } }            │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 9   Format codec         FormUrlCodec is a placeholder; would write                        │
│       UserName=docs-demo-user&Tags.member.1.Key=team&Tags.member.1.Value=docs                │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 10-11 Auth + transport   SigV4 (signing name iam), JDK HttpClient → 200 + XML              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 12  Format codec         XmlCodec is a placeholder; would read                             │
│       <CreateUserResponse> > <CreateUserResult> > <User>, lists as <member>                  │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 13  Shape codec (Hearth) → CreateUserResponse(user = Some(User(…)))                        │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 14  Errors               <ErrorResponse><Error><Code>DeleteConflict</Code>, 409            │
│       → DeleteConflictException through @awsQueryError(code: "DeleteConflict")               │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

## `ec2Query`: EC2 `CreateVpc`

Like `awsQuery`, but with EC2's naming (`@ec2QueryName`, flat lists, `<item>` elements) and a different envelope. The EC2 model has no error shapes at all, so every error would surface as an `UnknownServiceException` carrying the code. These details come from the captures in [ec2.md](../aws-emulator/ec2.md).

```text
BUILD TIME
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 1-4 Load, closure, shapes  ec2.json → EC2 (no modelled error shapes)                       │
│       ModelAssembler → Walker + TopDownIndex → one file per shape (protocol-independent)     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 3   Kept traits            core traits + the protocol's @protocolDefinition list:          │
│       ec2QueryName, xmlName, xmlFlattened, xmlNamespace, …                                   │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 5   Client file                                                                            │
│       protocolExpr has no Ec2QueryTrait case → UnsupportedOperationException                 │
│       and no ProtocolInfo.Ec2Query to emit                                                   │
└──────────────────────────────────────────────────────────────────────────────────────────────┘

RUN TIME: what ec2.createVpc(CreateVpcRequest(cidrBlock = Some("10.20.0.0/16"), …)) needs
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 6   Pipeline             unchanged                                                         │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 7   Protocol             Ec2QueryProtocol: POST /, form body,                              │
│       Action=CreateVpc&Version=2016-11-15                                                    │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 8   Shape codec (Hearth) unchanged                                                         │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 9   Format codec         FormUrlCodec with ec2 naming (placeholder): lists always flat,    │
│       names from @ec2QueryName / @xmlName → TagSpecification.1.Tag.1.Key=Name                │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 10-11 Auth + transport   SigV4 (signing name ec2), JDK HttpClient → 200 + XML              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 12  Format codec         XmlCodec (placeholder): <CreateVpcResponse> > <vpc>, no Result    │
│       wrapper, lists as <item>, lower-case element names                                     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 13  Shape codec (Hearth) → CreateVpcResult(vpc = Some(Vpc(…)))                             │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 14  Errors               <Response><Errors><Error><Code>DependencyViolation</Code>, 400    │
│       no error shapes in the model → UnknownServiceException("DependencyViolation")          │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

## JSON-RPC 2.0: Weather `GetForecast`

JSON-RPC 2.0 isn't a Smithy or AWS protocol, so the model needs a protocol trait you define yourself. This diagram uses the design in [Adding a protocol: JSON-RPC 2.0](new-protocol-json-rpc.md):
- **The protocol trait:** `example.protocols#jsonRpc2`, with `@protocolDefinition(traits: [httpError, jsonRpcErrorCode, smithy.api#jsonName])`.
- **The error trait:** `@jsonRpcErrorCode`, which maps an error shape to a JSON-RPC error number.

Because the generator reads `@protocolDefinition`, `@jsonRpcErrorCode` would reach the schemas with no further generator change.

It's the smallest runtime change of any missing protocol:
- **The format codec exists:** `JsonCodec` writes `params` and reads `result` as it is.
- **The protocol is mostly an envelope:** wrap and unwrap the JSON-RPC message, and match responses by `id`.

The rest is the protocol and error traits in the model, the error mapping by number, and the generator branch.

```text
BUILD TIME
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 1-4 Load, closure, shapes  weather.smithy with example.protocols#jsonRpc2 (your own trait) │
│       ModelAssembler → Walker + TopDownIndex → one file per shape (protocol-independent)     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 3   Kept traits            core traits + the protocol's @protocolDefinition list:          │
│       httpError, jsonRpcErrorCode, jsonName                                                  │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 5   Client file                                                                            │
│       protocolExpr has no jsonRpc2 case → UnsupportedOperationException                      │
│       and no ProtocolInfo.JsonRpc2 to emit (Smithy and AWS define no JSON-RPC trait)         │
└──────────────────────────────────────────────────────────────────────────────────────────────┘

RUN TIME: what weather.getForecast(GetForecastInput("Oslo")) needs
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 6   Pipeline             unchanged                                                         │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 7   Protocol             JsonRpcProtocol: POST /rpc, Content-Type: application/json,       │
│       envelope {"jsonrpc":"2.0","id":1,"method":"GetForecast","params":…}, match by id       │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 8   Shape codec (Hearth) unchanged: writeStruct(GetForecastInput) { writeString(city) … }  │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 9   Format codec         JsonCodec writes params as it is: {"city":"Oslo",…}               │
│       (jsonName is in the trait's list, so useJsonName = true)                               │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 10-11 Auth + transport   whatever auth the model declares; JDK HttpClient → 200 + JSON     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 12  Format codec         JsonCodec reads the "result" node (readTree + reader(node))       │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✓ 13  Shape codec (Hearth) → GetForecastOutput(…)                                            │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ ✗ 14  Errors               HTTP 200 with "error":{"code":-32001,"message":…}: map the number │
│       through @jsonRpcErrorCode(-32001) → Busy; unmatched codes → UnknownServiceException    │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

Two JSON-RPC features have no Smithy equivalent and would need a decision:
- **Notifications:** requests without an `id`, which get no response. They could map to operations whose output is `Unit`.
- **Batches:** an array of requests in one message. Smithy's operation model has nothing like it.

There is no official JSON-RPC 3.0. A community draft adds streaming over WebSockets. In Smithy terms that would be event streams, and it would need a WebSocket transport. This generator currently skips event-stream operations, and the runtime has no WebSocket transport.

## What it would take

Ordered by effort:

1. **JSON-RPC 2.0:** your own protocol and error traits in the model, plus a small envelope protocol. No new format codec.
2. **`rpcv2Cbor`:** `CborCodec` and a few header changes to the `rpcv2Json` protocol.
3. **`awsQuery`:** `FormUrlCodec`, `XmlCodec` for responses, and a protocol that maps `<ErrorResponse>` codes through `@awsQueryError`. This unlocks STS, IAM, Elastic Load Balancing v2, SNS, CloudFormation and RDS.
4. **`ec2Query`:** the same codecs with EC2 settings (`FormUrlSettings(ec2Names = true)`) and EC2's XML envelope.
5. **`restJson1`:** an HTTP-binding protocol using `HttpBindingIndex`, with URI templates, query and header serialization, and response headers. The JSON codec is unchanged.
6. **`restXml`:** the binding protocol from step 5 with `XmlCodec`. For S3, also payload signing and bucket addressing.

Each needs one more branch in the generator's `protocolExpr` and one more `ProtocolInfo` case. The generator already keeps the right traits for all of them, because it reads [the protocol's `@protocolDefinition`](scala-client.md#which-traits-the-generator-keeps).

Back to [the overview](README.md).
