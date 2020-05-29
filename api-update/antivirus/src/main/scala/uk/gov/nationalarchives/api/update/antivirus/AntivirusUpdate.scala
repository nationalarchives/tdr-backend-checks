package uk.gov.nationalarchives.api.update.antivirus

import java.util.UUID

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import com.typesafe.config.ConfigFactory
import graphql.codegen.AddAntivirusMetadata.AddAntivirusMetadata
import graphql.codegen.types.AddAntivirusMetadataInput
import io.circe
import io.circe.generic.auto._
import io.circe.parser.decode
import software.amazon.awssdk.regions.Region
import uk.gov.nationalarchives.api.update.common.{ApiUpdate, ResponseProcessor, SQSUpdate}
import uk.gov.nationalarchives.tdr.GraphQLClient
import uk.gov.nationalarchives.tdr.keycloak.KeycloakUtils
import software.amazon.awssdk.services.sqs.{SqsAsyncClient, SqsAsyncClientBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters._
import scala.language.postfixOps

object Test extends App {
  val event = new SQSEvent()
  val record = new SQSMessage()
  record.setBody("[{\"fileId\": \"4e0fd35b-8d6f-4498-b081-f7401ce6b99b\", \"result\": \"something\", \"datetime\": 1}]")

  event.setRecords(List(record).asJava)
  new AntivirusUpdate().update(event, null)
}

class AntivirusUpdate {

  case class BodyWithReceiptHandle(body: String, recieptHandle: String)

  case class InputWithReceiptHandle(input: List[AddAntivirusMetadataInput], receiptHandle: String)

  def update(event: SQSEvent, context: Context): Seq[String] = {

    val configFactory = ConfigFactory.load
    val inputWithReceiptHandleOrErrors: List[Either[circe.Error, InputWithReceiptHandle]] = event.getRecords.asScala
      .map(r => BodyWithReceiptHandle(r.getBody, r.getReceiptHandle))
      .map(bodyWithReceiptHandle => {
        decode[List[AddAntivirusMetadataInput]](bodyWithReceiptHandle.body)
          .map(avUpdates => InputWithReceiptHandle(avUpdates, bodyWithReceiptHandle.recieptHandle))
      }).toList

    val apiUpdate = ApiUpdate()
    val client = new GraphQLClient[AddAntivirusMetadata.Data, AddAntivirusMetadata.Variables](configFactory.getString("url.api"))
    val keycloakUtils = KeycloakUtils(configFactory.getString("url.auth"))
    val sqsClient: SqsAsyncClient = SqsAsyncClient.builder().region(Region.EU_WEST_2).build()
    val sqsUpdate = SQSUpdate(sqsClient)

    def processInput(inputWithReceiptHandle: InputWithReceiptHandle): List[Future[Either[String, String]]] = {
      inputWithReceiptHandle.input.map(avInput => {
        val response: Future[Either[String, AddAntivirusMetadata.Data]] =
          apiUpdate.send(keycloakUtils, client, AddAntivirusMetadata.document, AddAntivirusMetadata.Variables(List(avInput)))
        response.map {
          case Right(_) =>
            sqsUpdate.deleteSqsMessage("", inputWithReceiptHandle.receiptHandle)
            Right(s"${avInput.fileId} was successful")
          case Left(e) => Left(s"${avInput.fileId} failed with error $e")
        }
      })
    }

    val responses: Seq[Future[Either[String, String]]] = inputWithReceiptHandleOrErrors.flatMap {
      case Right(inputWithReceiptHandle) => processInput(inputWithReceiptHandle)
      case Left(err) => List(Future.successful(Left(err.getMessage)))
    }

    Await.result(ResponseProcessor().process(Future.sequence(responses)), 10 seconds)
  }
}
