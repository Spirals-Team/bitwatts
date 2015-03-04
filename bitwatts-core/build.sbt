name := "bitwatts-core"

// App
libraryDependencies ++= Seq(
  "org.powerapi" % "powerapi-core_2.11" % "3.0"
)

// Tests
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-testkit" % "2.3.6" % "test",
  "org.scalatest" %% "scalatest" % "2.2.2" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test"
)
