package com.currantino.service

import com.currantino.repository.VideoRepository
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
}
