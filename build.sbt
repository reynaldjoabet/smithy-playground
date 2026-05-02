import Dependencies.*

scalaVersion := "3.3.7"
version := "0.1.0-SNAPSHOT"

ThisBuild / javacOptions ++= Seq("--release", "17", "--release", "21")

val smithyVersion = "1.58.0"

val commonDependencies: Seq[ModuleID] = Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.17.1",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.17.1",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.17.1",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.17.1",
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
  .aggregate(
    `smithy-payments-api`,
    `smithy-payments-impl`,
    `smithy-openapi-playground`,
    `smithy-identity-api`,
    `smithy-ledger-api`
  )
  .settings(
    name := "smithy-playground",
    libraryDependencies += munit % Test,
    libraryDependencies ++= commonDependencies
  )

lazy val `smithy-payments-api` = (project in file("smithy-payments-api"))
  .settings(
    name := "smithy-payments-api",
    libraryDependencies ++= commonDependencies
  )

lazy val `smithy-payments-impl` = (project in file("smithy-payments-impl"))
  .settings(
    name := "smithy-payments-impl",
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(`smithy-payments-api`)

lazy val `smithy-openapi-playground` =
  (project in file("smithy-openapi-playground"))
    .settings(
      name := "smithy-openapi-playground",
      libraryDependencies ++= commonDependencies
    )

lazy val `smithy-identity-api` = (project in file("smithy-identity-api"))
  .settings(
    name := "smithy-identity-api",
    libraryDependencies ++= commonDependencies
  )

lazy val `smithy-ledger-api` = (project in file("smithy-ledger-api"))
  .settings(
    name := "smithy-ledger-api",
    libraryDependencies ++= commonDependencies
  )
// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
