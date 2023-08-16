package com.currantino.stream

import io.ktor.http.*

class StreamingResponse(
    val data: ByteArray = ByteArray(0),
    val contentType: ContentType,
    val contentRange: String
)
