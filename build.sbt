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

val smithyVersion = "1.74.0"

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

lazy val root = rootProject
  .settings(
    name                 := "smithy-playground",
    libraryDependencies  += munit      % Test,
    libraryDependencies  += "dev.zio" %% "zio-blocks-smithy" % "0.0.56",
    libraryDependencies ++= commonDependencies ++ jackson ++ fory
  )

// ---------------------------------------------------------------------------------------------
// A Smithy client generator for Scala 3 (see docs/smithy/scala-client.md)
// ---------------------------------------------------------------------------------------------

// The runtime that generated clients run on: schemas, the JSON codec (Jackson 3), protocols,
// SigV4, the HTTP transport and the retry pipeline.
lazy val clientRuntime = project
  .in(file("modules/client-runtime"))
  .settings(
    name                := "smithy-scala-client-runtime",
    libraryDependencies ++= Seq(
      "tools.jackson.core" % "jackson-core"     % "3.3.0",
      "tools.jackson.core" % "jackson-databind" % "3.3.0",
      munit                % Test
    )
  )

// The generator: reads a Smithy model and writes Scala sources for one service.
lazy val clientCodegen = project
  .in(file("modules/client-codegen"))
  .settings(
    name                := "smithy-scala-client-codegen",
    libraryDependencies ++= Seq(
      "software.amazon.smithy" % "smithy-model"           % smithyVersion,
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

// Generated SQS and DynamoDB clients (regenerate with scripts/generate-scala-clients.sh).
lazy val clientExample = project
  .in(file("modules/client-example"))
  .dependsOn(clientRuntime)
  .settings(
    name                := "smithy-scala-client-example",
    libraryDependencies += munit % Test
  )
