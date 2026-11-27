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

## HTTP and REST

"HTTP" and "REST" are often used as if they meant the same thing. They don't, but they were designed side by side, which is why they fit so well.

### REST is a style; HTTP is a protocol

- **HTTP** is a concrete protocol with a specification ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110) for its semantics): methods, status codes, headers, caching rules. Anything that sends `GET /path HTTP/1.1` is using HTTP.
- **REST** (Representational State Transfer) is an *architectural style*: a set of constraints for building distributed systems. Roy Fielding described it in his 2000 dissertation as the architecture of the Web, while he was also an author of the HTTP/1.1 specification. REST says how a system should behave; HTTP is the protocol most often used to build one.

So you can use HTTP without REST. `awsJson1_0`, `rpcv2Json` and JSON-RPC all run over HTTP and ignore most of what makes it RESTful. A "REST API" is one that uses HTTP's own features (URLs, methods, status codes, headers) as the application's vocabulary, instead of tunnelling calls through it.

### How each REST idea maps onto HTTP

| REST constraint or idea | What it means | The HTTP feature that delivers it |
|---|---|---|
| **Resources with identifiers** | Every thing the API exposes has its own address | URLs: `/2015-03-31/functions/my-fn` |
| **Representations** | A client never touches the resource itself, only a representation of its state | The body, labelled by `Content-Type` (`application/json`, `application/xml`) |
| **Content negotiation** | The client says which representations it can handle | `Accept`, `Accept-Language`, `Accept-Encoding` |
| **Uniform interface** | The same small set of operations works on every resource | The methods: `GET`, `PUT`, `POST`, `DELETE`, `PATCH`, `HEAD` |
| **Self-descriptive messages** | A message says what happened without outside knowledge | Status codes (`201 Created`, `404 Not Found`, `409 Conflict`) and headers (`Location`, `Content-Length`) |
| **Stateless** | Each request carries everything needed to handle it; no server-side session between requests | Credentials on every request (`Authorization`), no connection-bound state |
| **Cacheable** | Responses say whether and how long they can be reused | `Cache-Control`, `ETag`, `Last-Modified`, conditional requests, `304 Not Modified` |
| **Layered system** | Intermediaries can sit between client and server without either knowing | Proxies, gateways and CDNs that understand methods, status codes and cache headers |
| **Hypermedia** | Responses link to what the client can do next | `Link` headers, or links inside the body. Rarely used in practice (see below). |

The payoff of following the style is that generic infrastructure understands your API without being told:
- A CDN can cache a `GET` that returns `Cache-Control: max-age=60`.
- A proxy can retry an idempotent `PUT` after a dropped connection.
- A browser can follow a `Location` header.

None of that works for `POST / {"action": "..."}`, because to the infrastructure every call looks the same.

### What the methods promise

RFC 9110 gives each method properties that clients and intermediaries rely on:

| Method | Safe (no intended change) | Idempotent (repeating has the same effect) | Cacheable | Typical use |
|---|---|---|---|---|
| `GET` | yes | yes | yes | Read a resource |
| `HEAD` | yes | yes | yes | Read only the headers (does it exist, how big, what `ETag`) |
| `OPTIONS` | yes | yes | no | Ask what's allowed; CORS preflight |
| `PUT` | no | yes | no | Create or replace a resource at a known URL |
| `DELETE` | no | yes | no | Remove a resource |
| `POST` | no | **no** | rarely | Create in a collection, or run an action |
| `PATCH` ([RFC 5789](https://www.rfc-editor.org/rfc/rfc5789)) | no | **no** | no | Change part of a resource |

**Idempotent** is about the *effect*, not the response. Deleting a Lambda function twice leaves the same state: it's gone. The second call answers `404`, but nothing new happened. That's what makes a retry safe after a timeout, when you don't know whether the first call arrived. `POST` has no such promise, so retrying it can create a second resource. That's the gap that idempotency keys fill (`Idempotency-Key`, and Smithy's `@idempotencyToken`).

Smithy models the same promises as traits on operations, independent of the protocol:
- **`@readonly`** is the counterpart of a safe method.
- **`@idempotent`** is the counterpart of an idempotent one.

Real models don't always line up with the HTTP method. Lambda's `CreateFunction` is a `POST` but carries `@idempotent`. S3's `GetObject` is a `GET` without `@readonly`. Smithy tooling reads the traits, so the traits are what to trust.

### "REST API" in practice

Few APIs follow every constraint. A common way to describe how far one goes is Leonard Richardson's maturity model:

| Level | What the API uses | Examples |
|---|---|---|
| 0 | One URL, one method; the action is in the body or a header | `awsJson1_0` (`POST /` with `X-Amz-Target: AmazonSQS.SendMessage`), JSON-RPC 2.0 (`POST /rpc` with `"method"` in the body), SOAP |
| 1 | Separate URLs, but still one method | `rpcv2Json` looks like this (`POST /service/Weather/operation/GetForecast`), though its URLs name operations rather than resources |
| 2 | Resources, the HTTP methods, and status codes | `restJson1` and `restXml`: Lambda, S3, API Gateway. Most APIs called "REST" are here. |
| 3 | Level 2, plus hypermedia: responses link to the next possible actions | Rare. Smithy doesn't model it, and AWS doesn't use it. |

So "REST API" usually means level 2: resource URLs, the right methods, meaningful status codes, and JSON or XML bodies.

### Using HTTP without being RESTful

You can use HTTP without being RESTful, as awsJson, rpcv2Json and JSON-RPC do. They use HTTP only to carry messages. They keep a few REST constraints and drop the ones that make an API RESTful:

| REST constraint | `awsJson1_0` | `rpcv2Json` | JSON-RPC 2.0 |
|---|---|---|---|
| **Resources with their own URLs** | No: every call goes to `POST /` | No: URLs name operations (`/service/Weather/operation/GetForecast`), not things | No: every call goes to one URL (`POST /rpc`) |
| **Methods mean something** | No: always `POST`, even for reads | No: always `POST` | No: always `POST` |
| **Status codes describe the result** | Partly: `200` or `4xx`/`5xx`, with the error shape named in `__type` or `X-Amzn-Errortype` | Partly: `4xx`/`5xx`, with `__type` in the body | No: usually `200` even for errors; the failure is in `"error"` in the body |
| **Cacheable** | No: `POST` responses aren't cached | No | No |
| **Stateless** | Yes: each request is signed and self-contained | Yes | Yes |
| **Layered system** | Yes: proxies and load balancers pass the traffic through | Yes | Yes |

What that looks like on the wire. Reading a DynamoDB item is a read, but the protocol still sends it as a `POST` to `/`:

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.GetItem

{"TableName":"orders","Key":{"id":{"S":"o-1"}}}
```

A RESTful design would put the same read in the method and the URL:

```http
GET /tables/orders/items/o-1 HTTP/1.1
```

For DynamoDB's request, HTTP infrastructure sees only "a `POST` to `/`":
- **No caching:** a CDN won't cache it, because `POST` responses aren't cacheable by default.
- **No automatic retries:** a proxy won't retry it on its own, because `POST` isn't idempotent. That's why AWS SDKs decide retries themselves, from the error type and Smithy traits.
- **No per-operation routing:** a load balancer or WAF can't route or rate-limit by operation using the path alone. The operation is in a header (`X-Amz-Target`) or, for JSON-RPC, inside the body.

That isn't a mistake. It's the RPC trade-off:
- **For:** one rule covers every operation, so generators and runtimes stay small, and operations that are actions rather than resources fit naturally (`SendMessage`, `GetItem`, `TransactWriteItems`). JSON-RPC takes this furthest: it doesn't need HTTP at all, so the same message works over WebSockets or stdio.
- **Against:** you lose what HTTP's own vocabulary gives you for free.

### REST in Smithy: two real AWS services

A REST protocol trait (`@restJson1`, `@restXml`) tells generators to honour the HTTP binding traits. They map each operation and member onto parts of the HTTP message:

| Trait | Where it puts things | Applies to |
|---|---|---|
| `@http(method, uri, code)` | The method, a URI pattern, and the success status code | Operation |
| `@httpLabel` | A `{label}` segment of the path | Input member |
| `@httpQuery("name")`, `@httpQueryParams` | A query-string parameter, or a map of them | Input member |
| `@httpHeader("Name")`, `@httpPrefixHeaders("x-amz-meta-")` | A header, or a map of headers sharing a prefix | Input and output members |
| `@httpPayload` | The member *is* the whole body (a blob, a string, or one structure) | Input and output members |
| `@httpResponseCode` | The status code, read into an output member | Output member |
| `@httpError(404)` | The status code for an error | Error structure |
| (no binding trait) | Goes into the JSON or XML body with the other unbound members | Any member |

**Lambda (`restJson1`).** Functions are resources under `/2015-03-31/functions`, and the model's `@http` traits use the methods the way REST intends:

| Operation | `@http` in the model | Smithy trait |
|---|---|---|
| `CreateFunction` | `POST /2015-03-31/functions`, code `201` | `@idempotent` |
| `GetFunction` | `GET /2015-03-31/functions/{FunctionName}`, code `200` | `@readonly` |
| `UpdateFunctionCode` | `PUT /2015-03-31/functions/{FunctionName}/code`, code `200` | |
| `DeleteFunction` | `DELETE /2015-03-31/functions/{FunctionName}` | `@idempotent` |
| `Invoke` | `POST /2015-03-31/functions/{FunctionName}/invocations`, code `200` | |

`GetFunction`'s input has `FunctionName` with `@httpLabel` and `Qualifier` with `@httpQuery("Qualifier")`. A call becomes:

```http
GET /2015-03-31/functions/my-fn?Qualifier=PROD HTTP/1.1
Host: lambda.us-east-1.amazonaws.com
Authorization: AWS4-HMAC-SHA256 Credential=…, SignedHeaders=…, Signature=…
```

There's no request body, because every input member is bound to the URL. If the function doesn't exist, the answer is a status code from the model, `@httpError(404)` on `ResourceNotFoundException`. The error shape is named in the `X-Amzn-ErrorType` header, and its members are in the JSON body (the body below is illustrative):

```http
HTTP/1.1 404 Not Found
Content-Type: application/json
X-Amzn-ErrorType: ResourceNotFoundException

{"message":"Function not found: …"}
```

`Invoke` is an action, not a resource. Lambda models it as a `POST` to a sub-resource, `/invocations`, which is the usual REST compromise for verbs that don't fit `GET`, `PUT` or `DELETE`.

**S3 (`restXml`).** Objects are resources at `/{Bucket}/{Key+}`. The `+` makes `Key` a *greedy* label: it may contain `/`, so `photos/2026/cat.jpg` is one key. `GetObject` binds almost everything to HTTP itself:

| Member of `GetObject` | Binding |
|---|---|
| `Bucket`, `Key` (input) | `@httpLabel`: the path |
| `VersionId` (input) | `@httpQuery("versionId")` |
| `Range` (input) | `@httpHeader("Range")`: fetch part of the object |
| `IfNoneMatch`, `IfMatch` (input) | `@httpHeader("If-None-Match")`, `@httpHeader("If-Match")`: conditional requests |
| `Body` (output) | `@httpPayload`: the object's bytes *are* the response body |
| `ETag`, `ContentType`, `LastModified` (output) | `@httpHeader("ETag")`, `@httpHeader("Content-Type")`, `@httpHeader("Last-Modified")` |

The URI pattern in the model is `GET /{Bucket}/{Key+}?x-id=GetObject`. The `?x-id=GetObject` is a literal query parameter that every `GetObject` request carries. `PutObject` is `PUT` on the same path, `DeleteObject` is `DELETE` (code `204`), and `HeadObject` is `HEAD`.

S3 also shows REST's caching and concurrency features through ordinary headers:

```http
GET /my-bucket/photos/2026/cat.jpg?x-id=GetObject HTTP/1.1
If-None-Match: "9b2cf535f27731c974343645a3985328"

HTTP/1.1 304 Not Modified
ETag: "9b2cf535f27731c974343645a3985328"
```

- **Caching:** the object hasn't changed since the client's copy, so no body is sent. A client or CDN reuses what it has.
- **Optimistic concurrency:** `If-Match` with an `ETag` makes a write succeed only if the object is still the version the client read. Otherwise the server answers `412 Precondition Failed`.

No body format is involved in either case: it's all HTTP.

### Compared with the RPC protocols this repo implements

The same kind of call, "send to this queue", in SQS's RPC protocol:

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.SendMessage

{"QueueUrl":"https://sqs.us-east-1.amazonaws.com/123456789012/orders","MessageBody":"hello"}
```

| | RPC (`awsJson1_0`, as here) | REST (`restJson1`, `restXml`) |
|---|---|---|
| Operation | `X-Amz-Target` header | Method + URI pattern from `@http` |
| Which queue or function | A member in the JSON body | A path label (`@httpLabel`) |
| Options | Members in the body | Query parameters and headers |
| Success | Always `200` | `200`, `201`, `204` …, from `@http(code)` |
| Errors | `4xx`/`5xx`, with `__type` in the body naming the shape | `@httpError` status code, with the shape named in a header or the body |
| Caching, conditional requests | No (every call is a `POST`) | Yes, with ordinary HTTP headers |
| Generator and runtime work | Small: one rule for every operation | More: every member's binding must be honoured |

That last row is why this repo's runtime supports only RPC protocols so far. A `restJson1` client would need:
- **Request URIs:** expanding the URI template, with label escaping and greedy labels.
- **Query and headers:** writing query parameters and headers, with their own formats for timestamps and lists.
- **Bodies:** putting only the *unbound* members in the JSON body, or the `@httpPayload` member on its own.
- **Responses:** reading response headers and status codes back into output members.
- **Errors:** mapping status codes to errors.

Smithy's `HttpBindingIndex` answers "where does this member go" for all of it (see [Knowledge indexes](scala-client.md#knowledge-indexes)). The JSON codec itself wouldn't change: it still writes whatever ends up in the body.

### Trade-offs

REST gives:
- **Infrastructure support:** caching, conditional requests, and retries by intermediaries, all through standard headers.
- **Readable URLs** that tools and people can explore.
- **Bodies that can be anything:** raw bytes, as S3 shows.

It costs:
- **A mapping decision for every operation:** method, path, which member goes where.
- **Awkward fits for actions:** Lambda's `POST …/invocations`, SQS's `SendMessage`.
- **More rules for generators and runtimes** to get right: escaping, greedy labels, header formats.

RPC protocols make the opposite trade:
- **For:** every operation looks the same, so generation and the runtime are simpler.
- **Against:** HTTP infrastructure sees only opaque `POST`s.

AWS uses both: REST for resource-shaped services like S3, Lambda and API Gateway, and RPC for action-shaped ones like SQS, DynamoDB and Kinesis.

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
