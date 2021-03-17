package org.blublinsky.kafka

import java.util.Properties
import Configuration._
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import java.util.Arrays
import java.util.concurrent.ExecutionException

import collection.JavaConverters._

object KafkaSupport {

  private val properties = new Properties()
  properties.put("bootstrap.servers", bootstrapServers)
  private val client = AdminClient.create(properties)

  def createTopic() : Unit = {

    if (client.listTopics().names().get().asScala.filter(_ == topic).isEmpty) {
      println(s"Topic $topic does not exist - creating")
      internalCreateTopic()
    }
    else{
      val description = client.describeTopics(Arrays.asList(topic)).values().get(topic).get()
      if(description.partitions().size() < partitions){
        // Not enough partitions
        client.deleteTopics(Arrays.asList(topic))
        println(s"Topic $topic does not have enough partitions - recreating")
        internalCreateTopic()
      }
    }
  }

  private def internalCreateTopic() : Unit = {
    val result = client.createTopics(Arrays.asList(
      new NewTopic(topic, partitions, 1.toShort)))
    try result.all.get
    catch {
      case e@(_: InterruptedException | _: ExecutionException) =>
        throw new IllegalStateException(e)
    }
  }
}