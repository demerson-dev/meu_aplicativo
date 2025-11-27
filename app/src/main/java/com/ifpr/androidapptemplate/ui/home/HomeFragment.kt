package com.ifpr.androidapptemplate.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Treino
import com.ifpr.androidapptemplate.databinding.FragmentHomeBinding

/**
 * Fragment responsável por exibir a lista de treinos
 * Carrega os treinos do Firebase e permite criar novos treinos
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var treinosReference: DatabaseReference

    // RecyclerView e Adapter
    private lateinit var recyclerViewTreinos: RecyclerView
    private lateinit var treinosAdapter: TreinosAdapter
    private lateinit var textEmptyState: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Inicializa Firebase
        auth = FirebaseAuth.getInstance()
        treinosReference = FirebaseDatabase.getInstance().getReference("treinos")

        // Inicializa componentes
        recyclerViewTreinos = binding.recyclerViewTreinos
        textEmptyState = binding.textEmptyState
        val btnCriarTreino: Button = binding.btnCriarTreino

        // Configura RecyclerView
        setupRecyclerView()

        // Carrega treinos do Firebase
        carregarTreinos()

        // Configura o botão de criar treino para navegar para a tela de criar treino
        btnCriarTreino.setOnClickListener {
            findNavController().navigate(R.id.navigation_dashboard)
        }

        return root
    }

    /**
     * Configura a RecyclerView com o adapter e layout manager
     */
    private fun setupRecyclerView() {
        treinosAdapter = TreinosAdapter()
        recyclerViewTreinos.apply {
            adapter = treinosAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    /**
     * Carrega os treinos do usuário atual do Firebase
     */
    private fun carregarTreinos() {
        val user = auth.currentUser

        if (user == null) {
            Log.w("ListaTreinos", "Usuário não está logado")
            mostrarEmptyState(true)
            return
        }

        Log.d("ListaTreinos", "Carregando treinos do usuário: ${user.uid}")

        // Busca todos os treinos e filtra pelo userId
        treinosReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val treinos = mutableListOf<Treino>()

                for (treinoSnapshot in snapshot.children) {
                    val treino = treinoSnapshot.getValue(Treino::class.java)
                    // Filtra apenas os treinos do usuário atual
                    treino?.let {
                        if (it.userId == user.uid) {
                            treinos.add(it)
                            Log.d("ListaTreinos", "Treino carregado: ${it.nomeTreino}")
                        }
                    }
                }

                Log.d("ListaTreinos", "Total de treinos carregados: ${treinos.size}")

                if (treinos.isEmpty()) {
                    mostrarEmptyState(true)
                } else {
                    mostrarEmptyState(false)
                    treinosAdapter.updateTreinos(treinos)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ListaTreinos", "Erro ao carregar treinos", error.toException())
                Toast.makeText(
                    context,
                    "Erro ao carregar treinos: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                mostrarEmptyState(true)
            }
        })
    }

    /**
     * Mostra ou esconde a mensagem de lista vazia
     */
    private fun mostrarEmptyState(mostrar: Boolean) {
        if (mostrar) {
            textEmptyState.visibility = View.VISIBLE
            recyclerViewTreinos.visibility = View.GONE
        } else {
            textEmptyState.visibility = View.GONE
            recyclerViewTreinos.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}