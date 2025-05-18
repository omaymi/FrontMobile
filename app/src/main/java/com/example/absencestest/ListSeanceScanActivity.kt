package com.example.absencestest

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
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
        setContentView(R.layout.fragment_list_seance_scan)

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

                // Listener pour supprimer
                holderView.iconDelete.setOnClickListener {
                    val seanceId = seance.getInt("id")
                    val deleteUrl = "http://100.70.32.157:5000/seance/$seanceId"

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

                // Listener pour scanner
                holderView.iconScan.setOnClickListener {
                    val seanceId = seance.getInt("id")
                    val intent = Intent(this@ListSeanceScanActivity, CameraActivity::class.java)
                    intent.putExtra("SEANCE_ID", seanceId) // Passage de l'ID de la séance
                    startActivity(intent)
                }

                // Listener pour afficher les étudiants présents
                holderView.etudiantsP.setOnClickListener {
                    val seanceId = seance.getInt("id")
                    fetchEtudiantsPresents(seanceId)
                }

            }

            override fun getItemCount(): Int = seancesList.size
        }

        recyclerView.adapter = recyclerViewAdapter
        fetchSeances()
    }

    private fun fetchEtudiantsPresents(seanceId: Int) {
        val url = "http://100.70.32.157:5000/seance/$seanceId/etudiants_presents"
        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                val etudiants = mutableListOf<String>()
                for (i in 0 until response.length()) {
                    etudiants.add(response.getString(i))
                }
                showEtudiantsDialog(etudiants)
            },
            { error ->
                Log.e("ListSeanceScanActivity", "Erreur : ${error.message}")
                Toast.makeText(this, "Erreur lors de la récupération des présences", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun showEtudiantsDialog(etudiants: List<String>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Étudiants Présents")
        builder.setItems(etudiants.toTypedArray(), null)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }


    private fun fetchSeances() {
        val url = "http://100.70.32.157:5000/seance"

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
        val iconScan = itemView.findViewById<ImageView>(R.id.iconScan)
        val etudiantsP = itemView.findViewById<TextView>(R.id.etudiantsP)

    }
}