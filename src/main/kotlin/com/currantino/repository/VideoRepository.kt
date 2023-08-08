package com.currantino.repository

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.model.CompletedMultipartUpload
import aws.sdk.kotlin.services.s3.model.CompletedPart
import aws.smithy.kotlin.runtime.client.LogMode
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.net.Url
import io.ktor.http.content.*
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.http.Method
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import kotlin.math.min

private const val VIDEOS_BUCKET_NAME = "videos"
private const val DEFAULT_UPLOAD_BUFFER_SIZE = 1024 * 1024 * 10
private const val INCREASED_UPLOAD_BUFFER_SIZE = 1024 * 1024 * 100
private const val LARGE_UPLOAD_FILE_SIZE = 1024 * 1024 * 500

class VideoRepository {
    init {
        runBlocking {
            initBuckets()
        }
    }

    private suspend fun initBuckets() {
        createBucketIfNotExists(VIDEOS_BUCKET_NAME)
    }

    private suspend fun createBucketIfNotExists(bucketName: String) = getS3Client().use { s3 ->
        val bucketNames = s3.listBuckets().buckets?.map { it.name } ?: emptyList()
        if (VIDEOS_BUCKET_NAME !in bucketNames) {
            s3.createBucket {
                bucket = bucketName
            }
        }
    }

    suspend fun uploadFile(
        partData: PartData.FileItem,
        totalSize: Long
    ): String {
        getS3Client().use { s3 ->
            val createMultipartUploadResponse = s3.createMultipartUpload {
                bucket = VIDEOS_BUCKET_NAME
                key = partData.originalFileName
                contentType = partData.contentType.toString()
            }
            var partCounter = 1
            val myParts: MutableList<CompletedPart> = mutableListOf()
            partData.streamProvider().use { inputStream ->
                val bufferSize = getBufferSize(totalSize)
                var bytesRead = 0L
                while (totalSize > bytesRead || inputStream.available() > 0) {
                    val partSize = min(totalSize - bytesRead, bufferSize.toLong()).toInt()
                    val partByteStream = ByteStream.fromBytes(inputStream.readNBytes(partSize))
                    if (partByteStream.contentLength == 0L) {
                        break
                    }
                    bytesRead += partByteStream.contentLength!!
                    val uploadPartResponse = s3.uploadPart {
                        body = partByteStream
                        partNumber = partCounter++
                        bucket = createMultipartUploadResponse.bucket
                        key = createMultipartUploadResponse.key
                        uploadId = createMultipartUploadResponse.uploadId
                    }
                    myParts.add(CompletedPart {
                        eTag = uploadPartResponse.eTag
                        partNumber = partCounter - 1
                    })
                }
            }
            val completeMultiPartResponse = s3.completeMultipartUpload {
                bucket = createMultipartUploadResponse.bucket
                key = createMultipartUploadResponse.key
                uploadId = createMultipartUploadResponse.uploadId
                multipartUpload = CompletedMultipartUpload {
                    parts = myParts
                }
            }
            return@uploadFile "Video uploaded successfully to ${completeMultiPartResponse.bucket}/${completeMultiPartResponse.key}"
        }
    }

    private fun getBufferSize(totalSize: Long) =
        if (totalSize > LARGE_UPLOAD_FILE_SIZE) INCREASED_UPLOAD_BUFFER_SIZE else DEFAULT_UPLOAD_BUFFER_SIZE

    suspend fun getAvailableVideos(): List<Map<String, String>> {
        getS3Client().use { s3 ->
            val videos = s3.listObjectsV2 {
                bucket = VIDEOS_BUCKET_NAME
            }.contents?.map {
                mapOf(
                    "name" to it.key!!
                )
            } ?: emptyList()
            return videos
        }

    }

    private fun getS3Client(): S3Client {
        return S3Client {
            region = System.getenv("AWS_REGION") ?: "eu-central-1"
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = System.getenv("AWS_ACCESS_KEY_ID") ?: "admin"
                secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY") ?: "password"
            }
            endpointUrl = Url.parse(System.getenv("S3_SERVER_URL") ?: "http://localhost:9000")
            logMode = LogMode.LogRequest + LogMode.LogResponse
            forcePathStyle = true
        }
    }

    suspend fun getPresignedVideo(videoName: String): Map<String, String> {
        val minio = getMinio()
        val contentType = getS3Client().use { s3 ->
            s3.headObject {
                bucket = VIDEOS_BUCKET_NAME
                key = videoName
            }.contentType!!
        }
        val url = minio.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .bucket(VIDEOS_BUCKET_NAME)
                .`object`(videoName)
                .expiry(1, TimeUnit.HOURS)
                .method(Method.GET)
                .build()
        )
        return mapOf(
            "url" to url,
            "contentType" to contentType
        )
    }

    private fun getMinio(): MinioClient = MinioClient.builder()
        .region(System.getenv("AWS_REGION") ?: "eu-central-1")
        .credentials(
            System.getenv("AWS_ACCESS_KEY_ID") ?: "admin",
            System.getenv("AWS_SECRET_ACCESS_KEY") ?: "password"
        )
        .endpoint(System.getenv("S3_SERVER_URL") ?: "http://localhost:9000")
        .build()

}