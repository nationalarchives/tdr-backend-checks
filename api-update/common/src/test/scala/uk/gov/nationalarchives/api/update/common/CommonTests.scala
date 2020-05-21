package uk.gov.nationalarchives.api.update.common

import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers._
import sangria.ast.Document
import sttp.client.{HttpError, Response}
import sttp.model.StatusCode
import uk.gov.nationalarchives.api.update.common.TestGraphQLObjects.{Data, TestResponse, Variables}
import uk.gov.nationalarchives.api.update.common.exceptions.{AuthorisationException, GraphQlException}
import uk.gov.nationalarchives.tdr.GraphQLClient.Extensions
import uk.gov.nationalarchives.tdr.error.{GraphQlError, HttpException}
import uk.gov.nationalarchives.tdr.keycloak.KeycloakUtils
import uk.gov.nationalarchives.tdr.{GraphQLClient, GraphQlResponse}

import scala.concurrent.Future

class CommonTests extends WiremockTest with MockitoSugar {

    "The send method" should "request a service account token" in {
      val apiUpdate = ApiUpdate()

      val client = mock[GraphQLClient[Data, Variables]]
      val document = mock[Document]
      val keycloakUtils = mock[KeycloakUtils]

      when(keycloakUtils.serviceAccountToken(any[String], any[String]))
        .thenReturn(Future.successful(new BearerAccessToken("token")))
      when(client.getResult(any[BearerAccessToken], any[Document], any[Option[Variables]]))
        .thenReturn(Future.successful(GraphQlResponse(Some(Data(TestResponse())), List())))
      apiUpdate.send(keycloakUtils, client, document, Variables())

      val expectedId = sys.env("CLIENT_ID")
      val expectedSecret = sys.env("CLIENT_SECRET")

      verify(keycloakUtils).serviceAccountToken(expectedId, expectedSecret)
    }

    "The send method" should "call the graphql api with the correct data" in {
      val apiUpdate = ApiUpdate()

      val client = mock[GraphQLClient[Data, Variables]]
      val document = mock[Document]
      val keycloakUtils = mock[KeycloakUtils]

      val variables = Variables()

      when(keycloakUtils.serviceAccountToken(any[String], any[String]))
        .thenReturn(Future.successful(new BearerAccessToken("token")))
      when(client.getResult(any[BearerAccessToken], any[Document], any[Option[Variables]]))
        .thenReturn(Future.successful(GraphQlResponse(Some(Data(TestResponse())), List())))
      apiUpdate.send(keycloakUtils, client, document, variables)

      verify(client).getResult(new BearerAccessToken("token"), document, Some(variables))

    }

    "The send method" should "error if the auth server is unavailable" in {
      val client = mock[GraphQLClient[Data, Variables]]
      val document = mock[Document]
      val keycloakUtils = mock[KeycloakUtils]

      val variables = Variables()

      when(keycloakUtils.serviceAccountToken(any[String], any[String]))
        .thenThrow(HttpError("An error occurred contacting the auth server"))

      val exception = intercept[HttpError] {
        ApiUpdate().send(keycloakUtils, client, document, variables)
      }
      exception.body should equal("An error occurred contacting the auth server")

    }

    "The send method" should "error if the graphql server is unavailable" in {

      val client = mock[GraphQLClient[Data, Variables]]
      val document = mock[Document]
      val keycloakUtils = mock[KeycloakUtils]

      val variables = Variables()
      val body: Either[String, String] = if(false) {Right("ok")} else {Left("Graphql error")}
      val response = Response(body, StatusCode.ServiceUnavailable)

      when(keycloakUtils.serviceAccountToken(any[String], any[String]))
        .thenReturn(Future.successful(new BearerAccessToken("token")))
      when(client.getResult(any[BearerAccessToken], any[Document], any[Option[Variables]])).thenThrow(new HttpException(response))

      val exception = intercept[HttpException] {
        ApiUpdate().send(keycloakUtils, client, document, variables)
      }
      exception.getMessage should equal("Unexpected response from GraphQL API: Response(Left(Graphql error),503,,List(),List())")
    }

    "The send method" should "error if the graphql query returns not authorised errors" in {
      val client = mock[GraphQLClient[Data, Variables]]
      val document = mock[Document]
      val keycloakUtils = mock[KeycloakUtils]

      val variables = Variables()

      when(keycloakUtils.serviceAccountToken(any[String], any[String]))
        .thenReturn(Future.successful(new BearerAccessToken("token")))
      val graphqlResponse: GraphQlResponse[Data] =
        GraphQlResponse(Option.empty, List(GraphQlError(GraphQLClient.Error("Not authorised message",
          List(), List(), Some(Extensions(Some("NOT_AUTHORISED")))))))
      when(client.getResult(any[BearerAccessToken], any[Document], any[Option[Variables]]))
        .thenReturn(Future.successful(graphqlResponse))

      val exception = intercept[AuthorisationException] {
        ApiUpdate().send(keycloakUtils, client, document, variables)
      }
      exception.getMessage should equal("Not authorised message")
    }

    "The send method" should "error if the graphql query returns a general error" in {
      val client = mock[GraphQLClient[Data, Variables]]
      val document = mock[Document]
      val keycloakUtils = mock[KeycloakUtils]

      val variables = Variables()

      when(keycloakUtils.serviceAccountToken(any[String], any[String]))
        .thenReturn(Future.successful(new BearerAccessToken("token")))
      val graphqlResponse: GraphQlResponse[Data] =
        GraphQlResponse(Option.empty, List(GraphQlError(GraphQLClient.Error("General error",
          List(), List(), Option.empty))))
      when(client.getResult(any[BearerAccessToken], any[Document], any[Option[Variables]]))
        .thenReturn(Future.successful(graphqlResponse))

      val exception = intercept[GraphQlException] {
        ApiUpdate().send(keycloakUtils, client, document, variables)
      }
      exception.getMessage should equal("GraphQL response contained errors: General error")
    }
}
