package uk.gov.nationalarchives.api.update.checksum

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, postRequestedFor, urlEqualTo}
import uk.gov.nationalarchives.api.update.common.AWSInputs._
import uk.gov.nationalarchives.api.update.common.WiremockTest

import scala.io.Source.fromResource

class ChecksumUpdateTest extends WiremockTest {
  "The update method" should "call the graphql api with a single record with a single checksum update" in {
    authOkJson("access_token")
    graphqlOkJson("graphql_valid_checksum_response")
    val main = new ChecksumUpdate()
    main.update(sqsEvent("function_valid_checksum_input"), context)
    wiremockGraphqlServer.verify(postRequestedFor(urlEqualTo(graphQlPath))
      .withRequestBody(equalToJson(fromResource("json/graphql_valid_checksum_expected.json").mkString))
      .withHeader("Authorization", equalTo("Bearer token")))
  }

  "The update method" should "call the graphql api with a single record with multiple checksum updates" in {
    authOkJson("access_token")
    graphqlOkJson("graphql_valid_checksum_multiple_response")
    new ChecksumUpdate().update(sqsEvent("function_valid_checksum_multiple_input"), context)
    wiremockGraphqlServer.verify(postRequestedFor(urlEqualTo(graphQlPath))
      .withRequestBody(equalToJson(fromResource("json/graphql_valid_checksum_multiple_expected.json").mkString))
      .withHeader("Authorization", equalTo("Bearer token")))
  }

  "The update method" should "call the graphql api with multiple records with a single checksum update" in {
    authOkJson("access_token")
    graphqlOkJson("graphql_valid_checksum_multiple_response")
    new ChecksumUpdate().update(sqsEvent("function_valid_checksum_input", "function_valid_checksum_input"), context)
    wiremockGraphqlServer.verify(postRequestedFor(urlEqualTo(graphQlPath))
      .withRequestBody(equalToJson(fromResource("json/graphql_valid_checksum_multiple_records_expected.json").mkString))
      .withHeader("Authorization", equalTo("Bearer token")))
  }

  "The update method" should "call the graphql api with multiple records with multiple checksum updates" in {
    authOkJson("access_token")
    graphqlOkJson("graphql_valid_checksum_multiple_response")
    new ChecksumUpdate().update(sqsEvent("function_valid_checksum_multiple_input", "function_valid_checksum_multiple_input"), context)
    wiremockGraphqlServer.verify(postRequestedFor(urlEqualTo(graphQlPath))
      .withRequestBody(equalToJson(fromResource("json/graphql_valid_checksum_multiple_records_multiple_expected.json").mkString))
      .withHeader("Authorization", equalTo("Bearer token")))
  }
}
