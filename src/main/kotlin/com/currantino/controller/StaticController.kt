package com.currantino.controller

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.staticResourcesRouting() {
    routing {
        staticResources("/", "static") {
            default("index.html")
        }
    }
}
