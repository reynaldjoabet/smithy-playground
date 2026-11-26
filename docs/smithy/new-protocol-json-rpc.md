# Adding a protocol: JSON-RPC 2.0

[JSON-RPC 2.0](https://www.jsonrpc.org/specification) is one of the most widely used RPC formats: the Language Server Protocol, the Model Context Protocol and Ethereum nodes all speak it. This page walks through adding it to the Smithy stack as a new protocol. Like the [Protobuf over gRPC page](new-protocol-protobuf.md), it's a design exercise built on the real extension points, not a protocol any Smithy runtime ships.

It's the easy case. JSON-RPC bodies are JSON, and every runtime already has a JSON codec. Only the **protocol** is new: the envelope around the body, where the operation name goes, and how errors come back.

## What changes

```text
┌─ Model: new trait ─────────────────────────────────────────────────────────────────────────────┐
│ example.protocols#jsonRpc2 on the service, plus @jsonRpcErrorCode on each error                │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                                                 ▼
┌─ Generated code: unchanged ────────────────────────────────────────────────────────────────────┐
│ Same generators and the same schemas; jsonRpcErrorCode is listed in @protocolDefinition, so    │
│ errors carry it                                                                                │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                                                 ▼
┌─ Protocol: new ────────────────────────────────────────────────────────────────────────────────┐
│ JsonRpc2ClientProtocol: POST to the endpoint, the jsonrpc/method/params/id envelope, result or │
│ error in the response                                                                          │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                                                 ▼
┌─ Codec: reused ────────────────────────────────────────────────────────────────────────────────┐
│ JsonCodec, exactly as restJson1, awsJson1_0 and rpcv2Json use it                               │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                                                 ▼
┌─ Auth: unchanged ──────────────────────────────────────────────────────────────────────────────┐
│ Any scheme that adds headers                                                                   │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                                                 ▼
┌─ Transport: unchanged ─────────────────────────────────────────────────────────────────────────┐
│ Any HTTP/1.1 or HTTP/2 transport                                                               │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
```

Smithy itself has done exactly this recently. `smithy.protocols#rpcv2Json` is a new protocol on the existing JSON codec, and smithy-java's whole client for it is [`RpcV2JsonProtocol`](https://github.com/smithy-lang/smithy-java/blob/main/client/client-rpcv2-json/src/main/java/software/amazon/smithy/java/client/rpcv2json/RpcV2JsonProtocol.java), about 30 lines: a `JsonCodec`, a shared RPCv2 base class, and a factory.

## Three RPC protocols over JSON

JSON-RPC sits next to two protocols the stack already has. All three send an operation's input as a JSON object; they differ only in the envelope:

| | `awsJson1_0` | `rpcv2Json` | JSON-RPC 2.0 |
|---|---|---|---|
| Operation name | `X-Amz-Target: Inventory.GetItem` header | path: `/service/Inventory/operation/GetItem` | body: `"method": "GetItem"` |
| Request body | the input | the input | `{"jsonrpc": "2.0", "method", "params": <input>, "id"}` |
| Success | 200, the output as the body | 200, the output as the body | 200, `{"jsonrpc": "2.0", "result": <output>, "id"}` |
| Error | 4xx or 5xx; `__type` in the body or `X-Amzn-Errortype` | 4xx or 5xx; `__type` in the body | usually 200; `{"error": {"code", "message", "data"}}` |
| Request ids | none | none | `id`, echoed in the response |

The first two put the protocol in HTTP, and the body is just the shape. JSON-RPC puts everything in the body and uses HTTP only as a pipe, which is why it also runs over WebSockets and stdio (MCP and LSP both use stdio).

## Step 1: the model

### The protocol trait and an error-code trait

```smithy
$version: "2.1"

namespace example.protocols

use smithy.api#httpError

/// JSON-RPC 2.0 over HTTP.
@trait(selector: "service")
@protocolDefinition(traits: [httpError, jsonRpcErrorCode, smithy.api#jsonName])
structure jsonRpc2 {}

/// The JSON-RPC error.code for a modeled error.
@trait(selector: "structure[trait|error]")
@range(min: -32099, max: -32000)
integer jsonRpcErrorCode
```

JSON-RPC errors have a numeric `code`. The specification reserves -32768 to -32000 for itself and leaves -32099 to -32000 for "implementation-defined server errors", so the trait's `@range` keeps modeled errors in that band. `@jsonName` is listed so that renamed members keep their JSON names, as in `restJson1`.

As the [Protobuf page](new-protocol-protobuf.md#the-protocol-trait) explains, the `traits` list decides what smithy-java puts in generated schemas, so `jsonRpcErrorCode` reaches the runtime without a generator change.

### A service

The same inventory service as on the Protobuf page, without the member indexes:

```smithy
$version: "2.1"

namespace com.example.inventory.rpc

use example.protocols#jsonRpc2
use example.protocols#jsonRpcErrorCode

@jsonRpc2
service Inventory {
    version: "2026-11-01"
    operations: [GetItem]
}

operation GetItem {
    input := {
        @required
        sku: String
        includeTags: Boolean
    }

    output := {
        @required
        sku: String
        quantity: Integer
        tags: [String]
    }

    errors: [ItemNotFound]
}

@error("client")
@httpError(404)
@jsonRpcErrorCode(-32004)
structure ItemNotFound {
    message: String
}
```

Both models on these pages validate with Smithy 1.75.1.

## Step 2: the codec

Nothing to write. The protocol hands `JsonCodec` the input, and it writes `{"sku": "A-100", "includeTags": true}` exactly as for `restJson1`. The protocol owns everything around that object.

## Step 3: the protocol

### On the wire

A request:

```http
POST / HTTP/1.1
Content-Type: application/json

{"jsonrpc": "2.0", "method": "GetItem", "params": {"sku": "A-100", "includeTags": true}, "id": 7}
```

A success:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"jsonrpc": "2.0", "result": {"sku": "A-100", "quantity": 150, "tags": ["red", "xl"]}, "id": 7}
```

The protocol makes a few choices that JSON-RPC leaves open:

- **The method name** is the operation name. A service that wants `inventory/getItem` style names, as MCP's `tools/call`, would add an operation-level trait for it.
- **`params`** is always an object (the input structure), never the positional array form.
- **`id`** is a counter per client. The protocol checks that the response echoes it.
- **The path** is the endpoint as resolved; JSON-RPC doesn't define one.

### The request and response

```text
Request (top to bottom)                             Response (bottom to top)
┌────────────────────────────────────────────┐
│ client.getItem(sku: "A-100", includeTags:  │
│ true)                                      │
└────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────┐      ┌────────────────────────────────────────────┐
│ Codec: JsonCodec writes {"sku": "A-100",   │      │ GetItemOutput, or ItemNotFound             │
│ "includeTags": true}                       │      └────────────────────────────────────────────┘
└────────────────────────────────────────────┘                ▲
          │                                                   │
          ▼                                         ┌─────────┴──────────────────────────────────┐
┌────────────────────────────────────────────┐      │ Codec: JsonCodec reads result (or          │
│ Protocol: wraps it in {"jsonrpc": "2.0",   │      │ error.data) into the shape                 │
│ "method": "GetItem", "params": {...},      │      └────────────────────────────────────────────┘
│ "id": 7} and POSTs it as application/json  │                ▲
└────────────────────────────────────────────┘                │
          │                                         ┌─────────┴──────────────────────────────────┐
          ▼                                         │ Protocol: id matches; result -> output,    │
┌────────────────────────────────────────────┐      │ error -> the error path                    │
│ Auth: unchanged                            │      └────────────────────────────────────────────┘
└────────────────────────────────────────────┘                ▲
          │                                                   │
          ▼                                         ┌─────────┴──────────────────────────────────┐
┌────────────────────────────────────────────┐      │ Transport: HTTP 200, for results and for   │
│ Transport: any HTTP client                 │      │ errors                                     │
└─────────┬──────────────────────────────────┘      └────────────────────────────────────────────┘
          │                                                   ▲
          ▼                                                   │
┌─────────────────────────────────────────────────────────────┴──────────────────────────────────┐
│ JSON-RPC 2.0 server: a Smithy server with this protocol, or any server that speaks JSON-RPC    │
│ over HTTP                                                                                      │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### In smithy-java

`JsonRpc2ClientProtocol` extends `HttpClientProtocol`, the base the RPCv2 and AWS protocols use. It can't extend `AbstractRpcV2ClientProtocol`, because that class fixes the path and treats every non-200 status as an error, and JSON-RPC errors arrive with 200.

The envelope is easiest to build with smithy-java's `Document` type, which the codec serializes like any shape:

```java
// Sketch, not shipped code.
@Override
public <I extends SerializableStruct, O extends SerializableStruct> HttpRequest createRequest(
        ApiOperation<I, O> operation, I input, Context context, SmithyUri endpoint) {
    var envelope = Document.of(Map.of(
            "jsonrpc", Document.of("2.0"),
            "method", Document.of(operation.schema().id().getName()),
            "params", Document.of(input),
            "id", Document.of(nextId.incrementAndGet())));
    var request = HttpRequest.create();
    request.setMethod("POST");
    request.setUri(endpoint);
    request.addHeader(HeaderName.CONTENT_TYPE, "application/json");
    request.setBody(codec.serializeToDataStream(envelope, "application/json"));
    return request;
}

@Override
public <I extends SerializableStruct, O extends SerializableStruct> O deserializeResponse(
        ApiOperation<I, O> operation, Context context, TypeRegistry typeRegistry,
        HttpRequest request, HttpResponse response) {
    var body = codec.createDeserializer(response.body().asByteBuffer()).readDocument();
    var error = body.getMember("error");
    if (error != null) {
        throw createError(error, typeRegistry);  // the error path below
    }
    return body.getMember("result").asShape(operation.outputBuilder());
}
```

`Document.of(SerializableShape)`, `getMember` and `asShape(ShapeBuilder)` are all in smithy-java's [`Document`](https://github.com/smithy-lang/smithy-java/blob/main/core/src/main/java/software/amazon/smithy/java/core/serde/document/Document.java). Going through a `Document` costs one extra copy of the response. A faster version reads the envelope with the `ShapeDeserializer` directly and hands it the `result` member, the way `JsonCodec` already streams fields.

The factory and its `META-INF/services/software.amazon.smithy.java.client.core.ClientProtocolFactory` entry are the same as for any protocol (see [the Protobuf page](new-protocol-protobuf.md#in-smithy-java-1)).

### Errors

The error `code` is for clients that don't know Smithy, such as a generic JSON-RPC library. A Smithy client needs the exact shape, so the server also puts the shape id in `error.data.__type`, the same field `rpcv2Json` and `awsJson` use. `error.data` holds the error structure's members.

```text
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Body: {"jsonrpc": "2.0", "id": 7, "error": {"code": -32004, "message": "No item A-100",        │
│ "data": {"__type": "com.example.inventory.rpc#ItemNotFound", "message": "No item A-100"}}}     │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Protocol: error.data.__type names the shape; error.code is there for clients that don't know   │
│ Smithy                                                                                         │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ TypeRegistry: com.example.inventory.rpc#ItemNotFound -> the ItemNotFound builder               │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Codec: JsonCodec fills ItemNotFound from error.data                                            │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Caller gets ItemNotFound. No __type (for example -32601 Method not found): a generic error     │
│ with code and message                                                                          │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
```

The reserved codes cover failures that happen before an operation runs:

| Code | Meaning | Comparable to |
|---|---|---|
| -32700 | Parse error: the body isn't JSON | a serialization error |
| -32600 | Invalid Request: not a valid envelope | 400 |
| -32601 | Method not found | an unknown operation |
| -32602 | Invalid params | a validation error (400) |
| -32603 | Internal error | 500 |
| -32099 to -32000 | Server-defined | modeled errors, through `@jsonRpcErrorCode` |

Because the HTTP status is 200 even for errors, retry classification can't use it. The protocol has to decide from the error: modeled errors marked `@retryable` are retryable, and the reserved codes aren't. A server can still send 429 or 503 for throttling before it reads the body, so the protocol should check the HTTP status first.

### What doesn't map

Two JSON-RPC features have no Smithy equivalent:

- **Batches.** A request may be an array of calls, answered by an array of responses. A Smithy operation is one call, so a client sends one envelope per request. A server should still accept batches from other clients.
- **Notifications.** A request without `id` gets no response at all. The closest Smithy shape is an operation with `Unit` output, but the client would never learn about errors, so this protocol always sends an `id`.

## Step 4: tests

The protocol tests compare JSON bodies semantically, as `restJson1` tests do, with `bodyMediaType: "application/json"`:

```smithy
apply GetItem @httpRequestTests([
    {
        id: "JsonRpc2GetItem"
        protocol: jsonRpc2
        method: "POST"
        uri: "/"
        headers: { "Content-Type": "application/json" }
        body: """
            {"jsonrpc": "2.0", "method": "GetItem", "params": {"sku": "A-100", "includeTags": true}, "id": 1}"""
        bodyMediaType: "application/json"
        params: { sku: "A-100", includeTags: true }
    }
])
```

The test pins `"id": 1` because it is the first call on a new client. A test for the error path uses `@httpResponseTests` with status 200 and the error body above.

## Comparing the two examples

| | Protobuf over gRPC | JSON-RPC 2.0 |
|---|---|---|
| New model traits | protocol trait; uses Smithy's own `@idx` and `@indexed` | protocol trait, `@jsonRpcErrorCode` |
| Generator changes | none | none |
| New codec | yes: Protobuf wire format | no: `JsonCodec` |
| New protocol | yes: gRPC paths, framing, trailers | yes: the envelope |
| Transport | HTTP/2 with trailers | any HTTP |
| Error type from | `grpc-status-details-bin` | `error.data.__type` |
| Talks to non-Smithy peers | gRPC servers built from the matching `.proto` | any JSON-RPC 2.0 peer |

In both cases the generated code is the same as for any other protocol. That's the layering of [the protocol stack](protocol-stack.md) at work: generated code holds the model, codecs hold formats, and protocols hold HTTP.

Next: [A Scala client generator](scala-client.md).
