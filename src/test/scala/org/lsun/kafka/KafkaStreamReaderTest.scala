package org.lsun.kafka

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import kafka.consumer._
import kafka.message.MessageAndMetadata
import org.lsun.kafka.KafkaStreamReader._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration.{FiniteDuration, _}


class KafkaStreamReaderTest(system: ActorSystem) extends TestKit(system)
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with MockitoSugar {

  def this() = this(ActorSystem("test"))

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val actorSystem = system

  "KafkaStreamReader" must {
    "poll KafkaStream" in {
      // given
      val (stream, iterator) = createKafkaStream()
      val msg1 = createMessage("abc")
      val msg2 = createMessage("def")
      when(iterator.next())
        .thenAnswer(createMessageAnswer(msg1))
        .thenAnswer(createMessageAnswer(msg2, 0.5 second))

      // when
      val (reader, downstream) = createKafkaStreamReader(stream)

      // then
      downstream.expectMsg(received(msg1))
      downstream.expectNoMsg(0.1 second)
      downstream.expectMsg(received(msg2))

      // finally
      system.stop(reader)
    }

    "stop polling if unacknowledged message exceed threshold" in {
      // given
      val (stream, iterator) = createKafkaStream()
      val msg1 = createMessage("abc")
      val msg2 = createMessage("def")
      when(iterator.next())
        .thenAnswer(createMessageAnswer(msg1))
        .thenAnswer(createMessageAnswer(msg2))

      // when
      val (reader, downstream) = createKafkaStreamReader(stream, maxUnacknowledgedMessage = 1)

      // then
      downstream.expectMsg(received(msg1))
      downstream.expectNoMsg()

      // when
      reader.tell(Acknowledged(received(msg1)), downstream.ref)

      // then
      downstream.expectMsg(received(msg2))
      downstream.expectNoMsg()

      // finally
      system.stop(reader)
    }

    "recover from exception" in {
      // given
      val (stream, iterator) = createKafkaStream()
      val msg1 = createMessage("abc")
      val msg2 = createMessage("def")
      when(iterator.next())
        .thenThrow(new RuntimeException)
        .thenAnswer(createMessageAnswer(msg1))
        .thenAnswer(createMessageAnswer(msg2))

      // when
      val (reader, downstream) = createKafkaStreamReader(stream)

      // then
      downstream.expectMsg(received(msg1))
      downstream.expectMsg(received(msg2))

      // finally
      system.stop(reader)
    }
  }

  private def received(msg: MessageAndMetadata[Array[Byte], Array[Byte]]): Received = {
    Received(msg.key, msg.message(), msg.topic, msg.partition, msg.offset)
  }

  private def createKafkaStream(): (KafkaStream[Array[Byte], Array[Byte]], ConsumerIterator[Array[Byte], Array[Byte]]) = {
    val stream = mock[KafkaStream[Array[Byte], Array[Byte]]]
    val iterator = mock[ConsumerIterator[Array[Byte], Array[Byte]]]
    when(stream.iterator()).thenReturn(iterator)
    (stream, iterator)
  }

  private def createKafkaStreamReader(stream: KafkaStream[Array[Byte], Array[Byte]], maxUnacknowledgedMessage: Int = 10): (TestActorRef[KafkaStreamReader], TestProbe) = {
    val downstream = TestProbe()
    val streamReader = TestActorRef(new KafkaStreamReader(stream, "kafka.pinned-dispatcher", Some(maxUnacknowledgedMessage)))
    streamReader ! AddSubscriber(downstream.ref)
    streamReader ! Start
    (streamReader, downstream)
  }

  private def createMessage(msg: String): MessageAndMetadata[Array[Byte], Array[Byte]] = {
    val message = mock[MessageAndMetadata[Array[Byte], Array[Byte]]]
    when(message.message()).thenReturn(msg.getBytes())
    when(message.topic).thenReturn("")
    when(message.partition).thenReturn(0)
    when(message.offset).thenReturn(0)
    message
  }

  private def createMessageAnswer(msg: MessageAndMetadata[Array[Byte], Array[Byte]], delay: FiniteDuration = 0 second): Answer[MessageAndMetadata[Array[Byte], Array[Byte]]] = new Answer[MessageAndMetadata[Array[Byte], Array[Byte]]] {
    override def answer(invocation: InvocationOnMock): MessageAndMetadata[Array[Byte], Array[Byte]] = {
      Thread.sleep(delay.toMillis)
      msg
    }
  }
}
