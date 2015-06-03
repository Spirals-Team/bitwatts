name := "bitwatts"

version in ThisBuild := "1.1"

scalaVersion in ThisBuild := "2.11.6"

scalacOptions in ThisBuild ++= Seq(
  "-language:reflectiveCalls",
  "-language:implicitConversions",
  "-feature",
  "-deprecation"
)

// Logging
libraryDependencies in ThisBuild ++= Seq(
  "org.apache.logging.log4j" % "log4j-api" % "2.3",
  "org.apache.logging.log4j" % "log4j-core" % "2.3"
)

parallelExecution in (ThisBuild, Test) := false

resolvers in ThisBuild ++= Seq(
  "JBoss Thirdparty Uploads" at "https://repository.jboss.org/nexus/content/repositories/thirdparty-uploads/"
)
