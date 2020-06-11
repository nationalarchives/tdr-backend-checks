package uk.gov.nationalarchives.api.update.common

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.typesafe.config.{Config, ConfigFactory}
import io.circe
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}
import sangria.ast.Document
import uk.gov.nationalarchives.tdr.GraphQLClient
import uk.gov.nationalarchives.tdr.keycloak.KeycloakUtils

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.language.postfixOps

class Processor[Input, Data, Variables](document: Document, variablesFn: Input => Variables)(implicit val excecutionContext: ExecutionContext, val decoder: Decoder[Input], val dataDecoder: Decoder[Data], val variablesEncoder: Encoder[Variables]) {
  case class InputWithReceiptHandle(input: Input, receiptHandle: String)
  val configFactory: Config = ConfigFactory.load
  val apiUpdate: ApiUpdate = ApiUpdate()
  val keycloakUtils: KeycloakUtils = KeycloakUtils(configFactory.getString("url.auth"))

  case class BodyWithReceiptHandle(body: String, recieptHandle: String)

  def processInput(inputWithReceiptHandle: InputWithReceiptHandle): Future[Either[String, String]] = {
    val client = new GraphQLClient[Data, Variables](configFactory.getString("url.api"))
    val input = inputWithReceiptHandle.input
    val response: Future[Either[String, Data]] =
      apiUpdate.send[Data, Variables](keycloakUtils, client, document, variablesFn(input))
    response.map {
      case Right(_) =>
        SQSUpdate().deleteSqsMessage(configFactory.getString("sqs.url"), inputWithReceiptHandle.receiptHandle)
        Right(s"$input was successful")
      case Left(e) => Left(s"$input failed with error $e")
    }
  }
  def process(event: SQSEvent): Seq[String] = {
    val inputWithReceiptHandleOrErrors: List[Either[circe.Error, InputWithReceiptHandle]] = event.getRecords.asScala
      .map(r => BodyWithReceiptHandle(r.getBody, r.getReceiptHandle))
      .map(bodyWithReceiptHandle => {
        decode[Input](bodyWithReceiptHandle.body)
          .map(avUpdates => InputWithReceiptHandle(avUpdates, bodyWithReceiptHandle.recieptHandle))
      }).toList

    val responses: Seq[Future[Either[String, String]]] = inputWithReceiptHandleOrErrors.map {
      case Right(inputWithReceiptHandle) => processInput(inputWithReceiptHandle)
      case Left(err) => Future.successful(Left(err.getMessage))
    }

    Await.result(ResponseProcessor().process(Future.sequence(responses)), 10 seconds)
  }

}
