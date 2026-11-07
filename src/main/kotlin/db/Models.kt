package com.vitaflow.db

import kotlinx.serialization.Serializable


// --- PACIENTE ---
@Serializable
data class NovoPacienteRequest(
    val nome: String,
    val cpf: String,
    val dataNascimento: String,
    val cartaoSus: String? = null,
    val criadoEm: String
)

@Serializable
data class PacienteDto(
    val id: Int,
    val nome: String,
    val cpf: String,
    val dataNascimento: String,
    val cartaoSus: String? = null,
    val criadoEm: String
)

// --- ATENDIMENTO ---
@Serializable
data class AtendimentoDto(
    val id: Int,
    val nomePaciente: String,
    val cpf: String,
    val dataNascimento: String,
    val ePrioridade: Boolean,
    val pressaoArterial: String = "",
    val temperatura: String = "",
    val queixaPrincipal: String = "",
    val status: String = "AGUARDANDO_TRIAGEM",
    val localAtendimento: String = "",
    val horaChamada: String = ""
)

@Serializable
data class AgendamentoDto(
    val id: Int,
    val data: String,
    val hora: String,
    val nomePaciente: String,
    val cpf: String,
    val especialidade: String,
    val medico: String,
    val status: String = "AGENDADO"
)

@Serializable
data class NovoAtendimentoRequest(
    val nome: String,
    val cpf: String,
    val dataNasc: String,
    val ePrioridade: Boolean
)

@Serializable
data class AtualizarAtendimentoRequest(
    val status: String,
    val pressao: String,
    val temperatura: String,
    val queixa: String,
    val localAtendimento: String = ""
)

@Serializable
data class AgendamentoRequest(
    val data: String,
    val hora: String,
    val nome: String,
    val cpf: String,
    val especialidade: String,
    val medico: String
)
