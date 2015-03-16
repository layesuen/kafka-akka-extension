kafka-akka-extension
======

Akka extension for Kafka consumer. Features include
 * Automatically set up actors that poll message from Kafka
 * All blocking IO is hidden from user's perspective
 * Acknowledgement based back pressure implementation
 * Minimal code/configuration

## SBT dependencies
    resolvers += "Sonatype OSS" at "https://oss.sonatype.org/content/groups/public"
    libraryDependencies += "org.lsun" %% "kafka-akka-extension" % "0.0.1"

## Producer

### Configuration
    kafka {
      producer {
        metadata-broker-list = "host1:port1,host2:port2,host3:port3",
      }
    }

### Usage

    IO(Kafka) ! Message("topic1", key, message)
  
## Consumer

### Configuration

    kafka {
      consumer {
        group-id = "consumer-group-name"
        zookeeper-connect = "host1:port1,host2:port2,host3:port3"
        maximum-backlog = 5
      }
    }

_maximum-backlog_ is the maximum number of messages in backlog, set it to 0 to disable back pressure

### Usage
    val settings = KafkaConsumer.ConsumerSettings
        .withTopc("topic1")
        .withConsumerActor("topic", actor1)
    IO(KafkaConsumer) ! StartConsumer(settings)

### Receiver Actor Responsibility

For the receiver actors, it receives

    Received(key, message, topic, partition, offset)

for incoming messages. It is responsible to reply

    Acknowledge(Received(key, message, topic, partition, offset))

no matter if the message is processed successful or not. Otherwise the consumer will be suspended when it reach the configured maximum messages in backlog.


