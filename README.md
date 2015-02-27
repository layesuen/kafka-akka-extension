kafka-akka-extension
======
#### Configuration

    kafka {
      consumer {
        group-id = "consumer-group-name"
        zookeeper-connect = "host1:port1,host2:port2,host3:port3"
        maximum-backlog = 5
      }
    }

    akka.extension += "mfp.platform.kafka.akka.KafkaConsumerExtension"

_maximum-backlog_ is the maximum number of messages in backlog, set it to 0 to disable back pressure

#### Bootstrapping

    KafkaConsumerExtension(system).start(Map(
      "topic1" -> (numOfStreamsForTopic1, Set(receiverActor1Topic1)),
      "topic2" -> (numOfStreamsForTopic2, Set(receiverActor1Topic2, receiverActor2Topic2))
    ))

#### Receiver Actor Responsibility

For the receiver actors, it receives

    Received(key, message, topic, partition, offset)

for incoming messages. It is responsible to reply

    Acknowledge(Received(key, message, topic, partition, offset))

no matter if the message is processed successful or not. Otherwise the consumer will be suspended when it reach the configured maximum messages in backlog.

#### Shutdown (optional)

    KafkaConsumerExtension(system).shutdown()
