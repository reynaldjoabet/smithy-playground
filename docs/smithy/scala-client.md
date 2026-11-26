# A Scala client generator

This playground includes a small Smithy client generator for Scala 3, built the way the earlier pages describe: generated code holds only shapes and schemas, and a hand-written runtime holds the codec, the protocols, auth, the transport and the retry pipeline. It's a teaching implementation, not a production SDK; [what it leaves out](#what-it-leaves-out) is listed at the end.

## The three modules

| sbt project | Directory | What it is |
|---|---|---|
| `clientRuntime` | [modules/client-runtime](../../modules/client-runtime) | The runtime generated code runs on. Depends only on Jackson 3 (`tools.jackson.core`). |
| `clientCodegen` | [modules/client-codegen](../../modules/client-codegen) | The generator. Reads a model with the Smithy Java libraries and writes Scala sources. Also a smithy-build plugin. |
| `clientExample` | [modules/client-example](../../modules/client-example) | Generated SQS (`awsJson1_0`), DynamoDB (`awsJson1_0`) and Weather (`rpcv2Json`) clients, and tests that run them against a local HTTP server. |

Like aws-sdk-go-v2, the generated code is written into the source tree rather than generated during the build, so it can be read and reviewed. Regenerate it with:

```sh
scripts/generate-scala-clients.sh
```

The script runs `sbt --client "clientCodegen/runMain smithyscala.codegen.Main <model> <service id> <package> <output dir>"` once per service. Each run replaces the package directory it writes.

## Each layer, and where it lives

| Layer | File | What it does here |
|---|---|---|
| Schemas | [Schema.scala](../../modules/client-runtime/src/main/scala/smithyscala/runtime/Schema.scala) | `Schema` carries a shape's id, type, members and runtime traits. A member is a schema too; its target is a thunk, so recursive shapes such as DynamoDB's `AttributeValue` work. |
| Serde interfaces | [Serde.scala](../../modules/client-runtime/src/main/scala/smithyscala/runtime/Serde.scala) | `ShapeWriter` and `ShapeReader`: format-neutral calls such as `writeString(memberSchema, value)`. Nothing above the codec sees a format. |
| Shape codecs | [ShapeCodec.scala](../../modules/client-runtime/src/main/scala/smithyscala/runtime/ShapeCodec.scala), [Defaults.scala](../../modules/client-runtime/src/main/scala/smithyscala/runtime/Defaults.scala) | `ShapeCodec[A]` walks one Scala type with those calls. Primitives, `Option`, `List` and `Map` are givens; structures, unions and enum values are derived at compile time (see [below](#scala-3-features-in-use)). |
| Codec | [JsonCodec.scala](../../modules/client-runtime/src/main/scala/smithyscala/runtime/JsonCodec.scala) | JSON only. `JsonSettings` says whether to honour `@jsonName` and `@timestampFormat`, because protocols differ on that. |
| Protocols | [Protocols.scala](../../modules/client-runtime/src/main/scala/smithyscala/runtime/Protocols.scala) | `AwsJsonProtocol` (`X-Amz-Target`, `application/x-amz-json-1.0`) and `RpcV2JsonProtocol` (`/service/<S>/operation/<O>`, `Smithy-Protocol: rpc-v2-json`). Both share the error handling: find the code, look it up in the `TypeRegistry`, let the codec fill the error. |
| Errors | [Operation.scala](../../modules/client-runtime/src/main/scala/smithyscala/runtime/Operation.scala) | `ServiceException` (generated errors extend it), `UnknownServiceException`, `TypeRegistry.of[E1 *: E2 *: EmptyTuple]`, and `Operation.of[I, O]`, which bundles one operation's codecs, errors and auth schemes. |
| Auth | [Auth.scala](../../modules/client-runtime/src/main/scala/smithyscala/runtime/Auth.scala) | `CredentialsProvider` (the identity resolver) and `SigV4`, checked against cases from the AWS SigV4 test suite. |
| Transport | [Http.scala](../../modules/client-runtime/src/main/scala/smithyscala/runtime/Http.scala) | `HttpTransport`, with `JavaHttpTransport` on `java.net.http`. It can take an `SSLContext`, for example one that trusts the AWS Emulator's certificate. |
| Pipeline | [Pipeline.scala](../../modules/client-runtime/src/main/scala/smithyscala/runtime/Pipeline.scala) | `ClientPipeline.invoke`: resolve the endpoint and serialize once, then per attempt run interceptors, sign, send and deserialize, inside the retry loop. Also `RetryStrategy`, `Interceptor` and `Paginator`. |

The protocol is chosen from the service's protocol trait when the client is generated, and the same generated shapes would work with any other JSON protocol. Adding `restJson1` would mean a new class in `Protocols.scala` that reads the HTTP binding traits; the generator would only need to add those traits to the runtime traits it copies into schemas.

## What the generator writes

For each shape in the service closure ([Walker](https://github.com/smithy-lang/smithy/blob/main/smithy-model/src/main/java/software/amazon/smithy/model/neighbor/Walker.java)), one file in the target package:

| Smithy | Scala |
|---|---|
| `structure` | a `final case class`. `@required` members are plain types, `@default` members get the default value, everything else is an `Option` defaulting to `None`. The companion has `schema` and `given codec = rt.ShapeCodec.struct[X](schema)`: no serialization code. |
| `@error` structure | the same, extending `rt.ServiceException`; `@retryable` overrides `smithyRetryable` (and `smithyThrottling`). |
| `union` | an `enum` with one case per member, in member order (no fields for `Unit` members), plus `UnknownMember(name)` last. The companion has `schema` and `given codec = rt.ShapeCodec.union[X](schema)`. |
| `enum`, `intEnum` | an `enum` with a `value`, plus `UnknownValue(raw)`. `knownValues` is `rt.ShapeCodec.enumValues[X]`, `fromValue` looks a value up, and the codec is built from the two. |
| `list`, `map` | a type alias to `List` or `Map[String, _]` (elements are `Option` when `@sparse`), and a companion with just `schema`. The runtime's generic `List` and `Map` codecs read the element schema from the member. |
| `service` | `<SdkId>Client`: a trait with a method per operation and a `<operation>Paginator` per `@paginated` operation, a companion with `ServiceInfo`, one `rt.Operation.of[I, O](id, rt.TypeRegistry.of[...], auth)` per operation, and `apply(config)`. |

Member names become camelCase. Names that are Scala keywords are backticked (`` `type` ``), and names a case class already has get a trailing underscore (`copy_`). A missing `@idempotencyToken` member is filled with a UUID when the input is serialized, so the same token is sent on every retry.

The generated SQS `SendMessageRequest`, for example, is [SendMessageRequest.scala](../../modules/client-example/src/main/scala/example/sqs/SendMessageRequest.scala), and the client is [SQSClient.scala](../../modules/client-example/src/main/scala/example/sqs/SQSClient.scala).

## Scala 3 features in use

Generated code only describes shapes; the compiler writes the serialization code. The parts of Scala 3 that do it:

| Feature | Where | What it does |
|---|---|---|
| `Mirror` derivation with `inline`, `erasedValue`, `summonInline` | `ShapeCodec.struct` | Walks a case class's field types at compile time and summons a codec for each. Field `i` lines up with schema member `i`, so the codec writes `productElement(i)` with `members(i)` and builds the result with `mirror.fromProduct`. |
| `Mirror.SumOf`, `constValue[MirroredLabel]` | `ShapeCodec.union` | One entry per enum case: its name, its value's codec (none for `Unit` members) and a constructor. The case's `ordinal` picks the schema member when writing. |
| `summonFrom` and `ValueOf` | `ShapeCodec.enumValues` | Keeps the enum's singleton cases, so `knownValues` lists every value but `UnknownValue(raw)` with no hand-written list. |
| A quoted macro (`scala.quoted`) | `Defaults.of` | `Mirror` doesn't expose default arguments, so a macro reads the constructor's `HasDefault` parameters and calls the companion's default getters. A `@default` member missing from a response gets its default. |
| Inline tuple traversal and `compiletime.error` | `TypeRegistry.of` | `TypeRegistry.of[A *: B *: EmptyTuple]` summons each error's codec. A type that isn't a `ServiceException` fails to compile, which `DerivationSpec` checks with `compileErrors`. |
| By-name context parameters | the `Option`, `List` and `Map` givens | `(using inner: => ShapeCodec[A])` lets recursive types such as DynamoDB's `AttributeValue`, a map of itself, resolve without an initialization loop. Derived codecs also look their field codecs up lazily. |
| Context bounds with `as` | `Operation.of[I: SchemaCodec as in, O: SchemaCodec as out]` | Names the evidence of a context bound without a separate `using` clause. |
| Opaque types | `Traits` | A plain `Map` at runtime, with only `has`, `get` and `++` visible outside. The trait ids are `inline val` constants. |
| Named tuples | `Headers` | Header entries are `(name: String, value: String)`, read as `h.name` and `h.value`. |
| Union types | `CallFailure = ServiceException \| TransportException` | The retry loop's failure type. `RetryStrategy.isRetryable` matches it exhaustively, with no catch-all case. |
| Enums with `derives CanEqual` | generated unions and enums | Value equality under the build's `-language:strictEquality`. |

None of this uses experimental features. The build's strict flags (`-Wvalue-discard`, `-Wnonunit-statement`, `-language:strictEquality`, `-Xcheck-macros`) report nothing for the runtime or the 544 generated files.

Moving serialization out of the generated text shrank the generated sources from about 24,000 lines to about 11,000. In one run here, the example module also compiled in 42 seconds instead of 88.

## Using a generated client

```scala
import java.net.URI
import smithyscala.runtime.*
import example.sqs.*

val sqs = SQSClient(
  ClientConfig(
    region = "us-east-1",
    endpoint = Some(URI.create("https://127.0.0.1:9297")), // omit for https://sqs.<region>.amazonaws.com
    credentials = CredentialsProvider.environment
  )
)

val sent = sqs.sendMessage(SendMessageRequest(queueUrl = "https://127.0.0.1:9297/000000000000/orders", messageBody = "hello", delaySeconds = Some(5)))

try sqs.sendMessage(SendMessageRequest(queueUrl = "missing", messageBody = "x"))
catch { case e: QueueDoesNotExist => println(e.smithyMetadata.flatMap(_.requestId)) }

val allQueues = sqs.listQueuesPaginator(ListQueuesRequest()).flatMap(_.queueUrls.getOrElse(Nil)).toList
```

To talk to the [AWS Emulator](../aws-emulator/README.md), pass `transport = JavaHttpTransport(sslContext = Some(ctx))` with an `SSLContext` that trusts its certificate.

## Tests

| Test | What it checks |
|---|---|
| [SigV4Spec](../../modules/client-runtime/src/test/scala/smithyscala/runtime/SigV4Spec.scala) | Signatures from the AWS SigV4 test suite (`get-vanilla`, `post-vanilla`, `get-vanilla-query-order-key`) and session tokens. |
| [JsonCodecSpec](../../modules/client-runtime/src/test/scala/smithyscala/runtime/JsonCodecSpec.scala) | `@jsonName` and `@timestampFormat` per protocol settings, `NaN`, base64 blobs, skipping unknown fields. |
| [ScalaClientGeneratorSpec](../../modules/client-codegen/src/test/scala/smithyscala/codegen/ScalaClientGeneratorSpec.scala) | Generator output for a small model, rejection of unsupported protocols, and a run through `SmithyBuild` as a plugin. |
| [DerivationSpec](../../modules/client-example/src/test/scala/example/DerivationSpec.scala) | The derived codecs: defaults from the macro, error correction, enum values from the `Mirror`, union ordinals, recursive round-trips, and the compile error from `TypeRegistry.of`. |
| [GeneratedClientSpec](../../modules/client-example/src/test/scala/example/GeneratedClientSpec.scala) | The generated SQS, DynamoDB and Weather clients against a local server: wire format, signing, modelled and unknown errors, retries, paginators, recursive unions, idempotency tokens, `rpcv2Json` paths and Unit input. |

Run them all with `sbt --client "clientRuntime/test; clientCodegen/test; clientExample/test"`.

## What it leaves out

- **Protocols:** only `awsJson1_0`, `awsJson1_1` and `rpcv2Json`. The generator refuses other services and names their protocol, so S3, IAM or Lambda need `restXml`, `awsQuery` or `restJson1` support first.
- **Endpoints:** no rules engine. An explicit endpoint wins; otherwise `https://<endpointPrefix>.<region>.amazonaws.com`. FIPS, dual-stack and partitions other than `aws` aren't handled.
- **Credentials:** static and environment variables only; no profiles, SSO or instance metadata.
- **Streaming:** no event streams (such operations are skipped) and no streaming bodies; blobs are held in memory.
- **API style:** calls are blocking. They run well on virtual threads, but there is no async or effect-typed API.
- **Not generated:** waiters, client-side validation beyond `@required` being a non-optional type, and endpoint discovery (DynamoDB doesn't need it).
- **Error correction:** a required member missing from a response gets a zero value, as the Smithy spec suggests. A missing required structure is the exception: it fails deserialization.

For production Scala clients, [smithy4s](https://disneystreaming.github.io/smithy4s/) is the established generator; it's already in this playground's `project/plugins.sbt`.

Back to [the overview](README.md).
