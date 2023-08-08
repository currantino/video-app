package com.currantino.koin

import com.currantino.repository.VideoRepository
import com.currantino.service.VideoService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::VideoService)
    singleOf(::VideoRepository)
}