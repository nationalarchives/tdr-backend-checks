name := "awsLambda"

version := "0.1"

scalaVersion := "2.13.2"

libraryDependencies += "software.amazon.awssdk" % "sqs" % "2.13.15"

libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.13.3"

libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.13.3"

libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.13.3"

libraryDependencies += "org.apache.logging.log4j" % "log4j-1.2-api" % "2.13.3"

libraryDependencies += "software.amazon.awssdk" % "sts" % "2.13.16"

libraryDependencies += "software.amazon.awssdk" % "s3" % "2.13.18"

libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.2.1"
libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "3.1.0"

libraryDependencies += "uk.gov.nationalarchives" %% "tdr-generated-graphql" % "0.0.49"

libraryDependencies += "com.typesafe" % "config" % "1.4.0"

libraryDependencies += "io.circe" %% "circe-core" % "0.13.0"
libraryDependencies += "io.circe" %% "circe-generic" % "0.13.0"
libraryDependencies += "io.circe" %% "circe-parser" % "0.13.0"