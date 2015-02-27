package org.lsun.kafka

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import kafka.consumer.{ConsumerTimeoutException, KafkaStream}
import kafka.message.MessageAndMetadata

import scala.util.{Failure, Success, Try}

protected class KafkaStreamReader(stream: KafkaStream[Array[Byte], Array[Byte]],
                                  kafkaIODispatcher: String,
                                  maxUnacknowledgedMessage: Option[Int] = None)

  extends Actor
  with ActorLogging {

  import org.lsun.kafka.KafkaStreamReader._

  private var unacknowledged = Set[Received]()
  private val poller = context.actorOf(Props(classOf[Poller], stream).withDispatcher(kafkaIODispatcher), "poller")
  private var polling = false
  private var subscribers = Set[ActorRef]()

  override def preStart(): Unit = {
    log.info("Started")
  }

  override def postStop(): Unit = {
    log.info("Stopped")
  }

  val initializing: Receive = {
    case AddSubscriber(subscriber: ActorRef) =>
      subscribers += subscriber
    case Start =>
      log.info("Start polling")
      poll()
      context.become(working)
  }

  val working: Receive = {
    case PollResult(messageAndMetadata) =>
      finishPoll()
      val received = Received(
        key = messageAndMetadata.key,
        message = messageAndMetadata.message(),
        topic = messageAndMetadata.topic,
        partition = messageAndMetadata.partition,
        offset = messageAndMetadata.offset
      )
      unacknowledged += received
      subscribers.foreach(_ ! received)
      if (maxUnacknowledgedMessage.forall(_ > unacknowledged.size)) {
        poll()
      }
      else {
        log.debug(s"Suspend consumer as backlogs >= ${maxUnacknowledgedMessage.get}")
      }
    case PollEmptyResult =>
      finishPoll()
      poll()
    case PollTimeout =>
      finishPoll()
      poll()
    case PollFailure(exception) =>
      finishPoll()
      poll()
    case Acknowledged(received) =>
      unacknowledged -= received
      log.info(s"Message processed: ${received.topic}/${received.partition}/${received.offset}")
      if (!polling)
        log.debug(s"Resume receive from Kafka as backlog drop below ${maxUnacknowledgedMessage.get}")
      poll()

  }

  def receive = initializing

  private def poll(): Unit = {
    if (!polling) {
      poller ! Poll
      polling = true
    }
  }

  private def finishPoll(): Unit = {
    polling = false
  }
}

object KafkaStreamReader {
  case class Received(key: Array[Byte], message: Array[Byte], topic: String, partition: Int, offset: Long)
  case class Acknowledged(received: Received)
  case class AddSubscriber(subscriber: ActorRef)
  case object Start

  private class Poller(stream: KafkaStream[Array[Byte], Array[Byte]]) extends Actor with ActorLogging {
    private val messageIterator = stream.iterator()

    def receive = {
      case Poll =>
        Try(messageIterator.next()) match {
          case Success(messageAndMetadata) =>
            log.info(s"Message received: ${messageAndMetadata.topic}/${messageAndMetadata.partition}/${messageAndMetadata.offset}")
            context.parent ! PollResult(messageAndMetadata)
          case Failure(e: NoSuchElementException) =>
            log.debug("No more messages")
            context.parent ! PollEmptyResult
          case Failure(e: ConsumerTimeoutException) =>
            context.parent ! PollTimeout
          case Failure(e: Throwable) =>
            log.error(e, "Failed to read from Kafka")
            context.parent ! PollFailure(e)
        }
    }
  }

  private case object Poll
  private case class  PollResult(messageAndMetadata: MessageAndMetadata[Array[Byte], Array[Byte]])
  private case object PollEmptyResult
  private case object PollTimeout
  private case class  PollFailure(exception: Throwable)
}

