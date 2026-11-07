package com.vitaflow.plugins

import com.vitaflow.db.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

val connectedClients = mutableSetOf<DefaultWebSocketSession>()
val clientsMutex = Mutex()

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Vitaflow API Backend running successfully!")
        }

        route("/api") {
            // --- PACIENTES ---

            post("/pacientes") {
                val req = call.receive<NovoPacienteRequest>()
                val pacienteDto = transaction {
                    val pacienteId = PacientesTable.insertAndGetId {
                        it[nome] = req.nome.trim().uppercase()
                        it[cpf] = req.cpf.trim()
                        it[dataNascimento] = req.dataNascimento
                        it[cartaoSus] = req.cartaoSus
                    }.value

                    PacienteDto(
                        id = pacienteId,
                        nome = req.nome.trim().uppercase(),
                        cpf = req.cpf.trim(),
                        dataNascimento = req.dataNascimento,
                        cartaoSus = req.cartaoSus,
                        criadoEm = req.criadoEm
                    )
                }
                call.respond(HttpStatusCode.Created, pacienteDto)
            }


            // --- ATENDIMENTOS ---
            get("/atendimentos") {
                val list = transaction {
                    (AtendimentosTable innerJoin PacientesTable)
                        .selectAll()
                        .map { row ->
                            AtendimentoDto(
                                id = row[AtendimentosTable.id].value,
                                nomePaciente = row[PacientesTable.nome],
                                cpf = row[PacientesTable.cpf],
                                dataNascimento = row[PacientesTable.dataNascimento],
                                ePrioridade = row[AtendimentosTable.prioridade],
                                pressaoArterial = row[AtendimentosTable.pressaoArterial] ?: "",
                                temperatura = row[AtendimentosTable.temperatura]?.toString() ?: "",
                                queixaPrincipal = row[AtendimentosTable.queixa] ?: "",
                                status = row[AtendimentosTable.status].name,
                                localAtendimento = row[AtendimentosTable.localAtendimento] ?: ""
                            )
                        }
                }
                call.respond(list)
            }

            post("/atendimentos") {
                val req = call.receive<NovoAtendimentoRequest>()
                val atendimentoDto = transaction {
                    // Check or insert patient
                    val pacienteId = PacientesTable.selectAll().where { PacientesTable.cpf eq req.cpf }.singleOrNull()?.let {
                        it[PacientesTable.id]
                    } ?: PacientesTable.insertAndGetId {
                        it[cpf] = req.cpf
                        it[nome] = req.nome.trim().uppercase()
                        it[dataNascimento] = req.dataNasc
                    }

                    val atendimentoId = AtendimentosTable.insertAndGetId {
                        it[this.pacienteId] = pacienteId
                        it[status] = StatusAtendimento.AGUARDANDO_TRIAGEM
                        it[prioridade] = req.ePrioridade
                    }.value

                    AtendimentoDto(
                        id = atendimentoId,
                        nomePaciente = req.nome.trim().uppercase(),
                        cpf = req.cpf,
                        dataNascimento = req.dataNasc,
                        ePrioridade = req.ePrioridade,
                        status = StatusAtendimento.AGUARDANDO_TRIAGEM.name
                    )
                }

                // Broadcast via WebSocket
                broadcastMessage("ATUALIZACAO_ATENDIMENTOS")
                call.respond(HttpStatusCode.Created, atendimentoDto)
            }

            put("/atendimentos/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
                val req = call.receive<AtualizarAtendimentoRequest>()

                val success = transaction {
                    AtendimentosTable.update({ AtendimentosTable.id eq EntityID(id, AtendimentosTable) }) {
                        it[status] = StatusAtendimento.valueOf(req.status)
                        it[pressaoArterial] = req.pressao
                        it[temperatura] = req.temperatura.toDoubleOrNull()
                        it[queixa] = req.queixa
                        it[localAtendimento] = req.localAtendimento
                    } > 0
                }

                if (success) {
                    broadcastMessage("ATUALIZACAO_ATENDIMENTOS")
                    call.respond(HttpStatusCode.OK, mapOf("success" to true))
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            // --- AGENDAMENTOS ---
            get("/agendamentos") {
                val list = transaction {
                    AgendamentosTable.selectAll().map { row ->
                        AgendamentoDto(
                            id = row[AgendamentosTable.id].value,
                            data = row[AgendamentosTable.data],
                            hora = row[AgendamentosTable.hora],
                            nomePaciente = row[AgendamentosTable.nomePaciente],
                            cpf = row[AgendamentosTable.cpf],
                            especialidade = row[AgendamentosTable.especialidade],
                            medico = row[AgendamentosTable.medico],
                            status = row[AgendamentosTable.status]
                        )
                    }
                }
                call.respond(list)
            }

            post("/agendamentos") {
                val req = call.receive<AgendamentoRequest>()
                val dto = transaction {
                    val id = AgendamentosTable.insertAndGetId {
                        it[data] = req.data
                        it[hora] = req.hora
                        it[nomePaciente] = req.nome.trim().uppercase()
                        it[cpf] = req.cpf
                        it[especialidade] = req.especialidade
                        it[medico] = req.medico
                        it[status] = "AGENDADO"
                    }.value

                    AgendamentoDto(
                        id = id,
                        data = req.data,
                        hora = req.hora,
                        nomePaciente = req.nome.trim().uppercase(),
                        cpf = req.cpf,
                        especialidade = req.especialidade,
                        medico = req.medico
                    )
                }
                broadcastMessage("ATUALIZACAO_AGENDAMENTOS")
                call.respond(HttpStatusCode.Created, dto)
            }

            put("/agendamentos/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
                val req = call.receive<AgendamentoRequest>()
                val success = transaction {
                    AgendamentosTable.update({ AgendamentosTable.id eq EntityID(id, AgendamentosTable) }) {
                        it[data] = req.data
                        it[hora] = req.hora
                        it[nomePaciente] = req.nome.trim().uppercase()
                        it[cpf] = req.cpf
                        it[especialidade] = req.especialidade
                        it[medico] = req.medico
                    } > 0
                }
                if (success) {
                    broadcastMessage("ATUALIZACAO_AGENDAMENTOS")
                    call.respond(HttpStatusCode.OK, mapOf("success" to true))
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            delete("/agendamentos/{id}") {
                val agendamentoId = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val success = transaction {
                    AgendamentosTable.deleteWhere { with(SqlExpressionBuilder) { AgendamentosTable.id eq EntityID(agendamentoId, AgendamentosTable) } } > 0
                }
                if (success) {
                    broadcastMessage("ATUALIZACAO_AGENDAMENTOS")
                    call.respond(HttpStatusCode.OK, mapOf("success" to true))
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        webSocket("/ws") {
            clientsMutex.withLock {
                connectedClients.add(this)
            }
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (text.equals("ping", ignoreCase = true)) {
                            outgoing.send(Frame.Text("pong"))
                        }
                    }
                }
            } finally {
                clientsMutex.withLock {
                    connectedClients.remove(this)
                }
            }
        }
    }
}

suspend fun broadcastMessage(message: String) {
    val frame = Frame.Text(message)
    val clients = clientsMutex.withLock {
        connectedClients.toList()
    }
    for (client in clients) {
        try {
            client.outgoing.send(frame)
        } catch (e: Exception) {
            // ignore disconnected client
        }
    }
}
