import SonatypeKeys._

sonatypeSettings

organization := "org.lsun"

profileName := "org.lsun"

version := "0.0.1"

scalaVersion := "2.10.4"

scalacOptions in Compile ++= Seq("-unchecked", "-feature", "-language:postfixOps", "-deprecation", "-encoding", "UTF-8")

parallelExecution in Test := false

name := "kafka-akka-extension"

libraryDependencies := Seq(
  "org.apache.kafka"      %%  "kafka"           % "0.8.2.0",
  "com.typesafe.akka"     %%  "akka-actor"      % "2.3.6",
  "com.typesafe"          %   "config"          % "1.2.1",
  "com.typesafe.akka"     %%  "akka-testkit"    % "2.3.6" % "test",
  "org.scalatest"         %%  "scalatest"       % "2.2.2" % "test",
  "org.mockito"           %   "mockito-core"    % "1.9.5" % "test"
)

pomExtra := {
  <url>https://github.com/layesuen/kafka-akka-extension</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:github.com/layesuen/kafka-akka-extension.git</connection>
      <developerConnection>scm:git:git@github.com:layesuen/kafka-akka-extension.git</developerConnection>
      <url>github.com/layesuen/kafka-akka-extension.git</url>
    </scm>
    <developers>
      <developer>
        <id>lei</id>
        <name>Lei Sun</name>
        <url>lsun.org</url>
      </developer>
    </developers>
}
