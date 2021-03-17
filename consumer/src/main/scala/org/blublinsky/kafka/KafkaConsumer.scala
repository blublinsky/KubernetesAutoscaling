package org.blublinsky.kafka

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
import Configuration._

object KafkaConsumer {

  def main(args: Array[String]): Unit = {

    println(s"Kafka consumer -  bootstrap servers : $bootstrapServers; topic : $topic; group id : $groupId; execution : $delay")
    val sleep = delay.toMillis           // Delay - 2 sec

    // Actor system
    implicit val actorSystem: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "kafka-producer")

    // Create topic, if does not exist
    KafkaSupport.createTopic()

    // configure Kafka consumer (1)
    val kafkaConsumerSettings = ConsumerSettings(actorSystem.toClassic, new ByteArrayDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId(groupId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withStopTimeout(0.seconds)

    // Read messages
    Consumer
      .sourceWithOffsetContext(kafkaConsumerSettings, Subscriptions.topics(topic))
      .map (record => CloudEventMap.parseFrom(new ByteArrayInputStream(record.value())))
      .map{event =>
        println(s"New event $event")
        Thread.sleep(sleep)
        event
      }
      .toMat(Sink.ignore)(Consumer.DrainingControl.apply).run()
  }
}