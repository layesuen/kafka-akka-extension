package org.lsun.kafka

import akka.actor._
import akka.io.IO

protected class KafkaConsumer(system: ActorSystem) extends IO.Extension {

  private val config = system.settings.config.getConfig("kafka.consumer")
  private val managerProps = Props(classOf[KafkaConsumerManager], config)
    .withDispatcher(config.getString("manager-dispatcher"))
  private val managerActor = system.actorOf(managerProps)

  def manager = managerActor
}

object KafkaConsumer extends ExtensionId[KafkaConsumer] with ExtensionIdProvider {

  def lookup(): ExtensionId[_ <: Extension] = KafkaConsumer
  def createExtension(system: ExtendedActorSystem): KafkaConsumer = new KafkaConsumer(system)

  case class StartConsumer(consumerSettings: ConsumerSettings)
  case object StopConsumer

  class ConsumerSettings {
    def settings: Map[String, (Int, Set[ActorRef])] = Map()
    def withTopic(topicName: String, numStreams: Int): ConsumerSettings = {
      val oldSettings = settings
      val topicDetails = (numStreams, settings.get(topicName).map(_._2).getOrElse(Set()))
      new ConsumerSettings {
        override def settings = oldSettings + (topicName -> topicDetails)
      }
    }
    def withConsumerActor(topicName: String, consumerActor: ActorRef): ConsumerSettings = {
      val oldSettings = settings
      val oldTopicDetails = settings.get(topicName).getOrElse(throw new RuntimeException(s"topic $topicName is not defined"))
      val topicDetails = (oldTopicDetails._1, oldTopicDetails._2 + consumerActor)
      new ConsumerSettings {
        override def settings = oldSettings + (topicName -> topicDetails)
      }
    }
  }

  object ConsumerSettings extends ConsumerSettings
}
