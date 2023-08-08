package com.currantino

import com.currantino.controller.staticResourcesRouting
import com.currantino.controller.videoRouting
import com.currantino.koin.appModule
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    videoRouting()
    staticResourcesRouting()
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
    install(CallLogging)
    install(ContentNegotiation) {
        json()
    }
}
