# smithy-playground
Most IDLs bake the protocol into the model. OpenAPI assumes HTTP+JSON. Protobuf assumes gRPC. GraphQL assumes GraphQL-over-HTTP. Smithy deliberately separates what your API does from how it's transported. The same Smithy model can be served as REST+JSON, REST+CBOR, RPC, or a custom AWS protocol, by attaching different protocol traits.

The other big idea is traits. Instead of a fixed schema language, Smithy is built around composable metadata annotations. Authentication, pagination, retries, validation, deprecation, documentation, HTTP bindings — all of these are traits applied to shapes. You can define your own traits too. The core language is small; the expressive power comes from traits.

### Traits — the heart of the system
Traits are themselves shapes, marked with `@trait`. Smithy ships with a large built-in library:
Built-in categories include constraint traits (`@length`, `@range`, `@pattern`, `@required`, `@uniqueItems`), HTTP binding traits (`@http`, `@httpLabel`, `@httpQuery`, `@httpHeader`, `@httpPayload`, `@httpResponseCode`), behavior traits (`@readonly`, `@idempotent`, `@retryable`, `@paginated`), authentication traits (`@httpBasicAuth`, `@httpBearerAuth`, `@httpApiKeyAuth`, `@auth`), documentation traits (`@documentation`, `@examples`, `@externalDocumentation`, `@deprecated`), and protocol traits (`@restJson1`, `@awsJson1_0`, `@awsJson1_1`, `@restXml`).

```smithy
@trait(selector: "operation")
structure rateLimit {
    @required
    requestsPerMinute: Integer
}

@rateLimit(requestsPerMinute: 100)
operation GetForecast { ... }
```
The `selector` controls where the trait can be applied — Smithy has its own selector language (similar in spirit to CSS selectors) for matching shapes in the model, which is also how validation rules and code generators query the model.
## Build pipeline and projections
Smithy projects are driven by a `smithy-build.json` file. This declares your model files, plugins (code generators, validators, doc generators), and projections — transformations of the model used to produce different artifact sets.

A common pattern: a `source` projection contains the full model with internal-only operations, while a `public` projection uses transforms like `excludeShapesByTag` to strip internal shapes before generating the public SDK. Projections let one model serve multiple audiences without forking it.

```json
{
  "version": "1.0",
  "sources": ["model"],
  "projections": {
    "source": { "plugins": { "model": {} } },
    "public": {
      "transforms": [
        { "name": "excludeShapesByTag", "args": { "tags": ["internal"] } }
      ],
      "plugins": {
        "openapi": { "service": "example.weather#Weather" }
      }
    }
  }
}
```

For code generation there's Smithy `TypeScript`, `Smithy Rust` (used for the official AWS SDK for Rust),` Smithy Kotlin`, `Smithy Swift`, `Smithy Go`, `Smithy Java` (server and client), `Smithy Python`, and a OpenAPI converter that lets you publish OpenAPI 3 specs from a Smithy model for consumption by tools that don't speak Smithy.

```sh
@trait(selector: "*")
structure example {
    title: String
    description: String
}

@example(title: "Basic usage", description: "...")
operation Foo { ... }
```
The applied trait is just data. The behavior comes from tools that read it. The OpenAPI plugin, for example, looks for `@http` traits to determine the HTTP method and path for each operation. The AWS SDK code generators look for protocol traits (`@restJson1`, `@awsJson1_0`, etc.) to determine how to serialize requests and responses.

### Selectors — the model query language
Every trait declares where it can be applied via a selector. Selectors are Smithy's domain-specific language for matching shapes, modeled loosely on CSS selectors. Some examples:
- `*` — any shape
- `operation` — only operations
- `member` — only structure members
- `member :test(> string)` — members whose target is a string
- `[trait|http]` — shapes that have the @http trait
- `service ~> operation` — operations reachable from a service
- `:not([trait|deprecated])` — shapes without @deprecated

Selectors are how the model is queried. Linters, code generators, and the build's validation pipeline all use them
The process begins when you run `smithy build` (via the CLI) or trigger a Gradle build. The CLI entry point is:
```java
@Override
public String getSummary() {
    return "Builds Smithy models and creates plugin artifacts for each projection found in smithy-build.json.";
}
```
This command reads `smithy-build.json`, loads the model, and delegates to `SmithyBuild`.

##  Configuration: smithy-build.json

The build is configured via `smithy-build.json`, which defines projections (model views/filters) and plugins (code generators). Example structure:
```json
{
  "projections": {
    "myProjection": {
      "transforms": [
        "source:my-model.smithy",
        "filter:include[shapeType=structure]"
      ]
    }
  },
  "plugins": {
    "myPlugin": {
      "type": "codegen",
      "projection": "myProjection",
      "settings": {
        // plugin-specific settings
      }
    }
  }
}
```
Each projection can have transforms (to filter/modify the model) and plugins (to generate artifacts). The `type` field in plugins indicates the plugin type (e.g., codegen, validation).

## Model Assembly: Model.assembler()
Smithy `.smithy` or `.json` model files are parsed and assembled into an in-memory `Model` object:
```java
Model model = Model.assembler()
    .addImport("src/main/smithy/model.smithy")
    .assemble()
    .unwrap();

Model model = Model.assembler()
    .addImport(Paths.get("model/main.smithy"))
    .discoverModels(classLoader)
    .assemble()
    .unwrap();    
```
The `ModelAssembler` handles parsing, validation, and constructing the complete shape graph.

## Build Orchestration: SmithyBuild → SmithyBuildImpl
`SmithyBuild.build()` creates a `SmithyBuildImpl` which orchestrates the pipeline:
```java
SmithyBuildImpl buildImpl = new SmithyBuildImpl(model, projections, plugins);
buildImpl.execute();
```
```java
/**
     * Builds the model and applies all projections, passing each
     * {@link ProjectionResult} to the provided callback as they are
     * completed and each encountered exception to the provided
     * {@code exceptionCallback} as they are encountered.
     *
     * <p>This method differs from {@link #build()} in that it does not
     * require every projection and projection result to be loaded into
     * memory.
     *
     * <p>The result each projection is placed in the outputDirectory.
     * A {@code [projection]-build-info.json} file is created in the output
     * directory. A directory is created for each projection using the
     * projection name, and a file named model.json is place in each directory.
     *
     * @param resultCallback A thread-safe callback that receives projection
     *   results as they complete.
     * @param exceptionCallback A thread-safe callback that receives the name
     *   of each failed projection and the exception that occurred.
     * @throws IllegalStateException if a {@link SmithyBuildConfig} is not set.
     */
    public void build(Consumer<ProjectionResult> resultCallback, BiConsumer<String, Throwable> exceptionCallback) {
        new SmithyBuildImpl(this).applyAllProjections(resultCallback, exceptionCallback);
    }
```
For each projection in the config, `SmithyBuildImpl`:
- Applies transforms (e.g., includeShapesByTag, excludeShapesByTrait) to produce a projected model
- Resolves plugins by name using Java SPI (ServiceLoader)
- Executes each plugin, passing a `PluginContext` containing the projected model and a `FileManifest`

## Plugin System: SmithyBuildPlugin (SPI-based discovery)

Every code generator must implement `SmithyBuildPlugin`:
```java
public interface SmithyBuildPlugin {
    String getName();
    void execute(PluginContext context);
}
```
Plugins are discovered via Java SPI using `META-INF/services/software.amazon.smithy.build.SmithyBuildPlugin` files on the classpath. The `pluginFactory` looks up plugins by the name specified in `smithy-build.json`.

![alt text](image.png)

The AWS Java SDK is for calling AWS. The Smithy Java Client is for calling anything modeled with Smithy

Smithy Java generated clients are protocol-agnostic. The framework includes built-in support for HTTP transport, AWS protocols (including AWS `JSON 1.0/1.1`, `restJson1`, `restXml` and `Query`), and Smithy `RPCv2-CBOR`. You can swap protocols at runtime without rebuilding the client, enabling gradual protocol migrations and multi-protocol support with no code changes.

## Common
- `core` - Provides basic functionality for (de)serializing generated types and defining Schema's, minimal representations of the Smithy data model for use at runtime.
- `io` - Common I/O functionality for clients/servers.
- `auth-api` - shared Authorization/Authentication API for clients and servers.
- `framework-errors` - Common errors that could be thrown by the Smithy Java framework.

## Client
- `client-core` - Provides protocol and transport agnostic functionality for clients. All generated clients require this package as a runtime dependency.
- `client-http` - Client-side implementation of HTTP transport.
- `dynamic-client` - Smithy client that exposes a dynamic API that doesn't require codegen.

## Server
- `server-core` - Provides protocol and transport agnostic functionality for servers. All generated server-stubs require this package as a runtime dependency.
- `server-netty` - Provides an HTTP server implementation using the Netty runtime.

## Protocols
Smithy Java, like the Smithy IDL, is protocol-agnostic. Servers support any number of protocols and clients can set the protocol to use at runtime

The `rpcv2-cbor` protocol is a generic binary protocol provided by Smithy Java that can be a good choice for services that want a fast, compact data format.

### Client
- `client-rpcv2-cbor` - Implementation `rpcv2-cbor` protocol.
- `aws-client-awsjson` - Implementation of AWS JSON 1.0 and AWS JSON 1.1 protocols.
- `aws-client-restjson` - Implementation of AWS restJson1 protocol.
- `aws-client-restXml` - Implementation of AWS restXml protocol.
- `aws-client-awsquery` - Implementation of AWS Query protocol.

### Server
- `server-rpcv2-cbor` - Implementation `rpcv2-cbor` protocol.
- `aws-server-restjson` - Implementation of AWS restJson1 protocol.

### Codecs
Codecs provide basic (de)serialization functionality for protocols.

- `json-codec` - (de)serialization functionality for JSON format
- `cbor-codec` - Binary (de)serialization functionality for CBOR format
- `xml-codec` - (de)serialization functionality for XML format

## Utilities
- `jmespath` - `JMESPath` implementation that allows querying a Document using a `JMESPath` expression.


```
"sources": [
        "models/",
        "traits/"
    ]
``` 
where to look for models and traits. By default, the CLI will look for models in `src/main/smithy` and `src/test/smithy`, but you can customize this with the `sources` field in `smithy-build.json`. In this example, we're telling the build to look for models and traits in the `models/` and `traits/` directories at the root of the project.    

`software.amazon.smithy.python.codegen.types.PythonTypeCodegenPlugin` is a plugin that generates Python code from Smithy models. It is used in the `python-codegen` projection in `smithy-build.json`. This plugin takes the projected model and generates Python client code based on the shapes and operations defined in the model. The generated code will include classes for data structures, client methods for operations, and any necessary serialization/deserialization logic.

```java
package software.amazon.smithy.python.codegen.types;

import java.util.Arrays;
import java.util.Optional;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.python.codegen.PythonSettings;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Settings used by {@link PythonTypeCodegenPlugin}.
 *
 * @param service The id of the service that is being generated.
 * @param moduleName The name of the module to generate.
 * @param moduleVersion The version of the module to generate.
 * @param moduleDescription The optional module description for the module that will be generated.
 * @param selector An optional selector to reduce the set of shapes to be generated.
 */
@SmithyUnstableApi
public record PythonTypeCodegenSettings(
        Optional<ShapeId> service,
        String moduleName,
        String moduleVersion,
        String moduleDescription,
        Selector selector,
        Boolean generateInputsAndOutputs) implements ToSmithyBuilder<PythonTypeCodegenSettings> {

    private static final String SERVICE = "service";
    private static final String MODULE_NAME = "module";
    private static final String MODULE_DESCRIPTION = "moduleDescription";
    private static final String MODULE_VERSION = "moduleVersion";
    private static final String SELECTOR = "selector";
    private static final String GENERATE_INPUTS_AND_OUTPUTS = "generateInputsAndOutputs";

    private PythonTypeCodegenSettings(Builder builder) {
        this(
                Optional.ofNullable(builder.service),
                builder.moduleName,
                builder.moduleVersion,
                builder.moduleDescription,
                builder.selector,
                builder.generateInputsAndOutputs);
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .moduleName(moduleName)
                .moduleVersion(moduleVersion)
                .moduleDescription(moduleDescription)
                .selector(selector)
                .generateInputsAndOutputs(generateInputsAndOutputs);
        service.ifPresent(builder::service);
        return builder;
    }

    public PythonSettings toPythonSettings(ShapeId service) {
        return PythonSettings.builder()
                .service(service)
                .moduleName(moduleName)
                .moduleVersion(moduleVersion)
                .moduleDescription(moduleDescription)
                .artifactType(PythonSettings.ArtifactType.TYPES)
                .build();
    }

    public PythonSettings toPythonSettings() {
        return toPythonSettings(service.get());
    }

    /**
     * Create a settings object from a configuration object node.
     *
     * @param config Config object to load.
     * @return Returns the extracted settings.
     */
    public static PythonTypeCodegenSettings fromNode(ObjectNode config) {
        config.warnIfAdditionalProperties(Arrays.asList(SERVICE, MODULE_NAME, MODULE_DESCRIPTION, MODULE_VERSION));

        String moduleName = config.expectStringMember(MODULE_NAME).getValue();
        Builder builder = builder()
                .moduleName(moduleName)
                .moduleVersion(config.expectStringMember(MODULE_VERSION).getValue());
        config.getStringMember(SERVICE).map(StringNode::expectShapeId).ifPresent(builder::service);
        config.getStringMember(MODULE_DESCRIPTION).map(StringNode::getValue).ifPresent(builder::moduleDescription);
        config.getStringMember(SELECTOR).map(node -> Selector.parse(node.getValue())).ifPresent(builder::selector);
        config.getBooleanMember(GENERATE_INPUTS_AND_OUTPUTS)
                .map(BooleanNode::getValue)
                .ifPresent(builder::generateInputsAndOutputs);
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements SmithyBuilder<PythonTypeCodegenSettings> {

        private ShapeId service;
        private String moduleName;
        private String moduleVersion;
        private String moduleDescription;
        private Selector selector = Selector.IDENTITY;
        private Boolean generateInputsAndOutputs = false;

        @Override
        public PythonTypeCodegenSettings build() {
            SmithyBuilder.requiredState("moduleName", moduleName);
            SmithyBuilder.requiredState("moduleVersion", moduleVersion);
            return new PythonTypeCodegenSettings(this);
        }

        public Builder service(ShapeId service) {
            this.service = service;
            return this;
        }

        public Builder moduleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public Builder moduleVersion(String moduleVersion) {
            this.moduleVersion = moduleVersion;
            return this;
        }

        public Builder moduleDescription(String moduleDescription) {
            this.moduleDescription = moduleDescription;
            return this;
        }

        public Builder selector(Selector selector) {
            this.selector = selector == null ? Selector.IDENTITY : selector;
            return this;
        }

        public Builder generateInputsAndOutputs(boolean generateInputsAndOutputs) {
            this.generateInputsAndOutputs = generateInputsAndOutputs;
            return this;
        }
    }
}
```

```java
package software.amazon.smithy.python.codegen.types;

import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.TopologicalIndex;
import software.amazon.smithy.codegen.core.directed.CreateContextDirective;
import software.amazon.smithy.codegen.core.directed.CreateSymbolProviderDirective;
import software.amazon.smithy.codegen.core.directed.CustomizeDirective;
import software.amazon.smithy.codegen.core.directed.DirectedCodegen;
import software.amazon.smithy.codegen.core.directed.GenerateEnumDirective;
import software.amazon.smithy.codegen.core.directed.GenerateErrorDirective;
import software.amazon.smithy.codegen.core.directed.GenerateIntEnumDirective;
import software.amazon.smithy.codegen.core.directed.GenerateListDirective;
import software.amazon.smithy.codegen.core.directed.GenerateMapDirective;
import software.amazon.smithy.codegen.core.directed.GenerateServiceDirective;
import software.amazon.smithy.codegen.core.directed.GenerateStructureDirective;
import software.amazon.smithy.codegen.core.directed.GenerateUnionDirective;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.python.codegen.GenerationContext;
import software.amazon.smithy.python.codegen.PythonFormatter;
import software.amazon.smithy.python.codegen.PythonSettings;
import software.amazon.smithy.python.codegen.PythonSymbolProvider;
import software.amazon.smithy.python.codegen.SymbolProperties;
import software.amazon.smithy.python.codegen.generators.EnumGenerator;
import software.amazon.smithy.python.codegen.generators.InitGenerator;
import software.amazon.smithy.python.codegen.generators.IntEnumGenerator;
import software.amazon.smithy.python.codegen.generators.ListGenerator;
import software.amazon.smithy.python.codegen.generators.MapGenerator;
import software.amazon.smithy.python.codegen.generators.SchemaGenerator;
import software.amazon.smithy.python.codegen.generators.ServiceErrorGenerator;
import software.amazon.smithy.python.codegen.generators.StructureGenerator;
import software.amazon.smithy.python.codegen.generators.UnionGenerator;
import software.amazon.smithy.python.codegen.integrations.PythonIntegration;
import software.amazon.smithy.python.codegen.writer.PythonDelegator;

final class DirectedPythonTypeCodegen
        implements DirectedCodegen<GenerationContext, PythonSettings, PythonIntegration> {
    @Override
    public SymbolProvider createSymbolProvider(CreateSymbolProviderDirective<PythonSettings> directive) {
        return new PythonSymbolProvider(directive.model(), directive.settings());
    }

    @Override
    public GenerationContext createContext(CreateContextDirective<PythonSettings, PythonIntegration> directive) {
        return GenerationContext.builder()
                .model(directive.model())
                .settings(directive.settings())
                .symbolProvider(directive.symbolProvider())
                .fileManifest(directive.fileManifest())
                .writerDelegator(new PythonDelegator(
                        directive.fileManifest(),
                        directive.symbolProvider(),
                        directive.settings()))
                .integrations(directive.integrations())
                .build();
    }

    @Override
    public void customizeBeforeShapeGeneration(CustomizeDirective<GenerationContext, PythonSettings> directive) {
        new ServiceErrorGenerator(directive.settings(), directive.context().writerDelegator()).run();
        SchemaGenerator.generateAll(directive.context(), directive.connectedShapes().values(), shape -> {
            if (shape.isOperationShape() || shape.isServiceShape()) {
                return false;
            }
            if (shape.isStructureShape()) {
                return shouldGenerateStructure(directive.settings(), shape);
            }
            return true;
        });
    }

    @Override
    public void generateService(GenerateServiceDirective<GenerationContext, PythonSettings> directive) {}

    @Override
    public void generateStructure(GenerateStructureDirective<GenerationContext, PythonSettings> directive) {
        // If we're only generating data shapes, there's no need to generate input or output shapes.
        if (!shouldGenerateStructure(directive.settings(), directive.shape())) {
            return;
        }

        directive.context().writerDelegator().useShapeWriter(directive.shape(), writer -> {
            StructureGenerator generator = new StructureGenerator(
                    directive.context(),
                    writer,
                    directive.shape(),
                    TopologicalIndex.of(directive.model()).getRecursiveShapes());
            generator.run();
        });
    }

    private boolean shouldGenerateStructure(PythonSettings settings, Shape shape) {
        if (shape.getId().getNamespace().equals("smithy.synthetic")) {
            return false;
        }
        return !(settings.artifactType().equals(PythonSettings.ArtifactType.TYPES)
                && (shape.hasTrait(InputTrait.class) || shape.hasTrait(OutputTrait.class)));
    }

    @Override
    public void generateError(GenerateErrorDirective<GenerationContext, PythonSettings> directive) {
        directive.context().writerDelegator().useShapeWriter(directive.shape(), writer -> {
            StructureGenerator generator = new StructureGenerator(
                    directive.context(),
                    writer,
                    directive.shape(),
                    TopologicalIndex.of(directive.model()).getRecursiveShapes());
            generator.run();
        });
    }

    @Override
    public void generateUnion(GenerateUnionDirective<GenerationContext, PythonSettings> directive) {
        directive.context().writerDelegator().useShapeWriter(directive.shape(), writer -> {
            UnionGenerator generator = new UnionGenerator(
                    directive.context(),
                    writer,
                    directive.shape(),
                    TopologicalIndex.of(directive.model()).getRecursiveShapes());
            generator.run();
        });
    }

    @Override
    public void generateList(GenerateListDirective<GenerationContext, PythonSettings> directive) {
        var serSymbol = directive.context()
                .symbolProvider()
                .toSymbol(directive.shape())
                .expectProperty(SymbolProperties.SERIALIZER);
        var delegator = directive.context().writerDelegator();
        delegator.useFileWriter(serSymbol.getDefinitionFile(), serSymbol.getNamespace(), writer -> {
            new ListGenerator(directive.context(), writer, directive.shape()).run();
        });
    }

    @Override
    public void generateMap(GenerateMapDirective<GenerationContext, PythonSettings> directive) {
        var serSymbol = directive.context()
                .symbolProvider()
                .toSymbol(directive.shape())
                .expectProperty(SymbolProperties.SERIALIZER);
        var delegator = directive.context().writerDelegator();
        delegator.useFileWriter(serSymbol.getDefinitionFile(), serSymbol.getNamespace(), writer -> {
            new MapGenerator(directive.context(), writer, directive.shape()).run();
        });
    }

    @Override
    public void generateEnumShape(GenerateEnumDirective<GenerationContext, PythonSettings> directive) {
        if (!directive.shape().isEnumShape()) {
            return;
        }
        new EnumGenerator(directive.context(), directive.shape().asEnumShape().get()).run();
    }

    @Override
    public void generateIntEnumShape(GenerateIntEnumDirective<GenerationContext, PythonSettings> directive) {
        new IntEnumGenerator(directive).run();
    }

    @Override
    public void customizeBeforeIntegrations(CustomizeDirective<GenerationContext, PythonSettings> directive) {
        new InitGenerator(directive.context()).run();
        PythonIntegration.generatePluginFiles(directive.context());
    }

    @Override
    public void customizeAfterIntegrations(CustomizeDirective<GenerationContext, PythonSettings> directive) {
        new PythonFormatter(directive.context()).run();
    }
}
```

`software.amazon.smithy.openapi.fromsmithy.Smithy2OpenApi`

### Clients
- Java
- TypeScript
- Rust
- Scala
- Python
- Go
- Kotlin
- Swift
- Ruby

### Servers
- Java
- TypeScript
- Rust
- Scala


As I read, I began thinking of how one could use zio blocks( zio Schema)  with smithy. From smithy definitions straight to ZIO Schema.By doing so,one could leverage the power of ZIO schema codecs for formats like `json`,`protobuf`, `avro`, etc. This would allow for seamless serialization and deserialization of data defined in smithy models

it turns out there is an official library already doing this: `https://github.com/zio/zio-blocks/tree/main/smithy/src`

The `.types` Package: This package contains the actual data shapes defined in your Smithy model.  
The `.traits` Package: This package contains the metadata definitions (traits) that decorate your shapes. Traits in Smithy are used to add special behavior or constraints to shapes without changing their fundamental data type.

Everything in Smithy is a shape. There are 8 categories:

Simple shapes:
- blob
- boolean
- string
- byte
- short
- integer
- long
- float
- double
- bigInteger
- bigDecimal
- timestamp
- document

Aggregate shapes:  
- list
- map
- structure
- union

Service shapes:  
- service
- resource
- operation

A Smithy code generator is a JVM program (Java or Kotlin, usually) that:
- Receives a fully loaded, validated, post-projection-transform `Model` object.
- Walks the relevant shapes, deciding what to emit for each one.
- Maintains a symbol provider — a function from Smithy `ShapeId` to a language-specific `Symbol` (the type name, the file it lives in, the imports needed to reference it).
- Uses code writers to produce source files with proper indentation, import management, and template substitution.
- Plugs into the build via the `SmithyBuildPlugin` SPI, so it runs as a step in smithy build



| Resource | API Operations (all in EC2) |
|---|---|
| VPCs | `CreateVpc`, `DescribeVpcs`, `ModifyVpcAttribute`, `DeleteVpc` |
| VPC Peering | `CreateVpcPeeringConnection`, `AcceptVpcPeeringConnection`, `DescribeVpcPeeringConnections` |
| Subnets | `CreateSubnet`, `DescribeSubnets` |
| Route Tables | `CreateRouteTable`, `CreateRoute`, `AssociateRouteTable` |
| Security Groups | `CreateSecurityGroup`, `AuthorizeSecurityGroupIngress` |
| Internet/NAT Gateways | `CreateInternetGateway`, `CreateNatGateway` |
| VPC Endpoints (PrivateLink) | `CreateVpcEndpoint`, `DescribeVpcEndpoints` |
| Transit Gateway | `CreateTransitGateway`, `CreateTransitGatewayVpcAttachment` |


You register the plugin via Java's ServiceLoader. Create s`rc/main/resources/META-INF/services/software.amazon.smithy.build.SmithyBuildPlugin` containing one line: the fully-qualified class name of your plugin entry point.

```java
package com.example.smithy.markdown;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;

public final class MarkdownDocsPlugin implements SmithyBuildPlugin {
    @Override
    public String getName() {
        return "markdown-docs";
    }

    @Override
    public void execute(PluginContext context) {
        new MarkdownGenerator(context).run();
    }
}
```

The `PluginContext` gives you the model, the configured service shape, the output `FileManifest` (where you write your generated files), and the plugin's JSON config from `smithy-build.json`


A `SymbolProvider`. This maps each ShapeId to a Symbol representing its target-language identity — the type name, the namespace/module, and the dependencies needed to use it. You implement toSymbol(Shape) and Smithy's framework calls it whenever the generator needs to refer to a shape. Reserved-word handling, naming conventions (PascalCase vs snake_case), and disambiguation all live here.
A `SymbolWriter`. Smithy provides language-specific extensions (`TypeScriptWriter`, `RustWriter`, etc., from the official codegen libraries; or you subclass the generic `SymbolWriter` for a new language). It tracks imports automatically: when you write `$T` referencing a Symbol, the writer records the import and emits it at file top