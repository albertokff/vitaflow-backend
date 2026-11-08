package com.vitaflow.plugins

import com.vitaflow.db.AgendamentosTable
import com.vitaflow.db.AtendimentosTable
import com.vitaflow.db.PacientesTable
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val url = environment.config.property("database.url").getString()
    val driver = environment.config.property("database.driver").getString()
    val user = environment.config.property("database.user").getString()
    val password = environment.config.property("database.password").getString()

    // Run Flyway migrations with baselineVersion "0" so V1 migration executes
    val flyway = Flyway.configure()
        .dataSource(url, user, password)
        .baselineOnMigrate(true)
        .baselineVersion("0")
        .load()
    flyway.migrate()

    Database.connect(
        url = url,
        driver = driver,
        user = user,
        password = password
    )

    transaction {
        SchemaUtils.create(
            PacientesTable,
            AtendimentosTable,
            AgendamentosTable
        )
    }
}
