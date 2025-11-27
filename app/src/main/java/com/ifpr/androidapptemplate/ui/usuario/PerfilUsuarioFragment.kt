package com.ifpr.androidapptemplate.ui.usuario

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.ifpr.androidapptemplate.R
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
    private lateinit var changeProfilePhotoButton: Button

    // Referências do Firebase
    private lateinit var usersReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var storageReference: StorageReference

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

        // Inicializa o Firebase Auth e Storage
        auth = FirebaseAuth.getInstance()
        storageReference = FirebaseStorage.getInstance().reference

        // Vincula os componentes da interface
        userProfileImageView = view.findViewById(R.id.userProfileImageView)
        registerNameEditText = view.findViewById(R.id.registerNameEditText)
        registerEmailEditText = view.findViewById(R.id.registerEmailEditText)
        registerBirthDateEditText = view.findViewById(R.id.registerBirthDateEditText)
        registerCepEditText = view.findViewById(R.id.registerCepEditText)
        changeProfilePhotoButton = view.findViewById(R.id.changeProfilePhotoButton)

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
     * Faz upload da foto selecionada para o Firebase Storage
     */
    private fun uploadProfilePhoto() {
        val user = auth.currentUser
        if (user == null || selectedImageUri == null) {
            Toast.makeText(context, "Erro ao fazer upload da foto", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostra mensagem de carregamento
        Toast.makeText(context, "Fazendo upload da foto...", Toast.LENGTH_SHORT).show()

        // Cria referência no Storage para a foto do usuário
        val photoRef = storageReference.child("profile_photos/${user.uid}.jpg")

        // Faz upload da imagem
        photoRef.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                // Upload bem-sucedido, obtém a URL da imagem
                photoRef.downloadUrl.addOnSuccessListener { uri ->
                    // Atualiza a URL da foto no perfil do usuário
                    updateUserPhotoUrl(uri.toString())
                }
            }
            .addOnFailureListener { exception ->
                Log.e("UploadPhoto", "Erro ao fazer upload da foto", exception)
                Toast.makeText(context, "Erro ao fazer upload da foto", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Atualiza a URL da foto no perfil do usuário no Firebase
     */
    private fun updateUserPhotoUrl(photoUrl: String) {
        val user = auth.currentUser ?: return

        // Atualiza no Firebase Database
        usersReference.child(user.uid).child("photoUrl").setValue(photoUrl)
            .addOnSuccessListener {
                Toast.makeText(context, "Foto de perfil atualizada com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("UpdatePhoto", "Erro ao atualizar foto no banco de dados", exception)
                Toast.makeText(context, "Erro ao atualizar foto de perfil", Toast.LENGTH_SHORT).show()
            }
    }
}
