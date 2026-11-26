# How it all fits

## Who depends on whom

| Piece | Depends on | How the dependency is pinned |
|---|---|---|
| Smithy core libraries (`smithy-model`, `smithy-build`, `smithy-codegen-core`, `smithy-aws-traits`, `smithy-rules-engine` …) | nothing below | Released together; each generator chooses a version. |
| smithy-go codegen (`software.amazon.smithy.go:smithy-go-codegen`) | Smithy core libraries | Built from source. |
| aws-sdk-go-v2 codegen (`smithy-aws-go-codegen`) | smithy-go codegen, Smithy core | `smithyVersion=1.74.1` in `codegen/gradle.properties`. smithy-go codegen is consumed from Maven local; CI checks out the smithy-go commit named in [SMITHY_GO_CODEGEN_VERSION](https://github.com/aws/aws-sdk-go-v2/blob/main/SMITHY_GO_CODEGEN_VERSION) and builds it first (`ci-find-smithy-go.sh`). |
| aws-sdk-go-v2 service modules (`service/sqs` …) | `github.com/aws/smithy-go` runtime, `github.com/aws/aws-sdk-go-v2` core | Normal Go module versions; `service/sqs/go.mod` requires `github.com/aws/smithy-go v1.28.10`. |
| smithy-java codegen and runtime | Smithy core libraries | One Gradle build, `smithy = "1.75.0"` in `gradle/libs.versions.toml`; the project version is `1.9.1`. |
| aws-sdk-java-v2 | Its own codegen and C2J models; no Smithy library at runtime | Maven; `codegen` uses the Smithy rules engine concepts and the `smithy-rpcv2-protocol` module, not `smithy-build`. |

Put together:

- **smithy-go** owns Go's code generation and runtime and knows nothing about AWS accounts, regions or credentials. It does include the AWS *protocols* (awsJson, restXml, awsQuery and the rest), because those are Smithy protocols.
- **aws-sdk-go-v2** owns everything AWS-specific: the models, about 100 codegen integrations, SigV4, retry modes, the `config` and `credentials` packages, and the generated services.
- **smithy-java** is the Java counterpart of smithy-go plus more: it generates servers too, and it has a dynamic client that needs no generated code at all.
- **aws-sdk-java-v2** is a separate, older SDK with its own generator. It shares the architecture (interceptors, identity and auth schemes, endpoint rules), not the code.

## Bridges between them

smithy-java has modules that let it reuse aws-sdk-java-v2 pieces, in [aws/sdkv2](https://github.com/smithy-lang/smithy-java/tree/main/aws/sdkv2):

| Module | Bridge |
|---|---|
| `aws-sdkv2-auth` | `SdkCredentialsResolver`: use an aws-sdk-java-v2 `AwsCredentialsProvider` (profile, SSO, IMDS …) as a smithy-java identity resolver. |
| `aws-sdkv2-retries` | `SdkRetryStrategy`: run smithy-java calls with an aws-sdk-java-v2 retry strategy. |
| `aws-sdkv2-shapes` | `DocumentConverter`: convert between smithy-java documents and aws-sdk-java-v2 documents, per protocol. |

On the Go side, there's no separate smithy-go client: aws-sdk-go-v2 *is* the smithy-go client for AWS. smithy-go's codegen can also generate clients and servers for non-AWS services (`ShapeCodegenPlugin`, `ServerCodegenPlugin`), using the same runtime.

## A model change, end to end

When AWS adds an operation to a service:

1. A new model is published in aws/api-models-aws. aws-sdk-go-v2 copies it to `codegen/sdk-codegen/aws-models/<service>.json`; aws-sdk-java-v2 receives the matching `service-2.json`.
2. **Go:**
   - `generate-smithy-build` makes the projection;
   - `buildSdk` runs `go-codegen` with the AWS integrations;
   - `copyGoCodegen` copies the output into `service/<service>/`;
   - the release tags the module.
3. **Java:** the service module's Maven build runs `codegen-maven-plugin`, and the generated sources are compiled into the `software.amazon.awssdk:<service>` artifact.
4. No runtime changes are needed unless the change needs a new protocol feature, auth scheme or customization.

Side by side:

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ aws/api-models-aws: a service model changes (for example dynamodb.json gains StartBulkPutItems)  │
└───────────────────────┬───────────────────────────────────────────────────┬──────────────────────┘
                        │                                                   │
                        ▼                                                   ▼
┌──────────────────────────────────────────────┐    ┌──────────────────────────────────────────────┐
│ aws-sdk-go-v2: copy to                       │    │ aws-sdk-java-v2: service-2.json updated in   │
│ codegen/sdk-codegen/aws-models/dynamodb.json │    │ services/dynamodb/ codegen-resources/        │
└──────────────────────────────────────────────┘    └──────────────────────────────────────────────┘
                        │                                                   │
                        ▼                                                   ▼
┌──────────────────────────────────────────────┐    ┌──────────────────────────────────────────────┐
│ generate-smithy-build: one projection per    │    │ codegen-maven-plugin: C2jModels ->           │
│ model file                                   │    │ IntermediateModel -> JavaPoet                │
└──────────────────────────────────────────────┘    └──────────────────────────────────────────────┘
                        │                                                   │
                        ▼                                                   ▼
┌──────────────────────────────────────────────┐    ┌──────────────────────────────────────────────┐
│ buildSdk: go-codegen + ~100 AWS integrations │    │ compiled into                                │
└──────────────────────────────────────────────┘    │ software.amazon.awssdk:dynamodb and released │
                        │                           └──────────────────────────────────────────────┘
                        ▼
┌──────────────────────────────────────────────┐
│ copyGoCodegen: service/dynamodb/ committed;  │
│ Go module tagged                             │
└──────────────────────────────────────────────┘
```

The [DynamoDB page](../aws-emulator/dynamodb.md) shows this in practice: `ListBulkOperations` and `StartBulkPutItems` appeared in the model, and both the emulator (generated from the same models) and the SDKs picked them up without hand-written client code.

## Pointing the SDKs at the AWS Emulator

The [AWS Emulator](../aws-emulator/README.md) speaks the same protocols on `https://127.0.0.1:9297`, so the generated clients work against it unchanged. Only configuration differs. Both SDKs resolve the endpoint override as part of the endpoint step described in [the request lifecycle](request-lifecycle.md).

Only two steps of the request change:

```text
┌────────────────────┐    ┌────────────────────┐    ┌────────────────────┐    ┌────────────────────┐
│ Generated client   │    │ Endpoint step      │    │ TLS trust          │    │ AWS Emulator       │
│ sqs.NewFromConfig  │───►│ AWS_ENDPOINT_URL   │───►│ ca_bundle (Go) or  │───►│ https on           │
│ (Go) or            │    │ or endpoint_url    │    │ truststore.p12 via │    │ 127.0.0.1:9297,    │
│ SqsClient.create() │    │ replaces the       │    │ javax.net.ssl      │    │ account            │
│ (Java)             │    │ rules' result      │    │ (Java)             │    │ 000000000000       │
└────────────────────┘    └────────────────────┘    └────────────────────┘    └────────────────────┘
```

**aws-sdk-go-v2.** `config.LoadDefaultConfig` reads `AWS_ENDPOINT_URL` and the shared config file, including its `endpoint_url` and `ca_bundle` keys. The emulator writes both keys into `~/.aws/emulator/9297/config`. With the environment variables from the [Docker setup](../aws-emulator/README.md#running-the-emulator-in-docker) exported:

```go
cfg, err := config.LoadDefaultConfig(ctx)
if err != nil {
	return err
}
client := sqs.NewFromConfig(cfg)
out, err := client.ListQueues(ctx, &sqs.ListQueuesInput{})
```

**aws-sdk-java-v2.** The client builder reads `AWS_ENDPOINT_URL` (or `aws.endpointUrl`, or `endpoint_url` in the profile), but its profile file has no `ca_bundle` setting. Trust the emulator's certificate through the JVM trust store instead. The emulator writes a PKCS#12 trust store next to `ca.pem`:

```sh
java -Djavax.net.ssl.trustStore="$HOME/.aws/emulator/9297/truststore.p12" \
     -Djavax.net.ssl.trustStoreType=PKCS12 \
     -cp app.jar com.example.Main
```

```java
try (SqsClient sqs = SqsClient.create()) {
    System.out.println(sqs.listQueues().queueUrls());
}
```

That trust store replaces the JVM default, so use it only for processes that talk to the emulator.

**smithy-java.** The client builder takes an endpoint resolver, as in the end-to-end example: `.endpointResolver(EndpointResolver.staticEndpoint(URI.create("https://127.0.0.1:9297")))`. For AWS services you also need SigV4 (`aws-sigv4`) and a credentials resolver, for example the `aws-sdkv2-auth` bridge reading the emulator's credentials file.

## Where this playground fits

This repository works on the first stage, the model:

- [src/main/resources/models](../../src/main/resources/models) holds the AWS models, refreshed from aws/api-models-aws by [scripts/update-aws-models.sh](../../scripts/update-aws-models.sh), the same input aws-sdk-go-v2 copies.
- [smithy-build.json](../../smithy-build.json) defines projections (`public` and `admin`, filtered with `excludeByTrait`, `includeShapesByTag` and similar transforms) and runs the `openapi` plugin on them. That is steps 1 to 3 of [code generation](code-generation.md#inside-a-generator), with the OpenAPI converter where a code generator would run.
- [MarkdownDocsPlugin.java](../../src/main/java/example/MarkdownDocsPlugin.java) is a `SmithyBuildPlugin` of its own, the same extension point `go-codegen` and `java-codegen` use.
- [SmithyToScalaCodegen.scala](../../src/main/scala/SmithyToScalaCodegen.scala) is a minimal generator: it reads the JSON AST directly and writes Scala 3 case classes. It covers steps 5 and 6 (map names, generate code) in their simplest form, without the validation, symbol provider and integrations of a production generator.
- [codegen.md](../../codegen.md) and the root [README](../../README.md) cover smithy-build, plugins and smithy-java's modules in more depth.
- The [AWS Emulator pages](../aws-emulator/README.md) show the last stage, the bytes on the wire, for 36 services.

Next: [The Smithy ecosystem](ecosystem.md).
