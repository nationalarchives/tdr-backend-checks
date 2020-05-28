package uk.gov.nationalarchives.tdr.filecheck.checksum

import java.security.MessageDigest
import java.util

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.{HeadObjectRequest, ListObjectsRequest, ListObjectsResponse, S3Object}
import software.amazon.awssdk.services.s3.S3Client


object ChecksumGenerator extends App {
  val bucketName = ""
  val key = ""
  val chunkSizeInMB = 100

  val s3BucketFile = new UploadBucketClient(bucketName, key)
  val fileSizeInBytes: Long = s3BucketFile.getSizeOfS3Object

  val chunkSizeInBytes: Long = chunkSizeInMB * 1024 * 1024
  val messageDigester: MessageDigest = MessageDigest.getInstance("SHA-256")

  for(startByte <- 0L until fileSizeInBytes by chunkSizeInBytes) {
    val bytesFromFile: Array[Byte] = s3BucketFile.getBytesFromS3Object(startByte,  startByte + chunkSizeInBytes - 1)
    messageDigester.update(bytesFromFile)
  }
  val checkSum: Array[Byte] = messageDigester.digest
  val formattedCheckSum = checkSum.map(byte => f"$byte%02x").mkString
}
