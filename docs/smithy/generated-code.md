# Generated code

This page shows what each generator writes, using real files. The pattern is the same in every language: **types** for inputs, outputs and errors; **metadata** that describes those types to the runtime; and an **operation method** that hands both to the runtime.

## Go: a service package in aws-sdk-go-v2

[service/sqs](https://github.com/aws/aws-sdk-go-v2/tree/main/service/sqs) is entirely generated, apart from the `cust_*` files:

| File | Contents |
|---|---|
| `api_client.go` | `Client`, `New`, `NewFromConfig`, option resolution (retryer, HTTP client, signer, endpoint resolver), and `invokeOperation`, which builds and runs the middleware stack. |
| `api_op_<Operation>.go` | One file per operation: the client method, `<Operation>Input`, `<Operation>Output`, and `addOperation<Operation>Middlewares`. |
| `types/types.go`, `types/enums.go`, `types/errors.go` | Nested structures, enums and modelled errors. |
| `schemas/schemas.go` | One `*smithy.Schema` per shape and member: the runtime description of the model. |
| `type_registry.go` | Maps error and union shape ids to Go types, so the deserializer can construct the right error. |
| `endpoints.go` | The endpoint parameters and a resolver that evaluates the model's endpoint rules (as a BDD). |
| `auth.go` | The auth scheme resolver: which schemes an operation supports, in order. |
| `validators.go` | Client-side `@required` checks, run in the Initialize step. |
| `options.go`, `doc.go`, `go.mod` | Configuration, package docs, and the module definition (each service is its own Go module). |

### The operation method

From [api_op_SendMessage.go](https://github.com/aws/aws-sdk-go-v2/blob/main/service/sqs/api_op_SendMessage.go), with the doc comments removed:

```go
func (c *Client) SendMessage(ctx context.Context, params *SendMessageInput, optFns ...func(*Options)) (*SendMessageOutput, error) {
	if params == nil {
		params = &SendMessageInput{}
	}

	result, metadata, err := c.invokeOperation(ctx, "SendMessage", params, optFns, c.addOperationSendMessageMiddlewares)
	if err != nil {
		return nil, err
	}

	out := result.(*SendMessageOutput)
	out.ResultMetadata = metadata
	return out, nil
}

type SendMessageInput struct {
	MessageBody            *string
	QueueUrl               *string
	DelaySeconds           int32
	MessageAttributes      map[string]types.MessageAttributeValue
	MessageDeduplicationId *string
	MessageGroupId         *string
	// ...
	noSmithyDocumentSerde
}
```

The method itself does almost nothing. It passes the input and a function that registers this operation's middleware to `invokeOperation`, which runs the stack (see [the request lifecycle](request-lifecycle.md)).

### Serialization: schema-driven

SQS uses smithy-go's newer **schema serde**. The input knows how to walk its own members, but not what the wire format is:

```go
func (v *SendMessageInput) Serialize(s smithy.ShapeSerializer) {
	s.WriteStruct(schemas.SendMessageRequest)
	v.SerializeMembers(s)
	s.CloseStruct()
}

func (v *SendMessageInput) SerializeMembers(s smithy.ShapeSerializer) {
	if v.DelaySeconds != 0 {
		s.WriteInt32(schemas.SendMessageRequest_DelaySeconds, v.DelaySeconds)
	}
	if v.MessageBody != nil {
		s.WriteString(schemas.SendMessageRequest_MessageBody, *v.MessageBody)
	}
	// ...
}
```

Each call passes a member schema. The schema is generated in [schemas/schemas.go](https://github.com/aws/aws-sdk-go-v2/blob/main/service/sqs/schemas/schemas.go) and carries the traits a format might need:

```go
SendMessageRequest_QueueUrl = SendMessageRequest.AddMember("QueueUrl", _String)

SendMessageRequest_MessageAttributes = SendMessageRequest.AddMember("MessageAttributes", _MessageBodyAttributeMap,
	&smithytraits.XMLName{Name: "MessageAttribute"},
	&smithytraits.XMLFlattened{})
```

The `ShapeSerializer` is supplied by the protocol. For SQS that's awsJson 1.0, which writes JSON and ignores the XML traits. The same generated code could feed a Query or CBOR serializer, because the format lives in the runtime, not in the generated code. The output side mirrors this, with `Deserialize(d smithy.ShapeDeserializer)` and a `switch` over member schemas.

The operation registers the protocol-neutral serializer and deserializer with its schemas:

```go
func (c *Client) addOperationSendMessageMiddlewares(stack *middleware.Stack, options Options) (err error) {
	if err := stack.Serialize.Add(&serializeRequestMiddleware{options: &options,
		operationSchema: smithy.NewOperationSchema(schemas.SendMessage, schemas.SendMessageRequest, schemas.SendMessageResult)},
		middleware.After); err != nil {
		return err
	}
	// deserializeResponseMiddleware, then endpoint, checksum, retry, signing, validation ...
}
```

The protocol is chosen once, in `New`: `options.Protocol = awsjson.New10(schemas.AmazonSQS)`.

### Serialization: legacy, per protocol

Services that haven't moved to schema serde yet get a `serializers.go` and `deserializers.go` with code written out for one protocol. [service/s3/serializers.go](https://github.com/aws/aws-sdk-go-v2/blob/main/service/s3/serializers.go) has, for `PutObject` under `@restXml`:

```go
opPath, opQuery := httpbinding.SplitURI("/{Key+}?x-id=PutObject")
request.URL.Path = smithyhttp.JoinPath(request.URL.Path, opPath)
request.URL.RawQuery = smithyhttp.JoinRawQuery(request.URL.RawQuery, opQuery)
request.Method = "PUT"
// ...
if err := awsRestxml_serializeOpHttpBindingsPutObjectInput(input, restEncoder); err != nil {
	return out, metadata, &smithy.SerializationError{Err: err}
}
```

The model's `@http(method: "PUT", uri: "/{Bucket}/{Key+}?x-id=PutObject")` trait is compiled directly into Go statements. `{Bucket}` is missing from the path because S3's endpoint rules put the bucket in the host name, and the `S3HttpPathBucketFilterIntegration` customization removes it from the URI. That's fast and readable, but every protocol needs its own generator, and each service's package grows large. [Protocols and serialization](protocols-and-serde.md#the-move-to-schema-serde) explains which services still use it.

### The call path

Putting the pieces together, one `SendMessage` call passes through these files and packages before the request body exists:

```text
┌─ your code ──────────────────────────────────────────────────────────────────────────────────┐
│ client.SendMessage(ctx, &sqs.SendMessageInput{QueueUrl: ..., MessageBody: ...})              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                  │
                                                  ▼
┌─ api_op_SendMessage.go ──────────────────────────────────────────────────────────────────────┐
│ c.invokeOperation(ctx, "SendMessage", params, optFns, c.addOperationSendMessageMiddlewares)  │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                  │
                                                  ▼
┌─ api_client.go: invokeOperation ─────────────────────────────────────────────────────────────┐
│ new middleware.Stack -> common + operation middleware -> APIOptions -> wrap                  │
│ smithyhttp.ClientHandler                                                                     │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                  │
                                                  ▼
┌─ Serialize step: serializeRequestMiddleware ─────────────────────────────────────────────────┐
│ options.Protocol.SerializeRequest(ctx, operationSchema, input, req)                          │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                  │
                                                  ▼
┌─ smithy-go: awsjson.Protocol ────────────────────────────────────────────────────────────────┐
│ POST /, X-Amz-Target: AmazonSQS.SendMessage, Content-Type: application/x-amz-json-1.0;       │
│ creates a JSON ShapeSerializer                                                               │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                  │
                                                  ▼
┌─ generated: SendMessageInput.Serialize(s) ───────────────────────────────────────────────────┐
│ s.WriteStruct(schemas.SendMessageRequest);                                                   │
│ s.WriteString(schemas.SendMessageRequest_QueueUrl, ...) ...                                  │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
                                                  │
                                                  ▼
┌─ result ─────────────────────────────────────────────────────────────────────────────────────┐
│ request body {"QueueUrl": "...", "MessageBody": "..."}; Build and Finalize steps add         │
│ headers, sign and send                                                                       │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

## Java: a smithy-java structure

smithy-java generates the same three things. This is a structure from its [codegen test cases](https://github.com/smithy-lang/smithy-java/blob/main/codegen/codegen-plugin/src/test/resources/software/amazon/smithy/java/codegen/types/test-cases/defaults/expected/DefaultStructure.java), shortened:

```java
@SmithyGenerated
public final class DefaultStructure implements SerializableStruct {
    public static final Schema $SCHEMA = Schemas.$3;
    private static final Schema $0 = $SCHEMA.member(0); // boolean
    private static final Schema $1 = $SCHEMA.member(1); // bigDecimal
    // ...
    private final transient boolean booleanMember;
    private final transient BigDecimal bigDecimal;

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeBoolean($0, booleanMember);
        serializer.writeBigDecimal($1, bigDecimal);
        // ...
    }

    public static final class Builder implements ShapeBuilder<DefaultStructure> {
        private static void $deserializeMember(Builder builder, Schema member, ShapeDeserializer de) {
            switch (member.memberIndex()) {
                case 0 -> builder.booleanMember(de.readBoolean(member));
                case 1 -> builder.bigDecimal(de.readBigDecimal(member));
                // ...
            }
        }
    }
}
```

It's the same design as Go's schema serde: a generated `Schemas` class holds the shapes, each structure walks its members against their schemas, and the `ShapeSerializer` or `ShapeDeserializer` comes from a codec at runtime. Builders are the only way to construct a structure, which is how `@required`, `@default` and nullability are enforced.

The client and server sides are thin. In the [end-to-end example](https://github.com/smithy-lang/smithy-java/tree/main/examples/end-to-end), the generated client is used like this:

```java
var client = CoffeeShopClient.builder()
        .endpointResolver(EndpointResolver.staticEndpoint(URI.create("http://localhost:8888")))
        .build();
var menu = client.getMenu(GetMenuInput.builder().build());
```

The server side generates one interface per operation (`GetMenuOperation`). You implement each and register it:

```java
Server server = Server.builder()
        .endpoints(URI.create("http://localhost:8888"))
        .addService(CoffeeShop.builder()
                .addCreateOrderOperation(new CreateOrder())
                .addGetMenuOperation(new GetMenu())
                .addGetOrderOperation(new GetOrder())
                .build())
        .build();
```

Neither side contains serialization code. The server picks a protocol implementation at runtime from the protocol traits on the service (`@restJson1` here) and the protocol modules on the classpath.

## Java: an aws-sdk-java-v2 model class

aws-sdk-java-v2's model classes predate smithy-java but follow the same idea. Each class has a static list of `SdkField`s, each naming its marshalling type, member name, accessors, and where it goes on the wire (from [the codegen test fixtures](https://github.com/aws/aws-sdk-java-v2/blob/master/codegen/src/test/resources/software/amazon/awssdk/codegen/poet/model/basetype.java)):

```java
public final class BaseType implements SdkPojo, Serializable, ToCopyableBuilder<BaseType.Builder, BaseType> {
    private static final SdkField<String> BASE_MEMBER_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("BaseMember").getter(getter(BaseType::baseMember)).setter(setter(Builder::baseMember))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("BaseMember").build())
            .build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(BASE_MEMBER_FIELD));
    // ...
}
```

The protocol marshallers in `core/protocols` iterate over `sdkFields()`. The `LocationTrait` (`PAYLOAD`, `HEADER`, `QUERY_PARAM`, `URI`) plays the role of Smithy's `@httpHeader`, `@httpQuery` and `@httpLabel`.

The generated client method builds a `ClientExecutionParams` and gives it to the client handler (from the [JSON client fixture](https://github.com/aws/aws-sdk-java-v2/blob/master/codegen/src/test/resources/software/amazon/awssdk/codegen/poet/client/test-json-client-class.java)):

```java
return clientHandler.execute(new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
        .withOperationName("APostOperation").withProtocolMetadata(protocolMetadata)
        .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
        .withRequestConfiguration(clientConfiguration)
        .withInput(aPostOperationRequest).withMetricCollector(apiCallMetricCollector)
        .withAuthSchemeOptionsResolver(this::resolveAuthSchemeOptions).withEndpointResolver(this::resolveEndpoint)
        .withMarshaller(new APostOperationRequestMarshaller(protocolFactory)));
```

## Side by side

| | aws-sdk-go-v2 (schema serde) | smithy-java | aws-sdk-java-v2 |
|---|---|---|---|
| Shape metadata | `*smithy.Schema` vars in `schemas/` | `Schema` constants in a generated `Schemas` class | `SdkField` lists on each POJO |
| Struct writes itself | `Serialize(smithy.ShapeSerializer)` | `serializeMembers(ShapeSerializer)` | No: marshallers read `sdkFields()` |
| Struct reads itself | `Deserialize(smithy.ShapeDeserializer)` | `Builder.deserialize(ShapeDeserializer)` | No: unmarshallers fill builders through `SdkField` setters |
| Operation method | `invokeOperation(ctx, name, params, optFns, addMiddlewares)` | `call(input, GetMenu.instance(), overrideConfig)` on the runtime client | `clientHandler.execute(ClientExecutionParams)` |
| Where the protocol is chosen | `options.Protocol = awsjson.New10(...)` in `New` | The `protocol` codegen setting, or the client builder | Code generated for the C2J `protocol`, using a `protocolFactory` |

Next: [The request lifecycle](request-lifecycle.md).
