package org.lsun.kafka

import akka.actor._

/**
 * Kafka Consumer Akka Extension class
 * @param system
 */
protected class KafkaConsumerExtension(system: ActorSystem) extends Extension {

  private val config = system.settings.config.getConfig("kafka.consumer")

  private val managerProps = Props(classOf[KafkaConsumerManager], config)
    .withDispatcher(config.getString("manager-dispatcher"))
  private val manager = system.actorOf(managerProps)

  /**
   * Start Kafka consumer
   */
  def start(subscriptionMap: Map[String, (Int, Set[ActorRef])]): Unit = {
    manager ! KafkaConsumerManager.Start(subscriptionMap)
  }

  /**
   * Shutdown Kafka consumer
   */
  def shutdown(): Unit = {
    manager ! KafkaConsumerManager.Shutdown
  }
}

/**
 * Kafka Consumer Akka Extension companion object
 */
object KafkaConsumerExtension extends ExtensionId[KafkaConsumerExtension] with ExtensionIdProvider {

  def lookup(): ExtensionId[_ <: Extension] = KafkaConsumerExtension
  def createExtension(system: ExtendedActorSystem): KafkaConsumerExtension = new KafkaConsumerExtension(system)

}

