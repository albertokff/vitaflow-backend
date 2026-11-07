package com.vitaflow.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureHttp() {
    install(CORS) {
        allowHost("localhost:8081", schemes = listOf("http", "https"))
        allowHost("127.0.0.1:8081", schemes = listOf("http", "https"))
        
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Options)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("MyCustomHeader")
        
        allowNonSimpleContentTypes = true
        allowCredentials = true
    }
}
