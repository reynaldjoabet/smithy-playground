import Dependencies.*

ThisBuild / scalaVersion := "3.9.0"
ThisBuild / version      := "0.1.0-SNAPSHOT"

ThisBuild / scalacOptions := Seq(
  "-encoding",
  "UTF-8",
  "-no-indent",
  "-deprecation",
  "-feature",
  "-unchecked",
  // "-Werror",
  // "-Wunused:all",
  "-Wvalue-discard",
  "-Wnonunit-statement",
  "-language:strictEquality",
  "-Xcheck-macros",
  "-Xmax-inlines:64"
)

Global / onChangedBuildSource := ReloadOnSourceChanges

val smithyVersion = "1.75.1"

val commonDependencies: Seq[ModuleID] = Seq(
  "com.fasterxml.jackson.core"    % "jackson-databind"     % "2.23.0",
  "com.fasterxml.jackson.core"    % "jackson-core"         % "2.23.0",
  "com.fasterxml.jackson.core"    % "jackson-annotations"  % "2.23",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.23.0",
  // Smithy core
  "software.amazon.smithy" % "smithy-model"         % smithyVersion,
  "software.amazon.smithy" % "smithy-codegen-core"  % smithyVersion,
  "software.amazon.smithy" % "smithy-trait-codegen" % smithyVersion,
  "software.amazon.smithy" % "smithy-build"         % smithyVersion,
  "software.amazon.smithy" % "smithy-utils"         % smithyVersion,
  "software.amazon.smithy" % "smithy-jmespath"      % smithyVersion,
  "software.amazon.smithy" % "smithy-diff"          % smithyVersion,
  "software.amazon.smithy" % "smithy-linters"       % smithyVersion,
  "software.amazon.smithy" % "smithy-syntax"        % smithyVersion,
  // Smithy traits / protocols
  "software.amazon.smithy" % "smithy-protocol-traits"      % smithyVersion,
  "software.amazon.smithy" % "smithy-protocol-test-traits" % smithyVersion,
  "software.amazon.smithy" % "smithy-validation-model"     % smithyVersion,
  "software.amazon.smithy" % "smithy-rules-engine"         % smithyVersion,
  "software.amazon.smithy" % "smithy-waiters"              % smithyVersion,
  "software.amazon.smithy" % "smithy-mqtt-traits"          % smithyVersion,
  "software.amazon.smithy" % "smithy-contract-traits"      % smithyVersion,
  // Smithy AWS traits (needed to load AWS service models)
  "software.amazon.smithy" % "smithy-aws-traits"                % smithyVersion,
  "software.amazon.smithy" % "smithy-aws-iam-traits"            % smithyVersion,
  "software.amazon.smithy" % "smithy-aws-cloudformation-traits" % smithyVersion,
  "software.amazon.smithy" % "smithy-aws-cloudformation"        % smithyVersion,
  "software.amazon.smithy" % "smithy-aws-endpoints"             % smithyVersion,
  "software.amazon.smithy" % "smithy-aws-protocol-tests"        % smithyVersion,
  "software.amazon.smithy" % "smithy-aws-smoke-test-model"      % smithyVersion,
  "software.amazon.smithy" % "smithy-aws-apigateway-traits"     % smithyVersion,
  // Smithy converters
  "software.amazon.smithy" % "smithy-jsonschema"             % smithyVersion,
  "software.amazon.smithy" % "smithy-openapi"                % smithyVersion,
  "software.amazon.smithy" % "smithy-openapi-traits"         % smithyVersion,
  "software.amazon.smithy" % "smithy-aws-apigateway-openapi" % smithyVersion,
  // Smithy documentation generator
  "software.amazon.smithy" % "smithy-docgen" % smithyVersion
)

val fory =
  Seq("org.apache.fory" %% "fory-json-scala" % "1.7.8", "org.apache.fory" %% "fory-scala" % "1.7.8")

val jackson = Seq(
  "tools.jackson.core"    % "jackson-databind"     % "3.3.0",
  "tools.jackson.module" %% "jackson-module-scala" % "3.3.0"
)

// ZIO Blocks schema and its format modules. JSON is built into zio-blocks-schema (zio.blocks.schema.json);
// there is no separate JSON or Protobuf module. Stay on 0.0.x: the "0.017" on Maven Central is a stray,
// mis-numbered release that sorts above 0.0.56, so sbt-updates will suggest it. Don't take it.
val zioBlocksVersion = "0.0.56"

val zioBlocks = Seq(
  "dev.zio" %% "zio-blocks-schema"             % zioBlocksVersion,
  "dev.zio" %% "zio-blocks-schema-avro"        % zioBlocksVersion,
  "dev.zio" %% "zio-blocks-schema-thrift"      % zioBlocksVersion,
  "dev.zio" %% "zio-blocks-schema-bson"        % zioBlocksVersion,
  "dev.zio" %% "zio-blocks-schema-messagepack" % zioBlocksVersion,
  "dev.zio" %% "zio-blocks-schema-csv"         % zioBlocksVersion,
  "dev.zio" %% "zio-blocks-schema-xml"         % zioBlocksVersion,
  "dev.zio" %% "zio-blocks-schema-yaml"        % zioBlocksVersion
)

lazy val root = rootProject
  .settings(
    name                 := "smithy-playground",
    libraryDependencies  += munit % Test,
    libraryDependencies ++= commonDependencies ++ jackson ++ fory ++ zioBlocks
  )

// ---------------------------------------------------------------------------------------------
// A Smithy client generator for Scala 3 (see docs/smithy/scala-client.md)
// ---------------------------------------------------------------------------------------------

// The runtime that generated clients run on: schemas, the JSON codec (Jackson 3), protocols,
// SigV4, the HTTP transport and the retry pipeline.
lazy val clientRuntime = project
  .in(file("modules/client-runtime"))
  .settings(
    name                 := "smithy-scala-client-runtime",
    libraryDependencies ++= Seq(
      "tools.jackson.core" % "jackson-core"     % "3.3.0",
      "tools.jackson.core" % "jackson-databind" % "3.3.0",
      // The format bridge in runtime/codecs: our shape codecs write a ZIO Blocks DynamicValue, and
      // these modules turn it into bytes. zio-blocks-schema brings ZIO Blocks' own JSON; Smithy's JSON
      // protocols stay on Jackson (JsonCodec).
      "dev.zio" %% "zio-blocks-schema"             % zioBlocksVersion,
      "dev.zio" %% "zio-blocks-chunk"              % zioBlocksVersion, // Chunk, in DynamicValueBridge
      "dev.zio" %% "zio-blocks-schema-messagepack" % zioBlocksVersion,
      "dev.zio" %% "zio-blocks-schema-avro"        % zioBlocksVersion,
      "dev.zio" %% "zio-blocks-schema-yaml"        % zioBlocksVersion,
      "dev.zio" %% "zio-blocks-schema-thrift"      % zioBlocksVersion,
      // AvroSchemaCodec: Avro for the shape itself, with an Avro schema made from our Schema.
      "org.apache.avro" % "avro" % "1.12.2",
      munit             % Test
    )
  )

// The generator: reads a Smithy model and writes Scala sources for one service. It also exports JSON and
// Avro schemas for event shapes (EventSchemas), which is why it depends on the runtime: the Avro schema
// is the runtime's own AvroSchemas mapping, so it can't drift from what AvroSchemaCodec writes.
lazy val clientCodegen = project
  .in(file("modules/client-codegen"))
  .dependsOn(clientRuntime)
  .settings(
    name                 := "smithy-scala-client-codegen",
    libraryDependencies ++= Seq(
      "software.amazon.smithy" % "smithy-model"           % smithyVersion,
      "software.amazon.smithy" % "smithy-jsonschema"      % smithyVersion,
      "software.amazon.smithy" % "smithy-build"           % smithyVersion,
      "software.amazon.smithy" % "smithy-aws-traits"      % smithyVersion,
      "software.amazon.smithy" % "smithy-aws-iam-traits"  % smithyVersion,
      "software.amazon.smithy" % "smithy-aws-endpoints"   % smithyVersion,
      "software.amazon.smithy" % "smithy-rules-engine"    % smithyVersion,
      "software.amazon.smithy" % "smithy-waiters"         % smithyVersion,
      "software.amazon.smithy" % "smithy-protocol-traits" % smithyVersion,
      munit                    % Test
    )
  )

// Generated SQS, DynamoDB and Weather clients (regenerate with scripts/generate-scala-clients.sh). Their codecs
// are derived with Hearth (the generator's default), so this depends on clientHearth; Mirror-derived copies in
// its tests check that both derivations agree.
lazy val clientExample = project
  .in(file("modules/client-example"))
  .dependsOn(clientRuntime, clientHearth, clientCodegen % Test)
  .settings(
    name := "smithy-scala-client-example",
    libraryDependencies ++= Seq(
      munit % Test,
      // EventSchemasSpec validates real JsonCodec output against the exported JSON Schema.
      "com.networknt" % "json-schema-validator" % "3.0.9" % Test
    )
  )

// The codec derivation generated code uses by default, built on Hearth (https://github.com/kubuszok/hearth).
// The runtime's own Mirror derivation stays available with the generator's --derivation=mirror.
val hearthVersion = "0.4.5"

lazy val clientHearth = project
  .in(file("modules/hearth"))
  .dependsOn(clientRuntime)
  .settings(
    name                 := "smithy-scala-client-hearth",
    libraryDependencies ++= Seq(
      "com.kubuszok" %% "hearth" % hearthVersion,
      // Cross Quotes: Expr.quote / Expr.splice keep the Quotes context right inside Hearth's builders.
      compilerPlugin("com.kubuszok" % "hearth-cross-quotes_3" % hearthVersion),
      munit % Test
    )
  )

// JMH benchmarks for the format codecs: Jackson's JsonCodec against the ZIO Blocks bridge.
// Run with: sbt --client "clientBench/Jmh/run -i 5 -wi 3 -f 1"
lazy val clientBench = project
  .in(file("modules/bench"))
  .dependsOn(clientExample)
  .enablePlugins(JmhPlugin)
  .settings(
    name           := "smithy-scala-client-bench",
    publish / skip := true
  )
