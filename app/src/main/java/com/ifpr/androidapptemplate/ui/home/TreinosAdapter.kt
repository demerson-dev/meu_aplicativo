package com.ifpr.androidapptemplate.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Treino

/**
 * Adapter para exibir a lista de treinos na RecyclerView
 */
class TreinosAdapter(
    private var treinos: MutableList<Treino> = mutableListOf()
) : RecyclerView.Adapter<TreinosAdapter.TreinoViewHolder>() {

    // Interface para callback de cliques
    interface OnTreinoClickListener {
        fun onTreinoClick(treino: Treino)
    }

    private var clickListener: OnTreinoClickListener? = null

    fun setOnTreinoClickListener(listener: OnTreinoClickListener) {
        clickListener = listener
    }

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

        // Define os detalhes (séries x repetições)
        val detalhes = "${treino.series ?: 0}x${treino.repeticoes ?: 0} - ${treino.exercicios?.split("\n")?.size ?: 0} exercícios"
        holder.textDetalhes.text = detalhes

        // Configura o clique no item
        holder.itemView.setOnClickListener {
            clickListener?.onTreinoClick(treino)
        }
    }

    override fun getItemCount(): Int = treinos.size

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

