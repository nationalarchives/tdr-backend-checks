package uk.gov.nationalarchives.checksum

import java.security.MessageDigest

class ChecksumGenerator(bucketName: String, key: String) {

  def generate(): String = {
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
    checkSum.map(byte => f"$byte%02x").mkString
  }

}

object ChecksumGenerator {
  def apply(bucketName: String, key: String): ChecksumGenerator = new ChecksumGenerator(bucketName, key)
}