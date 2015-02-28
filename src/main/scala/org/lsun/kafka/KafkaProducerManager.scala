package org.lsun.kafka

import java.util.Properties

import akka.actor.Actor
import com.typesafe.config.Config
import kafka.producer.{ProducerConfig, KeyedMessage, Producer}

class KafkaProducerManager(config: Config) extends Actor {

  private val kafkaProducerConfig = {
    val props = new Properties()
    props.setProperty("metadata.broker.list", config.getString("metadata-broker-list"))
    props.setProperty("request.required-acks", config.getInt("request-required-acks").toString)
    props.setProperty("producer.type", config.getString("producer-type"))
    new ProducerConfig(props)
  }

  val producer = new Producer[Array[Byte], Array[Byte]](kafkaProducerConfig)

  def receive = {
    case Kafka.Message(topic, key, message) =>
      producer.send(new KeyedMessage(topic, key, message))
  }
}

