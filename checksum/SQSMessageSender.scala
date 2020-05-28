package uk.gov.nationalarchives.tdr.filecheck.checksum

import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, AwsSessionCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

object SQSMessageSender extends App {
  val sqsClient: SqsClient = SqsClient.builder()
    .region(Region.EU_WEST_2)
    .build()

  sqsClient.sendMessage(SendMessageRequest.builder()
    .queueUrl("")
    .messageBody("Hello world!")
    .delaySeconds(0)
    .build())
}
