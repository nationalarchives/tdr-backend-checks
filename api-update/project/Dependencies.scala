import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1"
  lazy val circeCore = "io.circe" %% "circe-core" % "0.13.0"
  lazy val circeGeneric = "io.circe" %% "circe-generic" % "0.13.0"
  lazy val circeParser = "io.circe" %% "circe-parser" % "0.13.0"
  lazy val generatedGraphql = "uk.gov.nationalarchives" %% "tdr-generated-graphql" % "0.0.45-SNAPSHOT"
  lazy val graphqlClient = "uk.gov.nationalarchives" %% "tdr-graphql-client" % "0.0.12"
  lazy val authUtils = "uk.gov.nationalarchives" %% "tdr-auth-utils" % "0.0.13-SNAPSHOT"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock-jre8" % "2.26.0"
  lazy val keycloakMock = "com.tngtech.keycloakmock" % "mock" % "0.3.0"
  lazy val mockito = "org.mockito" %% "mockito-scala" % "1.14.1"
}
