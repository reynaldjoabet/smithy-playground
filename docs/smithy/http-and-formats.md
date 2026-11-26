# HTTP, data formats and protocols

The other pages assume you know how HTTP, JSON and an RPC protocol such as `awsJson1_0` or JSON-RPC 2.0 relate. This page explains it from the ground up, using real requests from the [AWS Emulator pages](../aws-emulator/README.md).

In short:
- **HTTP** moves a request and a response between two programs.
- **JSON** (or XML, CBOR, Protobuf) is how values are written as bytes inside them.
- **A schema** such as a Smithy model or a `.proto` file says which values are valid in the first place.
- **A protocol** such as `awsJson1_0`, `restJson1` or JSON-RPC 2.0 is the agreement on how an operation call maps onto HTTP and a data format.

## What HTTP is

HTTP (HyperText Transfer Protocol) is a request/response protocol. A client sends one **request**; the server sends back one **response**. Each message has three parts:

| | Request | Response |
|---|---|---|
| First line | a **method** and a **target**: `POST /` | a **status code** and reason: `200 OK` |
| Headers | `Name: value` lines describing the message: `Host`, `Content-Type`, `Content-Length`, `Authorization`... | the same kind of lines: `content-type`, `content-length`, `date`, request ids... |
| Body | optional bytes: the data being sent | optional bytes: the answer |

HTTP gives these parts their meaning ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110)). It doesn't say what the body contains. A `Content-Type` header names the body's format, and the two sides must agree on it.

HTTP is also **stateless**: each request stands alone, and the server keeps nothing between requests unless something such as a cookie or a token carries it along.

## What HTTP leaves out

HTTP defines how a client and a server exchange requests and responses, and what the method, target, status code and headers mean. It leaves two big things to other specifications: what the body looks like, and who may do what.

### What's in the body

To HTTP, a body is just bytes. `Content-Type` names their format, but HTTP defines none of the formats. JSON, XML, CBOR and the rest are each specified elsewhere, and a higher-level protocol picks one and says how values are laid out in it (see [Data formats](#data-formats) and [Protocols](#protocols-putting-a-call-on-http)).

To see why that matters, compare HTTP with protocols that build their kind of resource into the protocol itself:

| Protocol | What it knows about | Its commands | Example |
|---|---|---|---|
| FTP ([RFC 959](https://www.rfc-editor.org/rfc/rfc959)) | files and directories | `RETR`, `STOR`, `LIST`, `CWD`, and a transfer type (`TYPE A` for text, `TYPE I` for binary) | `RETR reports/2026-11.csv` |
| SMTP ([RFC 5321](https://www.rfc-editor.org/rfc/rfc5321)) | mail messages, senders and recipients | `MAIL FROM`, `RCPT TO`, `DATA`; the message itself follows the header/body format of [RFC 5322](https://www.rfc-editor.org/rfc/rfc5322) | `RCPT TO:<ops@example.com>` |
| [PostgreSQL wire protocol](https://www.postgresql.org/docs/current/protocol.html) | SQL queries, rows and typed columns | `Query`, `Parse`/`Bind`/`Execute`; replies `RowDescription` (column names and type OIDs) and `DataRow` | a `Query` message carrying `SELECT * FROM transfers` |

Each of these can only move its one kind of thing, and both sides already agree on what a file, a message or a row is.

HTTP has one kind of thing, a **resource** named by a URI, and says nothing about what is behind it. These four requests look alike on the wire, but the servers do entirely different things:

```http
# 1. Static file on disk (Nginx serving a Swagger file)
GET /docs/openapi.json HTTP/1.1

# 2. In-memory JVM state (a ConcurrentHashMap or ZIO Ref inside your service)
GET /v1/rate-limits/client_123 HTTP/1.1

# 3. Database row + ledger state transition (PostgreSQL ACID transaction)
POST /v1/transfers HTTP/1.1

# 4. Proxy / orchestrator calling a third-party clearing rail (e.g., PAPSS / SWIFT)
POST /v1/fx-quotes HTTP/1.1
```

The client can't tell which is which, and doesn't need to. That generality is what lets HTTP carry every AWS API. It is also why HTTP can't say anything about the body: the meaning of a request lives in an agreement on top of HTTP.

### Who may do what

HTTP has an **authentication** framework ([RFC 9110, section 11](https://www.rfc-editor.org/rfc/rfc9110#section-11)). A server answers `401 Unauthorized` with a `WWW-Authenticate` header naming a scheme, and the client sends credentials in an `Authorization` header. The `Basic` scheme ([RFC 7617](https://www.rfc-editor.org/rfc/rfc7617)) sends a user name and password, base64-encoded: readable by anyone who sees the request, so it is only safe over TLS.

HTTP defines nothing about **authorization**: which caller may run which operation on which resource. A server can refuse with `403 Forbidden`, but making the decision is up to the application. (Despite its name, `401` means "I don't know who you are"; `403` means "I know, and the answer is no".) Other specifications fill the gap:
- **OAuth 2.0** ([RFC 6749](https://www.rfc-editor.org/rfc/rfc6749)): an authorization server issues an access token, limited to some scopes. The client sends it as `Authorization: Bearer <token>` ([RFC 6750](https://www.rfc-editor.org/rfc/rfc6750)), and the server holding the resource checks the token before acting. OpenID Connect adds the user's identity on top.
- **AWS SigV4 and IAM**: the request is signed with the caller's keys ([the real request below](#one-real-request-taken-apart) shows one), and IAM policies then decide whether that identity may call that action on that resource.

## The layers

From your code down to the network, a call passes through these layers. Each one knows only about the layer below:

```text
┌─ Your code ──────────────────────────────────────────────────────────────────────────────────────┐
│ client.sendMessage(SendMessageRequest(queueUrl, "hello"))                                        │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │   a protocol decides how the call travels
                                                 ▼
┌─ Protocol (RPC style) ───────────────────────────────────────────────────────────────────────────┐
│ awsJson1_0, restJson1, rpcv2Json, JSON-RPC 2.0, gRPC: where the operation name goes (header,     │
│ path or body), which HTTP method and status codes mean what, how errors are recognised           │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │   it uses a data format for the payload
                                                 ▼
┌─ Data format (the body) ─────────────────────────────────────────────────────────────────────────┐
│ JSON, XML, CBOR, form encoding, Protobuf: how values become bytes. A format knows nothing about  │
│ operations, URLs or errors                                                                       │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │   the bytes travel as an HTTP message
                                                 ▼
┌─ HTTP ───────────────────────────────────────────────────────────────────────────────────────────┐
│ method, path, headers, status code and a body: one request, one response. HTTP/1.1 sends them as │
│ text lines; HTTP/2 as binary frames on multiplexed streams                                       │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │   HTTPS is HTTP inside TLS
                                                 ▼
┌─ TLS ────────────────────────────────────────────────────────────────────────────────────────────┐
│ encryption and server authentication (the certificate the client must trust)                     │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                                                 ▼
┌─ TCP (QUIC for HTTP/3) ──────────────────────────────────────────────────────────────────────────┐
│ a reliable byte stream between two machines                                                      │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘```

[The protocol stack](protocol-stack.md) shows these layers inside each Smithy runtime. This page is about the boundaries between them: HTTP, the data format, and the protocol that combines the two.

## One real request, taken apart

This is SQS `CreateQueue`, sent to the AWS Emulator and captured with every header (from [sqs.md](../aws-emulator/sqs.md)):

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.CreateQueue
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183051Z
Content-Length: 62
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sqs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "QueueName": "orders",
  "Attributes": {
    "VisibilityTimeout": "30"
  }
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
x-amzn-requestid: 29babfc6-1dfd-495d-abaf-69bb2c2a6eb5
content-length: 57
date: Thu, 26 Nov 2026 18:30:51 GMT

{
  "QueueUrl": "https://127.0.0.1:9297/000000000000/orders"
}
```

Each part belongs to a different layer:

| Part | Layer | What it means |
|---|---|---|
| `POST`, `/`, `HTTP/1.1` | HTTP | Send data to the server's root path, using HTTP version 1.1. |
| `Host`, `Content-Length`, `date` | HTTP | Which server is meant; the body's size; when the response was made. |
| `200 OK` | HTTP | The request succeeded. |
| The body's `{ ... }` | JSON (the data format) | Values written as JSON: an object with a string and a nested object. |
| `Content-Type: application/x-amz-json-1.0` | HTTP naming the format, chosen by the protocol | "The body is JSON, written by the awsJson 1.0 rules." |
| `X-Amz-Target: AmazonSQS.CreateQueue` | the `awsJson1_0` protocol | Which operation is called. HTTP doesn't care about this header; the protocol does. |
| `POST` to `/` for every operation | the `awsJson1_0` protocol | The protocol sends every call the same way and names the operation in a header. |
| `X-Amz-Date`, `Authorization` | SigV4 (auth) | A signature over the method, path, headers and body, proving who sent it and that nothing changed. |
| `x-amzn-requestid` | the service | An id for this call, useful when reporting a problem. |

Notice what JSON alone doesn't say: which operation this is, where to send it, or how an error would look. Those come from the protocol.

## Methods and status codes

HTTP defines a small set of **methods**. Two properties matter for clients that retry:
- **Safe:** the method doesn't change anything on the server.
- **Idempotent:** sending the request twice has the same effect as sending it once.

| Method | Usual meaning | Safe | Idempotent |
|---|---|---|---|
| `GET` | read a resource | yes | yes |
| `HEAD` | `GET` without the body | yes | yes |
| `PUT` | create or replace at this path | no | yes |
| `DELETE` | delete at this path | no | yes |
| `POST` | anything else: create, run an action | no | no |
| `PATCH` | change part of a resource | no | no |

**Status codes** come in classes:

| Class | Meaning | Examples |
|---|---|---|
| `2xx` | success | `200 OK`, `201 Created`, `204 No Content` |
| `3xx` | go elsewhere | `301 Moved Permanently`, `304 Not Modified` |
| `4xx` | the client's request is wrong | `400 Bad Request`, `403 Forbidden`, `404 Not Found`, `429 Too Many Requests` |
| `5xx` | the server failed | `500 Internal Server Error`, `501 Not Implemented`, `503 Service Unavailable` |

Protocols use these differently. `restJson1` and `restXml` use the whole vocabulary: `GET` to read, `PUT` to create, `404` when something doesn't exist. RPC protocols (`awsJson1_0`, `rpcv2Json`, JSON-RPC 2.0, gRPC) send every call as `POST`, so a retry can't rely on the method being idempotent. Smithy marks such operations with `@idempotent` or `@idempotencyToken` instead.

## HTTP versions

The meaning of a request is the same in every version; how it travels differs:

| Version | On the wire | Notes |
|---|---|---|
| HTTP/1.1 ([RFC 9112](https://www.rfc-editor.org/rfc/rfc9112)) | Text lines, as in the example above. One request at a time per connection. | What the AWS Emulator speaks. The JDK's HTTP client tries HTTP/2 first over TLS, and with the emulator it settles on HTTP/1.1. |
| HTTP/2 ([RFC 9113](https://www.rfc-editor.org/rfc/rfc9113)) | Binary frames. Many requests share one connection as **streams**, and headers are compressed. | Header names are lowercase. **Trailers** (headers sent after the body) are routine; gRPC needs them. |
| HTTP/3 ([RFC 9114](https://www.rfc-editor.org/rfc/rfc9114)) | Like HTTP/2, but over QUIC (UDP) instead of TCP. | Avoids TCP's head-of-line blocking. |

**HTTPS** is HTTP sent inside a TLS connection. The client checks the server's certificate, which is why the emulator's clients are given its `ca.pem`.

## WebSockets

WebSocket ([RFC 6455](https://www.rfc-editor.org/rfc/rfc6455)) sits at the same layer as HTTP: both are application protocols running over TCP, or over TLS for `wss://`. But it is a different protocol, and it starts life as an HTTP request.

The client sends an ordinary HTTP/1.1 request asking to switch protocols:

```http
GET /chat HTTP/1.1
Host: example.com
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13
```

The server agrees with `101 Switching Protocols`. From then on, the same TCP connection carries WebSocket frames, not HTTP. HTTP is only the doorway. Over HTTP/2 and HTTP/3 the doorway is an extended `CONNECT` request instead ([RFC 8441](https://www.rfc-editor.org/rfc/rfc8441), [RFC 9220](https://www.rfc-editor.org/rfc/rfc9220)).

After that, the two work differently:

| | HTTP | WebSocket |
|---|---|---|
| Exchange | request then response, started by the client | messages either way, whenever either side wants |
| Lifetime | one exchange (connections can be reused) | one long-lived connection |
| Per-message metadata | method, path, headers, status code | none: a text or binary frame (plus control frames such as ping and close) |
| Matching replies to requests | built in: one response per request | not built in: the protocol on top must add ids |
| Errors | status codes | none per message: the protocol on top defines them |

In the layer diagram above, WebSocket sits beside HTTP, not inside it:

```text
┌─ Protocols over HTTP ─────────────────────────┐  ┌─ Protocols over WebSocket ────────────────────┐
│ awsJson1_0, restJson1, rpcv2Json, gRPC,       │  │ JSON-RPC 2.0 (it puts everything in the       │
│ JSON-RPC 2.0                                  │  │ body), or an application's own messages       │
└───────────────────────────────────────────────┘  └───────────────────────────────────────────────┘
                        │                                                  │
                        ▼                                                  ▼
┌─ HTTP ────────────────────────────────────────┐  ┌─ WebSocket ───────────────────────────────────┐
│ one request, one response: method, path,      │  │ messages either way on one long-lived         │
│ headers, status code, body                    │  │ connection, opened by an HTTP Upgrade request │
└───────────────────────────────────────────────┘  └───────────────────────────────────────────────┘
                        │                                                  │
                        ▼                                                  ▼
┌─ TLS ────────────────────────────────────────────────────────────────────────────────────────────┐
│ https:// and wss://: encryption and server authentication                                        │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                 │
                                                 ▼
┌─ TCP ────────────────────────────────────────────────────────────────────────────────────────────┐
│ a reliable byte stream between two machines                                                      │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘```

- **Above WebSocket, data formats work the same way.** The messages are usually JSON, sometimes binary such as Protobuf or CBOR.
- **On top of those, RPC protocols.** JSON-RPC 2.0 is the classic example. Ethereum nodes, for instance, serve JSON-RPC over WebSocket for subscriptions. This is exactly why JSON-RPC carries an `id`: WebSocket has no request/response pairing, so the envelope supplies it.
- **Raw HTTP-based protocols can't move over.** `awsJson1_0`, `restJson1` and `rpcv2Json` rely on HTTP paths, headers and status codes, which WebSocket messages don't have. JSON-RPC moves easily because it puts everything in the body.

Smithy's own streaming doesn't use WebSockets. Event streams (`@streaming` unions) travel as `application/vnd.amazon.eventstream` frames in a long-running HTTP request: HTTP/2, or chunked HTTP/1.1. AWS does offer WebSockets as a product, through API Gateway v2's WebSocket APIs ([apigatewayv2.md](../aws-emulator/apigatewayv2.md)). A backend pushes a message to a connected client with the `apigatewaymanagementapi` service's `PostToConnection`, which is itself an ordinary signed HTTP call. The WebSocket exists only between API Gateway and the end client.

## Format, schema and protocol: three separate jobs

People often say "JSON", "Smithy" or "gRPC" as if they were alternatives. They answer different questions, and a real API picks one of each:

| Job | Answers | Examples | Paper analogy |
|---|---|---|---|
| **Data format** (serialization, wire format) | How do runtime values become bytes (text or binary) to store or send? Numbers, strings, lists, maps. | JSON, CBOR, XML, MessagePack, the Protobuf wire format | The language and alphabet the form is written in |
| **Schema / IDL** (Interface Definition Language) | Which shapes are valid: field names, types, which fields are required, allowed ranges, which operations exist? | Smithy, OpenAPI, JSON Schema, Protobuf `.proto` files, XSD | The blueprint: what a correctly filled-in form looks like |
| **RPC / API protocol** | How is a remote action called: where the operation name goes, how a response is matched to its request, how typed errors come back? | `awsJson1_0`, `restJson1`, `rpcv2Json`, JSON-RPC 2.0, gRPC | The routing rules: which office the form goes to and how the reply comes back |

The filled-in form you actually post is a **message**: one value, written in a data format, shaped by a schema, sent according to a protocol.

Some names cover more than one job, which is where the confusion comes from:
- **Protobuf** is both a schema language (`.proto` files) and a binary data format. gRPC is the protocol that usually carries it.
- **Smithy** is a schema language. Its protocol traits (`@awsJson1_0`, `@rpcv2Json`, ...) pick a protocol, and the protocol picks the data format.
- **OpenAPI** is a schema for REST APIs. REST over HTTP is the protocol it describes; JSON is usually the format.
- **JSON Schema** validates JSON documents, but says nothing about how they are sent.

## Data formats

A data format turns values into bytes and back. It is only that: it has no idea of requests, operations or errors.

| Format | What it looks like | Media type | Smithy protocols that use it |
|---|---|---|---|
| JSON ([RFC 8259](https://www.rfc-editor.org/rfc/rfc8259)) | text: `{"QueueName":"orders"}` | `application/json` | `restJson1`, `awsJson1_0`/`1_1` (as `application/x-amz-json-1.0`/`1.1`), `rpcv2Json` |
| XML | text: `<QueueName>orders</QueueName>` | `application/xml`, `text/xml` | `restXml`, responses of `awsQuery` and `ec2Query` |
| Form encoding | text: `Action=CreateQueue&QueueName=orders` | `application/x-www-form-urlencoded` | requests of `awsQuery` and `ec2Query` |
| CBOR ([RFC 8949](https://www.rfc-editor.org/rfc/rfc8949)) | binary, JSON-like | `application/cbor` | `rpcv2Cbor` |
| Protobuf | binary, fields identified by number | `application/grpc+proto` (in gRPC) | none built in; see [the Protobuf design](new-protocol-protobuf.md) |

JSON has strings, numbers, booleans, `null`, arrays and objects, and nothing else. Everything richer is a convention set by the protocol on top:
- timestamps (epoch seconds, or an ISO 8601 string?);
- binary data (base64 text);
- big numbers;
- `NaN` (sent as the string `"NaN"`);
- which JSON key holds which member.

That is why Smithy's codecs take settings from the protocol (`@jsonName` and `@timestampFormat` apply in some protocols and not others).

## Protocols: putting a call on HTTP

A **protocol** answers the questions HTTP and JSON leave open:
- Where does the operation name go?
- Which HTTP method, and which path?
- Where are the inputs: path, query string, headers, or body?
- How is success told from failure, and which error was it?
- Is HTTP even required?

Here is the same idea, creating a queue named `orders`, in five styles. The `awsJson1_0` example is real. The others show how SQS's call would look if it used that protocol: SQS doesn't, but the shapes are what each protocol prescribes.

**`awsJson1_0`**: RPC over HTTP. The operation is named in a header, and every call is `POST /`.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.CreateQueue

{"QueueName":"orders"}
```

**`restJson1`**: REST style. The method and path carry meaning, and members can live in the path, query or headers (`@http`, `@httpLabel`, `@httpQuery`, `@httpHeader`).

```http
PUT /queues/orders HTTP/1.1
Content-Type: application/json

{"attributes":{"VisibilityTimeout":"30"}}
```

**`rpcv2Json`**: Smithy's RPC protocol. The operation is in the path, and a header names the protocol.

```http
POST /service/AmazonSQS/operation/CreateQueue HTTP/1.1
Smithy-Protocol: rpc-v2-json
Content-Type: application/json
Accept: application/json

{"QueueName":"orders"}
```

**JSON-RPC 2.0**: everything goes in a JSON envelope. HTTP is only a carrier, and it could just as well be WebSockets or stdio.

```http
POST / HTTP/1.1
Content-Type: application/json

{"jsonrpc":"2.0","method":"CreateQueue","params":{"QueueName":"orders"},"id":1}
```

**gRPC**: HTTP/2 only, with a binary Protobuf body. The result status arrives in trailers.

```text
:method: POST
:path: /com.amazonaws.sqs.AmazonSQS/CreateQueue
content-type: application/grpc+proto
te: trailers

<5-byte frame header><Protobuf bytes>
```

How each one handles the questions above:

| | `awsJson1_0` | `restJson1` | `rpcv2Json` | JSON-RPC 2.0 | gRPC |
|---|---|---|---|---|---|
| Operation name | `X-Amz-Target` header | method + path | path | `"method"` in the body | path |
| Inputs | JSON body | path, query, headers, body | JSON body | `"params"` in the body | Protobuf body |
| Success | `200` | `2xx` (from `@http`) | `200` | usually `200`, with `"result"` | `200`, and `grpc-status: 0` in trailers |
| Error | `4xx`/`5xx`, `__type` or `X-Amzn-Errortype` | `4xx`/`5xx`, `X-Amzn-Errortype` or the body | `4xx`/`5xx`, `__type` | usually `200`, with `"error": {"code", "message"}` | `grpc-status` and `grpc-message` trailers |
| Needs HTTP | yes | yes | yes | no | yes (HTTP/2) |
| Format | JSON | JSON | JSON | JSON | Protobuf |

Two patterns show up:
- **REST** uses HTTP's own vocabulary (methods, paths, status codes) to say what happens. Generic tools such as caches and proxies understand it, but each operation needs mapping rules.
- **RPC** treats HTTP as a pipe for "call this operation with these arguments". It's simpler to generate, and all operations look alike. JSON-RPC goes furthest: it doesn't need HTTP at all.

### Where the line between HTTP and JSON falls

Each protocol draws the line between HTTP and the body in a different place. Here is one call, reading the balance of account `acc_123`, moving from "HTTP does most of the work" to "the body does all of it".

**JSON on its own** is only a data format. `{"balance": 4200, "currency": "MUR"}` knows nothing about networks, URLs, methods or errors. When it travels over HTTP, HTTP carries the bytes and `Content-Type: application/json` tells the receiver to run a JSON parser on the body. Everything else comes from the protocol.

**REST** uses HTTP itself as the application protocol. The verb and the resource URL say what happens, and the status code says how it went.

```http
GET /accounts/acc_123/balance HTTP/1.1
Accept: application/json
```

- Action and target: split between the verb and the URL (`GET /accounts/acc_123`, `DELETE /accounts/acc_123`).
- Errors: plain HTTP status codes (`404 Not Found`, `422 Unprocessable Entity`).
- Body: plain JSON for the resource's state. There is no request body here at all.

**`rpcv2Json`** keeps RPC routing in HTTP, so the body stays clean.

```http
POST /service/LedgerService/operation/GetBalance HTTP/1.1
Content-Type: application/json
Smithy-Protocol: rpc-v2-json

{"accountId": "acc_123"}
```

- Action and target: the URL path names the service and operation, and the `Smithy-Protocol` header names the protocol.
- Errors: `4xx`/`5xx` status codes, with `__type` in the JSON body naming the error shape.
- Body: only the operation's input.
- Coupling: it can't run without HTTP, because the operation name lives in the URL path.

**JSON-RPC 2.0** treats HTTP as a dumb byte pipe. Routing and errors are repeated inside the JSON, so the exact same message works over WebSockets or stdio.

```http
POST /rpc HTTP/1.1
Content-Type: application/json

{"jsonrpc": "2.0", "id": 1, "method": "GetBalance", "params": {"accountId": "acc_123"}}
```

- Action and target: always one endpoint (`POST /rpc`), with `"method"` in the body.
- Errors: almost always `200 OK` at the HTTP layer, with the failure in the body: `"error": {"code": -32601, "message": "..."}`.
- Body: an envelope carrying the version, `id`, method and params.

| | Operation | Input | Error signal | Works without HTTP |
|---|---|---|---|---|
| REST | verb + URL | URL (and body for writes) | HTTP status | no |
| `rpcv2Json` | URL path + `Smithy-Protocol` header | body | HTTP status + `__type` | no |
| JSON-RPC 2.0 | `"method"` in the body | `"params"` in the body | `"error"` in the body | yes |

## Where Smithy fits

A Smithy model describes operations and shapes independently of all this. A **protocol trait** on the service, such as `@awsJson1_0`, `@restJson1` or `@rpcv2Json`, selects one set of the rules above. Each runtime then splits the work:
- **The codec** handles the data format: JSON, XML or CBOR bytes.
- **The protocol** handles the HTTP mapping: method, path, headers, status codes, error codes.
- **Auth** signs the finished HTTP request.
- **The transport** sends it.

So one model can be served over different protocols, and one codec can serve several protocols. `JsonCodec` is shared by `awsJson1_0`, `restJson1` and `rpcv2Json`.

### Traits fill in what HTTP leaves open

Smithy adds protocol-level meaning with annotations, which it calls **traits**. A trait is attached to a shape (a service, an operation, a member) and says something the shape alone doesn't: how it travels, how it's authenticated, how it fails. Here is the transfers call from [What HTTP leaves out](#what-http-leaves-out), modelled for `restJson1`:

```smithy
$version: "2"
namespace example.payments

use aws.protocols#restJson1

@restJson1
@httpBearerAuth
service Payments {
    operations: [CreateTransfer]
}

@http(method: "POST", uri: "/v1/transfers", code: 201)
operation CreateTransfer {
    input := {
        @httpHeader("Idempotency-Key")
        @idempotencyToken
        idempotencyKey: String

        @required
        amount: Long

        @required
        currency: String
    }
    output := {
        @required
        transferId: String
    }
    errors: [InsufficientFunds]
}

@error("client")
@httpError(422)
structure InsufficientFunds {
    message: String
}
```

| What HTTP leaves open | Traits that answer it |
|---|---|
| The body's format, and how an operation maps onto HTTP | a protocol trait: `@restJson1`, `@awsJson1_0`, `@rpcv2Json`, `@rpcv2Cbor`, `@restXml` |
| Which method and path, and where each input goes | `@http`, `@httpLabel`, `@httpQuery`, `@httpHeader`, `@httpPayload` (used by REST protocols; RPC protocols ignore them) |
| Which status code means which error | `@error`, `@httpError` |
| Whether a retry is safe | `@readonly`, `@idempotent`, `@idempotencyToken` |
| How the caller proves who they are | `@httpBasicAuth`, `@httpBearerAuth`, `@httpApiKeyAuth`, `@aws.auth#sigv4`; `@auth` sets their order per service or operation, `@optionalAuth` allows anonymous calls |

Authorization stays outside the model, as it does in HTTP. Smithy has no OAuth trait: `@httpBearerAuth` says "send a bearer token", and how the client got the token, and what the server decides when it sees it, are up to them.

## Further reading

- [The protocol stack](protocol-stack.md): what each layer does inside each Smithy runtime.
- [Protocols and serialization](protocols-and-serde.md): the AWS protocols in detail, and who implements them.
- [Adding a protocol: JSON-RPC 2.0](new-protocol-json-rpc.md): JSON-RPC as a Smithy protocol, and how it differs from `rpcv2Json`.
- [Adding a protocol: Protobuf over gRPC](new-protocol-protobuf.md): a binary format and HTTP/2 trailers.
- [RFC 6455](https://www.rfc-editor.org/rfc/rfc6455): the WebSocket protocol.
- [The AWS Emulator pages](../aws-emulator/README.md): every protocol above that AWS uses, captured with every header.

Back to [the overview](README.md).
