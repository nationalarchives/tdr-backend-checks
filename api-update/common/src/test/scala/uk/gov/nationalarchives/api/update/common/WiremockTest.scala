package uk.gov.nationalarchives.api.update.common

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.ScalaFutures.{PatienceConfig, scaled}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.ExecutionContext
import scala.io.Source.fromResource

class WiremockTest extends AnyFlatSpec with BeforeAndAfterEach with BeforeAndAfterAll {

  def getFunctionInput(jsonLocation: String) = new java.io.ByteArrayInputStream(fromResource(s"json/$jsonLocation.json").mkString.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))

  val wiremockGraphqlServer = new WireMockServer(9001)
  val wiremockAuthServer = new WireMockServer(9002)

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
  }

  override def afterAll(): Unit = {
    wiremockGraphqlServer.stop()
    wiremockAuthServer.stop()
  }

  override def afterEach(): Unit = {
    wiremockAuthServer.resetAll()
    wiremockGraphqlServer.resetAll()
  }
}

