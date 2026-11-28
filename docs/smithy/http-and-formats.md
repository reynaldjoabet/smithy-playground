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

### Protocols are not wire formats

`restJson1`, `restXml`, `awsQuery` and JSON-RPC 2.0 are all in the same category, but it isn't wire formats: they're **protocols**. A wire format is one ingredient a protocol picks, and a transport is another:

```text
transport      HTTP/1.1, HTTP/2, WebSocket, stdio      moves bytes
wire format    JSON, XML, CBOR, form, Protobuf          what the bytes mean as data
protocol       restJson1, awsQuery, JSON-RPC 2.0, …     how a call maps onto the two above
```

Most Smithy protocol names pack two choices into one word: a style (REST or RPC) and a format. Splitting them makes the differences clear:

| Protocol | Style | Request format | Response format | Transport | Where the operation goes |
|---|---|---|---|---|---|
| `restJson1` | REST | JSON | JSON | HTTP (required) | Method + URL (`@http`) |
| `restXml` | REST | XML | XML | HTTP (required) | Method + URL (`@http`) |
| `awsJson1_0` / `1_1` | RPC | JSON | JSON | HTTP (required) | `X-Amz-Target` header |
| `rpcv2Json` | RPC | JSON | JSON | HTTP (required) | URL path |
| `rpcv2Cbor` | RPC | CBOR | CBOR | HTTP (required) | URL path |
| `awsQuery` | RPC | form-urlencoded | XML | HTTP (required) | `Action=` in the body |
| `ec2Query` | RPC | form-urlencoded | XML | HTTP (required) | `Action=` in the body |
| JSON-RPC 2.0 | RPC | JSON | JSON | **Any** (HTTP, WebSocket, stdio) | `"method"` in the body |
| gRPC | RPC | Protobuf | Protobuf | HTTP/2 (required) | URL path |

So two protocols can share a wire format and still differ completely. `restJson1`, `awsJson1_0`, `rpcv2Json` and JSON-RPC 2.0 all use JSON, and in this repo they'd all share `JsonCodec`. They differ in where the operation name goes, where the inputs go, and how errors come back. Equally, one style can use different formats: `rpcv2Json` and `rpcv2Cbor` route identically and differ only in the bytes.

The "format" column describes the *body*. In the REST protocols, much of a message isn't in the body at all: path labels, query parameters and headers have their own text rules, and an `@httpPayload` blob is raw bytes. Lambda's `Invoke` is a `restJson1` operation for which the protocol writes no JSON at all (see [REST in Smithy](#rest-in-smithy-two-real-aws-services)).

JSON-RPC 2.0 is the one protocol here that isn't bound to HTTP. The others use HTTP's paths, headers or status codes, so they can't run without it. JSON-RPC puts everything (method, id, params, errors) inside the JSON envelope, so HTTP is only one possible transport.

[One call through each protocol](protocol-flows.md) follows a real operation through this repo for each of them.

### RPC and REST are styles, not protocols

RPC and REST are both styles: two ways of modelling how a client talks to a server. Neither is a protocol. The protocols in the table above are concrete rulebooks, each implementing one style:

```text
style          RPC, REST (also: messaging, GraphQL, …)    how you think about the interaction
protocol       awsJson1_0, restJson1, JSON-RPC 2.0, gRPC   the concrete rules for one style
wire format    JSON, XML, CBOR, form, Protobuf             what the bytes mean
transport      HTTP/1.1, HTTP/2, WebSocket, stdio          moves the bytes
```

The core difference is what the API is built around, and who carries the meaning of a call.
- **RPC** is built around operations: "call `GetForecast` with these arguments". HTTP is mostly a pipe, and the meaning travels in the payload.
- **REST** is built around resources: "`GET` the forecast at `/forecasts/Oslo`". HTTP itself carries the meaning: the method says what to do, the URL says to what, and the status code says how it went.

Everything else follows from that. Here's the same call both ways, from this repo's Weather model:

```http
RPC   POST /service/Weather/operation/GetForecast      {"city":"Oslo","units":"METRIC"}
REST  GET  /forecasts/Oslo?units=METRIC
```

| | RPC (Remote Procedure Call) | REST (Representational State Transfer) |
|---|---|---|
| Core idea | Call a named procedure with arguments, as if it were local | Act on resources through a small, uniform set of operations |
| What the API is made of | **Verbs**: `SendMessage`, `GetForecast`, `CreateUser` | **Nouns**: `/functions/my-fn`, `/my-bucket/cat.jpg`, plus `GET`, `PUT`, `DELETE` |
| Where it comes from | A long-standing idea, usually traced to Birrell and Nelson's 1984 paper "Implementing Remote Procedure Calls" | Defined precisely, as a set of constraints, in Roy Fielding's 2000 dissertation |
| How strict | Loose: any "call this by name" design qualifies | Strict in theory (statelessness, cacheability, uniform interface, hypermedia); "REST API" usually means the looser level 2 |
| Relationship to HTTP | HTTP is optional; it's often just a pipe | Designed alongside HTTP; uses its methods, status codes and caching |
| Concrete protocols | JSON-RPC 2.0, gRPC, `awsJson1_0`, `rpcv2Json`, `rpcv2Cbor`, `awsQuery`, `ec2Query`, XML-RPC, SOAP, Thrift, Java RMI | `restJson1`, `restXml`, most "REST APIs" described by OpenAPI |

Three nuances:
- **There are other styles too.** Messaging and event-driven designs (queues, pub/sub, as with SQS and SNS themselves) and GraphQL (one endpoint, with the client describing the data it wants) fit neither style neatly.
- **The line blurs in practice.** Most "REST APIs" have RPC-shaped endpoints for things that aren't resources, such as Lambda's `POST /functions/{name}/invocations`. Some RPC protocols borrow REST features: `rpcv2Json` puts the operation in the URL path and uses HTTP status codes for errors.
- **Smithy itself is RPC-shaped at heart.** A model is a set of named **operations** with input, output and errors, which is the RPC view. The protocol trait decides how they travel:
  - with an RPC protocol (`@awsJson1_0`), each operation becomes a call by name;
  - with a REST protocol (`@restJson1`), the `@http` bindings project each operation onto a method and a resource URL.

  Smithy also has `resource` shapes, with identifiers and lifecycle operations (create, read, update, delete, list), for modelling in a REST-friendly way.

So "REST or RPC" is a design decision about how the API is shaped. "Which protocol" is the concrete rulebook you pick, and the protocol then fixes the wire format and the transport.

### Other styles over HTTP

REST and RPC are the two big styles, but HTTP carries others too:

| Style | Core idea | How it uses HTTP | Example request |
|---|---|---|---|
| **GraphQL** | The client describes exactly the data it wants, in a query language; one schema covers the whole API | One endpoint, usually `POST /graphql`; errors usually come back in an `errors` array with status `200` | `POST /graphql` `{"query":"{ user(id: 1) { name orders { total } } }"}` |
| **Event-driven: webhooks** | The *server* calls the *client* when something happens; client and server swap roles | Plain `POST` requests to a URL the client registered, usually signed with an HMAC header | Stripe `POST https://you.example/hooks` `{"type":"payment_intent.succeeded",…}` |
| **Streaming: Server-Sent Events** | One long response that the server keeps writing events into | `Content-Type: text/event-stream`, lines of `data: …`, over a single `GET` | LLM APIs streaming tokens, live dashboards |
| **Streaming: long polling** | The client asks, and the server holds the request open until there's news | An ordinary request that the server answers late | SQS `ReceiveMessage` with `WaitTimeSeconds=20` is RPC with long polling |
| **Bidirectional: WebSockets** | Both sides send messages whenever they like | Starts as HTTP (`Upgrade: websocket`, `101 Switching Protocols`), then leaves HTTP (see [WebSockets](#websockets)) | Chat, multiplayer, API Gateway WebSocket APIs |
| **Messaging over HTTP** | Events or messages with a standard envelope, delivered over HTTP | CloudEvents' HTTP binding puts event metadata in `ce-*` headers | `POST /events` with `ce-type: com.example.order.created` |

### Variants of RPC over HTTP

These are all RPC, differing in envelope and format:

| Protocol | What's distinctive |
|---|---|
| **SOAP** | An XML envelope with a large standards family (WS-Security, WS-Addressing), described by WSDL. Still common in banking, telecom and government. |
| **XML-RPC** | JSON-RPC's XML predecessor: `<methodCall><methodName>…`. |
| **JSON-RPC 2.0** | A JSON envelope; independent of transport. |
| **gRPC / gRPC-Web** | Protobuf over HTTP/2 with trailers. gRPC-Web adapts it for browsers. |
| **Connect** | gRPC-compatible, but also works as plain `POST` with JSON over HTTP/1.1, so you can call it with `curl`. |
| **tRPC** | TypeScript-only RPC: the client's types come straight from the server's code. |
| **Smithy's `awsJson`, `rpcv2Json` and `rpcv2Cbor`** | The protocols this repo implements or plans (see [One call through each protocol](protocol-flows.md)). |

### What REST, GraphQL and WebSocket add to HTTP

HTTP provides the mechanisms: methods, status codes, headers, caching rules, content negotiation and connection handling. What it doesn't tell you is how to design an API with them, or what to do when request and response isn't enough. That's what each of these brings:
- **REST** is a discipline for using HTTP's own vocabulary consistently.
- **GraphQL** adds a capability HTTP doesn't have.
- **WebSocket** adds a different kind of connection.

| | What it adds on top of HTTP | What it gives up |
|---|---|---|
| **HTTP alone** | Mechanisms: methods with defined meanings, status codes, headers, caching, content negotiation | No opinion on URLs, bodies, how to model data, or how to push to the client |
| **REST** | **Rules for using those mechanisms consistently**: every thing is a resource with a URL; methods mean what they say (`GET` reads, `PUT` replaces, `DELETE` removes); status codes say what happened; responses say how long they can be cached. It adds constraints, not new machinery. | Each operation needs a mapping decision. Actions that aren't resources fit awkwardly. Clients often need several requests and get fixed response shapes. |
| **GraphQL** | **A typed schema and a query language**: the client chooses exactly which fields it wants and follows relationships in one request; the schema can be explored by introspection; subscriptions give live updates. | Most of HTTP's meaning: everything is usually one `POST /graphql`, errors come back with `200`, and HTTP caching stops working without extra conventions such as persisted queries over `GET`. The server must also guard against expensive queries. |
| **WebSocket** | **A different connection**: full-duplex and long-lived, with the server able to send at any time and low overhead per message. It uses HTTP only to open (`Upgrade: websocket`, then `101 Switching Protocols`), then switches to its own framing ([RFC 6455](https://www.rfc-editor.org/rfc/rfc6455)). | All of HTTP's meaning once connected: no methods, no status codes, no caching, no per-message headers. You must define your own message protocol on top, often JSON-RPC, GraphQL subscriptions or STOMP. |

The same task in each style: show user 1's name and the totals of their last three orders, and update the page when a new order arrives.

**REST:** two requests, each returning a fixed shape, and no push:

```http
GET /users/1                  → {"id":1,"name":"Ada","email":"…","address":{…}, …}
GET /users/1/orders?limit=3   → [{"id":91,"total":42.0,"items":[…], …}, …]
```

To notice new orders you poll, or add webhooks, SSE or a WebSocket. In exchange, both `GET`s can be cached by the browser or a CDN, and retried safely.

**GraphQL:** one request that returns exactly the fields asked for:

```http
POST /graphql
{"query":"{ user(id: 1) { name orders(last: 3) { total } } }"}

→ {"data":{"user":{"name":"Ada","orders":[{"total":42.0},{"total":17.5},{"total":8.0}]}}}
```

New orders come through a subscription, usually carried over a WebSocket. That request can't be cached by HTTP, and if `user` 1 didn't exist you'd likely still get `200`, with an `errors` array.

**WebSocket:** one connection, messages both ways, in a format you define:

```text
→ {"jsonrpc":"2.0","id":1,"method":"getUser","params":{"id":1}}
← {"jsonrpc":"2.0","id":1,"result":{"name":"Ada","orders":[…]}}
← {"jsonrpc":"2.0","method":"orderCreated","params":{"userId":1,"total":99.0}}   (pushed by the server)
```

The client gets new orders immediately. But nothing between client and server understands those messages, and reconnection, ordering and authentication are yours to design.

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

### "Supports HTTP but not REST": three AWS mix-ups

A claim like "the Application Load Balancer doesn't support REST, only HTTP" sounds as if REST were something infrastructure could switch on. It isn't: HTTP is the protocol, and REST is a style you can follow on top of it ([Using HTTP without being RESTful](#using-http-without-being-restful)). A load balancer can't "support REST" or not, because REST isn't something it can see. The claim usually mixes up one of three things.

**1. The load balancer handles HTTP, and REST is irrelevant to it.** An Application Load Balancer (ALB) works at layer 7, the HTTP level. It reads each request and routes it by host, path, method, headers or query string, and it handles HTTP/1.1, HTTP/2, gRPC and WebSocket. It doesn't know or care whether the API behind it is RESTful:

```text
GET  /orders/42                                ─┐
POST /     X-Amz-Target: AmazonSQS.SendMessage ─┼─► ALB matches host, path, method, headers ─► target
POST /rpc  {"jsonrpc":"2.0", ...}              ─┘
```

All three reach the ALB as HTTP requests, whether they come from a RESTful API (level 2), awsJson or JSON-RPC (both level 0). The real contrast is with the Network Load Balancer (NLB), which works at layer 4 (TCP/UDP) and doesn't read HTTP at all.

**2. The API that manages load balancers is not RESTful.** The Elastic Load Balancing v2 control plane (`CreateLoadBalancer`, `DescribeTargetGroups` and so on) uses `awsQuery`, a form-encoded RPC protocol:

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded

Action=CreateLoadBalancer&Version=2015-12-01&Name=my-alb&Subnets.member.1=subnet-a
```

Every call is a `POST /`, and the operation name travels in the body, not in the URL and method. That's plain HTTP, but not REST, so "ELB uses HTTP, not REST" is true of its management API. See [A third style: `awsQuery`](#a-third-style-awsquery-sts-iam-and-elastic-load-balancing-v2) for the full request and response.

**3. "REST" and "HTTP" are API Gateway product names.** API Gateway sells two products, called *REST API* and *HTTP API*:

| | REST API (v1) | HTTP API (v2) |
|---|---|---|
| What it is | The older product, with more features | The newer one: cheaper, faster, fewer features |
| Extras | API keys, usage plans, request validation, caching, AWS WAF, edge-optimized endpoints | Built-in JWT authorizers, simpler setup |
| Can you build a RESTful API on it? | Yes | Yes |
| Can you build an RPC-style one? | Yes | Yes |

Both serve APIs over HTTP, and both can host a RESTful design or an RPC one. The names label feature sets, not architectural styles.

In all three cases everything runs over HTTP. REST describes how an API designs its URLs and uses the methods, and no AWS product checks or enforces it.

### What HTTP looks like when an API isn't RESTful

An API that isn't RESTful still uses HTTP, but it ignores HTTP's own meanings and puts them somewhere else. These are the recognisable signs, each with its RESTful counterpart, using a generic "users" API.

**1. The action is in the URL or the body, not in the method**

```http
POST /api                {"action":"getUser","id":42}        ← one endpoint, action in the body
POST /getUser            {"id":42}                           ← action as the URL ("URL-RPC")
GET  /user.php?op=delete&id=42                               ← action in the query string
```

The RESTful version is `GET /users/42`, or `DELETE /users/42` to remove the user. The URL names the thing, and the method names what to do with it.

**2. Everything is `POST`, even reads**

```http
POST /users/search       {"name":"Ada"}
```

RESTful: `GET /users?name=Ada`. What's lost: a `POST` can't be cached, bookmarked or shared as a link, and a proxy or client won't retry it automatically.

**3. `GET` changes things**

```http
GET /users/42/delete
GET /unsubscribe?email=ada@example.com
```

RESTful: `DELETE /users/42`, or a `POST` from a form. This one isn't only unRESTful, it's dangerous. Link prefetchers, crawlers and security scanners follow `GET` links freely, because `GET` promises to be safe.

**4. The status code doesn't say what happened**

```http
HTTP/1.1 200 OK

{"success":false,"error":"User not found"}
```

RESTful: `404 Not Found`, ideally with a problem body ([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)). What's lost: monitoring, load balancers, retry logic and client libraries all read the status code. Here they see success and have to parse the body to learn otherwise.

**5. The server keeps conversation state**

```http
POST /search        {"name":"A"}        ← server stores the results in your session
GET  /search/next                       ← "next" only means something in that session
```

RESTful: each request carries what it needs, for example `GET /users?name=A&cursor=eyJpZCI6NDJ9`. What's lost: a request can't go to any server, survive a restart, or be retried in isolation, so scaling needs sticky sessions.

**6. Messages don't describe themselves**

```http
POST /api
Content-Type: text/plain

42|Ada|ada@example.com
```

RESTful: an accurate `Content-Type` such as `application/json`, standard headers, and responses that say whether they can be cached. What's lost: nothing between the two ends can interpret the message.

**7. HTTP features are re-implemented in the body**

```json
{"method":"PUT","resource":"/users/42","version":3,"auth":"…","data":{…}}
```

This is SOAP-style tunnelling, or `X-HTTP-Method-Override: DELETE` on a `POST`. RESTful: use the real method, `If-Match: "3"` for the version check, and `Authorization` for credentials.

These patterns are real, not hypothetical. The `POST /api` examples above are composites made up to show the shape, but each pattern has well-known users:

| Pattern | Real examples | Still done? |
|---|---|---|
| **One endpoint, action in the body** | SOAP (the operation in the XML envelope), XML-RPC, JSON-RPC: Bitcoin Core's and Ethereum's JSON-RPC (`POST` with `"method":"eth_getBalance"`). AWS's own `awsQuery` (`Action=CreateUser`) and `awsJson` (`X-Amz-Target`). | Yes, deliberately: it's RPC by design |
| **Action in the URL ("URL-RPC")** | Slack's Web API: `POST https://slack.com/api/chat.postMessage`, `/api/users.info`. Telegram's Bot API: `/bot<token>/sendMessage`. | Yes, common and documented |
| **`200` with the error in the body** | Slack again: failures come back as HTTP `200` with `{"ok":false,"error":"channel_not_found"}`. GraphQL usually returns `200` with an `errors` array. JSON-RPC over HTTP returns `200` with an `"error"` object. | Yes. It's a choice, but it confuses generic HTTP monitoring. |
| **Everything is `POST`, even reads** | GraphQL; DynamoDB `GetItem`; SOAP; Elasticsearch's `_search`, which accepts `POST` because search queries are large JSON bodies. | Yes, often for a reason: query bodies too big for a URL |
| **`GET` that changes things** | In 2005 Google Web Accelerator prefetched links on pages, and apps that deleted things through plain `GET` links (37signals' apps among them) lost data. Email "unsubscribe" links did the same, which is why [RFC 8058](https://www.rfc-editor.org/rfc/rfc8058) defines one-click unsubscribe as a `POST`. | Considered a bug today; still found in old apps |

Sources:
- Slack's `ok`/`error` response format: [Using the Slack Web API](https://api.slack.com/web) and the [`chat.postMessage` reference](https://docs.slack.dev/reference/methods/chat.postMessage/).
- The 2005 incident: 37signals' [contemporary write-up](https://signalvnoise.com/archives2/google_web_accelerator_hey_not_so_fast_an_alert_for_web_app_designers) and the [Google Web Accelerator](https://en.wikipedia.org/wiki/Google_Web_Accelerator) article.

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

### This repo's requests next to RESTful ones

Three of this repo's real operations, as the generated clients send them today, each next to what a RESTful version would look like. The "ours" requests are the ones the tests in `GeneratedClientSpec` assert.

#### 1. Weather `GetForecast` (ours: `rpcv2Json`)

**Ours**: the operation is in the path, everything goes as `POST`, and the input is in the body:

```http
POST /service/Weather/operation/GetForecast HTTP/1.1
Smithy-Protocol: rpc-v2-json
Content-Type: application/json
Accept: application/json

{"city":"Oslo","units":"METRIC","detail":1}
```

Errors come back as a status code plus `__type` in the body:

```http
HTTP/1.1 500 Internal Server Error

{"__type":"example.weather#Busy","message":"later"}
```

**RESTful**: the same model with `@restJson1` and HTTP bindings:

```smithy
@restJson1
service Weather { version: "2026-11-26", operations: [GetForecast, Ping] }

@readonly
@http(method: "GET", uri: "/forecasts/{city}")
@paginated(inputToken: "token", outputToken: "next", items: "days", pageSize: "size")
operation GetForecast {
    input := {
        @required @httpLabel city: String
        @httpQuery("size")   size: Integer
        @httpQuery("token")  token: String
        @httpQuery("units")  units: Units = "METRIC"
        @httpQuery("detail") detail: Detail = 1
    }
    output := { days: Days, next: String, @required type: Kind }
    errors: [Busy]
}

@error("server") @retryable(throttling: true) @httpError(503)
structure Busy { message: String }
```

Now the city is a resource in the URL, the options go in the query string, and there's no request body:

```http
GET /forecasts/Oslo?units=METRIC&detail=1 HTTP/1.1
Accept: application/json
```

The error status comes from `@httpError(503)`:

```http
HTTP/1.1 503 Service Unavailable
X-Amzn-ErrorType: Busy

{"message":"later"}
```

#### 2. SQS `SendMessage` (ours: `awsJson1_0`)

**Ours**: always `POST /`, with the operation named in a header and the queue identified inside the body:

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: AmazonSQS.SendMessage

{"QueueUrl":"https://sqs.us-east-1.amazonaws.com/123456789012/orders","MessageBody":"hello","DelaySeconds":5}
```

**RESTful** (SQS doesn't offer this; it's what the design would look like). The queue is the resource, and sending creates a message in its `messages` collection:

```http
POST /queues/orders/messages HTTP/1.1
Content-Type: application/json

{"MessageBody":"hello","DelaySeconds":5}

HTTP/1.1 201 Created
Location: /queues/orders/messages/5fea7756-0ea4-451a-a703-a558b933e274
```

#### 3. DynamoDB `GetItem` (ours: `awsJson1_0`)

**Ours**: a read, but still a `POST`:

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: DynamoDB_20120810.GetItem

{"TableName":"orders","Key":{"id":{"S":"o-1"}}}
```

If the item doesn't exist, DynamoDB answers `200` with no `Item` in the body. Not found isn't an error in this RPC design.

**RESTful** (again, not something DynamoDB offers):

```http
GET /tables/orders/items/o-1 HTTP/1.1

HTTP/1.1 404 Not Found
```

The status code itself says the item isn't there, and a found item could carry `ETag` and `Cache-Control`.

#### What differs

| | Ours (`awsJson1_0`, `rpcv2Json`) | RESTful (`restJson1`) |
|---|---|---|
| Method | Always `POST` | `GET`, `PUT`, `POST`, `DELETE`, by meaning (`@http`) |
| URL | `/`, or `/service/X/operation/Y` | A resource: `/forecasts/Oslo`, `/queues/orders/messages` |
| Operation name | `X-Amz-Target` header, or the path | Implied by method + URL |
| Inputs | All in the JSON body | Path, query, headers or body, per member binding |
| Success | `200` | `200`, `201 Created` + `Location`, `204`… |
| Not found | `200` with nothing in the body (DynamoDB), or a modelled error | `404` |
| Errors | Status + `__type` / `X-Amzn-Errortype` naming the shape | Status from `@httpError` + `X-Amzn-ErrorType` |
| Caching | None (`POST`) | `GET` responses cacheable by browsers and CDNs |
| Safe to retry automatically | Only via traits (`@idempotent`, `@readonly`) | Also by method: `GET`, `PUT`, `DELETE` are idempotent |
| Trying it by hand | Needs the right headers and body | `curl https://…/forecasts/Oslo` |
| Model | No HTTP traits | `@http`, `@httpLabel`, `@httpQuery`, `@httpError` on every operation |

Moving to the RESTful version would change two layers in this repo. The model gains its HTTP traits, and the runtime gains a `RestJson1Protocol` that reads them through `HttpBindingIndex`. The protocol puts `city` in the path, `units` and `detail` in the query, and the remaining members in the JSON body. The generated case classes, schemas, Hearth codecs and `JsonCodec` stay exactly as they are. [The `restJson1` diagram](protocol-flows.md#restjson1-lambda-getfunction) shows the missing steps.

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

### A third style: `awsQuery` (STS, IAM and Elastic Load Balancing v2)

Some of AWS's oldest services use neither REST nor JSON RPC, but `awsQuery`. STS, IAM, Elastic Load Balancing v2, SNS, CloudFormation and RDS are among them. It's RPC like `awsJson1_0`: every call goes to `POST /`, which makes it level 0 on Richardson's scale. But the request is an HTML-form-style body and the response is XML.

The models declare it like any other protocol, with an XML namespace for the responses:

| Service | Shape ID | `version` | Protocol traits |
|---|---|---|---|
| STS | `com.amazonaws.sts#AWSSecurityTokenServiceV20110615` | `2011-06-15` | `@awsQuery`, `@xmlNamespace(uri: "https://sts.amazonaws.com/doc/2011-06-15/")` |
| IAM | `com.amazonaws.iam#AWSIdentityManagementV20100508` | `2010-05-08` | `@awsQuery`, `@xmlNamespace(uri: "https://iam.amazonaws.com/doc/2010-05-08/")` |
| Elastic Load Balancing v2 | `com.amazonaws.elasticloadbalancingv2#ElasticLoadBalancing_v10` | `2015-12-01` | `@awsQuery`, `@xmlNamespace(uri: "http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/")` |

The exchanges below are real, captured from the AWS emulator (see [STS](../aws-emulator/sts.md), [IAM](../aws-emulator/iam.md) and [Elastic Load Balancing v2](../aws-emulator/elastic-load-balancing-v2.md)). Request bodies are one line on the wire; they're broken at each `&` here to make them readable. `Content-Length: 43` below is exactly `Action=GetCallerIdentity&Version=2011-06-15`.

#### STS `GetCallerIdentity`: the simplest call

The input has no members and the output has three:

```json
"com.amazonaws.sts#GetCallerIdentityResponse": {
  "type": "structure",
  "members": {
    "UserId":  { "target": "com.amazonaws.sts#userIdType" },
    "Account": { "target": "com.amazonaws.sts#accountType" },
    "Arn":     { "target": "com.amazonaws.sts#arnType" }
  },
  "traits": { "smithy.api#output": {} }
}
```

The request carries only the operation's name and the API version:

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
X-Amz-Date: 20261126T183052Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/sts/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetCallerIdentity
&Version=2011-06-15
```

The response wraps the output members twice: `<GetCallerIdentityResponse>` for the whole message, then `<GetCallerIdentityResult>` for the output. Request metadata goes in a sibling element:

```http
HTTP/1.1 200 OK
content-type: text/xml

<GetCallerIdentityResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
  <GetCallerIdentityResult>
    <UserId>AIDAH5I7GEMO7ADXNFAZU</UserId>
    <Account>000000000000</Account>
    <Arn>arn:aws:iam::000000000000:user/emulator</Arn>
  </GetCallerIdentityResult>
  <ResponseMetadata>
    <RequestId>fdf8c6b8-9c78-4950-9f16-a948176fadde</RequestId>
  </ResponseMetadata>
</GetCallerIdentityResponse>
```

#### IAM `CreateUser`: a list of structures, and an error

The input has a required name and a list of `Tag` structures:

```json
"com.amazonaws.iam#CreateUserRequest": {
  "type": "structure",
  "members": {
    "Path":                { "target": "com.amazonaws.iam#pathType" },
    "UserName":            { "target": "com.amazonaws.iam#userNameType", "traits": { "smithy.api#required": {} } },
    "PermissionsBoundary": { "target": "com.amazonaws.iam#arnType" },
    "Tags":                { "target": "com.amazonaws.iam#tagListType" }
  },
  "traits": { "smithy.api#input": {} }
},
"com.amazonaws.iam#tagListType": { "type": "list", "member": { "target": "com.amazonaws.iam#Tag" } },
"com.amazonaws.iam#Tag": {
  "type": "structure",
  "members": {
    "Key":   { "target": "com.amazonaws.iam#tagKeyType",   "traits": { "smithy.api#required": {} } },
    "Value": { "target": "com.amazonaws.iam#tagValueType", "traits": { "smithy.api#required": {} } }
  }
}
```

A form body is flat, so nesting is spelled out in the keys. List element *n* (counting from 1) is `Tags.member.n`, and a structure's fields follow with a `.`:

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/iam/aws4_request, …

Action=CreateUser
&Version=2010-05-08
&UserName=docs-demo-user
&Tags.member.1.Key=team
&Tags.member.1.Value=docs
```

The response follows the same `…Response` / `…Result` pattern. A list comes back as `<member>` elements:

```xml
<CreateUserResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateUserResult>
    <User xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <UserName>docs-demo-user</UserName>
      <UserId>AIDABGXEQES5NRCCJJWAN</UserId>
      <Arn>arn:aws:iam::000000000000:user/docs-demo-user</Arn>
      <CreateDate>2026-11-26T18:30:05.49Z</CreateDate>
      <Tags>
        <member>
          <Key>team</Key>
          <Value>docs</Value>
        </member>
      </Tags>
    </User>
  </CreateUserResult>
  <ResponseMetadata>
    <RequestId>ae14c80b-631e-43a3-9629-db98e193ce4c</RequestId>
  </ResponseMetadata>
</CreateUserResponse>
```

Errors carry their own trait. `@awsQueryError` gives the code that goes on the wire, which differs from the shape's name, and the status code:

```json
"com.amazonaws.iam#DeleteConflictException": {
  "type": "structure",
  "members": { "message": { "target": "com.amazonaws.iam#deleteConflictMessage" } },
  "traits": {
    "aws.protocols#awsQueryError": { "code": "DeleteConflict", "httpResponseCode": 409 },
    "smithy.api#error": "client",
    "smithy.api#httpError": 409
  }
}
```

Deleting a user that still has policies attached gets exactly that, in a fixed `<ErrorResponse>` envelope. `Type` is `Sender` for client errors:

```http
HTTP/1.1 409 Conflict
content-type: text/xml
x-amzn-requestid: e633372b-a70f-4010-8d50-2b4aa660deac

<ErrorResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <Error>
    <Type>Sender</Type>
    <Code>DeleteConflict</Code>
    <Message>Cannot delete entity, must detach all policies first.</Message>
  </Error>
  <RequestId>e633372b-a70f-4010-8d50-2b4aa660deac</RequestId>
</ErrorResponse>
```

A client finds the error shape by matching `<Code>` against the `@awsQueryError` codes, not against shape names: `DeleteConflict` is `DeleteConflictException`.

#### Elastic Load Balancing v2 `CreateLoadBalancer`: lists of strings, enums, nested lists back

In the input, `Subnets` is a list of strings, and `Scheme` and `Type` are enums:

```json
"com.amazonaws.elasticloadbalancingv2#CreateLoadBalancerInput": {
  "type": "structure",
  "members": {
    "Name":           { "target": "…#LoadBalancerName", "traits": { "smithy.api#clientOptional": {}, "smithy.api#required": {} } },
    "Subnets":        { "target": "…#Subnets" },
    "SubnetMappings": { "target": "…#SubnetMappings" },
    "SecurityGroups": { "target": "…#SecurityGroups" },
    "Scheme":         { "target": "…#LoadBalancerSchemeEnum" },
    "Tags":           { "target": "…#TagList" },
    "Type":           { "target": "…#LoadBalancerTypeEnum" },
    "IpAddressType":  { "target": "…#IpAddressType" },
    ...
  }
},
"com.amazonaws.elasticloadbalancingv2#Subnets": { "type": "list", "member": { "target": "…#SubnetId" } }
```

`Name` is `@required` but also `@clientOptional`, so this repo's generator would make it an `Option` (see [Optional members, and `NullableIndex`](scala-client.md#optional-members-and-nullableindex)). A list of strings is `Subnets.member.n=value`, and an enum is its string value:

```http
Action=CreateLoadBalancer
&Version=2015-12-01
&Name=docs-demo-alb
&Subnets.member.1=subnet-00000000000000007
&Subnets.member.2=subnet-00000000000000009
&Scheme=internet-facing
&Type=application
```

The response nests lists of structures inside lists of structures, each as `<member>` elements (shortened):

```xml
<CreateLoadBalancerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <CreateLoadBalancerResult>
    <LoadBalancers>
      <member>
        <LoadBalancerArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:loadbalancer/app/docs-demo-alb/a3fa9733f0c6283b</LoadBalancerArn>
        <CreatedTime>2026-11-26T18:33:21.918857Z</CreatedTime>
        <LoadBalancerName>docs-demo-alb</LoadBalancerName>
        <Scheme>internet-facing</Scheme>
        <State>
          <Code>provisioning</Code>
        </State>
        <Type>application</Type>
        <AvailabilityZones>
          <member>
            <ZoneName>us-east-1a</ZoneName>
            <SubnetId>subnet-00000000000000007</SubnetId>
          </member>
          <member>
            <ZoneName>us-east-1b</ZoneName>
            <SubnetId>subnet-00000000000000009</SubnetId>
          </member>
        </AvailabilityZones>
        <SecurityGroups>
          <member>sg-00000000000000013</member>
        </SecurityGroups>
      </member>
    </LoadBalancers>
  </CreateLoadBalancerResult>
  <ResponseMetadata>
    <RequestId>5fec2c36-81e7-48af-b103-2e1c21c7ed26</RequestId>
  </ResponseMetadata>
</CreateLoadBalancerResponse>
```

#### The `awsQuery` rules, from these examples

| Part | Rule | Seen in |
|---|---|---|
| Method and path | Always `POST /`; the operation is never in the URL | All three |
| Operation | `Action=<OperationName>` in the body | `Action=CreateUser` |
| Version | `Version=<service version>` in the body | `Version=2010-05-08` |
| Request format | `application/x-www-form-urlencoded`: flat `key=value` pairs | All three |
| Nested structure | Keys joined with `.` | `Tags.member.1.Key=team` |
| List element | `<Name>.member.<n>`, counting from 1 (`@xmlFlattened` drops the `member`) | `Subnets.member.2=…` |
| Enum | Its string value | `Scheme=internet-facing` |
| Success | `<Op>Response` › `<Op>Result` › output members, plus `<ResponseMetadata><RequestId>` | `<CreateUserResult>` |
| List in a response | `<member>` elements | `<Tags><member>…</member></Tags>` |
| Error | `<ErrorResponse><Error><Type/><Code/><Message/></Error><RequestId/>`; status and `<Code>` from `@awsQueryError` | `409`, `DeleteConflict` |

`ec2Query` is a close cousin. It uses the same form bodies, but EC2 renames members with `@ec2QueryName`, numbers lists without `.member`, and uses a different XML envelope (see the [EC2 page](../aws-emulator/ec2.md)).

This is also where SQS came from. SQS moved from `awsQuery` to `awsJson1_0`, which is why its model still carries `@xmlName`, `@xmlFlattened` and `@awsQueryError`, and why its service has the `@awsQueryCompatible` trait.

#### Can this repo generate clients for them?

Not yet. Running the generator on the STS, IAM or Elastic Load Balancing v2 model stops with:

```text
…: no supported protocol (supported: awsJson1_0, awsJson1_1, rpcv2Json; the service has: aws.protocols#awsQuery)
```

The model side is ready: the generator reads these models fine, and [the traits it keeps](scala-client.md#which-traits-the-generator-keeps) now come from `awsQuery`'s `@protocolDefinition`, so `@xmlName`, `@xmlFlattened`, `@xmlNamespace` and `@awsQueryError` would reach the schemas automatically. What's missing is in the runtime:
- **`FormUrlCodec`:** turn `ShapeWriter` calls into `Action`, `Version` and the dotted `member.n` keys.
- **`XmlCodec`:** read `<Op>Result`, `<member>` lists, namespaces and the `<ErrorResponse>` envelope.
- **A query protocol class:** `POST /` with a form body, mapping errors by their `@awsQueryError` code.

[One call through each protocol](protocol-flows.md#awsquery-iam-createuser) shows this as a step-by-step diagram, next to every other protocol. `FormUrlCodec` and `XmlCodec` are already placeholders in [codecs/](../../modules/client-runtime/src/main/scala/smithy/scala/runtime/codecs), and the protocol would sit next to `AwsJsonProtocol`. The shape codecs, Hearth derivation and schemas wouldn't change.

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
