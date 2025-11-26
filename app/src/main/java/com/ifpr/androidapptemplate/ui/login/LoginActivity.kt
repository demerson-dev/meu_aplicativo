package com.ifpr.androidapptemplate.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.ifpr.androidapptemplate.MainActivity
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.ui.usuario.CadastroUsuarioActivity

/**
 * Activity responsável pela tela de login
 * Permite login com email/senha ou com conta Google
 */
class LoginActivity : AppCompatActivity() {

    // Componentes da interface
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var btnGoogleSignIn: SignInButton

    // Firebase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Launcher para o resultado do Google Sign-In
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializa o Firebase
        FirebaseApp.initializeApp(this)
        firebaseAuth = FirebaseAuth.getInstance()

        // Configura o launcher para o Google Sign-In (API moderna)
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d(TAG, "Google Sign-In result received. ResultCode: ${result.resultCode}")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Google Sign-In bem-sucedido. Email: ${account.email}")
                Log.d(TAG, "ID Token: ${if (account.idToken != null) "presente" else "AUSENTE"}")
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.e(TAG, "Falha no Google Sign-In")
                Log.e(TAG, "Status code: ${e.statusCode}")
                Log.e(TAG, "Status message: ${e.status}")
                Log.e(TAG, "Local message: ${e.localizedMessage}")

                val errorMessage = when (e.statusCode) {
                    10 -> "Erro de configuração: Verifique o SHA-1 no Firebase Console"
                    12500 -> "Erro interno do Google Sign-In"
                    12501 -> "Login cancelado pelo usuário"
                    else -> "Erro ${e.statusCode}: ${e.localizedMessage}"
                }

                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Vincula os componentes da interface
        emailEditText = findViewById(R.id.edit_text_email)
        passwordEditText = findViewById(R.id.edit_text_password)
        loginButton = findViewById(R.id.button_login)
        registerLink = findViewById(R.id.registerLink)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)

        // Configura o botão de cadastro
        registerLink.setOnClickListener {
            val intent = Intent(this, CadastroUsuarioActivity::class.java)
            startActivity(intent)
        }

        // Configura o botão de login com email/senha
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha email e senha", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signIn(email, password)
        }

        // Configuração do Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Configura o botão de login com Google
        btnGoogleSignIn.setOnClickListener {
            Log.d(TAG, "Botão Google Sign-In clicado")
            signInGoogle()
        }
    }

    /**
     * Realiza login com email e senha no Firebase
     */
    private fun signIn(email: String, password: String) {
        Log.d(TAG, "Tentando login com email: $email")
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Login com email bem-sucedido")
                    updateUI(firebaseAuth.currentUser)
                } else {
                    Log.w(TAG, "Falha no login com email", task.exception)
                    Toast.makeText(
                        this,
                        "Falha na autenticação: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    /**
     * Atualiza a interface após login
     * Se o usuário não for nulo, navega para a MainActivity
     */
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Log.d(TAG, "Navegando para MainActivity. Usuário: ${user.email}")
            Toast.makeText(
                this,
                "Bem-vindo, ${user.displayName ?: user.email}!",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Fecha a LoginActivity para não voltar ao pressionar "voltar"
        } else {
            Log.d(TAG, "updateUI chamado com usuário nulo")
        }
    }

    /**
     * Inicia o fluxo de login com Google
     */
    private fun signInGoogle() {
        Log.d(TAG, "Iniciando fluxo de Google Sign-In")
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    /**
     * Autentica no Firebase usando a conta Google
     */
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "Autenticando no Firebase com conta Google: ${account.email}")
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login bem-sucedido
                    Log.d(TAG, "Login com Google no Firebase bem-sucedido")
                    updateUI(firebaseAuth.currentUser)
                } else {
                    // Falha no login
                    Log.e(TAG, "Falha na autenticação do Firebase com Google", task.exception)
                    Toast.makeText(
                        this,
                        "Falha na autenticação: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}

