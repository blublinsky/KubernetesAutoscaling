package org.blublinsky.kafka

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.kafka._
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.Sink
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import cloudevents.io.CloudEvent._
import org.apache.kafka.clients.consumer.ConsumerConfig

import java.io.ByteArrayInputStream
import scala.concurrent.duration._

object KafkaConsumer {

  private val bootstrapServers = ""               // Bootstrap server
  private val topic = "cloudevents"               // Topic
  private val groupId = "cloudevents"             // group id

  private val sleep = 2.second.toMillis           // Delay - 2 sec

  def main(args: Array[String]): Unit = {

    // Actor system
    implicit val actorSystem: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "kafka-producer")

    // configure Kafka consumer (1)
    val kafkaConsumerSettings = ConsumerSettings(actorSystem.toClassic, new ByteArrayDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId(groupId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withStopTimeout(0.seconds)

    val control: Consumer.DrainingControl[Done] = Consumer
      .sourceWithOffsetContext(kafkaConsumerSettings, Subscriptions.topics(topic))
      .map { consumerRecord => CloudEventMap.parseFrom(new ByteArrayInputStream(consumerRecord.value())) }
      .map{event =>
        println(s"New event $event")
        Thread.sleep(sleep)
        event
      }
      .toMat(Sink.ignore)(Consumer.DrainingControl.apply).run()
  }
}