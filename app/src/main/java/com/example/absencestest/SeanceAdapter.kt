/* package com.example.absencestest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class SeanceAdapter(seanceList: List<Seance>) :
    RecyclerView.Adapter<SeanceAdapter.SeanceViewHolder>() {
    private val seanceList: List<Seance> = seanceList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeanceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_seance, parent, false)
        return SeanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeanceViewHolder, position: Int) {
        val s: Seance = seanceList[position]
        holder.textViewInfos.text =
            "Prof: " + s.professeur + " | Module: " + s.module + " | Salle: " + s.salle + " | Filière: " + s.filiere
        holder.textViewDateHeure.text =
            "Date: " + s.date + " | Début: " + s.heureDebut + " | Fin: " + s.heureFin
    }

    override fun getItemCount(): Int {
        return seanceList.size
    }

    class SeanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewInfos: TextView = itemView.findViewById(R.id.textViewInfos)
        var textViewDateHeure: TextView = itemView.findViewById(R.id.textViewDateHeure)
    }
}
*/