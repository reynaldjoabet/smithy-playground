# Adding a protocol: Protobuf over gRPC

This page walks through what it would take to add a new protocol to the Smithy stack: Protocol Buffers messages sent over gRPC. No Smithy runtime ships this protocol today. It's a design exercise, built from the real extension points in smithy-java and the Smithy model, to show which layers of [the protocol stack](protocol-stack.md) a new protocol touches.

It is the harder of two examples. Protobuf needs a new **codec** (a binary format that no runtime has yet) *and* a new **protocol** (gRPC framing, paths and errors). The [JSON-RPC 2.0 page](new-protocol-json-rpc.md) shows the easy case, where only the protocol is new.

## What changes

```text
┌─ Model: new trait ─────────────────────────────────────────────────────────────────────────────┐
│ example.protocols#grpcProto on the service, plus @indexed, so every member has an idx          │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                                                 ▼
┌─ Generated code: unchanged ────────────────────────────────────────────────────────────────────┐
│ Same generators. The schemas now carry each member's idx, because grpcProto lists idx in       │
│ @protocolDefinition                                                                            │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                                                 ▼
┌─ Protocol: new ────────────────────────────────────────────────────────────────────────────────┐
│ GrpcProtoClientProtocol: POST /<package>.<Service>/<Method>, gRPC headers, the 5-byte message  │
│ frame, grpc-status in the trailers                                                             │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                                                 ▼
┌─ Codec: new ───────────────────────────────────────────────────────────────────────────────────┐
│ ProtobufCodec: field tags from idx, varints, length-delimited strings and nested messages      │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                                                 ▼
┌─ Auth: unchanged ──────────────────────────────────────────────────────────────────────────────┐
│ Any scheme that adds headers, for example a bearer token                                       │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                                                 ▼
┌─ Transport: new requirement ───────────────────────────────────────────────────────────────────┐
│ HTTP/2 with response trailers: SmithyHttpClientTransport, not the java.net.http transport      │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
```

With schema serde, the generators don't change at all. That is the point of [the move to schema serde](protocols-and-serde.md#the-move-to-schema-serde): a protocol is runtime code that reads traits from schemas.

## Step 1: the model

### Field numbers already exist: `@idx`

Protobuf identifies a field by a number, not a name. Smithy members have no stable order, so until recently a Protobuf protocol would have needed its own field-number trait. Smithy 1.75.0 (2026-11-09) added one to the prelude protocol traits: [`smithy.protocols#idx`](https://github.com/smithy-lang/smithy/blob/main/smithy-protocol-traits/src/main/resources/META-INF/smithy/smithy.protocols.indexed.smithy), with a service-level `@indexed` that requires every member in the closure to have one. Its [design document](https://github.com/smithy-lang/smithy/blob/main/designs/indexed-services.md) names Protobuf, Thrift and Cap'n Proto as prior art.

There are two differences from Protobuf field numbers:

- **Indexes must be contiguous from 1**, with no gaps. Protobuf allows gaps and `reserved` numbers. Removing a member that isn't the last one means renumbering, which breaks the wire format, so in practice members can only be added at the end.
- **Mixins can't assign indexes.** Each structure that uses a mixin numbers the mixin's members itself.

This playground pins Smithy 1.74.0 in [build.sbt](../../build.sbt), which doesn't have `idx` yet. The models below were validated with Smithy 1.75.1.

### The protocol trait

A protocol is a trait marked with `@protocolDefinition`, like [`smithy.protocols#rpcv2Json`](https://github.com/smithy-lang/smithy/blob/main/smithy-protocol-traits/src/main/resources/META-INF/smithy/smithy.protocols.rpcv2.smithy). Its `traits` list is the set of traits the protocol reads:

```smithy
$version: "2.1"

namespace example.protocols

use smithy.api#httpError
use smithy.protocols#idx

/// gRPC over HTTP/2, with Protocol Buffers messages.
@trait(selector: "service")
@protocolDefinition(traits: [idx, httpError])
structure grpcProto {}
```

That list matters for code generation. smithy-java copies a trait into generated schemas only if it is a "runtime trait". [CodeGenerationContext.collectRuntimeTraits](https://github.com/smithy-lang/smithy-java/blob/main/codegen/codegen-core/src/main/java/software/amazon/smithy/java/codegen/CodeGenerationContext.java) builds that set from a fixed prelude list, plus the `traits` of every `@protocolDefinition` and `@authDefinition` on the service, plus `runtimeTraits` from the codegen settings. Because `grpcProto` lists `idx`, every generated member schema carries its index, with no generator change.

### A service

```smithy
$version: "2.1"

namespace com.example.inventory

use example.protocols#grpcProto
use smithy.protocols#indexed

@grpcProto
@indexed
service Inventory {
    version: "2026-11-01"
    operations: [GetItem]
}

operation GetItem {
    input := {
        @required
        1. sku: String
        2. includeTags: Boolean
    }

    output := {
        @required
        1. sku: String
        2. quantity: Integer
        3. tags: [String]
    }

    errors: [ItemNotFound]
}

@error("client")
@httpError(404)
structure ItemNotFound {
    1. message: String
}
```

`1. sku` is IDL 2.1 shorthand for `@idx(1) sku`, and `tags: [String]` is an IDL 2.1 inline list. Leave out an index and `@indexed` rejects the model:

```text
[ERROR] com.example.inventory#GetItemInput$includeTags: Structure `com.example.inventory#GetItemInput`
is in the closure of an indexed service, but its member `com.example.inventory#GetItemInput$includeTags`
is missing an idx trait | IndexedTrait
```

The `.proto` file that describes the same wire format:

```proto
syntax = "proto3";
package com.example.inventory;

service Inventory {
  rpc GetItem(GetItemInput) returns (GetItemOutput);
}

message GetItemInput {
  string sku = 1;
  optional bool includeTags = 2;
}

message GetItemOutput {
  string sku = 1;
  optional int32 quantity = 2;
  repeated string tags = 3;
}
```

`optional` matters. In proto3, a plain `int32` field that is 0 isn't sent, so the receiver can't tell 0 from "not set". Smithy members without `@required` or `@default` are nullable, so they map to `optional` (explicit presence), and the codec writes a field whenever it is set, even to 0.

## Step 2: the codec

### Shape to wire mapping

Protobuf has five wire types, and the codec picks one for each Smithy type. The mapping is the codec's main design decision:

| Smithy shape | Wire type | Protobuf equivalent |
|---|---|---|
| `boolean`, `byte`, `short`, `integer`, `long`, `intEnum` | 0: varint | `bool`, `int32`, `int64`, enum numbers |
| `float` | 5: 32-bit | `float` |
| `double` | 1: 64-bit | `double` |
| `string`, `enum` | 2: length-delimited | `string` (enum values as their strings, since Smithy enum values are strings and their order isn't stable) |
| `blob` | 2: length-delimited | `bytes` |
| `bigInteger`, `bigDecimal` | 2: length-delimited | `string`, as rpcv2Json does with `useStringForArbitraryPrecision` |
| `timestamp` | 2: length-delimited | a nested `{1: int64 seconds, 2: int32 nanos}`, the layout of `google.protobuf.Timestamp` |
| `structure` | 2: length-delimited | a nested message |
| `union` | the member's own type | `oneof`: exactly one field is present |
| `list` | repeated fields; numbers in one packed length-delimited field | `repeated` |
| `map` | repeated `{1: key, 2: value}` entries | `map<K, V>` |
| `document` | none | not supported |

Two Smithy features don't fit. Protobuf has no `null` inside a repeated field, so `@sparse` lists and maps can't be sent, and there is no schemaless value for `document`. The protocol trait should reject both with `@traitValidators`, the way `rpcv2Cbor` warns about documents (`rpcv2Cbor.NoDocuments`).

### The bytes

Each field is a tag, `(idx << 3) | wireType`, as a varint, followed by the value. The request `GetItem(sku: "A-100", includeTags: true)` is 9 bytes:

| Bytes | Meaning |
|---|---|
| `0A` | tag: idx 1, wire type 2 (length-delimited) |
| `05 41 2D 31 30 30` | length 5, then `A-100` in UTF-8 |
| `10` | tag: idx 2, wire type 0 (varint) |
| `01` | `true` |

The response `{sku: "A-100", quantity: 150, tags: ["red", "xl"]}` is 19 bytes: `0A 05 41 2D 31 30 30 10 96 01 1A 03 72 65 64 1A 02 78 6C`. 150 needs two varint bytes (`96 01`), and the list is two separate fields with tag `1A` (idx 3, length-delimited).

Field names never appear on the wire. That's what makes the format small, and why the indexes must never change.

### In smithy-java

A codec implements [`Codec`](https://github.com/smithy-lang/smithy-java/blob/main/core/src/main/java/software/amazon/smithy/java/core/serde/Codec.java): `createSerializer(OutputStream)` returns a `ShapeSerializer`, and `createDeserializer(ByteBuffer)` returns a `ShapeDeserializer`. Generated code calls `writeString(schema, value)`, `writeInteger(schema, value)` and so on for each member, so the serializer finds the field number on the member schema:

```java
// Sketch, not shipped code.
final class ProtobufSerializer implements ShapeSerializer {
    private static final TraitKey<IdxTrait> IDX = TraitKey.get(IdxTrait.class);
    private final ProtoWriter out;

    @Override
    public void writeString(Schema schema, String value) {
        int field = schema.expectTrait(IDX).getValue();
        out.writeTag(field, WIRE_LEN);
        out.writeLengthDelimited(value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void writeInteger(Schema schema, int value) {
        out.writeTag(schema.expectTrait(IDX).getValue(), WIRE_VARINT);
        out.writeVarint(value);
    }

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        // A nested message is length-prefixed, so its size is needed before its bytes:
        // serialize it into a scratch buffer first, or compute sizes in a first pass.
        var nested = new ByteBufferOutputStream();
        struct.serializeMembers(new ProtobufSerializer(nested));
        out.writeTag(schema.expectTrait(IDX).getValue(), WIRE_LEN);
        out.writeLengthDelimited(nested.toByteBuffer());
    }
    // ... the other write* methods
}
```

`Schema.memberIndex()` is a different number. It's smithy-java's own position for the member in generated code, not the `idx` trait, and it isn't stable across model changes.

The deserializer is where Protobuf differs most from JSON, XML and CBOR:

- **Members are found by number.** `readStruct(schema, state, consumer)` reads a tag, looks up the member whose `idx` matches, and calls `consumer.accept(state, memberSchema, this)`. Because indexes are contiguous from 1, the lookup can be a plain array built once per schema.
- **Unknown fields are skipped by wire type.** When the server adds `4. warehouse`, an older client reads tag 4, finds no member, and skips the value using its wire type. That's how Protobuf stays forward compatible, and the deserializer must do it, not fail.
- **Repeated fields can be interleaved.** A sender may write `tags`, then `quantity`, then more `tags`. A `ShapeDeserializer` delivers each list member in one `readList` call, so the deserializer has to collect repeated fields before handing them to the builder, or require senders to keep them together.

## Step 3: the protocol

### gRPC on the wire

gRPC defines the HTTP/2 mapping:

| Part | Value |
|---|---|
| Method and path | `POST /com.example.inventory.Inventory/GetItem`: the Protobuf package, service and method. Here the package is the Smithy namespace. |
| Request headers | `content-type: application/grpc+proto`, `te: trailers`, optionally `grpc-timeout` |
| Body | Each message is framed: 1 byte compressed flag, 4 bytes big-endian length, then the message. The request above is `00 00 00 00 09` followed by the 9 bytes. |
| Response | HTTP `:status 200`, headers, one framed message, then **trailers** with `grpc-status` (0 for success) and `grpc-message` |

The status in the trailers, not the HTTP status, says whether the call worked. Errors are usually still HTTP 200.

### The request and response

```text
Request (top to bottom)                             Response (bottom to top)
┌────────────────────────────────────────────┐
│ client.getItem(sku: "A-100", includeTags:  │
│ true)                                      │
└────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────┐
│ Codec: ProtobufSerializer writes 0A 05 41  │
│ 2D 31 30 30 10 01 (9 bytes)                │
└────────────────────────────────────────────┘      ┌────────────────────────────────────────────┐
          │                                         │ GetItemOutput, or ItemNotFound             │
          ▼                                         └────────────────────────────────────────────┘
┌────────────────────────────────────────────┐                ▲
│ Protocol: POST                             │                │
│ /com.example.inventory.Inventory/GetItem,  │      ┌─────────┴──────────────────────────────────┐
│ content-type: application/grpc+proto, te:  │      │ Codec: ProtobufDeserializer reads tags,    │
│ trailers; body = 00 00 00 00 09 + message  │      │ finds members by idx, skips unknown fields │
└────────────────────────────────────────────┘      └────────────────────────────────────────────┘
          │                                                   ▲
          ▼                                                   │
┌────────────────────────────────────────────┐      ┌─────────┴──────────────────────────────────┐
│ Auth: for example authorization: Bearer    │      │ Protocol: grpc-status 0 -> strip the       │
│ ...                                        │      │ frame, decode; otherwise the error path    │
└────────────────────────────────────────────┘      └────────────────────────────────────────────┘
          │                                                   ▲
          ▼                                                   │
┌────────────────────────────────────────────┐      ┌─────────┴──────────────────────────────────┐
│ Transport: one HTTP/2 stream: HEADERS,     │      │ Transport: HEADERS (:status 200), DATA,    │
│ DATA, END_STREAM                           │      │ then trailers with grpc-status             │
└─────────┬──────────────────────────────────┘      └────────────────────────────────────────────┘
          │                                                   ▲
          ▼                                                   │
┌─────────────────────────────────────────────────────────────┴──────────────────────────────────┐
│ gRPC server for com.example.inventory.Inventory, in any language (grpc-java, grpc-go, ...)     │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
```

Because the wire format is plain gRPC, the server doesn't have to be generated from Smithy. A grpc-go or grpc-java server built from the `.proto` above can answer this client, as long as both sides agree on the field numbers.

### In smithy-java

The rpcv2 protocols show the shape of the code. [`RpcV2JsonProtocol`](https://github.com/smithy-lang/smithy-java/blob/main/client/client-rpcv2-json/src/main/java/software/amazon/smithy/java/client/rpcv2json/RpcV2JsonProtocol.java) is about 30 lines: it extends [`AbstractRpcV2ClientProtocol`](https://github.com/smithy-lang/smithy-java/blob/main/client/client-rpcv2/src/main/java/software/amazon/smithy/java/client/rpcv2/AbstractRpcV2ClientProtocol.java) and supplies a `JsonCodec`. The abstract class builds `POST /service/<Service>/operation/<Operation>` in `createRequest`, and in `deserializeResponse` sends any non-200 status to an `HttpErrorDeserializer`.

gRPC can't reuse that class: the path is different, the body is framed, and success is decided by a trailer. So `GrpcProtoClientProtocol` extends `HttpClientProtocol` directly:

- **`createRequest`:** the path is `"/" + service.getNamespace() + "." + service.getName() + "/" + operation.schema().id().getName()`. It sets the gRPC headers and writes the 5-byte prefix followed by `codec.serialize(input)`.
- **`deserializeResponse`:** it reads the body, then the trailers. smithy-java's HTTP API exposes trailers through [`TrailerSupport`](https://github.com/smithy-lang/smithy-java/blob/main/http/http-api/src/main/java/software/amazon/smithy/java/http/api/TrailerSupport.java): `if (response.body() instanceof TrailerSupport ts)`, then `ts.trailerHeaders()` once the body is read. If `grpc-status` is 0, it strips the frame and calls `codec.deserializeShape(bytes, operation.outputBuilder())`. Otherwise it takes the error path below.
- **A `Factory`:** it implements `ClientProtocolFactory<GrpcProtoTrait>`, returns the trait's shape id from `id()`, and is listed in `META-INF/services/software.amazon.smithy.java.client.core.ClientProtocolFactory`, exactly as rpcv2-json [lists its factory](https://github.com/smithy-lang/smithy-java/blob/main/client/client-rpcv2-json/src/main/resources/META-INF/services/software.amazon.smithy.java.client.core.ClientProtocolFactory).

The transport has to support HTTP/2 and trailers. smithy-java's own client, `SmithyHttpClientTransport` (`client-http-smithy`), does: its integration tests include `TrailerHeadersHttp2Test`. The java.net.http-based `JavaHttpClientTransport` can't be used, because `java.net.http.HttpClient` doesn't expose response trailers.

### Errors

gRPC has 17 status codes and no place for a typed error body. Its richer error model puts a `google.rpc.Status` message, base64-encoded, in the `grpc-status-details-bin` trailer. That message's `details` field holds `Any` values, and each `Any` has a `type_url` naming the message type. That's where the Smithy error goes:

```text
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Trailers: grpc-status: 5, grpc-message: No%20item%20A-100, grpc-status-details-bin: <base64    │
│ google.rpc.Status>                                                                             │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Protocol: grpc-status is not 0. Decode google.rpc.Status; details[0] is an Any whose type_url  │
│ names the Smithy error                                                                         │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ TypeRegistry: type.googleapis.com/com.example.inventory.ItemNotFound ->                        │
│ com.example.inventory#ItemNotFound                                                             │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Codec: ProtobufDeserializer fills ItemNotFound from the Any's value bytes                      │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Caller gets ItemNotFound. Without details, a generic error built from grpc-status and          │
│ grpc-message                                                                                   │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
```

The server picks `grpc-status` from the error's `@httpError`, the trait the protocol definition lists:

| `@httpError` | `grpc-status` |
|---|---|
| 400 | 3 `INVALID_ARGUMENT` |
| 401 | 16 `UNAUTHENTICATED` |
| 403 | 7 `PERMISSION_DENIED` |
| 404 | 5 `NOT_FOUND` |
| 409 | 6 `ALREADY_EXISTS` |
| 429 | 8 `RESOURCE_EXHAUSTED` |
| 500 | 13 `INTERNAL` |
| 503 | 14 `UNAVAILABLE` |

The status also decides retries. `UNAVAILABLE` and `RESOURCE_EXHAUSTED` should be classified as retryable, which happens in the protocol's error handling, before the retry strategy sees the error.

## Step 4: tests

Protocol tests are traits on the test model, as for every other protocol (see [protocols and serialization](protocols-and-serde.md#who-implements-what)). Binary bodies are compared as bytes: `rpcv2Cbor` tests give the body base64-encoded with `bodyMediaType: "application/cbor"`. A Protobuf suite would do the same with `application/grpc+proto` and the framed bytes:

```smithy
apply GetItem @httpRequestTests([
    {
        id: "GrpcProtoGetItem"
        protocol: grpcProto
        method: "POST"
        uri: "/com.example.inventory.Inventory/GetItem"
        headers: { "content-type": "application/grpc+proto", "te": "trailers" }
        body: "AAAAAAkKBUEtMTAwEAE="
        bodyMediaType: "application/grpc+proto"
        params: { sku: "A-100", includeTags: true }
    }
])
```

The body is the 5-byte frame plus the 9 message bytes, base64-encoded. smithy-java's `client-rpcv2-json/build.gradle.kts` shows how a protocol module turns such a suite into JUnit tests: `addGenerateSrcsTask(generator, "rpcv2Json", "smithy.protocoltests.rpcv2Json#RpcV2JsonProtocol")`.

## The same protocol in other runtimes

The steps are the same everywhere; only the interfaces have different names:

| Runtime | Codec | Protocol | HTTP/2 and trailers |
|---|---|---|---|
| smithy-java | `Codec`, `ShapeSerializer`, `ShapeDeserializer` | `ClientProtocol` plus a `ClientProtocolFactory` in `META-INF/services` | `SmithyHttpClientTransport`, `TrailerSupport` |
| smithy-go | `smithy.ShapeSerializer` and `ShapeDeserializer` | `ClientProtocol` in `transport/http` ([protocols and serialization](protocols-and-serde.md#schema-serde-the-direction-everything-is-moving)) | `net/http`: `Response.Trailer` once the body is read |
| smithy-php | a `smithy/codec` format | a `smithy/protocol` protocol ([the ecosystem](ecosystem.md)) | the `smithy/http` cURL client; trailer support not checked |

A generator only needs to know about the new protocol where it picks a default for the client. smithy-java's `ClientInterfaceGenerator` finds the factory with `ServiceLoader` at code-generation time, matching the `protocol` codegen setting, so the protocol jar must be on the codegen classpath as well as the runtime one.

Next: [Adding a protocol: JSON-RPC 2.0](new-protocol-json-rpc.md).
