package uk.gov.nationalarchives.api.update.common

object TestGraphQLObjects {

  case class Variables()
  case class Data(response : TestResponse)
  case class TestResponse()
}
