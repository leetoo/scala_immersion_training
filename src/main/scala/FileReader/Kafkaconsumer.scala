package FileReader

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.kafka.{ConsumerSettings, Subscriptions}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import akka.kafka.scaladsl.Consumer
import akka.kafka.ConsumerMessage.{CommittableMessage, CommittableOffsetBatch}
import akka.stream.scaladsl.{Keep, Sink}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import akka.kafka.scaladsl.Consumer.Control
import akka.stream.ActorMaterializer

import scala.concurrent.duration.{Duration, _}
/**
  * Created by Atul.Konaje on 5/2/2017.
  */
class KafkaConsumerFile extends  Actor with ActorLogging {

  import FileConsumerMain._
  override def preStart(): Unit = {
    super.preStart()
    self ! Run
  }

  override def postStop(): Unit = {
    super.postStop()
    println("Consumer stopped")
  }

  override def receive: Receive = {
    case  Run =>
      val consumerSettings = ConsumerSettings(context.system, new ByteArrayDeserializer, new StringDeserializer)
        .withBootstrapServers("localhost:9092")
        .withGroupId("genomec")
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      implicit  val mat =ActorMaterializer()
      val kafka_topic=config.getString("KafkaTopic.topic1")
      val subscription = Subscriptions.topics(kafka_topic)
      val(control,future)= Consumer.committableSource(consumerSettings,subscription)
      .mapAsync(1)(printMessage)
      .map(_.committableOffset)
//      .groupedWithin(10, 5 seconds)
//      .map(group => group.foldLeft(CommittableOffsetBatch.empty) { (batch, elem) => batch.updated(elem) })
//      .mapAsync(3)(_.commitScaladsl())
        .toMat(Sink.ignore)(Keep.both)
        .run()

  }

  def printMessage (msg :Message): Future[Message] ={
      log.info(s"Consumed Message : ${msg.record.value()}")
      Future.successful(msg)

  }
}
object FileConsumerMain extends App {
  case object Run
  case object Stop
  val config = ConfigFactory.load()
  val system =ActorSystem("FileConsumerMS")
  type Message = CommittableMessage[Array[Byte], String]
  val fc=system.actorOf(Props[KafkaConsumerFile], name="FileConsumer")
  println("Done!!")
}