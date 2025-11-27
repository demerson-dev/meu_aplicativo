package com.ifpr.androidapptemplate.baseclasses

/**
 * Classe de dados que representa um usuário do sistema
 * Contém todas as informações de perfil do usuário
 */
data class Usuario(
    var key: String? = null,
    var nome: String? = null,
    var email: String? = null,
    var endereco: String? = null,
    var cep: String? = null,
    var dataNascimento: String? = null,
    var photoUrl: String? = null
)
