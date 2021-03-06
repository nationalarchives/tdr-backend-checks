import Dependencies._
import sbt.Keys.fork

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "uk.gov.nationalarchives"
ThisBuild / organizationName := "api-update"


lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .disablePlugins(AssemblyPlugin)
  .aggregate(
    commonRef
  )

lazy val common = project
  .settings(
    name := "common",
    commonSettings,
    testSettings,
    libraryDependencies ++= commonDependencies
  )
  .disablePlugins(AssemblyPlugin)

lazy val commonRef = LocalProject("common")

lazy val antivirus = project
  .settings(
    name := "antivirus",
    commonSettings,
    assemblySettings,
    testSettings,
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(
    commonRef % "test->test;compile->compile"
  )
lazy val commonSrc = sourceDirectory.in(commonRef)

lazy val testSettings = Seq(
  fork in Test := true,
  javaOptions in Test += s"-Dconfig.file=${commonSrc.value}/test/resources/application.conf"
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
  lambdaCore,
  lambdaEvents,
  authUtils,
  sqs,
  typesafe,
  mockito % Test,
  wiremock % Test,
  scalaTest % Test,
  keycloakMock % Test,
  sqsMock % Test
)
