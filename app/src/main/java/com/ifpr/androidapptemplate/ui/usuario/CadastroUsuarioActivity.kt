package com.ifpr.androidapptemplate.ui.usuario

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.model.User

/**
 * Activity responsável pelo cadastro de novos usuários no sistema
 * Coleta informações como nome, telefone, e-mail e senha
 * Cria conta no Firebase Authentication e salva dados adicionais no Firebase Realtime Database
 */
class CadastroUsuarioActivity  : AppCompatActivity() {
    // Componentes da interface
    private lateinit var textCadastroUsuarioTitle: TextView
    private lateinit var registerNameEditText: EditText
    private lateinit var registerPhoneEditText: EditText
    private lateinit var registerEmailEditText: EditText
    private lateinit var registerPasswordEditText: EditText
    private lateinit var registerConfirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var sairButton: Button

    // Referências do Firebase
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_usuario)

        // Inicializa o Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Inicializa a referência do Firebase Realtime Database
        database = FirebaseDatabase.getInstance().reference

        // Vincula os componentes da interface com as variáveis
        textCadastroUsuarioTitle = findViewById(R.id.textCadastroUsuarioTitle)
        registerNameEditText = findViewById(R.id.registerNameEditText)
        registerPhoneEditText = findViewById(R.id.registerPhoneEditText)
        registerEmailEditText = findViewById(R.id.registerEmailEditText)
        registerPasswordEditText = findViewById(R.id.registerPasswordEditText)
        registerConfirmPasswordEditText = findViewById(R.id.registerConfirmPasswordEditText)
        registerButton = findViewById(R.id.salvarButton)
        sairButton = findViewById(R.id.sairButton)

        // Configura o botão de cadastrar
        registerButton.setOnClickListener {
            createAccount()
        }

        // Configura o botão de sair
        sairButton.setOnClickListener {
            finish()
        }
    }

    /**
     * Cria uma nova conta de usuário no Firebase
     * Valida os campos, cria a conta no Authentication e salva dados no Database
     */
    private fun createAccount() {
        // Obtém os valores dos campos de entrada
        val name = registerNameEditText.text.toString().trim()
        val phone = registerPhoneEditText.text.toString().trim()
        val email = registerEmailEditText.text.toString().trim()
        val password = registerPasswordEditText.text.toString().trim()
        val confirmPassword = registerConfirmPasswordEditText.text.toString().trim()

        // Valida se todos os campos foram preenchidos
        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Valida o tamanho mínimo da senha
        if (password.length < 6) {
            Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        // Valida se as senhas coincidem
        if (password != confirmPassword) {
            Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Cria a conta no Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Novo usuário cadastrado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                        val user = auth.currentUser

                        // Atualiza o perfil com o nome
                        updateProfile(user, name)

                        // Salva dados adicionais no Database
                        saveUserToDatabase(user, name, email, phone)

                        // Envia e-mail de verificação
                        sendEmailVerification(user)
                    } else {
                        val errorMessage = task.exception?.message ?: "Erro desconhecido"
                        Log.e("FirebaseAuth", "Erro ao cadastrar usuário: $errorMessage")
                        Toast.makeText(
                            this,
                            "Falha ao cadastrar novo usuário: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } catch (ex: Exception) {
            Log.e("FirebaseAuth", "Erro ao conectar com o Firebase", ex)
            Toast.makeText(
                this,
                "Falha ao conectar com o Firebase: ${ex.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Salva os dados adicionais do usuário no Firebase Realtime Database
     * Armazena nome, e-mail e telefone na estrutura users/{uid}
     *
     * @param firebaseUser Usuário autenticado do Firebase
     * @param name Nome completo do usuário
     * @param email E-mail do usuário
     * @param phone Telefone do usuário
     */
    private fun saveUserToDatabase(firebaseUser: FirebaseUser?, name: String, email: String, phone: String) {
        firebaseUser?.let { user ->
            // Cria objeto User com os dados
            val userData = User(
                uid = user.uid,
                name = name,
                email = email,
                phone = phone
            )

            // Salva no caminho users/{uid}
            database.child("users").child(user.uid).setValue(userData)
                .addOnSuccessListener {
                    Log.d("FirebaseDatabase", "Dados do usuário salvos com sucesso")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseDatabase", "Erro ao salvar dados do usuário: ${e.message}")
                    Toast.makeText(
                        this,
                        "Erro ao salvar dados adicionais do usuário",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    /**
     * Envia e-mail de verificação para o usuário recém-cadastrado
     *
     * @param user Usuário do Firebase que receberá o e-mail
     */
    private fun sendEmailVerification(user: FirebaseUser?) {
        user?.sendEmailVerification()
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        baseContext,
                        "E-mail de verificação enviado para ${user.email}",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        baseContext,
                        "Falha ao enviar e-mail de verificação",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    /**
     * Atualiza o nome de exibição do usuário no Firebase Authentication
     *
     * @param user Usuário do Firebase
     * @param displayName Nome que será exibido
     */
    private fun updateProfile(user: FirebaseUser?, displayName: String) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()

        user?.updateProfile(profileUpdates)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        baseContext,
                        "Nome do usuário alterado com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        baseContext,
                        "Não foi possível alterar o nome do usuário",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
