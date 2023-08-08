package com.currantino

import com.currantino.service.VideoService
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.videoRouting() {
    val videoService by inject<VideoService>()
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
        }
    }
}