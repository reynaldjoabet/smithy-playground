# The protocol stack

Every Smithy runtime, in every language, splits the work of a call into the same layers. Once you can name the layers, any of the generators in [the ecosystem](ecosystem.md) is easier to read, because each one has a module or package per layer:

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Generated code: typed inputs and outputs, plus a schema for every shape and member (id, type,    │
│ members, traits)                                                                                 │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                  │
                                                  ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Client pipeline: interceptors, retry loop, endpoint rules, identity and auth scheme selection;   │
│ protocol-agnostic                                                                                │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                  │
                                                  ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Protocol: implements one protocol trait (awsJson1_0, restJson1, restXml, awsQuery, ec2Query,     │
│ rpcv2Cbor, rpcv2Json): method, path, headers, framing, error envelopes. Calls a codec for the    │
│ body                                                                                             │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                  │
                                                  ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Codec: one wire format (JSON, XML, CBOR, form-url). Walks the schema and writes or reads values; │
│ knows nothing about HTTP                                                                         │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                  │
                                                  ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Auth scheme and signer: SigV4, SigV4a, bearer, API key, anonymous. Signs the serialized request  │
│ on each attempt                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                  │
                                                  ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Transport: the language's HTTP client sends the bytes and returns the raw response (net/http in  │
│ Go, java.net.http in smithy-java, cURL in PHP)                                                   │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

The rest of this page goes through the layers from the bottom up, then shows how they're wired together and tested.

## What each part does

In one sentence: **a codec turns a shape into bytes and back; a protocol turns an operation call into an HTTP request, and an HTTP response back into a result or an error.** Everything else supports those two jobs.

Take one call, `client.getItem(sku: "A-100")`, on `restJson1`:

| Part | What it knows | What it does in this call |
|---|---|---|
| **Model** (`.smithy` or JSON AST) | What the service is | Declares `GetItem`, its input, output and errors, and traits such as `@restJson1`, `@http(method: "GET", uri: "/items/{sku}")` and `@sigv4`. Nothing runs from it directly; everything below comes from it. |
| **Generated code and schemas** | The shapes, in one language | `GetItemInput` and `GetItemOutput` types, each with a **schema**: its members and their traits as runtime data. A shape can walk its own members (`serialize(serializer)`) without knowing any format. |
| **Codec** | **One format**: JSON, XML, CBOR, Protobuf | Writes and reads values: `{"sku": "A-100"}`, escaping, numbers and timestamps, field names (`@jsonName`) or field numbers (`@idx`). It never sees a URL, a header or a status code. A shape goes in and bytes come out, or bytes and a builder go in and a shape comes out. |
| **Protocol** | **The HTTP rules** of one protocol trait | Picks the method and path (`GET /items/A-100`), puts members in the path, query, headers or body (`@httpLabel`, `@httpHeader`), sets `Content-Type`, and calls the codec for the body only. On the way back it reads the status, finds the error code (`X-Amzn-Errortype`, `__type`, a gRPC trailer), and has the codec fill either the output or the right error. |
| **TypeRegistry** | Shape id to generated class | Turns the name `com.example#ItemNotFound` into an `ItemNotFound` object for the protocol. |
| **Endpoint resolver** | Where the service is | Runs the model's endpoint rules (region, FIPS, dual-stack) to get `https://inventory.us-east-1.example.com`, and can say how to sign (signing name and region). |
| **Auth**: identity resolver and signer | Who the caller is, and how to prove it | The resolver finds credentials or a token. The signer adds `Authorization` (SigV4, bearer, …) to the request the protocol already built, so it doesn't care which protocol built it. |
| **Transport** | Bytes on a connection | Sends the HTTP request and returns the raw response: connection pooling, TLS, HTTP/1.1 or HTTP/2, trailers. It knows nothing about Smithy. |
| **Client pipeline** (middleware) | The order of the steps | Serializes once, then on each attempt resolves the endpoint and auth, signs, sends and deserializes, inside retries, interceptors, timeouts and metrics. Go's middleware stack, smithy-java's `ClientPipeline` and aws-sdk-java-v2's pipeline stages are all this part. |

### Why the codec and the protocol are separate

They change independently, so each can be reused:

- **Same codec, different protocols.** `JsonCodec` serves `restJson1`, `awsJson1_0`, `rpcv2Json` and the [JSON-RPC design](new-protocol-json-rpc.md). Only the envelope differs: a path, an `X-Amz-Target` header, or a `"method"` field.
- **Same protocol shape, different codecs.** `rpcv2Cbor` and `rpcv2Json` share one base class in smithy-java and differ only in the codec they supply.
- **A new format means a new codec.** That's why [Protobuf](new-protocol-protobuf.md) needs one and JSON-RPC doesn't.

The traits divide the same way:

- **The codec reads format traits:** `@jsonName`, `@xmlName`, `@xmlFlattened`, `@timestampFormat` in a body, `@idx`.
- **The protocol reads HTTP traits:** `@http`, `@httpLabel`, `@httpQuery`, `@httpHeader`, `@httpPayload`, `@httpError`.
- **Neither sees** `@sigv4` (auth), the endpoint rules (the resolver), or `@retryable` (the retry strategy).

A protocol trait's `@protocolDefinition(traits: [...])` is the formal list of what that protocol reads. smithy-java also uses it to decide which traits to keep in generated schemas.

### On the way back

1. The **transport** returns the status, headers and bytes.
2. The **protocol** decides whether the call succeeded and which shape the body is.
3. The **TypeRegistry** supplies the class.
4. The **codec** fills it.
5. The **pipeline** decides whether to retry or return.

## Schemas: what every layer reads

A **schema** is a shape from the model turned into a runtime value. Generated code contains one schema per shape and member, and they carry:

- the shape id (`com.amazonaws.sqs#SendMessageRequest`);
- the shape type (structure, list, string …);
- the members, each with a name, an index and a target schema;
- the traits a runtime may need (`@jsonName`, `@xmlFlattened`, `@timestampFormat`, `@httpHeader`, `@httpLabel`, `@required`, `@sensitive` …).

Generated types then only need to walk their own members against those schemas. Every runtime that has moved to schemas generates the same pattern:

```go
// Go (smithy-go)
s.WriteString(schemas.SendMessageRequest_MessageBody, *v.MessageBody)
```

```java
// Java (smithy-java)
serializer.writeString($0, messageBody);
```

```php
// PHP (smithy-php): a generated structure dispatches its members to the serializer
$input->serialize($serializer, 0);
```

smithy-python goes one step further: its generated models hold compact schemas and are built lazily, and the runtime's `ShapeSerializer` (`write_string(schema, value)` and so on) walks them, so the generated files contain no serialization calls at all.

In every case the serializer is supplied at run time by a codec, so the same generated type can be written as JSON, XML or CBOR. Schemas exist in smithy-go (`schemas/`), smithy-java (`core/schema`), smithy-typescript (`@smithy/core` `schema`), smithy-python (`smithy_core.schemas`), smithy-php (`smithy/core` `Schema`), smithy-swift (`SmithySerialization` and the Swift-native generator's `Schemas` output), smithy-rs (`aws-smithy-schema`) and smithy-ruby (`smithy-schema`).

The older design, still used by parts of smithy-rs and by aws-sdk-go-v2's EC2, S3, Route 53 and CloudFront clients, generates a serializer per operation for one protocol instead. See [protocols and serialization](protocols-and-serde.md#the-move-to-schema-serde).

## Codecs: one format, no HTTP

A **codec** turns schema-described values into bytes in one format, and back:

| Format | Go | Java | Rust | Kotlin | Swift | TypeScript | Python | PHP | Ruby |
|---|---|---|---|---|---|---|---|---|---|
| JSON | `encoding/json` | `json-codec` | `aws-smithy-json` | `serde-json` | `SmithyJSON` | `@smithy/core` (json) | `smithy-json` | `Codec\Json` | `smithy-json` |
| XML | `encoding/xml` | `xml-codec` | `aws-smithy-xml` | `serde-xml` | `SmithyXML` | in the AWS SDK | `smithy-xml` | `Codec\Xml` | `smithy-xml` |
| CBOR | `encoding/cbor` | `cbor-codec` | `aws-smithy-cbor` | `serde-cbor` | `SmithyCBOR` | `@smithy/core/cbor` | `smithy-cbor` | `Codec\Cbor` | `smithy-cbor` |
| Form URL | `aws/protocol/query` (aws-sdk-go-v2) | (in `aws-client-awsquery`) | `aws-smithy-query` | `serde-form-url` | `SmithyFormURL` | in the AWS SDK | (`smithy_aws_core` query serializers) | `Codec\FormUrlEncoded` | |

A codec handles the traits that change how a value looks in its format: `@jsonName`, `@xmlName`, `@xmlAttribute`, `@xmlFlattened`, `@xmlNamespace`, and `@timestampFormat` for the body. It doesn't know about URLs, headers or status codes. The smithy-php codec README states it directly: "Codecs encode and decode message bodies. HTTP bindings and request framing belong to `smithy/protocol`."

## Protocols: codec + HTTP rules

A **protocol** implements one protocol trait. It builds the request around the codec's output and reads the response around the codec's input. It owns:

- **Framing.** The method, the URI, the `Content-Type`, and where the operation name goes: `X-Amz-Target` for awsJson, the path for `rpcv2Cbor`, the `Action` parameter for the query protocols.
- **HTTP bindings.** For REST protocols, moving members marked `@httpLabel`, `@httpQuery`, `@httpHeader`, `@httpPrefixHeaders`, `@httpPayload` and `@httpResponseCode` in and out of the URL, headers and status.
- **Errors.** Finding the error code (the `__type` field, the `X-Amzn-Errortype` header, `<Error><Code>`), looking it up in the **type registry** to build the modelled error, and falling back to a generic service error.
- **Compatibility rules**, such as `@awsQueryCompatible` (reading the old Query error code from `x-amzn-query-error`) and event-stream message framing.

Protocols come in two families, which differ only in where members go:

| Family | Protocols | Request | Response | Shared base class |
|---|---|---|---|---|
| RPC | `awsJson1_0`, `awsJson1_1`, `rpcv2Cbor`, `rpcv2Json` | Whole input in the body, operation named in a header or the path | Whole output in the body | TypeScript `RpcProtocol` / `AbstractRpcV2Protocol`; smithy-java `client-rpcv2` |
| HTTP binding | `restJson1`, `restXml` | Members spread over path, query, headers and body by `@http*` traits | Same, plus status code | PHP `AbstractHttpBindingProtocol`; TypeScript `HttpBindingProtocol`; smithy-java `client-http-binding`; Go `internal/httpbinding` |
| Query | `awsQuery`, `ec2Query` | Form-URL body | XML body | (two codecs: form URL out, XML in) |

So a protocol is mostly a **choice of codec plus a binding style**:

| Protocol | Binding style | Request codec | Response codec |
|---|---|---|---|
| `awsJson1_0` / `awsJson1_1` | RPC (`X-Amz-Target`) | JSON | JSON |
| `rpcv2Cbor` | RPC (`/service/<S>/operation/<O>`) | CBOR | CBOR |
| `rpcv2Json` | RPC (same paths) | JSON | JSON |
| `restJson1` | HTTP bindings | JSON | JSON |
| `restXml` | HTTP bindings | XML | XML |
| `awsQuery` | RPC (`Action=`) | Form URL | XML |
| `ec2Query` | RPC (`Action=`, EC2 naming) | Form URL | XML |

The protocol trait definitions live in [smithy](https://github.com/smithy-lang/smithy): the AWS protocols in `smithy-aws-traits` (`aws.protocols.smithy`), and `rpcv2Cbor` and `rpcv2Json` in `smithy-protocol-traits`. The `smithy.protocols` namespace isn't AWS-specific, which is why non-AWS examples default to it: the smithy-php pet store, for instance, runs over `rpcv2Cbor`. In smithy-php and smithy-typescript, the generic RPCv2 protocols live in the core packages and the AWS protocols in a separate package or SDK, which shows the same split.

### Choosing a protocol

A service can carry several protocol traits. The generated client normally picks the one it was generated or configured for:

- smithy-go sets `options.Protocol` in `New`;
- smithy-java takes the `protocol` codegen setting, or discovers a `ClientProtocolFactory` through `ServiceLoader`;
- smithy-php's `ClientConfig` has "protocol resolver and optional explicit protocol selection", and the pet-store client "selects RPCv2 CBOR from its service schema".

## Auth: beside the protocol, not inside it

Protocols never sign. Signing is a separate layer of **auth schemes**. Each scheme pairs:

- an **identity resolver**, which answers "who is calling": AWS credentials, a bearer token, an API key, or anonymous;
- a **signer**, which says "how to prove it on this request": SigV4, SigV4a, bearer header, API key header or query parameter.

The model lists which schemes apply (`@aws.auth#sigv4`, `@httpBearerAuth`, `@httpApiKeyAuth`, ordered by `@auth`, or `@optionalAuth`). The pipeline resolves the identity and calls the signer on the serialized request, once per attempt, after the endpoint is known. Endpoint rules can also return auth properties, such as the signing region or S3 Express session auth.

Each runtime has the same pieces under similar names:

| Runtime | Auth pieces |
|---|---|
| smithy-php `smithy/auth` | `IdentityResolverInterface`, `AuthSchemeInterface`, `SignerInterface`, `Aws\SigV4Signer` |
| smithy-java | `auth-api`, `client-auth-api`, `aws-sigv4` |
| smithy-swift | `SmithyIdentityAPI`, `SmithyHTTPAuthAPI` |
| smithy-kotlin | `identity-api`, `http-auth-api`, `aws-signing-default` |
| smithy-rs | `aws-sigv4`, and the identity and auth orchestrator stages |
| smithy-typescript | `signature-v4`, `signature-v4a` |

## Transport: bytes on the wire

The **transport** sends a serialized, signed request and returns the raw response:

| Runtime | Transport |
|---|---|
| smithy-go | `net/http` via `smithyhttp.ClientHandler` |
| smithy-java | `JavaHttpClientTransport`, or its own `http-client` with HTTP/2 |
| smithy-rs | `aws-smithy-http-client` |
| smithy-kotlin | the `http-client-engines` (default, OkHttp, CRT) |
| smithy-swift | `SmithyHTTPClient` and `SmithySwiftNIO` |
| smithy-typescript | `node-http-handler`, `fetch-http-handler` |
| smithy-php | `CurlClient` |

Protocols and transports must agree on a message type: an HTTP protocol needs an HTTP transport. smithy-java checks this with `MessageExchange`. smithy-php protocols "declare their request/response message exchange so configuration can reject an incompatible transport before execution". smithy-rs's orchestrator design leaves room for protocols that don't use HTTP at all (MQTT is mentioned).

## The pipeline ties the layers together

The client pipeline is the only layer that knows about all the others. It runs them in the order that the [request lifecycle](request-lifecycle.md) page describes for Go and Java, and the other runtimes use the same order:

1. Interceptors read or modify the input.
2. **Protocol** serializes the request (via the **codec**). This happens once, before retries.
3. Retry loop, once per attempt:
   1. resolve the endpoint and set it on the request (`protocol.setServiceEndpoint`);
   2. resolve the identity and **sign** (auth);
   3. send with the **transport**;
   4. the **protocol** deserializes the response, or classifies the error for the retry strategy.
4. Interceptors read or modify the output.

The same layers on the way out and on the way back:

```text
Request (top to bottom)                             Response (bottom to top)
┌────────────────────────────────────────────┐
│ Generated input + schemas                  │
└────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────┐      ┌────────────────────────────────────────────┐
│ Codec: JSON ShapeSerializer -> body bytes  │      │ Output, or a modelled error                │
└────────────────────────────────────────────┘      └────────────────────────────────────────────┘
          │                                                   ▲
          ▼                                                   │
┌────────────────────────────────────────────┐      ┌─────────┴──────────────────────────────────┐
│ Protocol: POST /, X-Amz-Target,            │      │ Codec: JSON ShapeDeserializer fills        │
│ Content-Type                               │      │ members                                    │
└────────────────────────────────────────────┘      └────────────────────────────────────────────┘
          │                                                   ▲
          ▼                                                   │
┌────────────────────────────────────────────┐      ┌─────────┴──────────────────────────────────┐
│ Auth: the signer adds Authorization        │      │ Protocol: status and error code ->         │
│ (SigV4)                                    │      │ TypeRegistry                               │
└────────────────────────────────────────────┘      └────────────────────────────────────────────┘
          │                                                   ▲
          ▼                                                   │
┌────────────────────────────────────────────┐      ┌─────────┴──────────────────────────────────┐
│ Transport: send the request                │      │ Transport: the raw response                │
└─────────┬──────────────────────────────────┘      └────────────────────────────────────────────┘
          │                                                   ▲
          ▼                                                   │
┌─────────────────────────────────────────────────────────────┴──────────────────────────────────┐
│ Service: AWS, a Smithy server, or the AWS Emulator at https://127.0.0.1:9297                   │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
```

The smithy-php protocol README describes the same contract: "Clients serialize an operation-relative request once before the retry loop. Each request attempt resolves and binds its service endpoint before signing."

Each runtime exposes the same stages through its own hooks:

| Runtime | Pipeline | User hooks | Runtime extension |
|---|---|---|---|
| smithy-go | `middleware.Stack` | middleware via `APIOptions` | `RuntimeClientPlugin`s from codegen |
| smithy-java | `ClientPipeline` | `ClientInterceptor` | `ClientPlugin` / `AutoClientPlugin` |
| smithy-rs | orchestrator | interceptors (the `Intercept` trait) | runtime plugins writing a `ConfigBag` |
| smithy-typescript | `middleware-stack` | middleware | plugins (`getSchemaSerdePlugin` …) |
| smithy-python | `smithy_core` client pipeline | `interceptors` | config |
| smithy-php | `ClientPipeline` | interceptors | plugins |

## Servers use the same layers in reverse

A server reads the request with the protocol and codec, routes to your handler by operation, and writes the response or a modelled error with the same protocol and codec. Server generators:

- smithy-java (`server-core`, `server-netty`, `server-rpcv2-cbor`, `aws-server-restjson`);
- smithy-rs (`rust-server-codegen`, plus Python and TypeScript bindings);
- smithy-typescript (`typescript-server-codegen`, `server-node`, `server-apigateway`);
- smithy-ruby (`smithy-server`).

The smithy-php pet-store fixture server "implements RPCv2 CBOR using the same schemas and codec as the generated client".

Because the schemas carry `@required`, `@length`, `@pattern` and `@range`, a schema-driven server validates input from schemas instead of generated validation code.

## Dynamic clients: schemas without codegen

Since protocols and codecs only need schemas, a runtime can build schemas from a model at run time and skip generation entirely:

- smithy-java's `dynamic-client`;
- smithy-php's `Client/Dynamic` ("generated and runtime-loaded Smithy services");
- smithy-python's `_dynamic_models`.

Input and output are documents instead of generated classes, but the request on the wire is the same. This suits tools that call arbitrary services from a model loaded at run time, such as generic CLIs and proxies.

## One test suite for every language

Protocol behaviour is pinned down by data, not by any one implementation. The smithy repository publishes the compliance tests as Smithy models:

- `smithy-protocol-tests` for `rpcv2Cbor` and `rpcv2Json`;
- `smithy-aws-protocol-tests` for the AWS protocols.

Each test is an `@httpRequestTests` or `@httpResponseTests` case: given this input, the request must be exactly this method, URI, headers and body; given this response, the output or error must be this.

Each generator turns the cases into tests in its own language:

| Repository | Where |
|---|---|
| smithy-go | `HttpProtocolTestGenerator` |
| aws-sdk-go-v2 | `codegen/protocol-test-codegen` |
| smithy-java | `protocol-test-harness` |
| smithy-rs | `codegen-client-test`, `codegen-server-test`; extra cases in `codegen-core/common-test-models` |
| smithy-kotlin | `codegen/protocol-tests` |
| smithy-typescript | `smithy-typescript-protocol-test-codegen` |
| smithy-python | `codegen/protocol-test` |
| smithy-ruby | `rake smithy:sync-protocol-tests` from a pinned `smithy-protocol-tests` version |
| smithy-php | generated into `codegen-output/` (`Smithy\ProtocolTests\AwsJson1_0`, `RestJson`, `RpcV2Cbor` …) |

This shared suite is why a `restJson1` request from Rust, PHP and TypeScript is byte-for-byte comparable. It's also why the [AWS Emulator pages](../aws-emulator/README.md), captured with a hand-written client, match what any of these SDKs sends.

Next: [Adding a protocol: Protobuf over gRPC](new-protocol-protobuf.md).
