name := "bitwatts-core"

// App
libraryDependencies ++= Seq(
  "org.powerapi" % "powerapi-core_2.11" % "3.1",
  "org.apache.thrift" % "libthrift" % "0.9.2",
  "org.zeromq" % "jeromq" % "0.3.4"
)

// Tests
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-testkit" % "2.3.6" % "test",
  "org.scalatest" %% "scalatest" % "2.2.2" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test"
)
