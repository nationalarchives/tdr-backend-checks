package uk.gov.nationalarchives.api.update.common.utils

import java.net.URI

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import io.findify.sqsmock.SQSService
import org.scalatest.concurrent.ScalaFutures.{PatienceConfig, scaled}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.{CreateQueueRequest, SendMessageRequest}

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source.fromResource

class ExternalServicesTest extends AnyFlatSpec with BeforeAndAfterEach with BeforeAndAfterAll {

  val wiremockGraphqlServer = new WireMockServer(9001)
  val wiremockAuthServer = new WireMockServer(9002)

  val port = 8001
  val account = 1
  val queueName = "testqueue"
  val api = new SQSService(port, account)
  val queueUrl = s"http://localhost:$port/$account/$queueName"

  implicit val ec: ExecutionContext = ExecutionContext.global

  implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(100, Millis)))

  val graphQlPath = "/graphql"
  val authPath = "/auth/realms/tdr/protocol/openid-connect/token"

  def graphQlUrl: String = wiremockGraphqlServer.url(graphQlPath)

  def graphqlOkJson(jsonLocation: String) = wiremockGraphqlServer.stubFor(post(urlEqualTo(graphQlPath))
    .willReturn(okJson(fromResource(s"json/$jsonLocation.json").mkString)))

  def authOkJson(jsonLocation: String) = wiremockAuthServer.stubFor(post(urlEqualTo(authPath))
    .willReturn(okJson(fromResource(s"json/$jsonLocation.json").mkString)))

  def authUnavailable = wiremockAuthServer.stubFor(post(urlEqualTo(authPath)).willReturn(serverError()))

  def graphqlUnavailable = wiremockGraphqlServer.stubFor(post(urlEqualTo(graphQlPath)).willReturn(serverError()))

  override def beforeAll(): Unit = {
    wiremockGraphqlServer.start()
    wiremockAuthServer.start()
    api.start()
    val client: SqsClient = SqsClient.builder()
      .region(Region.EU_WEST_2)
      .endpointOverride(new URI(s"http://localhost:$port"))
      .build()
    val request = CreateQueueRequest.builder().queueName(queueName).build()
    client.createQueue(request)
  }

  override def afterAll(): Unit = {
    wiremockGraphqlServer.stop()
    wiremockAuthServer.stop()
    api.shutdown()
  }

  override def afterEach(): Unit = {
    wiremockAuthServer.resetAll()
    wiremockGraphqlServer.resetAll()

  }

  implicit class AwaitFuture[T](future: Future[T]) {
    def await(timeout: Duration = 2.seconds): T = {
      Await.result(future, timeout)
    }
  }

}

