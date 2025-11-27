package com.ifpr.androidapptemplate.ui.notifications

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Treino
import com.ifpr.androidapptemplate.databinding.FragmentNotificationsBinding

/**
 * Fragment responsável por exibir os detalhes de um treino
 * Mostra todas as informações e permite excluir o treino
 */
class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private lateinit var treinosReference: DatabaseReference

    // Treino atual sendo exibido
    private var treinoAtual: Treino? = null

    // Componentes da interface
    private lateinit var btnVoltar: ImageButton
    private lateinit var textNomeTreino: TextView
    private lateinit var textSeries: TextView
    private lateinit var textRepeticoes: TextView
    private lateinit var textExercicios: TextView
    private lateinit var textObservacoes: TextView
    private lateinit var labelObservacoes: TextView
    private lateinit var btnExcluirTreino: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Inicializa Firebase
        treinosReference = FirebaseDatabase.getInstance().getReference("treinos")

        // Vincula componentes
        btnVoltar = binding.btnVoltar
        textNomeTreino = binding.textNomeTreino
        textSeries = binding.textSeries
        textRepeticoes = binding.textRepeticoes
        textExercicios = binding.textExercicios
        textObservacoes = binding.textObservacoes
        labelObservacoes = binding.labelObservacoes
        btnExcluirTreino = binding.btnExcluirTreino

        // Configura botão de voltar
        btnVoltar.setOnClickListener {
            voltarParaLista()
        }

        // Recebe o treino passado como argumento
        receberTreino()

        // Configura botão de excluir
        btnExcluirTreino.setOnClickListener {
            confirmarExclusao()
        }

        return root
    }

    /**
     * Recebe o objeto Treino passado como argumento da navegação
     */
    private fun receberTreino() {
        // Por enquanto, vamos usar argumentos do Bundle
        // TODO: Implementar Safe Args para passar objetos entre fragments

        arguments?.let { bundle ->
            val treinoKey = bundle.getString("treinoKey")
            val nomeTreino = bundle.getString("nomeTreino")
            val exercicios = bundle.getString("exercicios")
            val series = bundle.getInt("series", 0)
            val repeticoes = bundle.getInt("repeticoes", 0)
            val observacoes = bundle.getString("observacoes")
            val userId = bundle.getString("userId")

            // Reconstrói o objeto Treino
            treinoAtual = Treino(
                key = treinoKey,
                userId = userId,
                nomeTreino = nomeTreino,
                exercicios = exercicios,
                series = series,
                repeticoes = repeticoes,
                observacoes = observacoes
            )

            // Exibe as informações
            exibirDetalhes()
        }
    }

    /**
     * Exibe os detalhes do treino na tela
     */
    private fun exibirDetalhes() {
        treinoAtual?.let { treino ->
            textNomeTreino.text = treino.nomeTreino ?: "Sem nome"
            textSeries.text = treino.series?.toString() ?: "0"
            textRepeticoes.text = treino.repeticoes?.toString() ?: "0"
            textExercicios.text = treino.exercicios ?: "Nenhum exercício"

            // Mostra observações apenas se houver
            if (!treino.observacoes.isNullOrEmpty()) {
                textObservacoes.text = treino.observacoes
                textObservacoes.visibility = View.VISIBLE
                labelObservacoes.visibility = View.VISIBLE
            } else {
                textObservacoes.visibility = View.GONE
                labelObservacoes.visibility = View.GONE
            }

            Log.d("DetalhesTreino", "Exibindo treino: ${treino.nomeTreino}")
        }
    }

    /**
     * Volta para a tela de lista de treinos
     */
    private fun voltarParaLista() {
        findNavController().navigate(R.id.navigation_home)
    }

    /**
     * Mostra dialog de confirmação antes de excluir
     */
    private fun confirmarExclusao() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.btn_excluir) { dialog, _ ->
                excluirTreino()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.btn_cancelar) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Exclui o treino do Firebase Database
     */
    private fun excluirTreino() {
        val treino = treinoAtual

        if (treino?.key == null) {
            Toast.makeText(context, "Erro: Treino inválido", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("ExcluirTreino", "Excluindo treino: ${treino.key}")

        // Remove do Firebase
        treinosReference.child(treino.key!!).removeValue()
            .addOnSuccessListener {
                Log.d("ExcluirTreino", "Treino excluído com sucesso!")
                Toast.makeText(
                    context,
                    R.string.treino_excluido_sucesso,
                    Toast.LENGTH_SHORT
                ).show()

                // Volta para a lista de treinos
                findNavController().navigate(R.id.navigation_home)
            }
            .addOnFailureListener { exception ->
                Log.e("ExcluirTreino", "Erro ao excluir treino", exception)
                Toast.makeText(
                    context,
                    getString(R.string.erro_excluir_treino) + ": ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}