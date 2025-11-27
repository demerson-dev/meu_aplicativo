package com.ifpr.androidapptemplate.baseclasses

/**
 * Classe de dados que representa um treino de academia
 */
data class Treino(
    var key: String? = null,
    var userId: String? = null,
    var nomeTreino: String? = null,
    var exercicios: String? = null,
    var series: Int? = null,
    var repeticoes: Int? = null,
    var observacoes: String? = null
)

