package uk.gov.nationalarchives.api.update.antivirus

import java.io.ByteArrayOutputStream

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, postRequestedFor, urlEqualTo}

import scala.io.Source.fromResource
import uk.gov.nationalarchives.api.update.common.WiremockTest

class AntivirusAntivirusUpdateTest extends WiremockTest {
  "The update method" should "call the graphql api with a single antivirus updates" in {
    authOkJson("access_token")
    graphqlOkJson("graphql_valid_av_response")
    val main = new AntivirusUpdate()
    val json = getFunctionInput("function_valid_av_input")
    main.update(json, new ByteArrayOutputStream())
    wiremockGraphqlServer.verify(postRequestedFor(urlEqualTo(graphQlPath))
      .withRequestBody(equalToJson(fromResource("json/graphql_valid_av_expected.json").mkString))
      .withHeader("Authorization", equalTo("Bearer token")))
  }

  "The update method" should "call the graphql api with multiple antivirus updates" in {
    authOkJson("access_token")
    graphqlOkJson("graphql_valid_av_multiple_response")
    val json = getFunctionInput("function_valid_av_multiple_input")
    new AntivirusUpdate().update(json, new ByteArrayOutputStream())
    wiremockGraphqlServer.verify(postRequestedFor(urlEqualTo(graphQlPath))
      .withRequestBody(equalToJson(fromResource("json/graphql_valid_av_multiple_expected.json").mkString))
      .withHeader("Authorization", equalTo("Bearer token")))
  }
}
