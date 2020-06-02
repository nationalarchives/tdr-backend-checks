package uk.gov.nationalarchives.api.update.common

import org.mockito.MockitoSugar
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers._
import software.amazon.awssdk.services.sqs.SqsClient
import uk.gov.nationalarchives.api.update.common.utils.ExternalServicesTest

import scala.concurrent.Future

class ResponseProcessorTest extends ExternalServicesTest with MockitoSugar with EitherValues {
  "The process method" should "error if one of the responses have failed" in {
    val responseProcessor = ResponseProcessor()
    val id = "failed-id"
    val failed: Either[String, Nothing] = Left(id)
    responseProcessor.process(Future.successful(List(failed)))
    val caught =
      intercept[RuntimeException] {
        responseProcessor.process(Future.successful(List(failed))).await()
      }
    caught.getMessage should equal("The following file ids have failed failed-id")
  }

  "The process method" should "error if more than one of the responses have failed" in {
    val responseProcessor = ResponseProcessor()
    val id = "failed-id"
    val anotherId = "another-failed-id"
    val caught =
      intercept[RuntimeException] {
        responseProcessor.process(Future.successful(List(Left(id), Left(anotherId)))).await()
      }
    caught.getMessage should equal("The following file ids have failed failed-id, another-failed-id")
  }

  "The process method" should "return successfully if all responses have succeeded" in {
    val responseProcessor = ResponseProcessor()
    val id = "successful-id"
    val processed = responseProcessor.process(Future.successful(List(Right(id))))
    processed.await().head should equal(id)
  }

}
