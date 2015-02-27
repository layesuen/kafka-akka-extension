import sbt._
import Keys._

object KafkaAkkaExtensionBuild extends Build {
  lazy val rootSettings: Seq[Setting[_]] = Seq(
    organization := "org.lsun",
    version := "SNAPSHOT",
    scalaVersion := "2.10.4",
    scalacOptions in Compile ++= Seq("-unchecked", "-feature", "-language:postfixOps", "-deprecation", "-encoding", "UTF-8"),
    parallelExecution in Test := false
  )

  lazy val projSettings = rootSettings ++ Seq(
    name := "kafka-akka-extension",
    libraryDependencies := Seq(
      "org.apache.kafka"      %%  "kafka"           % "0.8.2.0",
      "com.typesafe.akka"     %%  "akka-actor"      % "2.3.6",
      "com.typesafe"          %   "config"          % "1.2.1",
      "com.typesafe.akka"     %%  "akka-testkit"    % "2.3.6" % "test",
      "org.scalatest"         %%  "scalatest"       % "2.2.2" % "test",
      "org.mockito"           %   "mockito-core"    % "1.9.5" % "test"
    )
  )

  lazy val root = Project("kafka-akka-extension", file("."))
    .settings(projSettings: _*)
}
