ThisBuild / organization := "fr.cy"
ThisBuild / version := "0.2.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"

lazy val pekkoVersion = "1.3.0"
lazy val pekkoHttpVersion = "1.3.0"
lazy val logbackVersion = "1.5.18"

lazy val root = (project in file("."))
  .settings(
    name := "CYFlyBy",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-encoding",
      "utf8"
    ),
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
      "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion,
      "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
      "ch.qos.logback" % "logback-classic" % logbackVersion
    ),
    Compile / mainClass := Some("fr.cy.cyflyby.app.ServerMain"),
    Compile / run / fork := true
  )
