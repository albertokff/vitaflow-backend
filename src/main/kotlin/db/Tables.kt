package com.vitaflow.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object PacientesTable: IntIdTable("pacientes") {
    val cpf = varchar("cpf", 11).uniqueIndex()
    val nome = varchar("nome", 100)
    val dataNascimento = varchar("data_nascimento", 10)
    val cartaoSus = varchar("cartao_sus", 15).nullable()
    val criadoEm = datetime("criado_em").default(LocalDateTime.now())
}

enum class StatusAtendimento {
    AGUARDANDO_TRIAGEM,
    EM_TRIAGEM,
    AGUARDANDO_CONSULTA,
    EM_CONSULTA,
    FINALIZADO,
    CANCELADO
}

object AtendimentosTable: IntIdTable("atendimentos") {
    val pacienteId = reference("paciente_id", PacientesTable)
    val status = enumerationByName("status", 30, StatusAtendimento::class)
    val prioridade = bool("prioridade").default(false)
    val queixa = text("queixa").nullable()

    val pressaoArterial = varchar("pressao_arterial", 10).nullable()
    val temperatura = double("temperatura").nullable()
    val localAtendimento = varchar("local_atendimento", 50).nullable()

    val criadoEm = datetime("criado_em").default(LocalDateTime.now())
    val atualizadoEm = datetime("atualizado_em").default(LocalDateTime.now())
}