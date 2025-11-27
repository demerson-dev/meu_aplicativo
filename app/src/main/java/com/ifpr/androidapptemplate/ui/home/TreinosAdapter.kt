package com.ifpr.androidapptemplate.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Treino

/**
 * Adapter para exibir a lista de treinos na RecyclerView
 * Ao clicar em um treino, navega para a tela de detalhes
 */
class TreinosAdapter(
    private var treinos: MutableList<Treino> = mutableListOf()
) : RecyclerView.Adapter<TreinosAdapter.TreinoViewHolder>() {

    class TreinoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNomeTreino: TextView = itemView.findViewById(R.id.textNomeTreino)
        val textDetalhes: TextView = itemView.findViewById(R.id.textDetalhes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreinoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_treino, parent, false)
        return TreinoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TreinoViewHolder, position: Int) {
        val treino = treinos[position]

        // Define o nome do treino
        holder.textNomeTreino.text = treino.nomeTreino ?: "Sem nome"

        // Define os detalhes (séries x repetições - número de exercícios)
        val numExercicios = treino.exercicios?.split("\n")?.size ?: 0
        val detalhes = "${treino.series ?: 0}x${treino.repeticoes ?: 0} - $numExercicios exercícios"
        holder.textDetalhes.text = detalhes

        // Configura o clique no item para navegar para detalhes
        holder.itemView.setOnClickListener {
            navegarParaDetalhes(it, treino)
        }
    }

    override fun getItemCount(): Int = treinos.size

    /**
     * Navega para a tela de detalhes do treino passando os dados como argumentos
     */
    private fun navegarParaDetalhes(view: View, treino: Treino) {
        val bundle = Bundle().apply {
            putString("treinoKey", treino.key)
            putString("nomeTreino", treino.nomeTreino)
            putString("exercicios", treino.exercicios)
            putInt("series", treino.series ?: 0)
            putInt("repeticoes", treino.repeticoes ?: 0)
            putString("observacoes", treino.observacoes)
            putString("userId", treino.userId)
        }

        Navigation.findNavController(view).navigate(
            R.id.navigation_notifications,
            bundle
        )
    }

    /**
     * Atualiza a lista de treinos e notifica o adapter
     */
    fun updateTreinos(novosTreinos: List<Treino>) {
        treinos.clear()
        treinos.addAll(novosTreinos)
        notifyDataSetChanged()
    }

    /**
     * Adiciona um treino à lista
     */
    fun addTreino(treino: Treino) {
        treinos.add(0, treino) // Adiciona no início
        notifyItemInserted(0)
    }
}

