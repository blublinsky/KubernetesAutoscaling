package org.blublinsky.kafka

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.kafka._
import akka.kafka.scaladsl._
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import cloudevents.io.CloudEvent._
import org.apache.kafka.clients.consumer.ConsumerConfig

import java.io.ByteArrayInputStream
import scala.concurrent.duration._
import Configuration._
import akka.Done
import akka.kafka.scaladsl.Consumer.DrainingControl

import scala.concurrent.{ExecutionContext, Future}

object KafkaConsumer {

  def main(args: Array[String]): Unit = {

    println(s"Kafka consumer -  bootstrap servers : $bootstrapServers; topic : $topic; group id : $groupId; execution : $delay")
    val sleep = delay.toMillis           // Delay - 2 sec

    // Actor system
    implicit val actorSystem: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "kafka-producer")
    implicit val executionContext: ExecutionContext = actorSystem.executionContext

    val committerSettings = CommitterSettings(actorSystem)

    // Create topic, if does not exist
    KafkaSupport.createTopic()

    // configure Kafka consumer (1)
    val kafkaConsumerSettings = ConsumerSettings(actorSystem.toClassic, new ByteArrayDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId(groupId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withStopTimeout(0.seconds)

    // Message processor
    def processor(key: Array[Byte], value: Array[Byte]): Future[Done] = {
      val event = CloudEventMap.parseFrom(new ByteArrayInputStream(value))
      println(s"New event $event")
      Thread.sleep(sleep)
      Future.successful(Done)
    }
    // Read messages
    Consumer
      .committableSource(kafkaConsumerSettings, Subscriptions.topics(topic))
      .mapAsync(1) { msg =>
        processor(msg.record.key, msg.record.value)
          .map(_ => msg.committableOffset)
      }
      .toMat(Committer.sink(committerSettings))(DrainingControl.apply)
      .run()
  }
}