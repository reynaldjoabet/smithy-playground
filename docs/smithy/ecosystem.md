# The Smithy ecosystem

The earlier pages followed one model through Go and Java. This page covers the rest of the [smithy-lang](https://github.com/smithy-lang) organization: the core project every generator builds on, the build tooling, and generators for Rust, Kotlin, Swift, TypeScript, Python, Ruby and PHP. The next page, [the protocol stack](protocol-stack.md), explains how protocols, codecs, auth and transports fit together inside any of those runtimes.

The repositories were read on 2026-11-26.

## How the pieces connect

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ smithy: the IDL and its Java libraries: smithy-model (parse, validate), smithy-build             │
│ (projections, plugins), smithy-cli, smithy-codegen-core, trait packages (smithy-aws-traits,      │
│ smithy-protocol-traits, smithy-rules-engine, smithy-waiters), and the shared protocol test       │
│ suites                                                                                           │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                        │                                                  │
                        ▼                                                  ▼
┌───────────────────────────────────────────────┐  ┌───────────────────────────────────────────────┐
│ Smithy CLI: smithy build, smithy validate,    │  │ smithy-gradle-plugin: smithy-base, smithy-jar │
│ smithy init. setup-smithy-cli installs it on  │  │ and smithy-trait-package plugins; a           │
│ GitHub Actions runners                        │  │ smithyBuild task inside Gradle builds         │
└───────────────────────────────────────────────┘  └───────────────────────────────────────────────┘
                        ▼                                                  ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ smithy-build: assemble and validate the model, apply each projection's transforms, then run the  │
│ projection's plugins                                                                             │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                │                                 │                                 │
                ▼                                 ▼                                 ▼
┌──────────────────────────────┐  ┌──────────────────────────────┐  ┌──────────────────────────────┐
│ JVM plugin in the same       │  │ run:: plugin, a separate     │  │ Native generator reading the │
│ process: go-codegen,         │  │ process that gets the model  │  │ JSON AST (a file or stdin):  │
│ java-codegen,                │  │ on stdin: smithy-ruby        │  │ smithy-php-codegen, the      │
│ rust-client-codegen,         │  │ (smithy-ruby smith client)   │  │ smithy-python CLI,           │
│ kotlin-codegen,              │  │                              │  │ smithy-swift's               │
│ swift-codegen,               │  │                              │  │ SmithyCodegenCLI             │
│ typescript-client-codegen,   │  │                              │  │                              │
│ python-client-codegen        │  │                              │  │                              │
└──────────────────────────────┘  └──────────────────────────────┘  └──────────────────────────────┘
                ▼                                 ▼                                 ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Generated code (types, schemas, clients, servers) depending on that language's runtime:          │
│ smithy-go, smithy-java, smithy-rs crates, smithy-kotlin, smithy-swift, @smithy/* npm packages,   │
│ smithy-python packages, smithy/* Composer packages (smithy-php-*), smithy-ruby gems              │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

There are three kinds of repository:

1. **The foundation**: [smithy](#smithy), which defines the language and the shared Java libraries.
2. **Build tooling** that runs those libraries: the Smithy CLI, [smithy-gradle-plugin](#smithy-gradle-plugin) and [setup-smithy-cli](#setup-smithy-cli).
3. **One generator and one runtime per language.** The generator turns a model into source code; the runtime is the library that source code calls at run time. For PHP, the runtime is also published as [nine separate packages](#smithy-php-and-its-nine-packages).

## smithy

[smithy-lang/smithy](https://github.com/smithy-lang/smithy) (version 1.75.1) is the Smithy project itself: the IDL specification, the documentation at smithy.io, and the Java libraries everyone else depends on. Its modules fall into groups:

| Group | Modules | What they do |
|---|---|---|
| The model | `smithy-model`, `smithy-syntax`, `smithy-utils` | Parse IDL and JSON AST into a `Model`, run validators, query with selectors; `smithy-syntax` is the parse tree and formatter. |
| Building | `smithy-build`, `smithy-cli`, `smithy-bom` | `smithy-build` reads `smithy-build.json`, applies projections and runs plugins; `smithy-cli` is the `smithy` command (`build`, `validate`, `select`, `diff`, `format`, `init`, `ast`, `lock`, `migrate`, `clean`, `warmup`). |
| Writing generators | `smithy-codegen-core`, `smithy-trait-codegen` | `SymbolProvider`, writers, `CodegenDirector` and the directed-codegen interfaces that smithy-java, smithy-rs, smithy-kotlin and others build on; `smithy-trait-codegen` generates Java classes for custom traits. |
| Traits | `smithy-aws-traits`, `smithy-protocol-traits`, `smithy-aws-iam-traits`, `smithy-waiters`, `smithy-rules-engine`, `smithy-aws-endpoints`, `smithy-smoke-test-traits`, `smithy-contract-traits`, `smithy-mqtt-traits`, `smithy-aws-cloudformation-traits`, `smithy-aws-apigateway-traits`, `smithy-openapi-traits`, `smithy-validation-model` | Trait definitions and their Java classes. `aws.protocols#awsJson1_0`, `restJson1`, `restXml`, `awsQuery` and `ec2Query` are in `smithy-aws-traits`. `smithy.protocols#rpcv2Cbor` and `rpcv2Json` are in `smithy-protocol-traits`. Endpoint rule sets and the BDD form are in `smithy-rules-engine`. |
| Testing | `smithy-protocol-test-traits`, `smithy-protocol-tests`, `smithy-aws-protocol-tests`, `smithy-rules-engine-tests`, `smithy-jmespath-tests` | `@httpRequestTests` and `@httpResponseTests`, and the shared test models every generator runs (see [the protocol stack](protocol-stack.md#one-test-suite-for-every-language)). |
| Conversions | `smithy-openapi`, `smithy-jsonschema`, `smithy-aws-apigateway-openapi`, `smithy-aws-cloudformation`, `smithy-docgen` | Smithy to OpenAPI, JSON Schema, API Gateway, CloudFormation resource schemas, and documentation sites. |
| Analysis | `smithy-diff`, `smithy-linters`, `smithy-jmespath`, `smithy-model-jmespath` | Backward-compatibility checks between model versions, extra validators, and JMESPath (used by waiters and paginators). |

Every Java-hosted generator below is a `SmithyBuildPlugin` compiled against these libraries. Generators written in other languages still depend on `smithy-build` to produce their input.

## smithy-gradle-plugin

[smithy-lang/smithy-gradle-plugin](https://github.com/smithy-lang/smithy-gradle-plugin) (version 1.5.0) runs `smithy-build` inside a Gradle build. It provides three plugins:

| Plugin id | Use |
|---|---|
| `software.amazon.smithy.gradle.smithy-base` | Adds the `smithy` source set, the `smithyCli` and `smithyBuild` configurations, and a `smithyBuild` task. Other plugins apply it. |
| `software.amazon.smithy.gradle.smithy-jar` | Builds the models and puts them, with build metadata, into the project's `jar`, so the models travel as a Maven dependency. |
| `software.amazon.smithy.gradle.smithy-trait-package` | Sets up a Java library for custom traits, together with `smithy-trait-codegen`. |

All of them also add a `select` task: `./gradlew select --selector '[trait|trait]'`.

smithy-rs, smithy-swift and smithy-typescript apply it in their Gradle builds, and aws-sdk-go-v2's `codegen` build uses an older version (plugin id `software.amazon.smithy`). It lets one build compile a generator and run it on models: the generator is on the `smithyBuild` classpath, so `smithy-build` finds its plugin through `ServiceLoader`.

## setup-smithy-cli

[smithy-lang/setup-smithy-cli](https://github.com/smithy-lang/setup-smithy-cli) is a GitHub Action that installs the Smithy CLI on a runner, on Linux, macOS or Windows:

```yaml
steps:
  - uses: actions/checkout@v4
  - uses: smithy-lang/setup-smithy-cli@v1
    with:
      version: '1.75.1'            # optional; latest release if omitted
      config: smithy-build.json    # optional; used for Maven dependency caching
  - run: smithy build
```

It caches the CLI binary, and caches the Maven artifacts listed under `maven.dependencies` in `smithy-build.json`. That matters because the CLI downloads generator jars, such as `smithy-typescript-codegen` or `smithy-aws-traits`, from Maven on every build otherwise. The installed version is available as the `cli-version` output.

It's the CI counterpart of smithy-gradle-plugin: projects that build with the plain CLI (smithy-ruby, smithy-php, or a model-only repository like this playground) use the action; Gradle projects use the plugin.

## The language generators

Each repository below holds a generator and the runtime its output depends on. None of them contains generated AWS clients; the AWS SDKs are built from them in separate repositories.

### smithy-rs (Rust)

[smithy-lang/smithy-rs](https://github.com/smithy-lang/smithy-rs) generates Rust clients, Rust servers, and the entire AWS SDK for Rust (`./gradlew :aws:sdk:assemble`).

| | |
|---|---|
| Generator | Kotlin, run by Gradle (JDK 17). Plugins: `rust-client-codegen`, `rust-server-codegen`, `rust-server-codegen-python`, `rust-server-codegen-typescript` and `fuzz-harness`. |
| Code layout | `codegen-core` is shared by `codegen-client` and `codegen-server`, and protocol generators live under `codegen-core/.../protocols`. `codegen-serde` generates `serde::Serialize` and `serde::Deserialize` implementations for generated types. `aws/codegen-aws-sdk` holds the AWS customizations. |
| Runtime | Crates under `rust-runtime/`: `aws-smithy-runtime` and `aws-smithy-runtime-api` (the orchestrator), `aws-smithy-types`, `aws-smithy-schema`, the codecs `aws-smithy-json`, `aws-smithy-xml`, `aws-smithy-cbor` and `aws-smithy-query`, `aws-smithy-http`, `aws-smithy-http-client`, and `aws-smithy-http-server` for servers. AWS-only crates are under `aws/rust-runtime/`: `aws-sigv4`, `aws-config`, `aws-credential-types`, `aws-runtime`. |
| Request lifecycle | An **orchestrator** ([design/src/client/orchestrator.md](https://github.com/smithy-lang/smithy-rs/blob/main/design/src/client/orchestrator.md)). Runtime plugins fill a `ConfigBag`, and interceptors run at each lifecycle stage. |

Two things set it apart:

- **It also generates servers for other languages.** The Python and TypeScript server plugins generate a Rust server with bindings (`aws-smithy-http-server-python`, `aws-smithy-http-server-typescript`), so a service can be written in Python or TypeScript while the HTTP handling, routing and validation run in Rust.
- **Serialization has been generated code.** Each protocol generator writes Rust serializers. The new `aws-smithy-schema` crate (1.2.0) adds runtime schemas as `static` values built at compile time, "enabling protocol-agnostic serialization and deserialization": the same move other runtimes have made.

### smithy-kotlin (Kotlin)

[smithy-lang/smithy-kotlin](https://github.com/smithy-lang/smithy-kotlin) is the generator and runtime behind aws-sdk-kotlin.

| | |
|---|---|
| Generator | Kotlin, plugin `kotlin-codegen`. `codegen/aws-codegen` holds the AWS-specific generation; `codegen/protocol-tests` generates the protocol tests. |
| Runtime | Kotlin Multiplatform, with JVM, Linux, Windows (`mingw`) and POSIX targets under `runtime/runtime-core`. `runtime/serde` has `serde-json`, `serde-xml`, `serde-cbor` and `serde-form-url`. `runtime/protocol` has `aws-json-protocols`, `aws-xml-protocols`, `smithy-rpcv2-protocols` and `aws-protocol-core`, plus `http-client-engines` (default, OkHttp, OkHttp 4 and CRT). `runtime/auth` has `aws-signing-default` and `aws-signing-crt`. `runtime/observability` has OpenTelemetry, Micrometer and CloudWatch EMF providers. |

### smithy-swift (Swift)

[smithy-lang/smithy-swift](https://github.com/smithy-lang/smithy-swift) is the generator and runtime behind the AWS SDK for Swift. It has **two generators**:

- `smithy-swift-codegen`: Kotlin, plugin `swift-codegen`, run by Gradle. It generates clients, operations and types.
- `SmithyCodegenCLI` with the SwiftPM build plugin `SmithyCodeGeneratorPlugin`. This is a Swift-native generator that reads the model's JSON AST (named in a `swift-settings.json`) during `swift build`. It generates schemas, serializers, deserializers and a type registry.

The runtime is split into API modules, which hold only protocols (Swift's interfaces), and implementation modules:

| API modules | Implementations |
|---|---|
| `SmithyHTTPAPI`, `SmithyHTTPAuthAPI`, `SmithyIdentityAPI`, `SmithyRetriesAPI`, `SmithyWaitersAPI`, `SmithyChecksumsAPI`, `SmithyEventStreamsAPI`, `SmithyTelemetryAPI` | `ClientRuntime`, `Smithy`, `SmithyHTTPClient`, `SmithySwiftNIO`, `SmithyHTTPAuth`, `SmithyIdentity`, `SmithyRetries`, `SmithyChecksums`, `SmithyEventStreams` |

Serialization sits in `SmithySerialization` and `SmithyReadWrite`. The codecs are `SmithyJSON`, `SmithyXML`, `SmithyCBOR` and `SmithyFormURL`, and the protocols are `SmithyAWSJSON`, `SmithyRestJSON1` and `SmithyRPCv2CBOR`.

### smithy-typescript (TypeScript)

[smithy-lang/smithy-typescript](https://github.com/smithy-lang/smithy-typescript) is the generator and runtime behind the AWS SDK for JavaScript v3 (aws-sdk-js-v3). It targets Node.js (version 20 or later), browsers and React Native.

| | |
|---|---|
| Generator | Java. Plugins: `typescript-client-codegen`, `typescript-server-codegen`, `typescript-schema-ssdk-codegen`, and the unified `typescript-codegen`, which takes a `modes` setting. `typescript-ssdk-codegen` is deprecated. |
| Runtime | About 60 npm packages under `packages/`, published as `@smithy/*`: `smithy-client`, `middleware-stack`, `middleware-serde`, `middleware-retry`, `middleware-endpoint`, `signature-v4`, `signature-v4a`, `node-http-handler`, `fetch-http-handler`, `util-endpoints`, `util-waiter`, and others. `@smithy/core` contains the schema system (`submodules/schema`, including `TypeRegistry` and `getSchemaSerdePlugin`), shape serializers (`submodules/serde`), the protocol base classes (`HttpProtocol`, `HttpBindingProtocol`, `RpcProtocol`, `AbstractRpcV2Protocol`), and the `rpcv2Cbor` and `rpcv2Json` protocols with their codecs. |

The AWS protocol classes (awsJson, restJson1, restXml, awsQuery) aren't in this repository. They belong to the AWS SDK, which builds them on these base classes.

### smithy-python (Python)

[smithy-lang/smithy-python](https://github.com/smithy-lang/smithy-python) (default branch `develop`) is an early-stage generator and runtime for synchronous and asynchronous Python clients. The README says all interfaces may change.

| | |
|---|---|
| Generators | The authoritative one is Java, under `codegen/`, with plugins `python-client-codegen` and `python-type-codegen`. The core generator supports `restJson1`, `rpcv2Cbor` and `rpcv2Json`; `codegen/aws/core` adds `awsJson1_0`, `awsJson1_1`, `awsQuery`, `ec2Query` and `restXml`. `packages/smithy-python` is an experimental Python-native CLI (`smithy-python generate client` or `generate types`) that reads the JSON AST from `--model` or stdin. |
| Runtime | `packages/smithy-core` holds schemas, codecs and serializer interfaces, the client pipeline, interceptors, retries, identity, auth, and the endpoint rules engine; `aio/` holds the async variants. `smithy-json`, `smithy-cbor` and `smithy-xml` are the codecs. The other packages are `smithy-http`, `smithy-aws-core`, `smithy-aws-event-stream`, `smithy-aws-checksums` and `aws-sdk-signers`. |

### smithy-ruby (Ruby)

[smithy-lang/smithy-ruby](https://github.com/smithy-lang/smithy-ruby) is being rewritten as a **Ruby-native generator**. The previous Java-based generator is on the `caffeinated` branch. The `smithy` gem provides a `smithy-ruby` executable, and `smithy-build` calls it through the built-in `run::` plugin:

```json
"plugins": {
  "run::smithy-ruby": {
    "command": ["smithy-ruby", "smith", "client", "--gem-name=weather", "--gem-version=1.0.0", "--destination-root=..."]
  }
}
```

The [`run` plugin](https://github.com/smithy-lang/smithy/blob/main/smithy-build/src/main/java/software/amazon/smithy/build/plugins/RunPlugin.java) starts the command, writes the projected model to its standard input, and sets `SMITHY_ROOT_DIR`, `SMITHY_PLUGIN_DIR`, `SMITHY_PROJECTION_NAME` and related variables. So a generator in any language can join a `smithy-build` run without being a Java plugin.

Within the gem, `welds` play the role of integrations. The runtime gems are `smithy-client`, `smithy-schema`, `smithy-json`, `smithy-xml`, `smithy-cbor` and `smithy-server`. Ruby 3.1 or later and JRuby are supported.

### smithy-php and its nine packages

[smithy-lang/smithy-php](https://github.com/smithy-lang/smithy-php) is a developer-preview generator and runtime for PHP 8.2 or later. Like Ruby's, the **generator is written in its own language**: `vendor/bin/smithy-php-codegen model.json --namespace=... --output=...` reads a JSON AST, usually produced by `smithy build` with the built-in `model` plugin. It generates a client, input and output classes, enums, unions, exceptions and lazily initialized runtime schemas. `--operations=GetForecast,ListForecasts` limits a client to some operations and the shapes they need.

The monorepo's `src/` holds nine components. On every version tag, a GitHub workflow ([split.yml](https://github.com/smithy-lang/smithy-php/blob/main/.github/workflows/split.yml)) uses `git subtree split` to push each component to its own **read-only repository** with the same tag. Composer installs them from there. Issues and pull requests go to the monorepo.

| Repository | Composer package | Source directory | Requires | Role |
|---|---|---|---|---|
| [smithy-php-async](https://github.com/smithy-lang/smithy-php-async) | `smithy/async` | `src/Async` | `revolt/event-loop` | Fiber-based concurrency: `Async::all()`, `Async::map()` with bounded concurrency (default 25), and an event-loop contract. |
| [smithy-php-core](https://github.com/smithy-lang/smithy-php-core) | `smithy/core` | `src/Core` | nothing | Runtime `Schema`, shape contracts (`SerializableStructInterface`, `OperationInterface`, `ServiceInterface` …), documents, endpoint, retry and checksum types. |
| [smithy-php-codec](https://github.com/smithy-lang/smithy-php-codec) | `smithy/codec` | `src/Codec` | core, PSR-7 | Schema-driven serializers and deserializers for JSON, XML, CBOR and form URL encoding. Message bodies only. |
| [smithy-php-http](https://github.com/smithy-lang/smithy-php-http) | `smithy/http` | `src/Http` | async, core, PSR-7, PSR-18 | PSR-7 messages and a cURL client with fiber-aware concurrency, streaming, cancellation and response checksum validation. |
| [smithy-php-auth](https://github.com/smithy-lang/smithy-php-auth) | `smithy/auth` | `src/Auth` | core, http | Identities, caching identity resolvers, auth schemes and signers: bearer, anonymous, and SigV4 (with chunked streaming signatures) under `Aws/`. |
| [smithy-php-protocol](https://github.com/smithy-lang/smithy-php-protocol) | `smithy/protocol` | `src/Protocol` | codec, core, http | `ProtocolInterface`, `AbstractHttpBindingProtocol` (HTTP binding traits) and RPCv2 (CBOR and JSON). |
| [smithy-php-protocol-aws](https://github.com/smithy-lang/smithy-php-protocol-aws) | `smithy/protocol-aws` | `src/ProtocolAws` | protocol, codec, core, http | `AwsJson1_0Protocol`, `AwsJson1_1Protocol`, `RestJsonProtocol`, `RestXmlProtocol`, `AwsQueryProtocol`, `Ec2QueryProtocol`, and AWS error envelopes and metadata. |
| [smithy-php-client](https://github.com/smithy-lang/smithy-php-client) | `smithy/client` | `src/Client` | async, auth, core, http, protocol | `AbstractClient`, `ClientPipeline`, immutable `ClientConfig`, interceptors, plugins, retries, pagination, waiters, and a dynamic client. |
| [smithy-php-codegen](https://github.com/smithy-lang/smithy-php-codegen) | `smithy/codegen` (dev) | `src/Codegen` | client, core | The `smithy-php-codegen` generator. |

The `Requires` column is the clearest picture in the whole ecosystem of how the layers depend on each other:

- `core` depends on nothing.
- `codec` knows only `core` and PSR-7 streams; it never sees HTTP.
- `protocol` adds HTTP on top of codecs.
- `protocol-aws` extends `protocol`.
- `client` brings protocol, auth and HTTP together.
- Notably, `client` doesn't depend on `codec`; it reaches bodies only through a protocol.

As a graph, where each arrow points at a package's main dependency and each box lists all of them:

```text
┌────────────────────────────┐     ┌────────────────────────────┐
│ smithy/async               │     │ smithy/core                │
│ needs: revolt/event-loop   │     │ needs: nothing             │
└────────────────────────────┘     └────────────────────────────┘
              ▲                                  ▲
              │                                  │
┌─────────────┴──────────────┐     ┌─────────────┴──────────────┐
│ smithy/http                │     │ smithy/codec               │
│ needs: async, core, PSR-7, │     │ needs: core, PSR-7         │
│ PSR-18                     │     └────────────────────────────┘
└────────────────────────────┘                   ▲
              ▲                                  │
              │                                  │
┌─────────────┴──────────────┐     ┌─────────────┴──────────────┐     ┌────────────────────────────┐
│ smithy/auth                │     │ smithy/protocol            │     │ smithy/protocol-aws        │
│ needs: core, http          │     │ needs: codec, core, http   │◄────│ needs: protocol, codec,    │
└────────────────────────────┘     └────────────────────────────┘     │ core, http                 │
                                                 ▲                    └────────────────────────────┘
                                                 │
                                                 │
                                   ┌─────────────┴──────────────┐
                                   │ smithy/client              │
                                   │ needs: async, auth, core,  │
                                   │ http, protocol             │
                                   └────────────────────────────┘
                                                 ▲
                                                 │
                                   ┌─────────────┴──────────────┐
                                   │ smithy/codegen             │
                                   │ needs: client, core        │
                                   └────────────────────────────┘
```

The [protocol stack](protocol-stack.md) page explains each layer.

The [pet-store example](https://github.com/smithy-lang/smithy-php/tree/main/examples/pet-store) generates a client with `composer generate`, runs a local `rpcv2Cbor` server with `composer serve`, and calls it with `composer demo`.

## The generators side by side

| Repository | Generator language | How smithy-build reaches it | Main plugin or command | Runtime packaging | AWS SDK built on it |
|---|---|---|---|---|---|
| smithy-go | Java | JVM plugin | `go-codegen` | Go modules | aws-sdk-go-v2 |
| smithy-java | Java | JVM plugin | `java-codegen` | Maven artifacts | (separate aws-sdk-java-v2) |
| smithy-rs | Kotlin | JVM plugin | `rust-client-codegen`, `rust-server-codegen` | Crates | AWS SDK for Rust |
| smithy-kotlin | Kotlin | JVM plugin | `kotlin-codegen` | Kotlin Multiplatform artifacts | aws-sdk-kotlin |
| smithy-swift | Kotlin + Swift | JVM plugin, plus a SwiftPM build plugin | `swift-codegen`, `SmithyCodegenCLI` | Swift packages | AWS SDK for Swift |
| smithy-typescript | Java | JVM plugin | `typescript-client-codegen` | `@smithy/*` npm packages | aws-sdk-js-v3 |
| smithy-python | Java (+ experimental Python) | JVM plugin, or the JSON AST | `python-client-codegen`, `smithy-python` | Python packages | none yet |
| smithy-ruby | Ruby | `run::` plugin (model on stdin) | `smithy-ruby smith client` | Gems | none yet |
| smithy-php | PHP | JSON AST from the `model` plugin | `smithy-php-codegen` | Nine Composer packages | none yet |

The last column names an AWS SDK only where the repository itself says it builds or backs one. "None yet" means the repository doesn't name one.

Next: [the protocol stack](protocol-stack.md).
