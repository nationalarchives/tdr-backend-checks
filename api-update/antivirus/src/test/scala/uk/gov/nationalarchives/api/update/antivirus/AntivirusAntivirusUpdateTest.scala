package uk.gov.nationalarchives.api.update.antivirus

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, postRequestedFor, urlEqualTo}
import uk.gov.nationalarchives.api.update.common.AWSInputs._
import uk.gov.nationalarchives.api.update.common.WiremockTest

import scala.io.Source.fromResource

class AntivirusAntivirusUpdateTest extends WiremockTest {

  def verifyWiremockResponse(fileName: String) = {
    wiremockGraphqlServer.verify(postRequestedFor(urlEqualTo(graphQlPath))
      .withRequestBody(equalToJson(fromResource(s"json/$fileName.json").mkString))
      .withHeader("Authorization", equalTo("Bearer token")))
  }

  "The update method" should "call the graphql api with a single record with a single antivirus update" in {
    authOkJson("access_token")
    graphqlOkJson("graphql_valid_av_response")
    val main = new AntivirusUpdate()
    main.update(sqsEvent("function_valid_av_input"), context)
    verifyWiremockResponse("graphql_valid_av_expected")
  }

  "The update method" should "call the graphql api with a single record with multiple antivirus updates" in {
    authOkJson("access_token")
    graphqlOkJson("graphql_valid_av_multiple_response")
    new AntivirusUpdate().update(sqsEvent("function_valid_av_multiple_input"), context)
    verifyWiremockResponse("graphql_valid_av_multiple_expected_1")
    verifyWiremockResponse("graphql_valid_av_multiple_expected_2")
  }

  "The update method" should "call the graphql api with multiple records with a single antivirus update" in {
    authOkJson("access_token")
    graphqlOkJson("graphql_valid_av_multiple_response")
    new AntivirusUpdate().update(sqsEvent("function_valid_av_input", "function_valid_av_input"), context)
    verifyWiremockResponse("graphql_valid_av_multiple_records_expected_1")
    verifyWiremockResponse("graphql_valid_av_multiple_records_expected_2")
  }

  "The update method" should "call the graphql api with multiple records with multiple antivirus updates" in {
    authOkJson("access_token")
    graphqlOkJson("graphql_valid_av_multiple_response")
    new AntivirusUpdate().update(sqsEvent("function_valid_av_multiple_input", "function_valid_av_multiple_input"), context)
    verifyWiremockResponse("graphql_valid_av_multiple_records_multiple_expected_1")
    verifyWiremockResponse("graphql_valid_av_multiple_records_multiple_expected_2")
    verifyWiremockResponse("graphql_valid_av_multiple_records_multiple_expected_3")
    verifyWiremockResponse("graphql_valid_av_multiple_records_multiple_expected_4")

  }
}
