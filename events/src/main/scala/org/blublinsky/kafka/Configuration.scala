package org.blublinsky.kafka

import com.typesafe.config._
import net.ceedubs.ficus.Ficus._
import scala.concurrent.duration._

object Configuration {
  val config: Config = ConfigFactory.load()                                 // standard Typesafe Config

  val bootstrapServers: String = config.as[String]("kafka.bootstrapServers")  // Bootstrap server
  val topic: String = config.as[String]("kafka.topic")                        // Topic
  val partitions: Int = config.getOrElse[Int]("kafka.partitions", 3)  // Partitions
  val groupId: String = config.getOrElse[String]("kafka.groupId", "cloudevents")  // Group ID

  val frequency: FiniteDuration = config.getOrElse[FiniteDuration]("kafka.frequency", 500.millisecond) // Message emit frequency
  val delay: FiniteDuration = config.getOrElse[FiniteDuration]("kafka.delay", 1.second) // Processing time
}
