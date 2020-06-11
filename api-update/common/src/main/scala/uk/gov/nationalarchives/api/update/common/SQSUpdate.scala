package uk.gov.nationalarchives.api.update.common

import java.net.URI

import com.typesafe.config.ConfigFactory
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest

class SQSUpdate(sqsClient: SqsClient) {
  def deleteSqsMessage(queueUrl: String, receiptHandle: String): Unit = {

    val request: DeleteMessageRequest =
      DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(receiptHandle).build()
    sqsClient.deleteMessage(request)
  }
}

object SQSUpdate {
  def apply(): SQSUpdate = {
    val configFactory = ConfigFactory.load
    val httpClient = ApacheHttpClient.builder.build
    val sqsClient: SqsClient = SqsClient.builder()
      .region(Region.EU_WEST_2)
      .endpointOverride(new URI(configFactory.getString("sqs.endpoint")))
      .httpClient(httpClient)
      .build()
    new SQSUpdate(sqsClient)
  }
}
