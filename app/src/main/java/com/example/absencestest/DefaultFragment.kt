package com.example.absencestest

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class DefaultFragment : Fragment() {

    private val TAG = "DefaultFragment"
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    private val seancesList = mutableListOf<JSONObject>()
    private var profId: Int = -1


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Ici tu lies le layout du fragment
        val view = inflater.inflate(R.layout.fragment_list_seance_scan, container, false)

        profId = arguments?.getInt("prof_id") ?: -1
        recyclerView = view.findViewById(R.id.recyclerViewSeances)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recyclerViewAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_seance, parent, false)
                return SeanceViewHolder(itemView)
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val seance = seancesList[position]
                val holderView = holder as SeanceViewHolder

                holderView.professeur.text = seance.getString("professeur")
                holderView.module.text = seance.getString("module")
                holderView.salle.text = seance.getString("salle")
                holderView.date.text = seance.getString("date")
                holderView.heureDebut.text = seance.getString("heure_debut")
                holderView.heureFin.text = seance.getString("heure_fin")

                holderView.iconDelete.setOnClickListener {
                    val seanceId = seance.getInt("id")
                    val deleteUrl = "http://192.168.43.18:5000/seance/$seanceId"

                    val requestQueue = Volley.newRequestQueue(requireContext())

                    val deleteRequest = object : com.android.volley.toolbox.StringRequest(
                        Method.DELETE, deleteUrl,
                        { response ->
                            seancesList.removeAt(position)
                            notifyItemRemoved(position)
                            notifyItemRangeChanged(position, seancesList.size)
                            Toast.makeText(requireContext(), "Séance supprimée", Toast.LENGTH_SHORT).show()
                        },
                        { error ->
                            Toast.makeText(requireContext(), "Erreur suppression", Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Erreur DELETE: ${error.message}")
                        }
                    ) {}

                    requestQueue.add(deleteRequest)
                }
                holderView.iconScan.setOnClickListener {
                    val seanceId = seance.getInt("id")
                    val intent = Intent(requireContext(), CameraActivity::class.java)
                    intent.putExtra("SEANCE_ID", seanceId) // Passage de l'ID de la séance
                    startActivity(intent)
                }
                holderView.etudiantsP.setOnClickListener {
                    val seanceId = seance.getInt("id")
                    fetchEtudiantsPresents(seanceId)
                }
            }

            override fun getItemCount(): Int = seancesList.size
        }

        recyclerView.adapter = recyclerViewAdapter

        // Charger les données
        fetchSeances()

        return view
    }
    private fun fetchEtudiantsPresents(seanceId: Int) {
        val url = "http://192.168.43.18:5000/seance/$seanceId/etudiants_presents"
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
                Toast.makeText(requireContext(), "Erreur lors de la récupération des présences", Toast.LENGTH_SHORT).show()

            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun showEtudiantsDialog(etudiants: List<String>) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Étudiants Présents")
        builder.setItems(etudiants.toTypedArray(), null)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun fetchSeances() {
        val url = "http://192.168.43.18:5000/seance/professeur/$profId"
        Log.d("URL_DEBUG", url)
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
                Toast.makeText(requireContext(), "Erreur lors de la récupération des données", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
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
