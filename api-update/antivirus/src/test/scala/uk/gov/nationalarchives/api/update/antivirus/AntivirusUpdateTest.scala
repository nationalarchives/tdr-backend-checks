package uk.gov.nationalarchives.api.update.antivirus

import java.net.URI

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, postRequestedFor, urlEqualTo}
import org.scalatest.matchers.should.Matchers._
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.{ReceiveMessageRequest, SendMessageRequest}
import uk.gov.nationalarchives.api.update.common.utils.AWSInputs._
import uk.gov.nationalarchives.api.update.common.utils.ExternalServicesTest

import scala.io.Source.fromResource
import scala.util.Try

class AntivirusUpdateTest extends ExternalServicesTest {

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

  "The update method" should "call the graphql api with multiple records with a single antivirus update" in {
    authOkJson("access_token")
    graphqlOkJson("graphql_valid_av_response")
    new AntivirusUpdate().update(sqsEvent("function_valid_av_input", "function_valid_av_input"), context)
    verifyWiremockResponse("graphql_valid_av_multiple_records_expected_1")
    verifyWiremockResponse("graphql_valid_av_multiple_records_expected_2")
  }

  "The update method" should "delete a successful message from the queue" in {
    val event = sqsEvent("function_valid_av_input")
    client.sendMessage(request(event.getRecords.get(0).getBody))
    authOkJson("access_token")
    graphqlOkJson("graphql_valid_av_response")
    val main = new AntivirusUpdate()
    main.update(sqsEvent("function_valid_av_input"), context)
    val messages = client.receiveMessage(ReceiveMessageRequest.builder.queueUrl(queueUrl).build)
    messages.hasMessages should be(false)
  }

  "The update method" should "leave a failed message in the queue" in {
    val event = sqsEvent("function_valid_av_input")
    client.sendMessage(request(event.getRecords.get(0).getBody))
    val main = new AntivirusUpdate()
    Try(main.update(event, context))
    val messages = client.receiveMessage(ReceiveMessageRequest.builder.queueUrl(queueUrl).build)
    messages.hasMessages should be(true)
  }

  "The update method" should "delete a successful message and leave a failed message in the queue" in {
    val event = sqsEvent("function_valid_av_input", "function_invalid_av_input")
    client.sendMessage(request(event.getRecords.get(0).getBody))
    client.sendMessage(request(event.getRecords.get(1).getBody))

    authOkJson("access_token")
    graphqlOkJson("graphql_valid_av_response")
    val main = new AntivirusUpdate()
    Try(main.update(event, context))
    val messages = client.receiveMessage(ReceiveMessageRequest.builder.queueUrl(queueUrl).build)
    messages.hasMessages should be(true)
    messages.messages.size should be(1)
  }

  private def request(body: String) = SendMessageRequest.builder()
    .messageBody(body)
    .queueUrl(queueUrl)
    .build()

  private def client = SqsClient.builder()
    .region(Region.EU_WEST_2)
    .endpointOverride(new URI(s"http://localhost:$port"))
    .build()
}
