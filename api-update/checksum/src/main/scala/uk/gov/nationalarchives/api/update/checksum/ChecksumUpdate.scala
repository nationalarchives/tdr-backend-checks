package uk.gov.nationalarchives.api.update.checksum

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import graphql.codegen.AddFileMetadata.addFileMetadata
import graphql.codegen.types.AddFileMetadataInput
import io.circe.parser.decode
import uk.gov.nationalarchives.api.update.common.ApiUpdate
import uk.gov.nationalarchives.tdr.GraphQLClient
import uk.gov.nationalarchives.tdr.keycloak.KeycloakUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._
import scala.language.postfixOps

class ChecksumUpdate {

  def update(event: SQSEvent, context: Context): Unit = {
    val fileMetadataInput: AddFileMetadataInput = event.getRecords.asScala.map(r => r.getBody).map(inputString => {
      val fileMetadataInputDecoded = decode[AddFileMetadataInput](inputString)
      fileMetadataInputDecoded match {
        case Right(avUpdates) => avUpdates
        case Left(e) => throw e
      }
    }).reduce((a, b) => AddFileMetadataInput(a.filePropertyName, a.fileMetadataValues ++ b.fileMetadataValues))


    val apiUpdate = ApiUpdate()
    val client = new GraphQLClient[addFileMetadata.Data, addFileMetadata.Variables](sys.env("API_URL"))
    val keycloakUtils = KeycloakUtils(sys.env("AUTH_URL"))
    apiUpdate.send(keycloakUtils, client, addFileMetadata.document, addFileMetadata.Variables(fileMetadataInput))
  }
}


