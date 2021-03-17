import sbt._
import sbt.Keys._
import Dependencies._

name := "KubernetesAutoscaling"

lazy val thisVersion = "0.1"
organization in ThisBuild := "blublinsky1"
version in ThisBuild := thisVersion
scalaVersion in ThisBuild := "2.12.13"

// settings for a native-packager based docker project based on sbt-docker plugin
def sbtdockerAppBase(id: String)(base: String = id): Project = Project(id, base = file(base))
  .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
  .settings(
    dockerfile in docker := {
      val appDir = stage.value
      val targetDir = "/opt/app"

      new Dockerfile {
        from("adoptopenjdk/openjdk15:alpine")
        run("apk", "add", "--no-cache", "bash")
        copy(appDir, targetDir)
        run("chmod", "-R", "777", "/opt/app")
        entryPoint(s"$targetDir/bin/${executableScriptName.value}")
      }
    },

    // Set name for the image
    imageNames in docker := Seq(
      ImageName(namespace = Some(organization.value),
        repository = name.value.toLowerCase,
        tag = Some(version.value))
    ),
    buildOptions in docker := BuildOptions(cache = false)
  )

lazy val CloudEvents = (project in file("./events"))
  .settings(
    libraryDependencies ++= Seq(typesafeConfig, ficus, kafka),
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    )
  )

lazy val KafkaProducer = sbtdockerAppBase("KafkaProducer")("./producer")
  .settings(
    libraryDependencies ++= Seq(akkaStreams, actors, slf4j, logback),
    mainClass in Compile := Some("org.blublinsky.kafka.KafkaProducer")
  )
  .dependsOn(CloudEvents)

lazy val KafkaConsumer = sbtdockerAppBase("KafkaConsumer")("./consumer")
  .settings(
    libraryDependencies ++= Seq(akkaStreams, actors, slf4j, logback),
    mainClass in Compile := Some("org.blublinsky.kafka.KafkaConsumer")
  )
  .dependsOn(CloudEvents)

