import Versions._
import sbt._

object Dependencies {
  val kafka         = "com.typesafe.akka"     %% "akka-stream-kafka"      % AlpakkaKafkaVersion
  val actors        = "com.typesafe.akka"     %% "akka-actor-typed"       % AkkaVersion
  val akkaStreams   = "com.typesafe.akka"     %% "akka-stream"            % AkkaVersion
  val slf4j         = "com.typesafe.akka"     %% "akka-slf4j"             % AkkaVersion
  val logback       = "ch.qos.logback"        % "logback-classic"         % LogbackVersion

  val typesafeConfig  = "com.typesafe"        %  "config"                 % TypesafeConfigVersion
  val ficus           = "com.iheart"          %% "ficus"                  % FicusVersion
}
