package org.lsun.kafka

import java.util.Properties

import akka.actor.{Props, ActorRef, Actor}
import com.typesafe.config.Config
import kafka.consumer.{Consumer, ConsumerConnector, ConsumerConfig}

/**
 * Consumer manager actor (used by the extension)
 */
protected class KafkaConsumerManager(config: Config) extends Actor {

  import KafkaConsumerManager._

  private val kafkaConsumerConfig = {
    val props = new Properties()
    props.put("group.id", config.getString(s"group-id"))
    props.put("zookeeper.connect", config.getString(s"zookeeper-connect"))
    props.put("zookeeper.session.timeout.ms", config.getInt(s"zookeeper-session-timeout-ms").toString)
    props.put("zookeeper.sync.time.ms", config.getInt(s"zookeeper-sync-time-ms").toString)
    props.put("auto.commit.interval.ms", config.getInt(s"auto-commit-interval-ms").toString)
    props.put("auto.commit.enabled", config.getBoolean(s"auto-commit-enabled").toString)
    new ConsumerConfig(props)
  }

  private val maximumBackLog = config.getInt("maximum-backlog")

  private var kafkaConsumer: ConsumerConnector = _
  private var streamReaderActors: Map[String, Seq[ActorRef]] = _

  private val idle: Receive = {
    case Start(subscriptionMap) =>
      // create consumer connector
      kafkaConsumer = Consumer.create(kafkaConsumerConfig)
      // create streams
      val topicCountMap = subscriptionMap.map { case (topic, x) => topic -> x._1 }
      val kafkaConsumerStreams = kafkaConsumer.createMessageStreams(topicCountMap).toMap.map { case (topic, xs) => topic -> xs.toSeq }
      // create stream reader actors
      streamReaderActors = kafkaConsumerStreams.map {
        case (topic, streams) =>
          val consumers = streams.zipWithIndex.map {
            case (stream, i) =>
              val props = Props(
                classOf[KafkaStreamReader],
                stream,
                config.getString("io-dispatcher"),
                if (maximumBackLog <= 0) None else Some(maximumBackLog)
              ).withDispatcher(config.getString("manager-dispatcher"))
              context.system.actorOf(props, s"consumer-$topic-$i")
          }
          topic -> consumers
      }
      // add subscriber to stream readers and start stream readers
      streamReaderActors.foreach {
        case (topic, actors) =>
          actors.foreach(actor => subscriptionMap(topic)._2.foreach(actor ! KafkaStreamReader.AddSubscriber(_)))
          actors.foreach(_ ! KafkaStreamReader.Start)
      }
      // become started
      context.become(started)
  }

  private val started: Receive = {
    case Shutdown =>
      // shutdown consumer
      kafkaConsumer.shutdown()
      // stop all actors
      context.children.foreach(context.stop(_))
      // back to idle
      context.become(idle)
  }

  def receive = idle
}

/**
 * Companion object of consumer manager actor
 */
protected object KafkaConsumerManager {
  case class Start(subscriptionMap: Map[String, (Int, Set[ActorRef])])
  case object Shutdown
}
