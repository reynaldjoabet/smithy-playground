import Dependencies.*

scalaVersion := "3.3.7"
version := "0.1.0-SNAPSHOT"

lazy val root = rootProject
  .settings(
    name := "smithy-playground",
    libraryDependencies += munit % Test
  )

lazy val `smithy-payments-api` = (project in file("smithy-payments-api"))
  .settings(
    name := "smithy-payments-api",
    libraryDependencies ++= Seq(
    )
  )

lazy val `smithy-payments-impl` = (project in file("smithy-payments-impl"))
  .settings(
    name := "smithy-payments-impl",
    libraryDependencies ++= Seq(
    )
  )
  .dependsOn(`smithy-payments-api`)

lazy val `smithy-openapi-playground` =
  (project in file("smithy-openapi-playground"))
    .settings(
      name := "smithy-openapi-playground",
      libraryDependencies ++= Seq(
      )
    )

lazy val `smithy-identity-api` = (project in file("smithy-identity-api"))
  .settings(
    name := "smithy-identity-api",
    libraryDependencies ++= Seq(
    )
  )

lazy val `smithy-ledger-api` = (project in file("smithy-ledger-api"))
  .settings(
    name := "smithy-ledger-api",
    libraryDependencies ++= Seq(
    )
  )
// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
