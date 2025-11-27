package com.ifpr.androidapptemplate.ui.usuario

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Usuario
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment responsável por exibir e editar o perfil do usuário
 * Mostra foto de perfil, nome e e-mail do usuário logado
 */
class PerfilUsuarioFragment : Fragment() {

    // Componentes da interface
    private lateinit var userProfileImageView: ImageView
    private lateinit var registerNameEditText: EditText
    private lateinit var registerEmailEditText: EditText
    private lateinit var registerBirthDateEditText: EditText
    private lateinit var registerCepEditText: EditText
    private lateinit var registerEnderecoEditText: EditText
    private lateinit var registerPasswordEditText: EditText
    private lateinit var registerConfirmPasswordEditText: EditText
    private lateinit var changeProfilePhotoButton: Button
    private lateinit var registerButton: Button
    private lateinit var sairButton: Button

    // Referências do Firebase
    private lateinit var usersReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // Variáveis para gerenciar a foto
    private var currentPhotoPath: String? = null
    private var selectedImageUri: Uri? = null


    // Launchers para câmera e galeria
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Foto tirada com sucesso
            currentPhotoPath?.let { path ->
                val file = File(path)
                selectedImageUri = Uri.fromFile(file)
                userProfileImageView.setImageURI(selectedImageUri)
                uploadProfilePhoto()
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Imagem selecionada da galeria com sucesso
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                userProfileImageView.setImageURI(selectedImageUri)
                uploadProfilePhoto()
            }
        }
    }

    // Launcher para permissão de câmera
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(context, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para permissão de galeria
    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(context, "Permissão de galeria negada", Toast.LENGTH_SHORT).show()
        }
    }


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
        registerBirthDateEditText = view.findViewById(R.id.registerBirthDateEditText)
        registerCepEditText = view.findViewById(R.id.registerCepEditText)
        registerEnderecoEditText = view.findViewById(R.id.registerEnderecoEditText)
        registerPasswordEditText = view.findViewById(R.id.registerPasswordEditText)
        registerConfirmPasswordEditText = view.findViewById(R.id.registerConfirmPasswordEditText)
        changeProfilePhotoButton = view.findViewById(R.id.changeProfilePhotoButton)
        registerButton = view.findViewById(R.id.salvarButton)
        sairButton = view.findViewById(R.id.sairButton)

        // Configura o clique no botão de alterar foto
        changeProfilePhotoButton.setOnClickListener {
            showPhotoPickerDialog()
        }

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
            // Mostra botão de sair e oculta campos de senha
            sairButton.visibility = View.VISIBLE
            registerPasswordEditText.visibility = View.GONE
            registerConfirmPasswordEditText.visibility = View.GONE

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

        registerButton.setOnClickListener {
            updateUser()
        }

        sairButton.setOnClickListener {
            signOut()
        }

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configura formatação automática de data
        setupDateFormatting()

        // Exibe os dados do usuário logado, se disponível
        val userFirebase = auth.currentUser
        if(userFirebase != null){
            registerNameEditText.setText(userFirebase.displayName)
            registerEmailEditText.setText(userFirebase.email)

            recuperarDadosUsuario(userFirebase.uid)
        }
    }

    /**
     * Configura formatação automática de data (dd/mm/aaaa)
     */
    private fun setupDateFormatting() {
        registerBirthDateEditText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val mask = "##/##/####"

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                if (isUpdating) return

                isUpdating = true

                // Remove caracteres não numéricos
                val unmasked = editable.toString().replace("[^\\d]".toRegex(), "")

                // Aplica a máscara
                val formatted = StringBuilder()
                var i = 0
                for (m in mask.toCharArray()) {
                    if (m != '#' && unmasked.length > i) {
                        formatted.append(m)
                        continue
                    }
                    if (i >= unmasked.length) break
                    formatted.append(unmasked[i])
                    i++
                }

                editable?.replace(0, editable.length, formatted.toString())

                isUpdating = false
            }
        })
    }

    /**
     * Exibe um diálogo para o usuário escolher entre tirar foto ou selecionar da galeria
     */
    private fun showPhotoPickerDialog() {
        val options = arrayOf("Tirar Foto", "Escolher da Galeria", "Cancelar")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Escolha uma opção")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> checkCameraPermissionAndOpen()
                1 -> checkGalleryPermissionAndOpen()
                2 -> dialog.dismiss()
            }
        }
        builder.show()
    }

    /**
     * Verifica permissão de câmera e abre a câmera
     */
    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Verifica permissão de galeria e abre a galeria
     */
    private fun checkGalleryPermissionAndOpen() {
        // Para Android 13+ (API 33+), usa READ_MEDIA_IMAGES
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            else -> {
                galleryPermissionLauncher.launch(permission)
            }
        }
    }

    /**
     * Abre a câmera para tirar uma foto
     */
    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Cria um arquivo para salvar a foto
        try {
            val photoFile = createImageFile()
            photoFile?.let {
                val photoURI: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                takePictureLauncher.launch(takePictureIntent)
            }
        } catch (ex: IOException) {
            Log.e("Camera", "Erro ao criar arquivo de imagem", ex)
            Toast.makeText(context, "Erro ao abrir câmera", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Cria um arquivo temporário para armazenar a foto tirada pela câmera
     */
    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    /**
     * Abre a galeria para selecionar uma foto
     */
    private fun openGallery() {
        val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(pickPhotoIntent)
    }

    /**
     * Converte a foto selecionada em Base64 e salva no Firebase Database
     */
    private fun uploadProfilePhoto() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "Usuário não está logado", Toast.LENGTH_SHORT).show()
            Log.e("UploadPhoto", "Usuário não está logado")
            return
        }

        if (selectedImageUri == null) {
            Toast.makeText(context, "Nenhuma foto selecionada", Toast.LENGTH_SHORT).show()
            Log.e("UploadPhoto", "URI da imagem é nula")
            return
        }

        try {
            // Mostra mensagem de carregamento
            Toast.makeText(context, "Processando foto...", Toast.LENGTH_SHORT).show()
            Log.d("UploadPhoto", "Iniciando conversão da foto para Base64")
            Log.d("UploadPhoto", "URI selecionada: $selectedImageUri")

            // Converte a URI em Bitmap
            val inputStream = requireContext().contentResolver.openInputStream(selectedImageUri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Toast.makeText(context, "Não foi possível processar a imagem", Toast.LENGTH_SHORT).show()
                Log.e("UploadPhoto", "Bitmap é nulo")
                return
            }

            Log.d("UploadPhoto", "Bitmap criado: ${bitmap.width}x${bitmap.height}")

            // Redimensiona a imagem para economizar espaço (máximo 800x800)
            val resizedBitmap = resizeBitmap(bitmap, 800, 800)
            Log.d("UploadPhoto", "Bitmap redimensionado: ${resizedBitmap.width}x${resizedBitmap.height}")

            // Comprime a imagem para JPEG com qualidade 80%
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()

            Log.d("UploadPhoto", "Tamanho da imagem: ${imageBytes.size / 1024} KB")

            // Converte para Base64
            val imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT)
            Log.d("UploadPhoto", "Conversão para Base64 concluída. Tamanho: ${imageBase64.length} caracteres")

            // Salva a imagem Base64 no perfil do usuário
            updateUserPhotoBase64(imageBase64)

            // Limpa da memória
            bitmap.recycle()
            resizedBitmap.recycle()

        } catch (e: Exception) {
            Log.e("UploadPhoto", "Exceção ao processar foto", e)
            Toast.makeText(context, "Erro ao processar foto: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Redimensiona um bitmap mantendo a proporção
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calcula a escala mantendo a proporção
        val scale = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height,
            1.0f // Não aumenta a imagem se já for menor
        )

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Atualiza a foto em Base64 no perfil do usuário no Firebase Database
     * Preserva os outros dados já existentes
     */
    private fun updateUserPhotoBase64(photoBase64: String) {
        val user = auth.currentUser ?: return

        Log.d("UpdatePhoto", "Salvando foto em Base64 para usuário: ${user.uid}")
        Log.d("UpdatePhoto", "Tamanho do Base64: ${photoBase64.length} caracteres")

        // Primeiro, recupera os dados existentes do usuário
        usersReference.child(user.uid).get()
            .addOnSuccessListener { snapshot ->
                val usuario = snapshot.getValue(Usuario::class.java)

                if (usuario != null) {
                    // Atualiza apenas a foto, mantendo os outros dados
                    usuario.photoUrl = photoBase64

                    // Salva de volta no banco
                    usersReference.child(user.uid).setValue(usuario)
                        .addOnSuccessListener {
                            Log.d("UpdatePhoto", "Foto de perfil salva com sucesso no banco!")
                            Toast.makeText(context, "Foto de perfil atualizada com sucesso!", Toast.LENGTH_SHORT).show()

                            // Atualiza a visualização da imagem
                            displayPhotoFromBase64(photoBase64)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("UpdatePhoto", "Erro ao salvar foto no banco de dados", exception)
                            Toast.makeText(context, "Erro ao salvar foto: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Se não existe usuário, cria um novo registro apenas com a foto
                    Log.w("UpdatePhoto", "Usuário não encontrado no banco, salvando apenas photoUrl")
                    usersReference.child(user.uid).child("photoUrl").setValue(photoBase64)
                        .addOnSuccessListener {
                            Log.d("UpdatePhoto", "Foto salva com sucesso!")
                            Toast.makeText(context, "Foto de perfil atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                            displayPhotoFromBase64(photoBase64)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("UpdatePhoto", "Erro ao salvar foto no banco de dados", exception)
                            Toast.makeText(context, "Erro ao salvar foto: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("UpdatePhoto", "Erro ao recuperar dados do usuário", exception)
                // Tenta salvar apenas a foto mesmo assim
                usersReference.child(user.uid).child("photoUrl").setValue(photoBase64)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show()
                        displayPhotoFromBase64(photoBase64)
                    }
                    .addOnFailureListener { ex ->
                        Toast.makeText(context, "Erro ao atualizar foto: ${ex.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    /**
     * Exibe a foto a partir de uma string Base64
     */
    private fun displayPhotoFromBase64(photoBase64: String) {
        try {
            if (photoBase64.isNotEmpty()) {
                val decodedBytes = Base64.decode(photoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                userProfileImageView.setImageBitmap(bitmap)
                Log.d("DisplayPhoto", "Foto exibida com sucesso")
            }
        } catch (e: Exception) {
            Log.e("DisplayPhoto", "Erro ao exibir foto", e)
        }
    }


    /**
     * Recupera os dados do usuário do Firebase Database
     */
    fun recuperarDadosUsuario(usuarioKey: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("users")

        databaseReference.child(usuarioKey).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val usuario = snapshot.getValue(Usuario::class.java)
                    usuario?.let {
                        // Preenche todos os campos do usuário
                        registerEnderecoEditText.setText(it.endereco ?: "")
                        registerCepEditText.setText(it.cep ?: "")
                        registerBirthDateEditText.setText(it.dataNascimento ?: "")

                        // Carrega e exibe a foto em Base64
                        if (!it.photoUrl.isNullOrEmpty()) {
                            Log.d("RecuperarDados", "Carregando foto em Base64")
                            displayPhotoFromBase64(it.photoUrl!!)
                        } else {
                            Log.d("RecuperarDados", "Usuário não possui foto")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Erro ao recuperar dados: ${error.message}")
            }
        })
    }

    /**
     * Atualiza os dados do usuário
     */
    private fun updateUser() {
        val name = registerNameEditText.text.toString().trim()
        val endereco = registerEnderecoEditText.text.toString().trim()
        val cep = registerCepEditText.text.toString().trim()
        val dataNascimento = registerBirthDateEditText.text.toString().trim()

        // Validação básica
        if (name.isEmpty()) {
            Toast.makeText(context, "Por favor, preencha o nome", Toast.LENGTH_SHORT).show()
            return
        }

        // Acessar currentUser
        val user = auth.currentUser

        // Verifica se o usuário atual já está definido
        if (user != null) {
            // Se o usuário já existe, atualiza os dados
            updateProfile(user, name, endereco, cep, dataNascimento)
        } else {
            Toast.makeText(context, "Não foi possível encontrar o usuário logado", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Atualiza o perfil do usuário no Firebase
     */
    private fun updateProfile(user: FirebaseUser?, displayName: String, endereco: String, cep: String, dataNascimento: String) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()

        // Cria objeto usuário com todos os dados
        val usuario = Usuario(
            key = user?.uid.toString(),
            nome = displayName,
            email = user?.email,
            endereco = endereco,
            cep = cep,
            dataNascimento = dataNascimento,
            photoUrl = null // A URL da foto é atualizada separadamente no upload
        )

        user?.updateProfile(profileUpdates)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserToDatabase(usuario)
                    Toast.makeText(context, "Perfil atualizado com sucesso!",
                        Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Não foi possível atualizar o perfil.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Salva os dados do usuário no Firebase Database preservando a foto
     */
    private fun saveUserToDatabase(usuario: Usuario) {
        if (usuario.key != null) {
            // Primeiro busca os dados existentes para preservar a foto
            usersReference.child(usuario.key.toString()).get()
                .addOnSuccessListener { snapshot ->
                    val usuarioExistente = snapshot.getValue(Usuario::class.java)

                    // Preserva a foto existente se houver
                    if (usuarioExistente?.photoUrl != null) {
                        usuario.photoUrl = usuarioExistente.photoUrl
                    }

                    // Agora salva com a foto preservada
                    usersReference.child(usuario.key.toString()).setValue(usuario)
                        .addOnSuccessListener {
                            Log.d("SaveUser", "Usuário atualizado com sucesso! Foto preservada.")
                            Toast.makeText(context, "Usuario atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                        .addOnFailureListener { exception ->
                            Log.e("SaveUser", "Falha ao atualizar o usuario", exception)
                            Toast.makeText(context, "Falha ao atualizar o usuario", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { exception ->
                    Log.e("SaveUser", "Erro ao recuperar dados existentes", exception)
                    // Se falhar ao buscar, salva assim mesmo (mas sem foto)
                    usersReference.child(usuario.key.toString()).setValue(usuario)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Usuario atualizado!", Toast.LENGTH_SHORT).show()
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Falha ao atualizar o usuario", Toast.LENGTH_SHORT).show()
                        }
                }
        } else {
            Toast.makeText(context, "ID invalido", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Faz logout do usuário e retorna para a tela de login
     */
    private fun signOut() {
        auth.signOut()
        Toast.makeText(context, "Você foi desconectado com sucesso!", Toast.LENGTH_SHORT).show()

        // Retorna para a tela de login
        activity?.finish()
    }
}
