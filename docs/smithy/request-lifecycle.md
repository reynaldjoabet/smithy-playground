# The request lifecycle

This page follows one call, such as `SendMessage`, from the moment your code calls the client to the moment it gets an output or an error. All three runtimes do the same work in nearly the same order, because they implement the same design: the **Smithy Reference Architecture (SRA)**. aws-sdk-java-v2's design notes call it that by name ([docs/design/core/sra-identity-auth](https://github.com/aws/aws-sdk-java-v2/tree/master/docs/design/core/sra-identity-auth)), and aws-sdk-go-v2 generates a test per operation that asserts the order (`sra_operation_order_test.go`).

## The common steps

| # | Step | What happens |
|---|---|---|
| 1 | Validate and prepare the input | Check `@required` members, fill `@idempotencyToken` members, run user hooks that may change the input. |
| 2 | Serialize | The protocol turns the input into an HTTP request: method, path, query, headers and body. The host isn't known yet. |
| 3 | Build | Add headers that don't depend on the attempt: `Content-Length`, user agent, request checksum, invocation id. |
| 4 | Retry loop | Take a token from the retry strategy. Each pass of the loop runs steps 5 to 10; a retryable failure waits (with backoff and jitter) and starts the loop again. |
| 5 | Resolve the auth scheme | Choose an auth scheme from the operation's options (for AWS usually `aws.auth#sigv4`, sometimes `sigv4a` or `smithy.api#noAuth`). |
| 6 | Get the identity | Ask the scheme's identity resolver for credentials. In AWS SDKs this is the credentials provider chain: environment, profile, SSO, web identity, container, IMDS. |
| 7 | Resolve the endpoint | Run the endpoint rules with the client's region, FIPS and dual-stack settings and the operation's endpoint parameters. The result is a URL, plus auth scheme properties such as the signing region. |
| 8 | Sign | Sign the request with the identity, e.g. SigV4: canonical request, string to sign, `Authorization` header. |
| 9 | Transmit | Send it with the HTTP client and wait for the response. |
| 10 | Deserialize | The protocol turns the HTTP response into the output, or into a modelled error (found by error code) or a generic service error. |

As a flow, with the retry loop around the per-attempt steps:

```text
    ┌────────────────────────────────────────────────────────────┐
    │ 1. Validate and prepare input: @required,                  │
    │ @idempotencyToken, interceptors                            │
    └────────────────────────────────────────────────────────────┘
                    │
                    ▼
    ┌────────────────────────────────────────────────────────────┐
    │ 2. Serialize: protocol + codec build method, path,         │
    │ headers, body (once)                                       │
    └────────────────────────────────────────────────────────────┘
                    │
                    ▼
    ┌────────────────────────────────────────────────────────────┐
    │ 3. Build: Content-Length, user agent, checksum, invocation │
    │ id                                                         │
    └───────────────┬────────────────────────────────────────────┘
                    │
                    ▼
┌─ 4. retry loop ────────────────────────────────────────────────────────────────────────────────┐
│ take a retry token; steps 5 to 10 run once per attempt                                         │
│       ┌──────────────────────────────────────────────────────┐                                 │
│       │ 5. Resolve auth scheme: sigv4, sigv4a, noAuth ...    │◄─────────────────┐              │
│       └──────────────────────────────────────────────────────┘                  │              │
│                   │                                                             │              │
│                   ▼                                                             │              │
│       ┌──────────────────────────────────────────────────────┐                  │              │
│       │ 6. Get identity: credentials provider chain          │                  │              │
│       └──────────────────────────────────────────────────────┘                  │              │
│                   │                                                             │              │
│                   ▼                                                             │              │
│       ┌──────────────────────────────────────────────────────┐                  │              │
│       │ 7. Resolve endpoint: rules -> URL + signing          │                  │              │
│       │ properties                                           │                  │              │
│       └──────────────────────────────────────────────────────┘                  │ retryable    │
│                   │                                                             │ error: back  │
│                   ▼                                                             │ off, retry   │
│       ┌──────────────────────────────────────────────────────┐                  │              │
│       │ 8. Sign: SigV4 Authorization header for this attempt │                  │              │
│       └──────────────────────────────────────────────────────┘                  │              │
│                   │                                                             │              │
│                   ▼                                                             │              │
│       ┌──────────────────────────────────────────────────────┐                  │              │
│       │ 9. Transmit: the HTTP client sends and waits for the │                  │              │
│       │ response                                             │                  │              │
│       └──────────────────────────────────────────────────────┘                  │              │
│                   │                                                             │              │
│                   ▼                                                             │              │
│       ┌──────────────────────────────────────────────────────┐                  │              │
│       │ 10. Deserialize: output, modelled error or service   │──────────────────┘              │
│       │ error                                                │                                 │
│       └──────────────────────────────────────────────────────┘                                 │
│                                                                                                │
└───────────────────┬────────────────────────────────────────────────────────────────────────────┘
                    │
                    ▼
    ┌────────────────────────────────────────────────────────────┐
    │ Output returned to the caller, or the final error after    │
    │ the last attempt                                           │
    └────────────────────────────────────────────────────────────┘
```

Two details matter:

- **Endpoint resolution and signing happen inside the retry loop.** Each attempt is signed again, with a new timestamp and possibly refreshed credentials. A retry can also go to a different endpoint.
- **Serialization happens once, before the loop.** A retry resends the same serialized body, which is why request bodies must be replayable (streams need to be seekable or buffered).

## Go: the middleware stack

aws-sdk-go-v2 builds a new [middleware.Stack](https://github.com/aws/smithy-go/blob/main/middleware/stack.go) for every call. `invokeOperation` in `api_client.go`:

1. creates the stack and copies the client options;
2. applies per-call option functions (`optFns`);
3. adds the common middleware and then the operation's own (`addOperationSendMessageMiddlewares`);
4. applies `options.APIOptions`, which is where your own middleware goes;
5. wraps the stack around `smithyhttp.ClientHandler`, which calls `options.HTTPClient.Do`.

The stack has five fixed **steps**. Each one is an ordered list of middleware, and each middleware calls `next` to continue:

| Step | Works on | aws-sdk-go-v2 middleware in it (examples) |
|---|---|---|
| Initialize | The typed input | Input validation (`validators.go`), idempotency token, presign options, the long-polling flag for SQS `ReceiveMessage`. |
| Serialize | Input to `*smithyhttp.Request` | `OperationSerializer`, which calls `options.Protocol.SerializeRequest(ctx, operationSchema, input, req)`. |
| Build | The request, once | `Content-Length`, `amz-sdk-invocation-id`, user agent, payload SHA-256, request checksums. |
| Finalize | The request, per attempt | `Retry` → `ResolveAuthScheme` → `GetIdentity` → `ResolveEndpointV2` → `Signing` |
| Deserialize | The raw response | `OperationDeserializer`, which calls `options.Protocol.DeserializeResponse(...)`; raw response capture; response timing. |

The middlewares nest like function calls. The request goes down through Initialize, Serialize, Build, Finalize and Deserialize to the handler, and the result comes back up through the same middleware in reverse. A middleware can act on the way in (signing), on the way out (deserializing), or both (retry calls `next` again on failure).

Drawn as nested layers, each step wraps the ones inside it:

```text
┌─ Initialize ─────────────────────────────────────────────────────────────────────────────────┐
│ validators.go, idempotency token, operation input in context                                 │
│ ┌─ Serialize ──────────────────────────────────────────────────────────────────────────────┐ │
│ │ OperationSerializer: Protocol.SerializeRequest -> *smithyhttp.Request                    │ │
│ │ ┌─ Build ──────────────────────────────────────────────────────────────────────────────┐ │ │
│ │ │ Content-Length, amz-sdk-invocation-id, user agent, checksums                         │ │ │
│ │ │ ┌─ Finalize ───────────────────────────────────────────────────────────────────────┐ │ │ │
│ │ │ │ Retry -> ResolveAuthScheme -> GetIdentity -> ResolveEndpointV2 -> Signing        │ │ │ │
│ │ │ │ ┌─ Deserialize ────────────────────────────────────────────────────────────────┐ │ │ │ │
│ │ │ │ │ OperationDeserializer: Protocol.DeserializeResponse -> output or error       │ │ │ │ │
│ │ │ │ │ ┌──────────────────────────────────────────────────────────────────────────┐ │ │ │ │ │
│ │ │ │ │ │ Handler: smithyhttp.ClientHandler -> options.HTTPClient.Do(req)          │ │ │ │ │ │
│ │ │ │ │ └──────────────────────────────────────────────────────────────────────────┘ │ │ │ │ │
│ │ │ │ └──────────────────────────────────────────────────────────────────────────────┘ │ │ │ │
│ │ │ └──────────────────────────────────────────────────────────────────────────────────┘ │ │ │
│ │ └──────────────────────────────────────────────────────────────────────────────────────┘ │ │
│ └──────────────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

The Finalize order above is what `sra_operation_order_test.go` checks for every SQS operation:

```go
expect := []string{
	"OperationSerializer",
	"Retry",
	"ResolveAuthScheme",
	"GetIdentity",
	"ResolveEndpointV2",
	"Signing",
	"OperationDeserializer",
}
```

The retry middleware (`aws/retry`) gets an attempt token from the retryer, calls `next`, and if the error is retryable (`IsErrorRetryable`), sleeps for `RetryDelay` and calls `next` again. Everything after `Retry` in Finalize therefore runs once per attempt.

You change behaviour by adding middleware, not by subclassing:

```go
cfg, _ := config.LoadDefaultConfig(ctx)
client := sqs.NewFromConfig(cfg, func(o *sqs.Options) {
	o.APIOptions = append(o.APIOptions, func(stack *middleware.Stack) error {
		return stack.Build.Add(myHeaderMiddleware, middleware.After)
	})
})
```

## smithy-java: the client pipeline and interceptors

smithy-java runs each call through [ClientPipeline](https://github.com/smithy-lang/smithy-java/blob/main/client/client-core/src/main/java/software/amazon/smithy/java/client/core/ClientPipeline.java). The steps are numbered in its comments, and at each one the pipeline calls a hook on the call's `ClientInterceptor`:

| Pipeline step | Interceptor hooks |
|---|---|
| Start | `readBeforeExecution` |
| Before serialization | `modifyBeforeSerialization`, `readBeforeSerialization` |
| Serialize: `protocol.createRequest(operation, input, context, UNRESOLVED)` | `readAfterSerialization`, `modifyBeforeRetryLoop` |
| Acquire the first retry token: `retryStrategy.acquireInitialToken(...)` | |
| Per attempt: before signing | `readBeforeAttempt`, `modifyBeforeSigning`, `readBeforeSigning` |
| Resolve the identity, then the endpoint, then `protocol.setServiceEndpoint(request, endpoint)` | |
| Sign: `resolvedAuthScheme.sign(request)` | `readAfterSigning`, `modifyBeforeTransmit`, `readBeforeTransmit` |
| Send: `transport.send(context, request)` | then the matching after-transmit and deserialization hooks |

Like Go, it serializes once with an unresolved endpoint (`/`), then resolves the endpoint and signs on each attempt.

The protocol and transport are separate objects. `ClientPipeline` checks that they speak the same message type (an HTTP protocol needs an HTTP transport) before the first call. Both are found through `ServiceLoader`:

- `ClientProtocolFactory` implementations, such as `AwsJson1Protocol$Factory` in `aws-client-awsjson`;
- `ClientTransportFactory` implementations, such as `JavaHttpClientTransport$Factory` in `client-http`.

Cross-cutting behaviour is packaged as **client plugins**. `client-http` registers `UserAgentPlugin`, `ApplyHttpRetryInfoPlugin`, `RequestCompressionPlugin` and `HttpChecksumPlugin` as `AutoClientPlugin`s, so they apply to every client on the classpath. `client-core` adds `InjectIdempotencyTokenPlugin` and `ApplyModelRetryInfoPlugin`.

## aws-sdk-java-v2: pipeline stages and execution interceptors

aws-sdk-java-v2's generated method calls `clientHandler.execute(...)`. That runs a pipeline of **stages** assembled in [AmazonSyncHttpClient](https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/http/AmazonSyncHttpClient.java). The marshaller has already turned the request object into an `SdkHttpFullRequest` by then. The stages run in this order:

```text
┌─ once per call, before the retry loop ─────────────────────────────────────────────────────────┐
│ MakeRequestMutable -> ApplyTransactionId -> MergeCustomHeaders -> MergeCustomQueryParams ->    │
│ QueryParametersToBody -> CompressRequest -> AuthSchemeResolution -> EndpointResolution ->      │
│ HttpChecksum -> ApplyUserAgent -> MakeRequestImmutable                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┬──────┘
                                                                                          │
                                                                                          ▼
┌─ RetryableStage: repeats the inner block on a retryable failure ───────────────────────────────┐
│   ┌─ per attempt ──────────────────────────────────────────────────────────────────────────┐   │
│   │ Signing -> BeforeTransmissionInterceptors -> MakeHttpRequest ->                        │   │
│   │ AfterTransmissionInterceptors -> ModifyHttpResponseInterceptors ->                     │   │
│   │ BeforeUnmarshallingInterceptors -> HandleResponse                                      │   │
│   │ (wrapped by ApiCallAttemptMetricCollection, TimeoutExceptionHandling,                  │   │
│   │ ApiCallAttemptTimeoutTracking)                                                         │   │
│   └────────────────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────────┬──────┘
                                                                                          │
                                                                                          ▼
┌─ once per call, after the retry loop ──────────────────────────────────────────────────────────┐
│ UnwrapResponseContainer -> AfterExecutionInterceptors (the whole call is wrapped by            │
│ StreamManagingStage, ApiCallTimeoutTrackingStage and ExecutionFailureExceptionReportingStage)  │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
```

The stages that wrap others (`wrappedWith` in the code) are named in each block's note.

Unlike Go and smithy-java, aws-sdk-java-v2 resolves the auth scheme and endpoint **once, before** the retry loop; only signing is inside it. The outcome is mostly the same, since the endpoint rarely changes between attempts and the signature does.

User hooks are `ExecutionInterceptor`s, with the same hook names as smithy-java's apart from a few:

- `beforeExecution`, `modifyRequest`;
- `beforeMarshalling`, `afterMarshalling`;
- `modifyHttpRequest`, `modifyHttpContent`;
- `beforeTransmission`, `afterTransmission`;
- `modifyHttpResponse`, `modifyHttpResponseContent`;
- `beforeUnmarshalling`, `afterUnmarshalling`;
- `modifyResponse`, `afterExecution`;
- `modifyException`, `onExecutionFailure`.

SQS's `customization.config` adds `MessageMD5ChecksumInterceptor`, which checks the MD5 digests in `SendMessage` and `ReceiveMessage` responses. It is the Java counterpart of aws-sdk-go-v2's `cust_checksum_validation.go` middleware.

## The same step in each runtime

| Step | aws-sdk-go-v2 / smithy-go | smithy-java | aws-sdk-java-v2 |
|---|---|---|---|
| User hook | `APIOptions` middleware | `ClientInterceptor` | `ExecutionInterceptor` |
| Serialize | Serialize step, `Protocol.SerializeRequest` | `protocol.createRequest` | Generated `…RequestMarshaller` + `protocolFactory` |
| Retry | `Retry` in Finalize (`aws/retry`) | `RetryStrategy` tokens in the pipeline | `RetryableStage` |
| Auth scheme | `ResolveAuthScheme` (`auth.go`) | `resolvedAuthScheme` | `AuthSchemeResolutionStage` |
| Identity | `GetIdentity`: `aws.CredentialsProvider` from `config` | Identity resolvers (`aws-credential-chain`) | `IdentityProvider` / `AwsCredentialsProvider` chain |
| Endpoint | `ResolveEndpointV2` (`endpoints.go`, BDD) | `resolveEndpoint` (`client-rulesengine`) | `EndpointResolutionStage` (generated endpoint provider) |
| Sign | `Signing`, `aws/signer/v4` | `aws-sigv4` | `SigningStage`, `http-auth-aws` |
| Send | `smithyhttp.ClientHandler` → `net/http` | `ClientTransport` (`client-http`) | `MakeHttpRequestStage` → Apache, Netty, URLConnection or CRT client |
| Deserialize | Deserialize step, `Protocol.DeserializeResponse` | `protocol.deserializeResponse` | `HandleResponseStage` + unmarshaller |

Next: [Protocols and serialization](protocols-and-serde.md).
