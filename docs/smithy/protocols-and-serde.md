# Protocols and serialization

A Smithy **protocol** is a trait on the service, such as `@aws.protocols#awsJson1_0`, that defines how operations map to HTTP: which method and path, where the operation name goes, how members are encoded, and how errors are recognized. The model says *which* protocol a service uses; each runtime has to implement it.

## The AWS protocols

| Protocol trait | Request | Response and errors | Used by (examples) |
|---|---|---|---|
| `aws.protocols#awsJson1_0`, `awsJson1_1` | `POST /`, `X-Amz-Target: <Service>.<Operation>`, JSON body, `Content-Type: application/x-amz-json-1.0` or `1.1` | JSON body; error code in `__type` or `X-Amzn-Errortype` | DynamoDB, SQS, Secrets Manager, ECS, Kafka … |
| `aws.protocols#restJson1` | Method and path from `@http`; members bound to labels, query, headers or a JSON body with `@httpLabel`, `@httpQuery`, `@httpHeader`, `@httpPayload` | Status from `@http` or `@httpError`; JSON body; error code in `X-Amzn-Errortype` or the body | API Gateway v2, EKS, Lambda, MSK … |
| `aws.protocols#restXml` | Like restJson1, with an XML body (`@xmlName`, `@xmlFlattened`, `@xmlNamespace`) | XML body; `<Error><Code>` | S3, Route 53, CloudFront |
| `aws.protocols#awsQuery` | `POST /`, form-encoded body: `Action`, `Version`, members flattened to keys like `Tags.member.1.Key` | XML `<OperationResponse><OperationResult>`; errors in `<ErrorResponse><Error><Code>` | IAM, STS, SNS, RDS, Auto Scaling … |
| `aws.protocols#ec2Query` | Like awsQuery with EC2's naming rules (`TagSpecification.1.Tag.1.Key`, `@ec2QueryName`) | XML without the `Result` wrapper; errors in `<Response><Errors><Error>` | EC2 only |
| `smithy.protocols#rpcv2Cbor` | `POST /service/<Service>/operation/<Operation>`, `Smithy-Protocol: rpc-v2-cbor`, CBOR body | CBOR body; error type in `__type` | Newer services such as CloudWatch and Compute Optimizer |

The [AWS Emulator pages](../aws-emulator/README.md) show real requests and responses for all of these except `rpcv2Cbor`. For example, [sqs.md](../aws-emulator/sqs.md) shows awsJson1_0 and [iam.md](../aws-emulator/iam.md) shows awsQuery.

## Who implements what

| Protocol | smithy-go runtime (schema serde) | aws-sdk-go-v2 legacy codegen | smithy-java | aws-sdk-java-v2 |
|---|---|---|---|---|
| awsJson1_0 / 1_1 | `transport/http/protocol/awsjson` | `AwsJsonRpc1_0`, `AwsJsonRpc1_1` | `aws:client:aws-client-awsjson` | `core/protocols/aws-json-protocol` |
| restJson1 | `transport/http/protocol/restjson1` | `AwsRestJson1` | `aws:client:aws-client-restjson`, server: `aws:server:aws-server-restjson` | `aws-json-protocol` |
| restXml | `transport/http/protocol/restxml` | `AwsRestXml` | `aws:client:aws-client-restxml` | `aws-xml-protocol` |
| awsQuery | `transport/http/protocol/awsquery` | `AwsQuery` | `aws:client:aws-client-awsquery` | `aws-query-protocol` |
| ec2Query | `transport/http/protocol/ec2query` | `Ec2Query` | | `aws-query-protocol` |
| rpcv2Cbor | `transport/http/protocol/rpcv2` | | `client:client-rpcv2-cbor`, server: `server:server-rpcv2-cbor` | `smithy-rpcv2-protocol` |

Every implementation is checked against the same **protocol compliance tests**. These are `@httpRequestTests` and `@httpResponseTests` traits in the `smithy-aws-protocol-tests` models, which give an input, the exact expected HTTP request, a response, and the expected output. smithy-go generates Go tests from them, aws-sdk-go-v2 builds them in `codegen/protocol-test-codegen`, and smithy-java runs them with its `protocol-test-harness` module.

This playground depends on `smithy-aws-protocol-tests` in [build.sbt](../../build.sbt), so the test models are on its classpath. Their test services reuse shape names across protocols, so assembling them together with the AWS service models can report conflicts that aren't real.

## How serialization is split

Every runtime splits serialization into three layers:

1. **The shape** knows its members and their values: generated code.
2. **The protocol** knows HTTP: method, path, headers, which members go where, how errors are found.
3. **The codec** knows the format: JSON, XML, CBOR or form encoding: tokens and escaping.

What differs is where the shape-to-protocol knowledge lives.

The two approaches, for the same S3 `PutObject` request:

```text
Generated serializers (legacy)                      Schema serde
┌────────────────────────────────────────────┐      ┌────────────────────────────────────────────┐
│ Generated per protocol, per operation:     │      │ Generated, protocol-neutral: Serialize(s)  │
│ serializers.go                             │      │ + schemas/ (shapes, members, traits)       │
│ awsRestxml_serializeOpPutObject sets       │      └────────────────────────────────────────────┘
│ method, path, headers                      │                │
└────────────────────────────────────────────┘                ▼
          │                                         ┌────────────────────────────────────────────┐
          ▼                                         │ Runtime protocol (e.g. restxml.Protocol)   │
┌────────────────────────────────────────────┐      │ reads @http traits from schemas            │
│ Encoders: encoding/httpbinding +           │      └────────────────────────────────────────────┘
│ encoding/xml write tokens                  │                │
└────────────────────────────────────────────┘                ▼
          │                                         ┌────────────────────────────────────────────┐
          ▼                                         │ Runtime codec: XML ShapeSerializer writes  │
┌────────────────────────────────────────────┐      │ the body                                   │
│ HTTP request                               │      └────────────────────────────────────────────┘
└────────────────────────────────────────────┘                │
                                                              ▼
                                                    ┌────────────────────────────────────────────┐
                                                    │ HTTP request                               │
                                                    └────────────────────────────────────────────┘
```

### Generated serializers (the older approach)

aws-sdk-go-v2's legacy path and aws-sdk-java-v2's generated marshallers put protocol logic into generated code. Each protocol has a codegen class that writes serializers for every operation. For S3 `PutObject`, the generated Go sets `request.Method = "PUT"` and builds the path from `/{Key+}?x-id=PutObject` itself ([generated code](generated-code.md#serialization-legacy-per-protocol)).

It works, but:

- every protocol needs its own generator in every language;
- generated code grows with services × operations × protocols;
- a protocol fix means regenerating every affected service.

### Schema serde (the direction everything is moving)

With schema serde, generated code contains only **schemas** (shapes, members and their traits as runtime values) and a `Serialize` / `Deserialize` method that walks members against those schemas. The protocol is ordinary runtime code that reads the traits it cares about from the schema:

```go
// smithy-go, transport/http/protocol/awsjson: the protocol sets the wire details...
req.Method = http.MethodPost
req.Header.Set("X-Amz-Target", fmt.Sprintf("%s.%s", p.serviceName, schema.ID().Name))
req.Header.Set("Content-Type", "application/x-amz-json-"+p.version)
// ...then hands a JSON ShapeSerializer to the generated input.Serialize(s)
```

A restXml serializer reading the same schemas looks for `XMLName` and `XMLFlattened`, while awsJson ignores them. That's why `SendMessageRequest_MessageAttributes` in SQS's schemas carries XML traits even though SQS speaks JSON today: the schema describes the model, not one protocol.

The interface between the runtime and a protocol is small. In smithy-go it is [ClientProtocol](https://github.com/aws/smithy-go/blob/main/transport/http/protocol.go):

```go
type ClientProtocol interface {
	ID() smithy.ShapeID
	SerializeRequest(context.Context, *smithy.OperationSchema, smithy.Serializable, *Request) error
	DeserializeResponse(ctx context.Context, schema *smithy.OperationSchema, types *smithy.TypeRegistry, resp *Response, out smithy.Deserializable) error
	// plus event stream methods
}
```

smithy-java's `ClientProtocol` has the same core (`createRequest`, `setServiceEndpoint`, `deserializeResponse`, plus `payloadCodec()`), and its codecs live in separate `codecs:json-codec`, `codecs:xml-codec` and `codecs:cbor-codec` modules. smithy-java was designed this way from the start. Its `Schema` objects also carry precomputed validation constraints, so a server can validate input against `@length` or `@pattern` without generated validation code.

The `TypeRegistry` (generated as `type_registry.go` in Go, `TypeRegistryGenerator` in smithy-java) completes the picture. When a response says the error is `com.amazonaws.sqs#QueueDoesNotExist`, the protocol looks up that shape id in the registry, creates the right error type, and deserializes into it.

For SQS, a request to a queue that doesn't exist comes back like this:

```text
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ HTTP/1.1 400 with body {"__type": "com.amazonaws.sqs#QueueDoesNotExist", "message": "..."}     │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Protocol: reads the error code (__type, X-Amzn-Errortype, or <Error><Code>); with              │
│ @awsQueryCompatible also x-amzn-query-error                                                    │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ TypeRegistry: "com.amazonaws.sqs#QueueDoesNotExist" -> { Schema: schemas.QueueDoesNotExist,    │
│ New: &types.QueueDoesNotExist{} }                                                              │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Codec: Deserialize(d) fills the error's members from the body                                  │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Caller gets *types.QueueDoesNotExist (wrapped in smithy.OperationError); unknown codes become  │
│ a generic APIError                                                                             │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### The move to schema serde

aws-sdk-go-v2 is moving every service from generated serializers to schema serde, in waves defined in [codegen/sdk-codegen/build.gradle.kts](https://github.com/aws/aws-sdk-go-v2/blob/main/codegen/sdk-codegen/build.gradle.kts):

1. `rpcv2Cbor`;
2. the awsJson services in batches of about 25;
3. restJson1;
4. awsQuery;
5. EC2, the restXml services, and S3.

A service uses schema serde unless its wave is in `useLegacySerdeServices`. As of this snapshot, waves 1 to 20 have moved, so almost every service uses schema serde. Only these still use the legacy generators:

- `w21_ec2Query`: EC2;
- `w22_restXml`: CloudFront, S3 Control, Route 53;
- `w23_s3`: S3;
- `w99_deprecated`: FinSpace, FinSpace Data, TNB.

`SMITHY_GO_SCHEMA_SERDE=com.amazonaws.s3` forces schema serde for a service during a local build.

You can tell which kind a package is by its files. A schema-serde package has `schemas/` and `type_registry.go`, as [service/sqs](https://github.com/aws/aws-sdk-go-v2/tree/main/service/sqs) does. A legacy package has `serializers.go` and `deserializers.go`, as [service/s3](https://github.com/aws/aws-sdk-go-v2/tree/main/service/s3) does.

## Encoders

The lowest layer is shared:

- **smithy-go:** `encoding/json`, `encoding/xml`, `encoding/cbor` and `encoding/httpbinding`, used by both serde paths. They produce and consume tokens, not shapes.
- **smithy-java:** `codecs:*` modules behind a `Codec` interface, with a `ShapeSerializer` and `ShapeDeserializer` per format.
- **aws-sdk-java-v2:** `protocol-core` plus per-protocol marshallers in `core/protocols`, driven by each POJO's `SdkField` list.

Next: [How it all fits](how-it-fits.md).
