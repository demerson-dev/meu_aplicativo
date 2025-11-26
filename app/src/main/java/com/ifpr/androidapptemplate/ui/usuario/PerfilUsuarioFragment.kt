package com.ifpr.androidapptemplate.ui.usuario

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.androidapptemplate.R

/**
 * Fragment responsável por exibir e editar o perfil do usuário
 * Mostra foto de perfil, nome e e-mail do usuário logado
 */
class PerfilUsuarioFragment : Fragment() {

    // Componentes da interface
    private lateinit var userProfileImageView: ImageView
    private lateinit var registerNameEditText: EditText
    private lateinit var registerEmailEditText: EditText

    // Referências do Firebase
    private lateinit var usersReference: DatabaseReference
    private lateinit var auth: FirebaseAuth


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_perfil_usuario, container, false)

        // Inicializa o Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Vincula os componentes da interface
        userProfileImageView = view.findViewById(R.id.userProfileImageView)
        registerNameEditText = view.findViewById(R.id.registerNameEditText)
        registerEmailEditText = view.findViewById(R.id.registerEmailEditText)

        try {
            // Obtém a referência do Firebase Database para os usuários
            usersReference = FirebaseDatabase.getInstance().getReference("users")
        } catch (e: Exception) {
            Log.e("DatabaseReference", "Erro ao obter referência para o Firebase Database", e)
            Toast.makeText(context, "Erro ao acessar o Firebase Database", Toast.LENGTH_SHORT).show()
        }

        // Obtém o usuário atualmente logado
        val user = auth.currentUser

        if (user != null) {
            // Desabilita a edição do e-mail
            registerEmailEditText.isEnabled = false

            // TODO: Carregar foto do perfil usando Glide quando a biblioteca estiver disponível
            // Código comentado temporariamente devido a problemas de sincronização do Gradle
            /*
            user.photoUrl?.let { photoUri ->
                context?.let { ctx ->
                    Glide.with(ctx)
                        .load(photoUri)
                        .circleCrop()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .into(userProfileImageView)
                }
            }
            */
        }

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Exibe os dados do usuário logado, se disponível
        val userFirebase = auth.currentUser
        if(userFirebase != null){
            registerNameEditText.setText(userFirebase.displayName)
            registerEmailEditText.setText(userFirebase.email)
        }
    }
}
