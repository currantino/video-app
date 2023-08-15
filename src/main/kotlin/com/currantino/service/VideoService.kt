package com.currantino.service

import com.currantino.repository.VideoRepository
import com.currantino.stream.StreamingResponse
import io.ktor.http.*
import io.ktor.http.content.*

class VideoService(private val videoRepository: VideoRepository) {

    suspend fun uploadVideo(
        data: MultiPartData,
        totalSize: Long
    ): String {
        val results: MutableList<String> = mutableListOf()
        data.forEachPart { partData ->
            when (partData) {
                is PartData.FileItem -> {
                    val uploadResult = videoRepository.uploadFile(partData, totalSize)
                    results.add(uploadResult)
                }

                else -> results.add("${partData.name}: Could not upload")
            }
        }
        return results.joinToString(separator = "\n")
    }

    suspend fun getAvailableVideos(): List<Map<String, String>> = videoRepository.getAvailableVideos()

    suspend fun getPresignedVideo(videoName: String): Map<String, String> = videoRepository.getPresignedVideo(videoName)

    suspend fun stream(ranges: RangesSpecifier, videoName: String): StreamingResponse =
        videoRepository.getVideoPart(videoName, ranges)

    suspend fun getVideoSize(videoName: String) = videoRepository.getVideoSize(videoName)

}
