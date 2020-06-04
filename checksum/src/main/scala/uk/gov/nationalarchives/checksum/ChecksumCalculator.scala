package uk.gov.nationalarchives.checksum

import java.util.UUID

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNS
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.{S3Entity, S3EventNotificationRecord}
import com.amazonaws.services.lambda.runtime.events.{S3Event, SQSEvent}
import com.typesafe.config.ConfigFactory
import io.circe.syntax._
import io.circe
import io.circe.parser.decode
import io.circe.{Decoder, HCursor}
import graphql.codegen.AddFileMetadata.addFileMetadata.Variables
import graphql.codegen.types.AddFileMetadataInput
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageResponse

import scala.jdk.CollectionConverters._
import scala.language.postfixOps

object Test extends App {
  //  val json =
  //    """
  //      |{
  //      |  "Type": "Notification",
  //      |  "MessageId": "d138ef90-e606-513b-8e8d-b8c0e7d4183e",
  //      |  "TopicArn": "arn:aws:sns:eu-west-2:229554778675:tdr-s3-dirty-upload-intg",
  //      |  "Subject": "Amazon S3 Notification",
  //      |  "Message": "{\"Records\":[{\"eventVersion\":\"2.1\",\"eventSource\":\"aws:s3\",\"awsRegion\":\"eu-west-2\",\"eventTime\":\"2020-06-02T07:28:12.755Z\",\"eventName\":\"ObjectCreated:Put\",\"userIdentity\":{\"principalId\":\"AWS:AROATK4UH6IZ46ZUY3WXT:CognitoIdentityCredentials\"},\"requestParameters\":{\"sourceIPAddress\":\"86.13.160.237\"},\"responseElements\":{\"x-amz-request-id\":\"90A277FE8EE7650B\",\"x-amz-id-2\":\"Nv86gCOScND9spKDlnhnMdJeSy8X2k0qu/klkNolgOlolOjATmlHMPm36K2dttcVVNhqtaytTHZ7XSOxutzNSRYJK72hPIOu\"},\"s3\":{\"s3SchemaVersion\":\"1.0\",\"configurationId\":\"tf-s3-topic-20200514093840224100000001\",\"bucket\":{\"name\":\"tdr-upload-files-dirty-intg\",\"ownerIdentity\":{\"principalId\":\"A2GRKIKXX71714\"},\"arn\":\"arn:aws:s3:::tdr-upload-files-dirty-intg\"},\"object\":{\"key\":\"eu-west-2%3A5de0e3f8-681f-487e-931e-370fe6ca457b/a2b5e61b-5699-4a96-8a8d-8b88d45ec24e/ec6a4bce-65b3-4189-8450-e912c4a32b16\",\"size\":0,\"eTag\":\"d41d8cd98f00b204e9800998ecf8427e\",\"versionId\":\"IpN_NReN7r8NnXOiVW0Acz48E.AdsBFA\",\"sequencer\":\"005ED5FF90676C2E46\"}}}]}",
  //      |  "Timestamp": "2020-06-02T07:28:18.365Z",
  //      |  "SignatureVersion": "1",
  //      |  "Signature": "s+2dWJBAGyZHbv4V7mB0xh0JxvageumdaLUFoYGuBxkagrNt8ZExVI3nqRvE15P8uO3BrZb3HVCZ+6fyhjulso3U77l2SLqLi0dWRAwd9UY8gonYtbay41+lme0WQJ8g7Vwp8gNtNU8fqPbcDi9RjRyOensUDES/4UG5whpRa4yCgWp8cr6WVBpYgPskoVx1PnRjU6SK5Z0yC9eDmc1r9M+5pOtCnYzQprZRrSwss1rprZTliQlx2EYxWjZRHfpK7RhCRuDtkW7HICZYAgeR48R0Srya6krzSVZkD2au95/7vtF+zoxX9iFAhmasef3HW7Rbw8HeZTDb5Yul0VRHzw==",
  //      |  "SigningCertURL": "https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-a86cb10b4e1f29c941702d737128f7b6.pem",
  //      |  "UnsubscribeURL": "https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:229554778675:tdr-s3-dirty-upload-intg:daf73ce9-d688-4c0c-b911-5dd88dc8a62d"
  //      |}
  //      |""".stripMargin
  //  val json =
  //  """
  //    |{
  //    |  "Type": "Notification",
  //    |  "MessageId": "d138ef90-e606-513b-8e8d-b8c0e7d4183e",
  //    |  "TopicArn": "arn:aws:sns:eu-west-2:229554778675:tdr-s3-dirty-upload-intg",
  //    |  "Subject": "Amazon S3 Notification",
  //    |  "Message": "{\"Recordsa\":[]}",
  //    |  "Timestamp": "2020-06-02T07:28:18.365Z",
  //    |  "SignatureVersion": "1",
  //    |  "Signature": "s+2dWJBAGyZHbv4V7mB0xh0JxvageumdaLUFoYGuBxkagrNt8ZExVI3nqRvE15P8uO3BrZb3HVCZ+6fyhjulso3U77l2SLqLi0dWRAwd9UY8gonYtbay41+lme0WQJ8g7Vwp8gNtNU8fqPbcDi9RjRyOensUDES/4UG5whpRa4yCgWp8cr6WVBpYgPskoVx1PnRjU6SK5Z0yC9eDmc1r9M+5pOtCnYzQprZRrSwss1rprZTliQlx2EYxWjZRHfpK7RhCRuDtkW7HICZYAgeR48R0Srya6krzSVZkD2au95/7vtF+zoxX9iFAhmasef3HW7Rbw8HeZTDb5Yul0VRHzw==",
  //    |  "SigningCertURL": "https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-a86cb10b4e1f29c941702d737128f7b6.pem",
  //    |  "UnsubscribeURL": "https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:229554778675:tdr-s3-dirty-upload-intg:daf73ce9-d688-4c0c-b911-5dd88dc8a62d"
  //    |}
  //    |""".stripMargin
  val json = """{"a": "b"}"""
  val event = new SQSEvent()
  val message = new SQSMessage()
  message.setBody(json)
  event.setRecords(List(message).asJava)
  val checksumCalculator = new ChecksumCalculator()
  val u = checksumCalculator.update(event, null)
  print(u)
}

class ChecksumCalculator {
  case class EventWithReceiptHandle(event: S3Event, receiptHandle: String)

  def update(event: SQSEvent, context: Context): List[SendMessageResponse] = {
    val eventsOrError: List[Either[circe.Error, EventWithReceiptHandle]] = event.getRecords.asScala.map(record => {
      for {
        snsDecoded <- decode[SNS](record.getBody)
        s3 <- decode[S3Event](snsDecoded.getMessage)
      } yield EventWithReceiptHandle(s3, record.getReceiptHandle)
    }).toList

    val (failed: Seq[String], succeeded: List[EventWithReceiptHandle]) = eventsOrError.foldRight[(List[String], List[EventWithReceiptHandle])](Nil, Nil) {
      case (Left(error), (failed, succeeded)) => (error :: failed, succeeded)
      case (Right(result), (failed, succeeded)) => (failed, result :: succeeded)
    }

    val sqsClient: SqsClient = SqsClient.builder()
      .region(Region.EU_WEST_2)
      .build()

    val sqsUtils: SQSUtils = SQSUtils(sqsClient)
    val configFactory = ConfigFactory.load

    List[String]().map(er => er)
    val values: List[SendMessageResponse] = succeeded.flatMap(eventWithReceiptHandle => eventWithReceiptHandle.event.getRecords.asScala.toList.map(record => {
      val s3 = record.getS3
      val s3Client: S3Client = S3Client.builder.region(Region.EU_WEST_2).build()
      val uploadBucketClient = new UploadBucketClient(s3Client, s3.getBucket.getName, s3.getObject.getKey)
      val checksumGenerator = ChecksumGenerator().generate(uploadBucketClient, record.getS3.getObject.getSizeAsLong)
      val keyToArray: Array[String] = s3.getObject.getKey.split("/")
      val fileId = UUID.fromString(keyToArray(keyToArray.length - 1))

      val messageBody = AddFileMetadataInput("SHA256ServerSideChecksum", fileId, checksumGenerator).asJson.noSpaces
      sqsUtils.delete(configFactory.getString("sqs.queue.input"), eventWithReceiptHandle.receiptHandle)

      sqsUtils.send(configFactory.getString("sqs.queue.input"), messageBody)

    }))

    if(failed.nonEmpty) {
      throw new RuntimeException(failed.mkString(", "))
    } else {
      values
    }
  }
}
