package uk.gov.nationalarchives.api.update.antivirus

import java.net.URI

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.typesafe.config.ConfigFactory
import graphql.codegen.AddAntivirusMetadata.AddAntivirusMetadata
import graphql.codegen.types.AddAntivirusMetadataInput
import io.circe.generic.auto._
import io.circe.parser.decode
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import uk.gov.nationalarchives.api.update.common.{ApiUpdate, ResponseProcessor, SQSUpdate}
import uk.gov.nationalarchives.tdr.GraphQLClient
import uk.gov.nationalarchives.tdr.keycloak.KeycloakUtils
import software.amazon.awssdk.http.apache.ApacheHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters._
import scala.language.postfixOps

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
    val endpoint = configFactory.getString("sqs.endpoint")

    val httpClient = ApacheHttpClient.builder.build
    val sqsClient: SqsClient = SqsClient.builder()
      .region(Region.EU_WEST_2)
      .endpointOverride(new URI(endpoint))
      .httpClient(httpClient)
      .build()
    val sqsUpdate = SQSUpdate(sqsClient)

    def processInput(inputWithReceiptHandle: InputWithReceiptHandle): List[Future[Either[String, String]]] = {
      inputWithReceiptHandle.input.map(avInput => {
        val response: Future[Either[String, AddAntivirusMetadata.Data]] =
          apiUpdate.send(keycloakUtils, client, AddAntivirusMetadata.document, AddAntivirusMetadata.Variables(List(avInput)))
        response.map {
          case Right(_) =>
            sqsUpdate.deleteSqsMessage(configFactory.getString("sqs.url"), inputWithReceiptHandle.receiptHandle)
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
