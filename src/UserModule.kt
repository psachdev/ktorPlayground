package com.psachdev

import org.koin.dsl.module
import org.koin.experimental.builder.single
import org.koin.experimental.builder.singleBy

val userAppModule = module(createdAtStart = true) {
    singleBy<UserService, UserServiceImpl>()
    single<UserRepository>()
}