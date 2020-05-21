package uk.gov.nationalarchives.api.update.common

import sangria.ast.Document
import uk.gov.nationalarchives.api.update.common.exceptions.{AuthorisationException, GraphQlException}
import uk.gov.nationalarchives.tdr.error.NotAuthorisedError
import uk.gov.nationalarchives.tdr.keycloak.KeycloakUtils
import uk.gov.nationalarchives.tdr.{GraphQLClient, GraphQlResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class ApiUpdate()(implicit val executionContext: ExecutionContext) {

  def send[D, V](keycloakUtils: KeycloakUtils, client: GraphQLClient[D, V], document: Document, variables: V): Future[D] = {
    val queryResult: Future[GraphQlResponse[D]] = for {
      token <- keycloakUtils.serviceAccountToken(sys.env("CLIENT_ID"), sys.env("CLIENT_SECRET"))
      result <- client.getResult(token, document, Option(variables))
    } yield result

    queryResult.map(result => {
      result.errors match {
        case Nil => result.data.get
        case List(authError: NotAuthorisedError) => throw new AuthorisationException(authError.message)
        case errors => throw new GraphQlException(errors)
      }
    })
  }
}

object ApiUpdate {
  def apply()(implicit executionContext: ExecutionContext): ApiUpdate = new ApiUpdate()(executionContext)
}
