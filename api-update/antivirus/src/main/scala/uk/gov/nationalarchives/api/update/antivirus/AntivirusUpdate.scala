package uk.gov.nationalarchives.api.update.antivirus

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import graphql.codegen.AddAntivirusMetadata.AddAntivirusMetadata.{Data, Variables, document}
import graphql.codegen.types.AddAntivirusMetadataInput
import io.circe.generic.auto._
import uk.gov.nationalarchives.api.update.common.Processor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class AntivirusUpdate  {

  def update(event: SQSEvent, context: Context): Seq[String] = {
    val processor = new Processor[AddAntivirusMetadataInput, Data, Variables](document, i => Variables(i))
    processor.process(event)
  }

}
