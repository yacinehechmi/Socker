ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.0"

lazy val root = (project in file("."))
  .settings(name := "DockerApi")

val scalaTestVersion = "3.2.18"

libraryDependencies ++= Seq(
  // testing
  "org.scalatest" %% "scalatest" % scalaTestVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
  // parsing
  "com.lihaoyi" %% "upickle" % "3.1.3",
  // unixSocket
  "com.github.jnr" % "jnr-unixsocket" % "0.38.21",
)
