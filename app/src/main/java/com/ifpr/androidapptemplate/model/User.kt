package com.ifpr.androidapptemplate.model

/**
 * Modelo de dados para representar um usuário no sistema
 *
 * @property uid Identificador único do usuário no Firebase Authentication
 * @property name Nome completo do usuário
 * @property email Endereço de e-mail do usuário
 * @property phone Número de telefone do usuário
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = ""
)

