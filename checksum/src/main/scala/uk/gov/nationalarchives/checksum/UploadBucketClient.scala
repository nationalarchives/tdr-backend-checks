package uk.gov.nationalarchives.checksum

import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, GetObjectResponse, HeadObjectRequest}


class UploadBucketClient(bucketName: String, key: String) {
  val s3Client: S3Client = S3Client.builder.region(Region.EU_WEST_2).build()

  def getSizeOfS3Object: Long = {
    val headObjectRequest: HeadObjectRequest = HeadObjectRequest.builder() // Probably not needed since the SQS JSON contains the size of the object
      .bucket(bucketName)
      .key(key)
      .build()
    val headObjectResponse = s3Client.headObject(headObjectRequest)
    headObjectResponse.contentLength
  }

  def getBytesFromS3Object(start: Long, end: Long): Array[Byte] = {
    val objectRequest: GetObjectRequest = GetObjectRequest.builder()
      .bucket(bucketName)
      .key(key)
      .range(s"bytes=$start-$end")
      .build()
    val byteTransformer: ResponseTransformer[GetObjectResponse, ResponseBytes[GetObjectResponse]] = ResponseTransformer.toBytes()
    s3Client.getObject(objectRequest, byteTransformer).asByteArray()
  }
}
