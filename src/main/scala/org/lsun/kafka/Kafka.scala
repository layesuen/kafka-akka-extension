package org.lsun.kafka

import akka.actor._
import akka.io.IO
import akka.routing.SmallestMailboxPool

protected class Kafka(system: ActorSystem) extends IO.Extension {

  private val config = system.settings.config.getConfig("kafka.producer")

  private val managerDispatcher = config.getString("manager-dispatcher")
  private val routeeProps = Props(classOf[KafkaProducerManager], config).withDispatcher(managerDispatcher)
  private val poolSize = config.getInt("pool-size")
  private val managerProps = SmallestMailboxPool(nrOfInstances = poolSize, routerDispatcher = managerDispatcher).props(routeeProps)
  private val managerActor = system.actorOf(managerProps)

  def manager = managerActor
}

object Kafka extends ExtensionId[Kafka] with ExtensionIdProvider {

  def lookup(): ExtensionId[_ <: Extension] = Kafka
  def createExtension(system: ExtendedActorSystem): Kafka = new Kafka(system)

  case class Message[K, V](topic: String, key: Array[Byte], message: Array[Byte])
}

