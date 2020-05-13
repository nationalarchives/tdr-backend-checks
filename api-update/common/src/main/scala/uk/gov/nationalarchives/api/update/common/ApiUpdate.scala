package uk.gov.nationalarchives.api.update.common

import sangria.ast.Document
import uk.gov.nationalarchives.api.update.common.exceptions.AuthorisationException
import uk.gov.nationalarchives.api.update.common.exceptions.{AuthorisationException, GraphQlException}
import uk.gov.nationalarchives.tdr.error.NotAuthorisedError
import uk.gov.nationalarchives.tdr.keycloak.KeycloakUtils
import uk.gov.nationalarchives.tdr.{GraphQLClient, GraphQlResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class ApiUpdate()(implicit val executionContext: ExecutionContext) {

  def send[D, V](keycloakUtils: KeycloakUtils, client: GraphQLClient[D, V], document: Document, variables: V): D = {
    val queryResult: Future[GraphQlResponse[D]] = for {
      token <- keycloakUtils.serviceAccountToken(sys.env("CLIENT_ID"), sys.env("CLIENT_SECRET"))
      result <- client.getResult(token, document, Option(variables))
    } yield result

    val result: Future[D] = queryResult.map(result => {
      result.errors match {
        case Nil => result.data.get
        case List(authError: NotAuthorisedError) => throw new AuthorisationException(authError.message)
        case errors => throw new GraphQlException(errors)
      }
    })

    val output: Try[D] = Await.ready(result, 20 seconds).value.get
    output match {
      case Success(t) => t
      case Failure(e) => throw e
    }
  }
}

object ApiUpdate {
  def apply()(implicit executionContext: ExecutionContext): ApiUpdate = new ApiUpdate()(executionContext)
}
