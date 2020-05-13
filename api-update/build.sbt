import Dependencies._
import sbt.Keys.fork

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"


lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .disablePlugins(AssemblyPlugin)
  .aggregate(
    common
  )

lazy val common = project
  .settings(
    name := "common",
    commonSettings,
    testSettings,
    libraryDependencies ++= commonDependencies
  )
  .disablePlugins(AssemblyPlugin)

lazy val antivirus = project
  .settings(
    name := "antivirus",
    commonSettings,
    assemblySettings,
    testSettings,
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(
    common % "test->test;compile->compile"
  )

lazy val testSettings = Seq(
  fork in Test := true,
  envVars in Test := Map("API_URL" -> "http://localhost:9001/graphql", "AUTH_URL" -> "http://localhost:9002/auth", "CLIENT_ID" -> "id", "CLIENT_SECRET" -> "secret")
)

lazy val assemblySettings = Seq(
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs@_*) => MergeStrategy.discard
    case _ => MergeStrategy.first
  },
)

lazy val commonSettings = Seq(
  resolvers += "TDR Releases" at "s3://tdr-releases-mgmt"
)

lazy val commonDependencies = Seq(
  circeCore,
  circeGeneric,
  circeParser,
  generatedGraphql,
  graphqlClient,
  authUtils,
  mockito % Test,
  wiremock % Test,
  scalaTest % Test,
  keycloakMock % Test
)

