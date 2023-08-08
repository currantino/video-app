package com.currantino.repository

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.model.CompletedMultipartUpload
import aws.sdk.kotlin.services.s3.model.CompletedPart
import aws.smithy.kotlin.runtime.client.LogMode
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.net.Host
import aws.smithy.kotlin.runtime.net.Scheme
import aws.smithy.kotlin.runtime.net.Url
import io.ktor.http.content.*
import kotlinx.coroutines.runBlocking
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

    private suspend fun createBucketIfNotExists(bucketName: String) {
        val s3 = getS3Client()
        val bucketNames = s3.listBuckets().buckets?.map { it.name } ?: emptyList()
        if (VIDEOS_BUCKET_NAME !in bucketNames) {
            s3.createBucket {
                bucket = bucketName
            }
        }
        s3.close()
    }

    suspend fun uploadFile(
        partData: PartData.FileItem,
        totalSize: Long
    ): String {
        val s3 = getS3Client()
        val createMultipartUploadResponse = s3.createMultipartUpload {
            bucket = VIDEOS_BUCKET_NAME
            key = partData.originalFileName
            contentType = partData.contentType.toString()
        }
        var partCounter = 1
        val myParts: MutableList<CompletedPart> = mutableListOf()
        val inputStream = partData.streamProvider()
        val bufferSize = getBufferSize(totalSize)
        var bytesRead = 0L
        while (totalSize > bytesRead || inputStream.available() > 0) {
            val partSize = min(totalSize - bytesRead, bufferSize.toLong()).toInt()
            val partByteStream = ByteStream.fromBytes(inputStream.readNBytes(partSize))
            if (partByteStream.contentLength == 0L) {
                break;
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
        inputStream.close()
        val completeMultiPartResponse = s3.completeMultipartUpload {
            bucket = createMultipartUploadResponse.bucket
            key = createMultipartUploadResponse.key
            uploadId = createMultipartUploadResponse.uploadId
            multipartUpload = CompletedMultipartUpload {
                parts = myParts
            }
        }
        return "Video uploaded successfully to ${completeMultiPartResponse.bucket}/${completeMultiPartResponse.key}"
    }

    fun getS3Client(): S3Client {
        return S3Client {
            region = "us-west-rack-2"
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = System.getenv("MINIO_ROOT_USER") ?: "admin"
                secretAccessKey = System.getenv("MINIO_ROOT_PASSWORD") ?: "password"
            }
            endpointUrl =
                Url(scheme = Scheme.HTTP, host = Host.parse("localhost"), port = 9000)
            logMode = LogMode.LogRequest + LogMode.LogResponse
            forcePathStyle = true
        }
    }

    private fun getBufferSize(totalSize: Long) =
        if (totalSize > LARGE_UPLOAD_FILE_SIZE) INCREASED_UPLOAD_BUFFER_SIZE else DEFAULT_UPLOAD_BUFFER_SIZE
}
