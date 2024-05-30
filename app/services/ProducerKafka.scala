package services

import play.api.libs.json.{Json, Writes}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import models.Book
import java.util.Properties
import javax.inject.Inject
import scala.concurrent.ExecutionContext


class ProducerKafka @Inject()(implicit ec: ExecutionContext) {

      val props = new Properties()
      props.put("bootstrap.servers", "localhost:9092")
      props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

      val producer = new KafkaProducer[String, String](props)

      def publishUserAction(genre: String,books:Seq[Book]): Unit = {
            implicit val bookWrites: Writes[Book] = Json.writes[Book]
            val jsonString = Json.toJson(books).toString()
        val record = new ProducerRecord[String, String]("user-data", genre,jsonString)
      producer.send(record)
      }
}
