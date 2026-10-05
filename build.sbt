import Dependencies.*

ThisBuild / scalaVersion := "3.9.0"
ThisBuild / version      := "0.1.0-SNAPSHOT"

ThisBuild / crossScalaVersions := Seq("3.3.8", "3.9.0")

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
  "com.fasterxml.jackson.core"    % "jackson-databind"     % "2.22.3",
  "com.fasterxml.jackson.core"    % "jackson-core"         % "2.22.3",
  "com.fasterxml.jackson.core"    % "jackson-annotations"  % "2.22",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.22.3.1",
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
  "software.amazon.smithy" % "smithy-docgen" % smithyVersion,
  // BOM: POM-only, carries no classpath entries of its own
  ("software.amazon.smithy" % "smithy-bom" % smithyVersion).pomOnly()
)

val fory =
  Seq("org.apache.fory" %% "fory-json-scala" % "1.7.6", "org.apache.fory" %% "fory-scala" % "1.7.6")

val jackson = Seq(
  "tools.jackson.core"    % "jackson-databind"     % "3.2.3",
  "tools.jackson.module" %% "jackson-module-scala" % "3.2.3"
)

lazy val root = rootProject
  .settings(
    name                 := "smithy-playground",
    libraryDependencies  += munit % Test,
    libraryDependencies ++= commonDependencies ++ jackson ++ fory
  )
