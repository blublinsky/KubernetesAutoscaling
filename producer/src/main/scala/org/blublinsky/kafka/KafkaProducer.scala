package org.blublinsky.kafka

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.Source
import akka.actor.typed.scaladsl.adapter._
import akka.kafka.scaladsl.Producer
import akka.kafka.ProducerSettings
import org.apache.kafka.common.serialization.ByteArraySerializer
import cloudevents.io.CloudEvent._
import org.apache.kafka.clients.producer.ProducerRecord

import java.io.ByteArrayOutputStream
import java.time.ZonedDateTime
import java.util.UUID
import Configuration._

object KafkaProducer {

  private var value: Long = -1

  def main(args: Array[String]): Unit = {

    println(s"Kafka producer -  bootstrap servers : $bootstrapServers; topic : $topic; frequency : $frequency")
    // Actor system
    implicit val actorSystem: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "kafka-producer")

    // Create topic, if does not exist
    KafkaSupport.createTopic()

    // Kafka setting
    val kafkaProducerSettings = ProducerSettings(actorSystem.toClassic, new ByteArraySerializer, new ByteArraySerializer)
      .withBootstrapServers(bootstrapServers)

    // New message creation
    def nextRecord(): Array[Byte] = {
      value = value + 1
      // Create cloud event
      val event = CloudEventMap(Map(
        "id" -> CloudEventAny().withStringValue(UUID.randomUUID().toString),
        "type" -> CloudEventAny().withStringValue("org.blublinsky.kafka.event"),
        "specversion" -> CloudEventAny().withStringValue("0.2"),
        "time" -> CloudEventAny().withStringValue(ZonedDateTime.now().toString),
        "source" -> CloudEventAny().withStringValue("org.blublinsky.kafka.producer"),
        "datacontenttype" -> CloudEventAny().withStringValue("text/plain"),
        "dataschema" -> CloudEventAny().withStringValue("https://knative.dev/cloudevents/V2"),
        "subject"  -> CloudEventAny().withStringValue("important message"),
        "data" ->CloudEventAny().withStringValue(value.toString)
      ))
      val byteArray = new ByteArrayOutputStream()
      event.writeTo(byteArray)
      byteArray.toByteArray
    }

    // Make source - create a new record with a given frequence
    def makeSource(): Source[Array[Byte], NotUsed] = {
      Source.repeat(NotUsed)
        .map(_ ⇒ nextRecord())
        .throttle(1, frequency)
    }

    // Publish to Kafka
    makeSource().map { record => new ProducerRecord[Array[Byte], Array[Byte]](topic, record)}
      .runWith(Producer.plainSink(kafkaProducerSettings))
  }
}