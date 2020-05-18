package uk.gov.nationalarchives.api.update.common

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import org.mockito.MockitoSugar

import scala.io.Source.fromResource
import scala.jdk.CollectionConverters._

object AWSInputs extends MockitoSugar {

  val context = mock[Context]
  def sqsEvent(jsonLocations: String*): SQSEvent = {
    val event = new SQSEvent()
    val records: Seq[SQSMessage] = jsonLocations.map(jsonLocation => {
      val record = new SQSMessage()
      record.setBody(fromResource(s"json/$jsonLocation.json").mkString)
      record
    })

    event.setRecords(records.asJava)
    event
  }

}
