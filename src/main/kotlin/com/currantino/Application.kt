package com.currantino

import com.currantino.koin.appModule
import com.currantino.plugins.configureRouting
import com.currantino.plugins.configureSerialization
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.event.Level

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureRouting()
    videoRouting()
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
    install(CallLogging) {
        level = Level.INFO
    }
    install(ContentNegotiation) {
        json()
    }
}
