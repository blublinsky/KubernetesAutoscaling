import sbt._
import sbt.Keys._
import Dependencies._

name := "KubernetesAutoscaling"

lazy val thisVersion = "0.1"
version in ThisBuild := thisVersion
scalaVersion in ThisBuild := "2.12.13"

lazy val CloudEvents = (project in file("./events"))
  .settings(
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    )
  )

lazy val KafkaProducer = (project in file("./producer"))
  .settings(
    libraryDependencies ++= Seq(kafka, akkaStreams, actors, slf4j, logback)
  )
  .dependsOn(CloudEvents)

lazy val KafkaConsumer = (project in file("./consumer"))
  .settings(
    libraryDependencies ++= Seq(kafka, akkaStreams, actors, slf4j, logback)
  )
  .dependsOn(CloudEvents)

