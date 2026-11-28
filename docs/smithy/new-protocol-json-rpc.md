# Adding a protocol: JSON-RPC 2.0

[JSON-RPC 2.0](https://www.jsonrpc.org/specification) is one of the most widely used RPC formats: the Language Server Protocol, the Model Context Protocol and Ethereum nodes all speak it. This page walks through adding it to the Smithy stack as a new protocol. Like the [Protobuf over gRPC page](new-protocol-protobuf.md), it's a design exercise built on the real extension points. A real definition exists too, from the jsonrpclib library; it's in this repo, and [compared below](#a-real-definition-jsonrpclib).

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

### `rpcv2Json` and JSON-RPC 2.0 side by side

The names are close, but the protocols are unrelated. The "v2" in `rpcv2Json` means Smithy's second generation of RPC protocols, after `awsJson`. The "2.0" in JSON-RPC is the version of an independent spec. The `rpcv2Json` column follows the [Smithy RPC v2 JSON spec](https://smithy.io/2.0/additional-specs/protocols/smithy-rpc-v2-json.html).

| | `rpcv2Json` | JSON-RPC 2.0 |
|---|---|---|
| Defined by | Smithy: the protocol trait `smithy.protocols#rpcv2Json`, the JSON sibling of `rpcv2Cbor` | An open spec at [jsonrpc.org](https://www.jsonrpc.org/specification), with no schema language |
| Used by | Smithy services and clients | Language servers (LSP), MCP, Ethereum node APIs, and other blockchain node RPC interfaces |
| Transport | HTTP only. The trait's `http` list names the versions (`h2`, `http/1.1`), and the protocol relies on HTTP paths, headers and status codes. | Transport-agnostic: the same messages go over HTTP, WebSockets, raw TCP or stdio |
| Where the call goes | `POST {prefix?}/service/{serviceName}/operation/{operationName}`. Servers route on the last four path segments, so a prefix such as `/v1` can sit in front. Both names are shape names, never absolute shape ids. | Wherever the transport delivers it. Over HTTP the path isn't defined; the operation is `"method"` in the body. |
| Required headers | `Smithy-Protocol: rpc-v2-json` on every request and response. `Content-Type: application/json` and `Accept: application/json` when there's a body. | None defined. Over HTTP, `Content-Type: application/json` by convention. |
| Forbidden headers | `X-Amz-Target` and `X-Amzn-Target`: servers must reject requests that carry them | None |
| Body marker | None. The body is the bare input or output structure. | `"jsonrpc": "2.0"` in every request and response object |
| `Unit` input or output | No body, and no `Content-Type` | `params` left out, or `"result": null` |
| Success | Status `200`, the output as the body | Over HTTP, usually `200`, with `{"jsonrpc": "2.0", "result": ..., "id": ...}` |
| Error signalling | An HTTP status: the error's `@httpError`, else `500` for `@error("server")`, else `400` | Usually HTTP `200` as well. The spec doesn't define HTTP; the error is in the body. |
| Error type | The body field `__type`, holding the error's absolute shape id. `X-Amzn-ErrorType` should not be sent and clients must ignore it; `code` and `Code` must not be used. | `{"error": {"code": -32004, "message": "...", "data": ...}}`, with a numeric code |
| Correlation | One call per HTTP request; no ids | `id`, echoed back, so one connection can carry many calls, batches and notifications |
| Malformed responses | If the `Smithy-Protocol` header doesn't match, the client handles the response by status code alone | No equivalent |

In short, `rpcv2Json` is "Smithy shapes over HTTP": HTTP does the routing and error signalling, and the body is just the shape. JSON-RPC is "everything in the message": the envelope carries the method, the id and the error, and the transport is only a pipe.

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

### A real definition: jsonrpclib

[jsonrpclib](https://github.com/neandertech/jsonrpclib) is a Scala JSON-RPC library with a Smithy protocol of its own, used with smithy4s (`jsonrpclib-smithy4s`). Its definition is in this repo at [jsonrpc.smithy](../../modules/client-codegen/src/main/resources/META-INF/smithy/jsonrpc.smithy). It's listed in the directory's `manifest`, so every model the generator loads can use `jsonrpc#jsonRpc` without importing a file. Three things differ from the original: the namespace is `jsonrpc` rather than `jsonrpclib`; `$version` is 2.1 like the repo's other models; and the four `alloy#` traits are dropped from the protocol's list. Models written for jsonrpclib-smithy4s say `use jsonrpclib#...`, so they need those lines changed to load here, and models written here need the reverse to work with jsonrpclib-smithy4s.

The alloy traits describe JSON shapes that Smithy's core traits can't, and that APIs designed outside Smithy use a lot:

| Trait | What it lets a model say | Example |
|---|---|---|
| `alloy#uuidFormat` | This string is a UUID, so generated code uses a UUID type | `"requestId": "3f2a…"` |
| `alloy#discriminated` | A union is a flat object with a tag field | `{"kind": "markdown", "value": "…"}` instead of `{"markdown": {…}}` |
| `alloy#untagged` | A union with no tag; the reader tries each branch | LSP's `string \| MarkupContent` |
| `alloy#nullable` | An explicit `null` differs from a missing member | LSP's `"result": null` |

Nothing in this repo implements them (`JsonCodec` writes unions only as `{"member": …}`, and has no explicit nulls), and listing them in the protocol makes the definition depend on `alloy-core`. Without it on the classpath, the definition fails to load, as it did in the VS Code Smithy language server: `Shape ID 'alloy#uuidFormat' was not found in the model`. So this copy drops them. Add them back, with `alloy-core`, to model an API such as LSP or MCP whose JSON uses those shapes.

It makes different choices from the design above:

| | The design on this page (`jsonRpc2`) | jsonrpclib (`jsonrpc#jsonRpc` here) |
|---|---|---|
| Method name | The operation's name (`GetItem`) | Given on each operation: `@jsonRpcRequest("textDocument/hover")`. Real JSON-RPC APIs use names like that, which aren't valid Smithy identifiers. |
| Notifications (no `id`, no response) | Not modelled | `@jsonRpcNotification("exit")`; an operation can't be both (the two traits conflict) |
| `params` | The whole input structure | The input structure, or one member bound with `@jsonRpcPayload`, like `@httpPayload`. Its own trait validator allows that only on a top-level input, output or error member. |
| Error codes | `@jsonRpcErrorCode` on each error | No error-code trait |
| Traits the protocol keeps | `httpError`, `jsonRpcErrorCode`, `jsonName` | Its three traits, `jsonName`, `timestampFormat` and the constraint traits. The original also lists alloy's `uuidFormat`, `discriminated`, `nullable` and `untagged`; this repo's copy drops them (see above). |

The notification and per-operation method name are what a protocol like the Language Server Protocol needs: an LSP model is written like this:

```smithy
use jsonrpc#jsonRpc
use jsonrpc#jsonRpcNotification
use jsonrpc#jsonRpcPayload
use jsonrpc#jsonRpcRequest

@jsonRpc
service Lsp {
    operations: [Hover, Exit]
}

@jsonRpcRequest("textDocument/hover")
operation Hover {
    input := {
        @required
        @jsonRpcPayload
        params: HoverParams
    }
    output := {
        contents: String
    }
}

@jsonRpcNotification("exit")
operation Exit {}

structure HoverParams {
    @required
    uri: String
    line: Integer
}
```

[JsonRpcSpec](../../modules/client-codegen/src/test/scala/smithy/scala/codegen/JsonRpcSpec.scala) checks that a model like this loads strictly, that an operation marked both a request and a notification is rejected, that `@jsonRpcPayload` on a nested member is rejected, and that this repo's client generator, which doesn't implement JSON-RPC yet, says so by naming `jsonrpc#jsonRpc` in its error.

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
