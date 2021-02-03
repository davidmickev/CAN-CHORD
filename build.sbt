import sbt.project

name := "akka-quickstart-scala"
version := "1.0"
scalaVersion := "2.13.1"

lazy val akkaVersion = "2.6.10"
lazy val akkaHttpVersion = "10.2.1"

// define main class which is Can.Driver but what calls driver is Can.Simulation
mainClass in (Compile, run) := Some("CAN.Simulation")

assemblyMergeStrategy in assembly := {
  x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}


lazy val app = (project in file("app")).
  settings(
    mainClass in assembly := Some("CAN.Simulation"),
    // more settings here ...
  )

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.2" % "test",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.iheart" %% "ficus" % "1.5.0"
)
