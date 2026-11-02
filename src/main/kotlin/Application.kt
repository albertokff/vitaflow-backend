package com.vitaflow

import com.vitaflow.plugins.*
import io.ktor.server.application.*

@Suppress("unused")
fun Application.module() {
    configureDatabases()
    configureHttp()
    configureSerialization()
    configureWebsockets()
    configureRouting()
}
