package uk.gov.nationalarchives.api.update.antivirus

import java.io.{InputStream, OutputStream}

import graphql.codegen.AddAntivirusMetadata.AddAntivirusMetadata
import graphql.codegen.types.AddAntivirusMetadataInput
import io.circe.generic.auto._
import io.circe.parser.decode
import uk.gov.nationalarchives.api.update.common.ApiUpdate
import uk.gov.nationalarchives.tdr.GraphQLClient
import uk.gov.nationalarchives.tdr.keycloak.KeycloakUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.language.postfixOps

class AntivirusUpdate {

  def update(in: InputStream, out: OutputStream): Unit = {
    val inputString = Source.fromInputStream(in).mkString
    val antivirusInputDecoded = decode[List[AddAntivirusMetadataInput]](inputString)
    val antivirusInput = antivirusInputDecoded match {
      case Right(avUpdates) => avUpdates
      case Left(e) => throw e
    }
    val apiUpdate = ApiUpdate()
    val client = new GraphQLClient[AddAntivirusMetadata.Data, AddAntivirusMetadata.Variables](sys.env("API_URL"))
    val keycloakUtils = KeycloakUtils(sys.env("AUTH_URL"))
    val result: AddAntivirusMetadata.Data = apiUpdate.send(keycloakUtils, client, AddAntivirusMetadata.document, AddAntivirusMetadata.Variables(antivirusInput))
    out.write(result.toString.getBytes)
  }
}


