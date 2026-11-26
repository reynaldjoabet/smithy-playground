# Code generation

A Smithy code generator is a `smithy-build` plugin: a Java class that receives a validated model and writes files. smithy-go and smithy-java are both built this way. aws-sdk-java-v2 is the exception, with its own Maven plugin; it's covered at the end of this page.

## smithy-build runs the plugin

`smithy-build` reads a `smithy-build.json`, assembles the model, and runs the plugins named in each **projection**. A projection is a view of the model (possibly filtered or transformed) plus the plugins to run on it.

smithy-java's [end-to-end example](https://github.com/smithy-lang/smithy-java/blob/main/examples/end-to-end/smithy-build.json) runs one plugin on the whole model:

```json
{
  "version": "1.0",
  "plugins": {
    "java-codegen": {
      "service": "com.example#CoffeeShop",
      "namespace": "software.amazon.smithy.java.example.etoe",
      "protocol": "aws.protocols#restJson1",
      "modes": ["client", "server"]
    }
  }
}
```

aws-sdk-go-v2 doesn't commit a `smithy-build.json`. Its Gradle task `generate-smithy-build` in [codegen/sdk-codegen/build.gradle.kts](https://github.com/aws/aws-sdk-go-v2/blob/main/codegen/sdk-codegen/build.gradle.kts) writes one projection per model file, and the `buildSdk` task then runs them all. For SQS the generated entry is:

```json
"sqs.2012-11-05": {
  "imports": [".../aws-models/sqs.json"],
  "plugins": {
    "go-codegen": {
      "service": "com.amazonaws.sqs#AmazonSQS",
      "module": "github.com/aws/aws-sdk-go-v2/service/sqs",
      "useLegacySerde": false
    }
  }
}
```

The projection name is the `sdkId` (lower-cased, without spaces or dashes), a dot and the API version. Setting `SMITHY_GO_BUILD_API=com.amazonaws.sqs` limits a build to matching services, which is how you regenerate one service without waiting for all 446.

The plugin is found by its name. smithy-build looks it up with Java's `ServiceLoader`: each generator jar lists its plugin classes in `META-INF/services/software.amazon.smithy.build.SmithyBuildPlugin`.

| Generator | Plugin name | Class | Other plugins in the same jar |
|---|---|---|---|
| smithy-go | `go-codegen` | [GoCodegenPlugin](https://github.com/aws/smithy-go/blob/main/codegen/smithy-go-codegen/src/main/java/software/amazon/smithy/go/codegen/GoCodegenPlugin.java) | `ServerCodegenPlugin` (Go server stubs), `ShapeCodegenPlugin` (types only) |
| smithy-java | `java-codegen` | [JavaCodegenPlugin](https://github.com/smithy-lang/smithy-java/blob/main/codegen/codegen-plugin/src/main/java/software/amazon/smithy/java/codegen/JavaCodegenPlugin.java) | One plugin; the `modes` setting chooses `client`, `server` and/or `types` |

## Inside a generator

Both generators follow the same steps once the plugin starts:

1. **Assemble.** `ModelAssembler` loads the model files, the prelude, and the trait definitions found on the classpath (`smithy-aws-traits`, `smithy-rules-engine` and others) into one in-memory `Model`.
2. **Validate.** Built-in and trait validators run. Any `ERROR` stops the build; this is where a bad `@http` URI or a member targeting a missing shape is caught.
3. **Project.** smithy-build applies the projection's transforms, such as filtering shapes or removing unused ones, and hands the result to the plugin.
4. **Preprocess.** The generator and its integrations rewrite the model for their own convenience. For example, every operation gets its own unique input and output shapes, so the generator can name them `SendMessageInput` and `SendMessageOutput` without clashes.
5. **Map symbols.** A `SymbolProvider` maps each shape to a name, a type and a file in the target language: `smithy.api#String` becomes `*string` in Go and `String` in Java; a structure becomes a Go struct in `types/types.go` or a Java `final class`.
6. **Generate.** A walk over the service closure visits each shape and writes its code: structures, enums, unions, errors, operations, schemas, the client, and the endpoint resolver.
7. **Integrate.** Integrations add behaviour the base generator doesn't know about: config fields, runtime plugins (middleware in Go, client plugins in Java), and extra files.
8. **Write.** Writers flush the files under `build/smithyprojections/<projection>/<plugin>/`. A build step then copies them where they belong.

The same steps as one flow. smithy-build runs steps 1 to 3 for every plugin; each plugin runs 4 to 8 itself:

```text
┌─ smithy-build (shared by every generator) ───────────────────────────────────────────────────────┐
│  ┌──────────────────────────┐     ┌──────────────────────────┐     ┌──────────────────────────┐  │
│  │ 1. Assemble              │     │ 2. Validate              │     │ 3. Project               │  │
│  │ model files + prelude +  │ ──► │ validators run; any      │ ──► │ transforms filter,       │  │
│  │ trait jars -> one Model  │     │ ERROR stops the build    │     │ include or exclude       │  │
│  │                          │     │                          │     │ shapes                   │  │
│  └──────────────────────────┘     └──────────────────────────┘     └──────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────────────┬─────────────┘
                                                                                     │
                                                                                     ▼
┌─ plugin: go-codegen, java-codegen, rust-client-codegen ... ──────────────────────────────────────┐
│  ┌──────────────────────────┐     ┌──────────────────────────┐     ┌──────────────────────────┐  │
│  │ 4. Preprocess            │     │ 5. Map symbols           │     │ 6. Generate              │  │
│  │ integrations rewrite the │ ──► │ SymbolProvider: shape -> │ ──► │ walk the closure: types, │  │
│  │ model (unique inputs,    │     │ name, type, file         │     │ schemas, client,         │  │
│  │ auth backfill)           │     │                          │     │ endpoints                │  │
│  └──────────────────────────┘     └──────────────────────────┘     └─────────────┬────────────┘  │
│                                                                                  │               │
│                                                                                  ▼               │
│                                   ┌──────────────────────────┐     ┌──────────────────────────┐  │
│                                   │ 8. Write                 │     │ 7. Integrate             │  │
│                                   │ files under build/       │ ◄── │ middleware or client     │  │
│                                   │ smithyprojections/       │     │ plugins, config, extra   │  │
│                                   │                          │     │ files                    │  │
│                                   └─────────────┬────────────┘     └──────────────────────────┘  │
└─────────────────────────────────────────────────┼────────────────────────────────────────────────┘
                                                  │
                                                  ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Output: Go files are copied into service/<name>/ in aws-sdk-go-v2 and committed; Java, Rust and  │
│ Kotlin sources are compiled into libraries                                                       │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### smithy-go

`GoCodegenPlugin.execute` creates a [CodegenVisitor](https://github.com/aws/smithy-go/blob/main/codegen/smithy-go-codegen/src/main/java/software/amazon/smithy/go/codegen/CodegenVisitor.java), which:

- walks every shape reachable from the service and calls the matching generator (`StructureGenerator`, `EnumGenerator`, `UnionGenerator`, `OperationGenerator`, `SchemaGenerator` and so on);
- lets each integration write additional files;
- generates event-stream types, the endpoint resolver, auth resolvers and the client (`ServiceGenerator` writes `api_client.go`).

Code is written through `GoWriter`, which tracks imports for you: referring to a symbol with `$T` adds its import. The repository's [AGENTS.md](https://github.com/aws/smithy-go/blob/main/AGENTS.md) documents both writing styles (positional `$L $S $T` arguments and named `goTemplate` templates).

Everything AWS-specific is an **integration**, an implementation of [GoIntegration](https://github.com/aws/smithy-go/blob/main/codegen/smithy-go-codegen/src/main/java/software/amazon/smithy/go/codegen/integration/GoIntegration.java), again discovered through `ServiceLoader`. Its hooks are:

| Hook | Used for |
|---|---|
| `preprocessModel` | Rewriting the model before generation: backfilling auth traits, filtering shapes. |
| `decorateSymbolProvider` | Changing names or types. |
| `writeAdditionalFiles` | Extra files: paginators, waiters, presigners, customizations. |
| `getClientPlugins` | `RuntimeClientPlugin`s: config fields on `Options`, and middleware to register on each operation's stack. |
| `getProtocolGenerators` | Legacy serde: one generator per protocol (`AwsJsonRpc1_0`, `AwsRestJson1`, `AwsRestXml`, `AwsQuery`, `Ec2Query`). |
| `renderPreEndpointResolutionHook` / `renderPostEndpointResolutionHook` | Code around endpoint resolution (S3 and S3 Express use these). |

aws-sdk-go-v2 ships about 100 of them in [codegen/smithy-aws-go-codegen](https://github.com/aws/aws-sdk-go-v2/tree/main/codegen/smithy-aws-go-codegen/src/main/java/software/amazon/smithy/aws/go/codegen), listed in its `META-INF/services/software.amazon.smithy.go.codegen.integration.GoIntegration` file. Among them:

- `AddProtocols` registers the AWS protocol generators.
- `AwsSignatureVersion4` adds SigV4 signing.
- `AwsRetryMiddlewareHelper` adds retries.
- `AwsEndpointGenerator` adds endpoint resolution.
- `AwsIdempotencyTokenProvider` fills idempotency tokens.
- `AwsClientUserAgent` adds the user agent.
- `AwsHttpChecksumGenerator` adds checksums.
- `AwsPaginators` and `AwsWaiters` add paginators and waiters.
- A `customization` package holds service-specific behaviour, for example for S3, Glacier, Route 53 and DynamoDB.

So the split is:

- **smithy-go** knows Smithy and Go.
- **aws-sdk-go-v2/codegen** knows AWS.
- **aws-sdk-go-v2/service/** is their output, copied into the repository by the `copyGoCodegen` task and committed.

In a picture:

```text
┌──────────────────────────────────────┐
│ smithy-go/codegen                    │
│ go-codegen plugin, CodegenVisitor,   │
│ GoWriter, the GoIntegration          │────┐   ┌──────────────────────────────────────────────────┐
│ interface                            │    │   │ buildSdk -> copyGoCodegen                        │
└──────────────────────────────────────┘    │   │ service/<name>/ in aws-sdk-go-v2: generated      │
                                            ├──►│ once, committed, released as Go modules          │
┌──────────────────────────────────────┐    │   │                                                  │
│ aws-sdk-go-v2/codegen                │    │   └──────────────────────────────────────────────────┘
│ ~100 GoIntegrations (SigV4, retries, │    │
│ endpoints, paginators, waiters, S3   │────┘
│ ...) + aws-models/*.json             │
└──────────────────────────────────────┘
```

### smithy-java

`JavaCodegenPlugin.execute` reads `modes` and runs a `CodegenDirector` from smithy-codegen-core with `DirectedJavaCodegen`. The director is the standard Smithy "directed codegen" loop: it calls `generateStructure`, `generateUnion`, `generateEnumShape`, `generateService` and so on, once for each shape. The generators live in [codegen/codegen-core/.../generators](https://github.com/smithy-lang/smithy-java/tree/main/codegen/codegen-core/src/main/java/software/amazon/smithy/java/codegen/generators):

- `StructureGenerator`, `UnionGenerator`, `EnumGenerator` and `BuilderGenerator` write the types;
- `SchemasGenerator` and `SchemaIndexGenerator` write the runtime schemas;
- `ClientInterfaceGenerator` and `ClientImplementationGenerator` write the client;
- `ServiceGenerator` and `OperationInterfaceGenerator` write the server stubs.

The modes decide what is written:

| Mode | Output |
|---|---|
| `types` | Structures, unions, enums and their schemas only, for sharing a data model without any service code. A `closure` setting can pick which shapes. |
| `client` | Types plus a client interface, its implementation, and paginators and waiters. |
| `server` | Types plus one interface per operation (for example `GetMenuOperation`) and a service class that routes requests to your implementations. |

Integrations implement `JavaCodegenIntegration` and are listed in `META-INF/services/software.amazon.smithy.java.codegen.JavaCodegenIntegration`. The plugin jar itself registers two: `ClientJavadocExamplesIntegration` and `AwsCredentialChainIntegration`.

Unlike smithy-go, smithy-java keeps protocols out of generated code entirely. The generated client names a protocol, the runtime provides it, and serialization goes through schemas (see [protocols and serialization](protocols-and-serde.md)). That's why the plugin takes a `protocol` setting instead of having protocol generators.

### aws-sdk-java-v2

aws-sdk-java-v2 generates each service module during its Maven build with [codegen-maven-plugin](https://github.com/aws/aws-sdk-java-v2/blob/master/codegen-maven-plugin/src/main/java/software/amazon/awssdk/codegen/maven/plugin/GenerationMojo.java):

1. **Read the inputs.** `GenerationMojo` reads `service-2.json`, `customization.config`, `paginators-1.json`, `waiters-2.json`, and `endpoint-rule-set.json` or `endpoint-bdd-1.json` from `src/main/resources/codegen-resources/`. Together they form a `C2jModels`.
2. **Build the intermediate model.** `IntermediateModelBuilder` turns them into an `IntermediateModel`: Java names, getters and setters, marshalling locations, with the customizations applied. The work is split into steps such as `AddOperations`, `AddShapes` and `AddExceptionShapes`.
3. **Emit the code.** `CodeGenerator` runs emitters that write Java with JavaPoet (the `poet` package): clients, model classes, marshallers, paginators, waiters and endpoint providers.

As a flow:

```text
┌────────────────────────────┐     ┌────────────────────────────┐     ┌────────────────────────────┐
│ codegen-resources/         │     │ GenerationMojo             │     │ CodeGenerator              │
│ service-2.json,            │────►│ loads C2jModels;           │────►│ JavaPoet emitters: client, │
│ customization.config,      │     │ IntermediateModelBuilder   │     │ model POJOs, marshallers,  │
│ paginators-1.json,         │     │ adds Java names,           │     │ endpoint provider          │
│ waiters-2.json,            │     │ marshalling locations,     │     │                            │
│ endpoint-*.json            │     │ customizations             │     │                            │
└────────────────────────────┘     └────────────────────────────┘     └────────────────────────────┘
```

`customization.config` is where hand-written behaviour is wired into generated code. SQS's adds an interceptor and enables the batch manager:

```json
{
  "verifiedSimpleMethods": ["listQueues"],
  "interceptors": ["software.amazon.awssdk.services.sqs.internal.MessageMD5ChecksumInterceptor"],
  "batchManagerSupported": true
}
```

That plays the same role as a Go integration or a smithy-java client plugin, but per service and declared in data instead of code.

Next: [Generated code](generated-code.md).
