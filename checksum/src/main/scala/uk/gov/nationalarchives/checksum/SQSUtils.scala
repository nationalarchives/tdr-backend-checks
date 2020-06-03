package uk.gov.nationalarchives.checksum

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.{DeleteMessageRequest, SendMessageRequest}

class SQSUtils(sqsClient: SqsClient) {

  def send(queueUrl: String, messageBody: String): Unit = {
    sqsClient.sendMessage(SendMessageRequest.builder()
      .queueUrl("")
      .messageBody(messageBody)
      .delaySeconds(0)
      .build())
  }

  def delete(queueUrl: String, receiptHandle: String) = {
    sqsClient.deleteMessage(
      DeleteMessageRequest.builder()
        .queueUrl(queueUrl)
        .receiptHandle(receiptHandle)
        .build())
  }
}

object SQSUtils {
  def apply(sqsClient: SqsClient): SQSUtils = new SQSUtils(sqsClient)
}
