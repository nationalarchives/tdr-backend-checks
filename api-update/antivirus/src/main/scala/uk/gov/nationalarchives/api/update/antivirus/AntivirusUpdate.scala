package uk.gov.nationalarchives.api.update.antivirus

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
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

class AntivirusUpdate {

  def update(event: SQSEvent, context: Context): AddAntivirusMetadata.Data = {
    val antivirusInput: List[AddAntivirusMetadataInput] = event.getRecords.asScala.map(r => r.getBody).flatMap(inputString => {
      val antivirusInputDecoded = decode[List[AddAntivirusMetadataInput]](inputString)
      antivirusInputDecoded match {
        case Right(avUpdates) => avUpdates
        case Left(e) => throw e
      }
    }).toList

    val apiUpdate = ApiUpdate()
    val client = new GraphQLClient[AddAntivirusMetadata.Data, AddAntivirusMetadata.Variables](sys.env("API_URL"))
    val keycloakUtils = KeycloakUtils(sys.env("AUTH_URL"))
    Await.result(
      apiUpdate.send(keycloakUtils, client, AddAntivirusMetadata.document, AddAntivirusMetadata.Variables(antivirusInput)), 20 seconds
    )
  }
}
