ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.0"

lazy val root = (project in file("."))
  .settings(name := "DockerApi")

val AkkaVersion = "2.6.21"

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "upickle" % "3.1.3",
  "com.github.jnr" % "jnr-unixsocket" % "0.38.21",
  "com.lihaoyi" %% "requests" % "0.8.0",
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test
)

