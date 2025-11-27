package com.ifpr.androidapptemplate.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Treino
import com.ifpr.androidapptemplate.databinding.FragmentDashboardBinding

/**
 * Fragment responsável por criar e salvar novos treinos
 * Permite ao usuário definir nome, exercícios, séries, repetições e observações
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // Esta propriedade só é válida entre onCreateView e onDestroyView
    private val binding get() = _binding!!

    // Referências do Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var treinosReference: DatabaseReference

    // Campos do formulário
    private lateinit var editNomeTreino: TextInputEditText
    private lateinit var editExercicios: TextInputEditText
    private lateinit var editSeries: TextInputEditText
    private lateinit var editRepeticoes: TextInputEditText
    private lateinit var editObservacoes: TextInputEditText
    private lateinit var btnSalvarTreino: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Inicializa Firebase
        auth = FirebaseAuth.getInstance()
        treinosReference = FirebaseDatabase.getInstance().getReference("treinos")

        // Vincula os campos do formulário
        editNomeTreino = binding.editNomeTreino
        editExercicios = binding.editExercicios
        editSeries = binding.editSeries
        editRepeticoes = binding.editRepeticoes
        editObservacoes = binding.editObservacoes
        btnSalvarTreino = binding.btnSalvarTreino

        // Configura o botão de salvar treino
        btnSalvarTreino.setOnClickListener {
            salvarTreino()
        }

        return root
    }

    /**
     * Salva o treino no Firebase Database
     */
    private fun salvarTreino() {
        // Obtém os valores dos campos
        val nomeTreino = editNomeTreino.text.toString().trim()
        val exercicios = editExercicios.text.toString().trim()
        val seriesStr = editSeries.text.toString().trim()
        val repeticoesStr = editRepeticoes.text.toString().trim()
        val observacoes = editObservacoes.text.toString().trim()

        // Validações
        if (nomeTreino.isEmpty()) {
            Toast.makeText(context, "Por favor, preencha o nome do treino", Toast.LENGTH_SHORT).show()
            editNomeTreino.requestFocus()
            return
        }

        if (exercicios.isEmpty()) {
            Toast.makeText(context, "Por favor, adicione os exercícios", Toast.LENGTH_SHORT).show()
            editExercicios.requestFocus()
            return
        }

        if (seriesStr.isEmpty()) {
            Toast.makeText(context, "Por favor, informe o número de séries", Toast.LENGTH_SHORT).show()
            editSeries.requestFocus()
            return
        }

        if (repeticoesStr.isEmpty()) {
            Toast.makeText(context, "Por favor, informe o número de repetições", Toast.LENGTH_SHORT).show()
            editRepeticoes.requestFocus()
            return
        }

        // Converte séries e repetições para número
        val series = seriesStr.toIntOrNull()
        val repeticoes = repeticoesStr.toIntOrNull()

        if (series == null || series <= 0) {
            Toast.makeText(context, "Número de séries inválido", Toast.LENGTH_SHORT).show()
            return
        }

        if (repeticoes == null || repeticoes <= 0) {
            Toast.makeText(context, "Número de repetições inválido", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtém o usuário atual
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "Você precisa estar logado para criar um treino", Toast.LENGTH_SHORT).show()
            return
        }

        // Gera uma chave única para o treino
        val treinoKey = treinosReference.push().key

        if (treinoKey == null) {
            Toast.makeText(context, "Erro ao gerar ID do treino", Toast.LENGTH_SHORT).show()
            return
        }

        // Cria o objeto Treino
        val treino = Treino(
            key = treinoKey,
            userId = user.uid,
            nomeTreino = nomeTreino,
            exercicios = exercicios,
            series = series,
            repeticoes = repeticoes,
            observacoes = observacoes
        )

        // Salva no Firebase Database
        treinosReference.child(treinoKey).setValue(treino)
            .addOnSuccessListener {
                Log.d("CriarTreino", "Treino salvo com sucesso: $treinoKey")
                Toast.makeText(context, "Treino criado com sucesso!", Toast.LENGTH_SHORT).show()

                // Limpa os campos
                limparCampos()

                // Volta para a lista de treinos
                findNavController().navigate(R.id.navigation_home)
            }
            .addOnFailureListener { exception ->
                Log.e("CriarTreino", "Erro ao salvar treino", exception)
                Toast.makeText(context, "Erro ao criar treino: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Limpa todos os campos do formulário
     */
    private fun limparCampos() {
        editNomeTreino.text?.clear()
        editExercicios.text?.clear()
        editSeries.text?.clear()
        editRepeticoes.text?.clear()
        editObservacoes.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}