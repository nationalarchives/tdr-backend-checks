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