import sbt._

object Dependencies {
  lazy val sqsSdk = "software.amazon.awssdk" % "sqs" % "2.13.15"
  lazy val s3Sdk = "software.amazon.awssdk" % "s3" % "2.13.18"
  lazy val lambdaJavaCore = "com.amazonaws" % "aws-lambda-java-core" % "1.2.1"
  lazy val lambdaJavaEvents = "com.amazonaws" % "aws-lambda-java-events" % "3.1.0"
  lazy val generatedGraphql = "uk.gov.nationalarchives" %% "tdr-generated-graphql" % "0.0.49"
  lazy val typesafe = "com.typesafe" % "config" % "1.4.0"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1"
  lazy val sqsMock = "io.findify" %% "sqsmock" % "0.3.5"
  lazy val s3Mock = "io.findify" %% "s3mock" % "0.2.6"
  lazy val mockito = "org.mockito" %% "mockito-scala" % "1.14.1"
  lazy val circeCore = "io.circe" %% "circe-core" % "0.13.0"
  lazy val circeGeneric = "io.circe" %% "circe-generic" % "0.13.0"
  lazy val circeParser = "io.circe" %% "circe-parser" % "0.13.0"
}
