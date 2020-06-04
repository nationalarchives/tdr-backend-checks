package uk.gov.nationalarchives.api.update.common

import scala.concurrent.{ExecutionContext, Future}

class ResponseProcessor()(implicit val executionContext: ExecutionContext) {

  def process(response: Future[Seq[Either[String, String]]]): Future[Seq[String]] = {
    response.map(r => {
      val failures: Seq[String] = r.collect { case Left(msg) => msg }
      val successes: Seq[String] = r.collect { case Right(msg) => msg }
      if (failures.nonEmpty) {
        throw new RuntimeException(s"The following file ids have failed ${failures.mkString(", ")}")
      } else {
        successes
      }
    })
  }
}

object ResponseProcessor {
  def apply()(implicit executionContext: ExecutionContext): ResponseProcessor = new ResponseProcessor()
}