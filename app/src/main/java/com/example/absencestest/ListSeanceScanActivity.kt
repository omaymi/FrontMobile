package com.example.absencestest

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

class ListSeanceScanActivity : AppCompatActivity() {

    private val TAG = "ListSeanceScanActivity"
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    private val seancesList = mutableListOf<JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_list_seance_scan) // tu peux le renommer si tu veux

        recyclerView = findViewById(R.id.recyclerViewSeances)
        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerViewAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val itemView = LayoutInflater.from(this@ListSeanceScanActivity)
                    .inflate(R.layout.item_seance, parent, false)
                return SeanceViewHolder(itemView)
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, @SuppressLint("RecyclerView") position: Int) {
                val seance = seancesList[position]
                val holderView = holder as SeanceViewHolder

                holderView.professeur.text = seance.getString("professeur")
                holderView.module.text = seance.getString("module")
                holderView.salle.text = seance.getString("salle")
                holderView.date.text = seance.getString("date")
                holderView.heureDebut.text = seance.getString("heure_debut")
                holderView.heureFin.text = seance.getString("heure_fin")

                holderView.iconDelete.setOnClickListener {
                    val seanceId = seance.getInt("id") // Assure-toi que chaque séance a un champ "id"
                    val deleteUrl = "http://192.168.0.103:5000/seance/$seanceId"

                    val requestQueue = Volley.newRequestQueue(this@ListSeanceScanActivity)

                    val deleteRequest = object : com.android.volley.toolbox.StringRequest(
                        Method.DELETE, deleteUrl,
                        { response ->
                            // Supprimer de la liste
                            seancesList.removeAt(position)
                            notifyItemRemoved(position)
                            notifyItemRangeChanged(position, seancesList.size)
                            Toast.makeText(this@ListSeanceScanActivity, "Séance supprimée", Toast.LENGTH_SHORT).show()
                        },
                        { error ->
                            Toast.makeText(this@ListSeanceScanActivity, "Erreur suppression", Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Erreur DELETE: ${error.message}")
                        }
                    ) {}

                    requestQueue.add(deleteRequest)
                }

            }

            override fun getItemCount(): Int = seancesList.size
        }

        recyclerView.adapter = recyclerViewAdapter
        fetchSeances()
    }

    private fun fetchSeances() {
        val url = "http://192.168.0.103:5000/seance"

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                for (i in 0 until response.length()) {
                    val seanceObject = response.getJSONObject(i)
                    seancesList.add(seanceObject)
                }
                recyclerViewAdapter.notifyDataSetChanged()
            },
            { error ->
                Log.e(TAG, "Erreur de chargement: ${error.message}")
                Toast.makeText(this, "Erreur lors de la récupération des données", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    inner class SeanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val professeur = itemView.findViewById<TextView>(R.id.professeur)
        val module = itemView.findViewById<TextView>(R.id.module)
        val salle = itemView.findViewById<TextView>(R.id.salle)
        val date = itemView.findViewById<TextView>(R.id.date)
        val heureDebut = itemView.findViewById<TextView>(R.id.heureDebut)
        val heureFin = itemView.findViewById<TextView>(R.id.heureFin)
        val iconDelete = itemView.findViewById<ImageView>(R.id.iconDelete)

    }
}
