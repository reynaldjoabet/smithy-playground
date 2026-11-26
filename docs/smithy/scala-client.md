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

The first version of the generator wrote serialization code as text. Every structure, union, list and map got hand-shaped `write` and `read` methods, about 24,000 lines across the three example clients. The second version uses Scala 3 to move that work into the compiler. Generated code now only describes shapes, and the runtime derives everything else.

The table is the summary; each feature has a section after it saying why it was used and what it changed.

| Feature | Where | Problem it solved |
|---|---|---|
| [Mirror derivation](#mirror-derivation-for-structures) | `ShapeCodec.struct` | A `write`/`read` pair generated as text for every structure |
| [Sum mirrors](#sum-mirrors-for-unions) | `ShapeCodec.union` | The same for every union, plus pattern matches kept in sync by hand |
| [`summonFrom` and `ValueOf`](#summonfrom-and-valueof-for-enum-values) | `ShapeCodec.enumValues` | A list of enum values written out a second time |
| [A quoted macro](#a-quoted-macro-for-default-values) | `Defaults.of` | Default values duplicated in the reading code |
| [Inline tuple traversal](#inline-tuple-traversal-for-error-registries) | `TypeRegistry.of` | Error registrations as boilerplate lambdas, with no type check |
| [By-name context parameters](#by-name-context-parameters-for-recursive-shapes) | `Option`, `List`, `Map` codecs | Recursive shapes, which derivation alone would loop on |
| [Context bounds with `as`](#context-bounds-with-as) | `Operation.of` | Seven positional constructor arguments per operation |
| [Opaque types](#opaque-types-and-inline-val) | `Traits` | A wrapper object around a `Map` |
| [Named tuples](#named-tuples) | `Headers` | `_1` and `_2`, or `(k, v)` patterns everywhere |
| [Union types](#union-types) | `CallFailure` | A `Throwable` parameter with a catch-all case |
| [`derives CanEqual`](#derives-canequal) | generated enums | Equality under `-language:strictEquality` |

### Mirror derivation for structures

**Why.** A structure codec only needs two things: each field's type and its position. The generator was writing that knowledge out as text. For each structure it emitted one `var` per field, a `match` on member indexes, and the constructor call, for SQS's 100 shapes and DynamoDB's 400 alike. The same template ran hundreds of times, so a template bug became a bug in hundreds of files.

**How.** A `Mirror.ProductOf[A]` gives the field types as a tuple type. `ShapeCodec.struct` walks it with `inline erasedValue[T] match` and `summonInline` to find a codec per field. At runtime, field `i` is written with schema member `i` (`productElement(i)`, `members(i)`) and built back with `mirror.fromProduct`.

**What it changed.** A structure's companion went from about 40 lines to one:

```scala
given codec: rt.SchemaCodec[GetForecastInput] = rt.ShapeCodec.struct[GetForecastInput](schema)
```

The serialization logic now exists once, in [ShapeCodec.scala](../../modules/client-runtime/src/main/scala/smithyscala/runtime/ShapeCodec.scala), where it's tested directly. If a field type has no codec, compiling the generated file fails, rather than the call failing at runtime.

### Sum mirrors for unions

**Why.** Generated unions had a `write` that matched every case and a `read` that switched on every member index. Both had to agree with the enum's case order, and nothing checked that.

**How.** `Mirror.SumOf[U]` lists the cases in declaration order. For each case, `ShapeCodec.union` takes its name (`constValue[MirroredLabel]`), its value's codec, or none for a `Unit` member, and a constructor. Writing uses the case's `ordinal` as the member index. The generator only has to declare cases in member order, with `UnknownMember` last.

**What it changed.** There are no per-union matches left to fall out of sync. `DerivationSpec` checks the mapping directly: `Kind.Sunny` writes `{"sunny":{}}`, `Kind.Rain(3)` writes `{"rain":3}`, and an unknown member reads as `UnknownMember("hail")`.

### `summonFrom` and `ValueOf` for enum values

**Why.** Every enum listed its values twice: once as cases, and again in a hand-built `knownValues` list. Adding a case meant changing both.

**How.** `ShapeCodec.enumValues[E]` walks the enum's cases. `summonFrom { case v: ValueOf[C] => ... }` keeps the ones that are singletons, which is every case except `UnknownValue(raw)`.

**What it changed.** `knownValues` is now `rt.ShapeCodec.enumValues[Units]`, so the cases are the only list. The test checks that `Units.knownValues == List(METRIC, IMPERIAL)`.

### A quoted macro for default values

**Why.** A `@default` member is a case-class parameter with a default. When it was missing from a response, the old `read` method used a copy of that default, which the generator wrote a second time. `Mirror` exposes field types and names, but not defaults, so derivation alone couldn't do this.

**How.** [Defaults.scala](../../modules/client-runtime/src/main/scala/smithyscala/runtime/Defaults.scala) is a `scala.quoted` macro. It finds the constructor parameters flagged `HasDefault` and calls the companion's synthetic default getters, giving a `Map` from field index to default value.

**What it changed.** Each default exists in one place, the case-class parameter list. The test reads `{"city":"Oslo"}` and gets `units = METRIC, detail = LOW`. It also checks `Defaults.of` directly.

### Inline tuple traversal for error registries

**Why.** Each operation registered its errors with one `new rt.ErrorType(X.schema, r => X.read(r, X.schema))` line per error, 13 lines for SQS `SendMessage`. Nothing stopped a non-error type from being listed.

**How.** `TypeRegistry.of[E1 *: E2 *: EmptyTuple]` walks the tuple type at compile time. For each type it checks `case _: ServiceException`, summons the codec, and otherwise calls `compiletime.error`.

**What it changed.** The registry is one type expression, and a wrong type is a compile error with a clear message. The test confirms this with munit's `compileErrors`: `TypeRegistry.of[String *: EmptyTuple]` fails with "every error type must extend ServiceException".

### By-name context parameters for recursive shapes

**Why.** DynamoDB's `AttributeValue` is a union whose `M` and `L` cases hold maps and lists of `AttributeValue`. To build the `Map` codec you need the `AttributeValue` codec, which is the very thing being built. If either were evaluated eagerly while the other is still being initialized, that would be an initialization loop.

**How.** Two things defer the lookup:

- The `Option`, `List` and `Map` givens take `(using inner: => ShapeCodec[A])` and keep it in a `lazy val`.
- Derived structure and union codecs wrap their field codecs in a thunk.

In both cases, nothing is resolved until the first `write` or `read`, after every companion's `given codec` exists.

**What it changed.** Recursive models compile and run with nothing special in the generator. `DerivationSpec` round-trips a nested `M`-of-`L`-of-`M` value. This was the riskiest part of the change, which is why the DynamoDB client was compiled before anything else. Whether either deferral alone would be enough hasn't been tested. Both are cheap, so both stay.

### Context bounds with `as`

**Why.** `Operation` took seven positional arguments: id, input schema, input writer, output schema, output reader, errors and auth. The four serde arguments were the same pattern every time.

**How.** `Operation.of[I: SchemaCodec as in, O: SchemaCodec as out](id, errors, auth)`. A codec carries its own schema, so the context bounds supply four of the seven arguments. `as` names the evidence without a separate `using` clause.

**What it changed.** Each operation is now one line in the generated client.

### Opaque types and `inline val`

**Why.** `Traits` was a case class wrapping a `Map[String, String]`: an extra object per schema, and an API that leaked `values`.

**How.** `opaque type Traits = Map[String, String]` exposes only `has`, `get` and `++` as extension methods. The trait ids (`Traits.JsonName` and so on) are `inline val`s, so uses compile to string literals.

**What it changed.** There's no wrapper at runtime and nothing extra in the API. Generated code is unaffected: `rt.Traits("smithy.api#required" -> "")` reads the same.

### Named tuples

**Why.** Headers were `Vector[(String, String)]`, read with `(k, v)` patterns or `_1` and `_2`. SigV4 does a lot of header handling, and positional access there is easy to get wrong.

**How.** `Vector[(name: String, value: String)]`. Named tuples are still tuples, so building and matching them works as before.

**What it changed.** Code reads `h.name` and `h.value`. The transport's response-header conversion became a `for` comprehension that yields `(name = ..., value = ...)`.

### Union types

**Why.** `RetryStrategy.isRetryable` took a `Throwable`, so it needed a `case _ => false`. That catch-all would quietly swallow a new kind of failure.

**How.** `type CallFailure = ServiceException | TransportException`. The retry loop's `Either` uses it, its `catch` catches exactly that union, and `isRetryable` matches the two cases with no default.

**What it changed.** The compiler checks that every failure kind is handled. Adding a third kind would turn the match into a warning instead of hiding the gap.

### `derives CanEqual`

**Why.** The build turns on `-language:strictEquality`, so `==` only compiles between types that allow it. Generated enums are compared in `fromValue` and in tests.

**How.** Generated unions and enums say `derives CanEqual`.

**What it changed.** Equality keeps its type checks without casts. Comparing an enum with an unrelated type is still a compile error.

### Costs

- **Errors move.** A missing codec now shows up as a compile error in the generated file that uses it. That's earlier than before, but the message comes out of an inline expansion.
- **Runtime type information is erased.** Derived codecs read fields as `productElement(i)` and keep codecs as `ShapeCodec[Any]`. That means some boxing and casts that the old hand-written code didn't need. The serde tests cover correctness, but nothing here measures speed.
- **The macro needs separate compilation.** `Defaults` lives in the runtime module, compiled before the generated code that expands it.

None of this uses experimental features. The build's strict flags (`-Wvalue-discard`, `-Wnonunit-statement`, `-language:strictEquality`, `-Xcheck-macros`) report nothing for the runtime or the 544 generated files. Overall, the generated sources shrank from about 24,000 lines to about 11,000. In one run here, the example module also compiled in 42 seconds instead of 88.

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
