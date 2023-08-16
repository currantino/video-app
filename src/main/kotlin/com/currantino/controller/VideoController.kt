package com.currantino.controller

import com.currantino.service.VideoService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.videoRouting() {
    val videoService by inject<VideoService>()
    install(AutoHeadResponse)
    routing {
        route("/videos") {
            post("/upload") {
                val data = call.receiveMultipart()
                val contentLength =
                    call.request.contentLength() ?: throw BadRequestException("Provide content-length header.")
                val response = videoService.uploadVideo(data, contentLength)
                call.respond(response)
            }

            get("/available") {
                val videos = videoService.getAvailableVideos()
                call.respond(mapOf("videos" to videos))
            }

            get("/presigned") {
                val videoName = call.request.queryParameters["name"]
                if (videoName.isNullOrBlank()) {
                    throw BadRequestException("Invalid video name.")
                }
                val presignedVideo = videoService.getPresignedVideo(videoName)
                call.respond(presignedVideo)
            }

            route("/stream") {
                install(PartialContent)
                get {
                    val ranges = call.request.ranges()
                    val videoName = call.request.queryParameters["name"]
                    if (videoName.isNullOrBlank()) {
                        throw BadRequestException("Invalid video name.")
                    }
                    if (ranges == null) {
                        throw BadRequestException("Provide Range header.")
                    }
                    if (ranges.ranges.size > 1) {
                        throw BadRequestException("Provide only one range.")
                    }
                    val response = videoService.stream(ranges, videoName)
                    call.response.header(HttpHeaders.AcceptRanges, RangeUnits.Bytes.unitToken)
                    call.response.header(HttpHeaders.ContentRange, response.contentRange)
                    call.respondBytes(
                        contentType = response.contentType,
                        status = HttpStatusCode.PartialContent,
                        bytes = response.data,
                    )
                }
            }
        }
    }
}