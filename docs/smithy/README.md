# Smithy end to end

How a Smithy model becomes a working SDK, explained with four real code bases, then set beside smithy-rs, smithy-kotlin and smithy-python:

| Repository | Language | What it is |
|---|---|---|
| [aws/smithy-go](https://github.com/aws/smithy-go) | Java (codegen) + Go (runtime) | The Smithy code generator for Go, and the Go packages generated code runs on: the middleware stack, protocols, encoders, endpoint and auth interfaces. |
| [aws/aws-sdk-go-v2](https://github.com/aws/aws-sdk-go-v2) | Java (codegen) + Go | The AWS SDK for Go v2. Its build feeds every AWS model to smithy-go's generator, adds AWS behaviour through about 100 codegen integrations, and checks the generated `service/<name>` packages into the repository. |
| [smithy-lang/smithy-java](https://github.com/smithy-lang/smithy-java) | Java | The Smithy code generator and runtime for Java: clients, servers and standalone types, all driven by runtime schemas. Not tied to AWS, with AWS protocols and SigV4 as add-on modules. |
| [aws/aws-sdk-java-v2](https://github.com/aws/aws-sdk-java-v2) | Java | The AWS SDK for Java v2. It predates smithy-java and has its own generator, which reads a JSON rendering of each service (`service-2.json`), but its runtime follows the same Smithy reference architecture. |

The pages were written from the repositories as of 2026-11-26 (smithy-go `54a5320`, aws-sdk-go-v2 `fab6ce7`, smithy-java `093fe37`, aws-sdk-java-v2 `a36ea43`). File names and class names are linked so you can check them.

## The big picture

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Service model: Smithy 2.0 (one JSON AST file per AWS service, published in aws/api-models-aws)   │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                │                                 │                                 │
                ▼                                 ▼                                 ▼
┌──────────────────────────────┐  ┌──────────────────────────────┐  ┌──────────────────────────────┐
│ aws-sdk-go-v2: a copy of     │  │ smithy-java: any .smithy or  │  │ aws-sdk-java-v2:             │
│ each Smithy JSON AST in its  │  │ JSON model you point         │  │ service-2.json (C2J, a JSON  │
│ codegen/ directory           │  │ smithy-build at              │  │ rendering of the same        │
│ (aws-models/sqs.json)        │  │                              │  │ service)                     │
└──────────────────────────────┘  └──────────────────────────────┘  └──────────────────────────────┘
                ▼                                 ▼                                 ▼
┌──────────────────────────────┐  ┌──────────────────────────────┐  ┌──────────────────────────────┐
│ smithy-build, one projection │  │ smithy-build, plugin         │  │ codegen-maven-plugin:        │
│ per model, plugin go-codegen │  │ java-codegen, modes client / │  │ C2jModels ->                 │
│ (Gradle)                     │  │ server / types               │  │ IntermediateModel ->         │
│                              │  │                              │  │ JavaPoet                     │
└──────────────────────────────┘  └──────────────────────────────┘  └──────────────────────────────┘
                ▼                                 ▼                                 ▼
┌──────────────────────────────┐  ┌──────────────────────────────┐  ┌──────────────────────────────┐
│ Generated: service/sqs with  │  │ Generated: client interface  │  │ Generated: SqsClient, model  │
│ api_op_*.go, types/,         │  │ and impl, structures with a  │  │ POJOs with SdkField lists,   │
│ schemas/, endpoints.go,      │  │ Schema, server stubs         │  │ marshallers                  │
│ auth.go                      │  │                              │  │                              │
└──────────────────────────────┘  └──────────────────────────────┘  └──────────────────────────────┘
                ▼                                 ▼                                 ▼
┌──────────────────────────────┐  ┌──────────────────────────────┐  ┌──────────────────────────────┐
│ Runtime: smithy-go           │  │ Runtime: core (schemas,      │  │ Runtime: sdk-core, aws-core, │
│ (middleware, protocols,      │  │ serde), client-core, codecs, │  │ core/protocols,              │
│ encoding) + aws/ (SigV4,     │  │ aws-client-*, aws-sigv4      │  │ http-auth-aws, http-clients  │
│ retry), config, credentials  │  │                              │  │                              │
└──────────────────────────────┘  └──────────────────────────────┘  └──────────────────────────────┘
                │                                 │                                 │
                ▼                                 ▼                                 ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ HTTPS request, signed with SigV4, to the AWS endpoint, or to https://127.0.0.1:9297 when you run │
│ the AWS Emulator                                                                                 │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

The same shape holds for the other generators. Here are three more: Rust, Kotlin and Python (details in [the Smithy ecosystem](ecosystem.md)):

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Service model: any Smithy model, or the AWS service models for the AWS SDKs built on these       │
│ generators                                                                                       │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
                │                                 │                                 │
                ▼                                 ▼                                 ▼
┌──────────────────────────────┐  ┌──────────────────────────────┐  ┌──────────────────────────────┐
│ smithy-rs: any model; for    │  │ smithy-kotlin: any model;    │  │ smithy-python: any model;    │
│ the AWS SDK, aws/sdk takes   │  │ aws-sdk-kotlin runs it on    │  │ aws-sdk-python (v4,          │
│ the models from aws-sdk-rust │  │ the AWS service models       │  │ Developer Preview) runs it   │
│ (-Paws.sdk.models.path)      │  │                              │  │ on 39 AWS models             │
└──────────────────────────────┘  └──────────────────────────────┘  └──────────────────────────────┘
                ▼                                 ▼                                 ▼
┌──────────────────────────────┐  ┌──────────────────────────────┐  ┌──────────────────────────────┐
│ smithy-build, plugin         │  │ smithy-build, plugin         │  │ smithy-build, plugin         │
│ rust-client-codegen (Kotlin, │  │ kotlin-codegen (Kotlin); AWS │  │ python-client-codegen        │
│ Gradle); AWS customizations  │  │ generation in                │  │ (Java); or the experimental  │
│ in aws/codegen-aws-sdk       │  │ codegen/aws-codegen          │  │ smithy-python CLI            │
└──────────────────────────────┘  └──────────────────────────────┘  └──────────────────────────────┘
                ▼                                 ▼                                 ▼
┌──────────────────────────────┐  ┌──────────────────────────────┐  ┌──────────────────────────────┐
│ Generated: a crate per       │  │ Generated: a Kotlin          │  │ Generated: a client and      │
│ service with the client,     │  │ Multiplatform client, model  │  │ models holding compact       │
│ operations, types and        │  │ classes and operation        │  │ schemas; no serialization    │
│ per-protocol serializers     │  │ serializers                  │  │ code                         │
└──────────────────────────────┘  └──────────────────────────────┘  └──────────────────────────────┘
                ▼                                 ▼                                 ▼
┌──────────────────────────────┐  ┌──────────────────────────────┐  ┌──────────────────────────────┐
│ Runtime: aws-smithy-runtime  │  │ Runtime: runtime-core,       │  │ Runtime: smithy-core         │
│ (the orchestrator), codecs   │  │ serde-json and other serde   │  │ (pipeline, schemas, retries, │
│ such as aws-smithy-json,     │  │ modules, aws-json-protocols, │  │ auth), the smithy-json,      │
│ aws-smithy-http-client;      │  │ http-client-engines,         │  │ smithy-cbor and smithy-xml   │
│ aws-sigv4, aws-config        │  │ aws-signing-default          │  │ codecs, smithy-http,         │
│                              │  │                              │  │ aws-sdk-signers              │
└──────────────────────────────┘  └──────────────────────────────┘  └──────────────────────────────┘
                │                                 │                                 │
                ▼                                 ▼                                 ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ HTTPS request to the service: an AWS endpoint for the AWS SDKs for Rust, Kotlin and Python, or   │
│ any Smithy service                                                                               │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

Every path from model to wire has the same three stages:

1. **A model** says what the service is: its operations, their inputs, outputs and errors, and traits that say how they travel over HTTP, how they're authenticated, and where the endpoint is.
2. **A generator** turns the model into source code in one language. The generator knows the language; the model knows the service.
3. **A runtime** does the work at call time: serializing, resolving the endpoint, signing, retrying, sending and parsing. Generated code is mostly a thin, typed layer over it.

Because the generator and runtime are shared, adding a service to an SDK is a model change, not hand-written code. aws-sdk-go-v2 has 446 model files and the same number of generated service packages, and none of them is written by hand.

## Pages

New to HTTP, JSON or RPC protocols? Start with [HTTP, data formats and protocols](http-and-formats.md), which explains how they fit together.

Read them in order the first time:

1. [The model](models.md): what the input looks like, which traits SDKs depend on, and where each repository gets its models.
2. [Code generation](code-generation.md): how `smithy-build` runs a generator plugin, and how the Go and Java generators are built (symbol providers, directed codegen, integrations).
3. [Generated code](generated-code.md): what comes out, side by side for the same operations: a Go struct with schema-driven serde, a smithy-java structure, an aws-sdk-java-v2 POJO.
4. [The request lifecycle](request-lifecycle.md): one call from method invocation to parsed response, in each runtime: Go's middleware stack, smithy-java's client pipeline, aws-sdk-java-v2's pipeline stages.
5. [Protocols and serialization](protocols-and-serde.md): how `@awsJson1_0`, `@restJson1`, `@restXml`, `@awsQuery`, `@ec2Query` and `@rpcv2Cbor` are implemented, and why every runtime is moving to schema-driven serde.
6. [How it all fits](how-it-fits.md): dependencies and versions between the repositories, the bridges between them, and how this playground and the [AWS Emulator docs](../aws-emulator/README.md) relate to them.
7. [The Smithy ecosystem](ecosystem.md): the core `smithy` project, smithy-gradle-plugin, setup-smithy-cli, and the generators for Rust, Kotlin, Swift, TypeScript, Python, Ruby and PHP (including smithy-php's nine split packages).
8. [The protocol stack](protocol-stack.md): what each part of a call does (model, schemas, codec, protocol, endpoint resolver, auth, transport, pipeline), how the layers fit together in every runtime, and the shared protocol test suite.
9. [Adding a protocol: Protobuf over gRPC](new-protocol-protobuf.md): a worked design for a new binary protocol: a protocol trait, Smithy's `@idx` field numbers, a Protobuf codec, gRPC framing and trailers, and errors.
10. [Adding a protocol: JSON-RPC 2.0](new-protocol-json-rpc.md): the easy case, a new protocol on the existing JSON codec, compared with `awsJson1_0` and `rpcv2Json`.
11. [A Scala client generator](scala-client.md): the playground's own Scala 3 generator and runtime, layer by layer, with generated SQS, DynamoDB and `rpcv2Json` clients and their tests.
12. [Schemas in Scala](schema.md): what Tapir, ZIO Schema, ZIO Blocks and smithy4s mean by a schema, read from their source, how each is used in production, and how they relate to this repo's runtime schema.
