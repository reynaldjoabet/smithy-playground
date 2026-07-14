import Dependencies.*

scalaVersion := "3.3.8"
version := "0.1.0-SNAPSHOT"

ThisBuild / javacOptions ++= Seq("--release", "17")

val smithyVersion = "1.72.0"

val commonDependencies: Seq[ModuleID] = Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.22.1",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.22.1",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.22",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.22.1",
  // Smithy core
  "software.amazon.smithy" % "smithy-model" % smithyVersion,
  "software.amazon.smithy" % "smithy-codegen-core" % smithyVersion,
  "software.amazon.smithy" % "smithy-build" % smithyVersion,
  "software.amazon.smithy" % "smithy-utils" % smithyVersion,
  "software.amazon.smithy" % "smithy-diff" % smithyVersion,
  "software.amazon.smithy" % "smithy-linters" % smithyVersion,
  // Smithy traits / protocols
  "software.amazon.smithy" % "smithy-protocol-traits" % smithyVersion,
  "software.amazon.smithy" % "smithy-protocol-test-traits" % smithyVersion,
  "software.amazon.smithy" % "smithy-validation-model" % smithyVersion,
  "software.amazon.smithy" % "smithy-rules-engine" % smithyVersion,
  "software.amazon.smithy" % "smithy-waiters" % smithyVersion,
  // Smithy AWS traits (needed to load AWS service models)
  "software.amazon.smithy" % "smithy-aws-traits" % smithyVersion,
  "software.amazon.smithy" % "smithy-aws-iam-traits" % smithyVersion,
  "software.amazon.smithy" % "smithy-aws-cloudformation-traits" % smithyVersion,
  "software.amazon.smithy" % "smithy-aws-endpoints" % smithyVersion,
  "software.amazon.smithy" % "smithy-aws-protocol-tests" % smithyVersion,
  // Smithy converters
  "software.amazon.smithy" % "smithy-jsonschema" % smithyVersion,
  "software.amazon.smithy" % "smithy-openapi" % smithyVersion
)

lazy val root = rootProject
  .settings(
    name := "smithy-playground",
    libraryDependencies += munit % Test,
    libraryDependencies ++= commonDependencies
  )
