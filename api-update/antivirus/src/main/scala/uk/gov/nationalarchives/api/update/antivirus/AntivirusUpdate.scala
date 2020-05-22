package uk.gov.nationalarchives.api.update.antivirus

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import graphql.codegen.AddAntivirusMetadata.AddAntivirusMetadata
import graphql.codegen.types.AddAntivirusMetadataInput
import io.circe.generic.auto._
import io.circe.parser.decode
import uk.gov.nationalarchives.api.update.common.ApiUpdate
import uk.gov.nationalarchives.tdr.GraphQLClient
import uk.gov.nationalarchives.tdr.keycloak.KeycloakUtils

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.language.postfixOps
import com.typesafe.config.ConfigFactory

import scala.io.Source.fromResource

object Test extends App {
  val event = new SQSEvent()
  val record = new SQSMessage()
  record.setBody("[{\"fileId\": \"4e0fd35b-8d6f-4498-b081-f7401ce6b99b\", \"result\": \"something\", \"datetime\": 1}]")

  event.setRecords(List(record).asJava)
  new AntivirusUpdate().update(event, null)
}

class AntivirusUpdate {

  def update(event: SQSEvent, context: Context): AddAntivirusMetadata.Data = {

   val configFactory = ConfigFactory.load
    val antivirusInput: List[AddAntivirusMetadataInput] = event.getRecords.asScala.map(r => r.getBody).flatMap(inputString => {
      val antivirusInputDecoded = decode[List[AddAntivirusMetadataInput]](inputString)
      antivirusInputDecoded match {
        case Right(avUpdates) => avUpdates
        case Left(e) => throw e
      }
    }).toList

    val apiUpdate = ApiUpdate()
    val client = new GraphQLClient[AddAntivirusMetadata.Data, AddAntivirusMetadata.Variables](configFactory.getString("url.api"))
    val keycloakUtils = KeycloakUtils(configFactory.getString("url.auth"))
    Await.result(
      apiUpdate.send(keycloakUtils, client, AddAntivirusMetadata.document, AddAntivirusMetadata.Variables(antivirusInput)), 20 seconds
    )
  }
}
