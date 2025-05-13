package com.example.absencestest

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import org.json.JSONObject
import kotlin.math.roundToInt

class HomeFragment : Fragment() {

    private lateinit var spinnerFilieres: Spinner
    private lateinit var spinnerMonths: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var donutChart: PieChart
    private lateinit var recyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    private lateinit var requestQueue: RequestQueue

    private val filiereList = mutableListOf<String>()
    private val filiereIdList = mutableListOf<Int>()
    private val monthList = mutableListOf<String>()
    private val seancesList = mutableListOf<JSONObject>()
    private val presencesMap = mutableMapOf<Int, Int>()
    private var numberOfStudents = 22 // À remplacer par une requête API si disponible
    private var selectedMonth = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        requestQueue = Volley.newRequestQueue(requireContext())

        spinnerFilieres = view.findViewById(R.id.spinnerFilieres)
        spinnerMonths = view.findViewById(R.id.spinnerMonths)
        recyclerView = view.findViewById(R.id.recyclerViewSeances)
        donutChart = view.findViewById(R.id.donutChart)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recyclerViewAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val itemView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_seance, parent, false)
                return SeanceViewHolder(itemView)
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, @SuppressLint("RecyclerView") position: Int) {
                val seance = seancesList[position]
                val holderView = holder as SeanceViewHolder

                // Safe handling of JSON fields
                holderView.professeur.text = seance.optString("professeur", "N/A")
                holderView.module.text = seance.optString("module", "N/A")
                holderView.salle.text = seance.optString("salle", "N/A")
                holderView.date.text = seance.optString("date", "N/A")
                holderView.heureDebut.text = seance.optString("heure_debut", "N/A")
                holderView.heureFin.text = seance.optString("heure_fin", "N/A")

                // Listener pour supprimer
                holderView.iconDelete.setOnClickListener {
                    val seanceId = seance.optInt("id", -1)
                    if (seanceId == -1) {
                        Toast.makeText(requireContext(), "Erreur: ID de séance invalide", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val deleteUrl = "http://192.168.43.18:5000/seance/$seanceId"

                    val deleteRequest = object : StringRequest(
                        Method.DELETE, deleteUrl,
                        { response ->
                            seancesList.removeAt(position)
                            recyclerViewAdapter.notifyItemRemoved(position)
                            recyclerViewAdapter.notifyItemRangeChanged(position, seancesList.size)
                            Toast.makeText(requireContext(), "Séance supprimée", Toast.LENGTH_SHORT).show()
                            // Mettre à jour le chart et le tableau
                            fetchNumberOfStudents(filiereIdList[spinnerFilieres.selectedItemPosition]) {
                                fetchSeancesByFiliere(filiereIdList[spinnerFilieres.selectedItemPosition])
                            }
                        },
                        { error ->
                            Toast.makeText(requireContext(), "Erreur suppression", Toast.LENGTH_SHORT).show()
                            Log.e("DashboardFragment", "Erreur DELETE: ${error.message}")
                        }
                    ) {}
                    requestQueue.add(deleteRequest)
                }

                // Listener pour scanner
                holderView.iconScan.setOnClickListener {
                    val seanceId = seance.optInt("id", -1)
                    if (seanceId == -1) {
                        Toast.makeText(requireContext(), "Erreur: ID de séance invalide", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val intent = Intent(requireContext(), CameraActivity::class.java)
                    intent.putExtra("SEANCE_ID", seanceId)
                    startActivity(intent)
                }

                // Listener pour afficher les étudiants présents
                holderView.etudiantsP.setOnClickListener {
                    val seanceId = seance.optInt("id", -1)
                    if (seanceId == -1) {
                        Toast.makeText(requireContext(), "Erreur: ID de séance invalide", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    fetchEtudiantsPresents(seanceId)
                }
            }

            override fun getItemCount(): Int = seancesList.size
        }

        recyclerView.adapter = recyclerViewAdapter
        fetchFilieres()

        return view
    }

    private fun fetchFilieres() {
        val url = "http://192.168.43.18:5000/filieres"
        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                filiereList.clear()
                filiereIdList.clear()
                for (i in 0 until response.length()) {
                    val filiere = response.getJSONObject(i)
                    filiereList.add(filiere.optString("nom", "Unknown"))
                    filiereIdList.add(filiere.optInt("id", -1))
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filiereList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerFilieres.adapter = adapter

                spinnerFilieres.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val selectedFiliereId = filiereIdList[position]
                        if (selectedFiliereId != -1) {
                            fetchNumberOfStudents(selectedFiliereId) {
                                fetchSeancesByFiliere(selectedFiliereId)
                            }
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            },
            { error ->
                Log.e("DashboardFragment", "Erreur filières: ${error.message}")
                Toast.makeText(requireContext(), "Erreur lors de la récupération des filières", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(request)
    }

    private fun fetchNumberOfStudents(filiereId: Int, callback: () -> Unit) {
        // Endpoint hypothétique pour le nombre d'étudiants
        // Remplacer par le vrai endpoint, par exemple: "http://192.168.43.18:5000/filieres/$filiereId/etudiants"
        numberOfStudents = 22 // À remplacer par une requête API si disponible
        callback()
    }

    private fun fetchSeancesByFiliere(filiereId: Int) {
        val url = "http://192.168.43.18:5000/seances/filiere/$filiereId"
        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                seancesList.clear()
                presencesMap.clear()
                val monthsSet = mutableSetOf<String>()
                var fetchedCount = 0

                for (i in 0 until response.length()) {
                    val seanceObject = response.getJSONObject(i)
                    seancesList.add(seanceObject)
                    val date = seanceObject.optString("date", "")
                    if (date.length >= 7) {
                        val yearMonth = date.substring(0, 7) // YYYY-MM
                        monthsSet.add(yearMonth)
                    }

                    // Récupérer les présences pour chaque séance
                    val seanceId = seanceObject.optInt("id", -1)
                    if (seanceId != -1) {
                        fetchPresencesForSeance(seanceId) {
                            fetchedCount++
                            if (fetchedCount == seancesList.size) {
                                populateMonths(monthsSet)
                                if (monthList.isNotEmpty()) {
                                    spinnerMonths.setSelection(0)
                                    selectedMonth = monthList[0]
                                    updateDonutChart()
                                } else {
                                    donutChart.clear()
                                }
                            }
                        }
                    } else {
                        fetchedCount++
                    }
                }
                recyclerViewAdapter.notifyDataSetChanged()
            },
            { error ->
                Log.e("DashboardFragment", "Erreur séances: ${error.message}")
                Toast.makeText(requireContext(), "Erreur lors de la récupération des séances", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(request)
    }

    private fun fetchPresencesForSeance(seanceId: Int, callback: () -> Unit) {
        val url = "http://192.168.43.18:5000/seance/$seanceId/etudiants_presents"
        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                presencesMap[seanceId] = response.length()
                callback()
            },
            { error ->
                Log.e("DashboardFragment", "Erreur présences: ${error.message}")
                presencesMap[seanceId] = 0 // En cas d'erreur, supposer 0 présents
                callback()
            }
        )
        requestQueue.add(request)
    }

    private fun populateMonths(monthsSet: Set<String>) {
        monthList.clear()
        monthList.addAll(monthsSet.sorted())
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, monthList)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonths.adapter = monthAdapter

        spinnerMonths.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMonth = monthList[position]
                updateDonutChart()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedMonth = ""
                donutChart.clear()
            }
        }
    }

    private fun fetchEtudiantsPresents(seanceId: Int) {
        val url = "http://192.168.43.18:5000/seance/$seanceId/etudiants_presents"
        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                val etudiants = mutableListOf<String>()
                for (i in 0 until response.length()) {
                    etudiants.add(response.optString(i, "Unknown"))
                }
                showEtudiantsDialog(etudiants)
            },
            { error ->
                Log.e("DashboardFragment", "Erreur : ${error.message}")
                Toast.makeText(requireContext(), "Erreur lors de la récupération des présences", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(request)
    }

    private fun showEtudiantsDialog(etudiants: List<String>) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Étudiants Présents")
        builder.setItems(etudiants.toTypedArray(), null)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun updateDonutChart() {
        // Filtrer les séances pour le mois sélectionné
        val filteredSessions = seancesList.filter { it.optString("date", "").startsWith(selectedMonth) }

        // Grouper par module
        val sessionsByModule = filteredSessions.groupBy { it.optString("module", "Unknown") }

        // Calculer les absences par module
        val entries = mutableListOf<PieEntry>()
        for ((module, moduleSessions) in sessionsByModule) {
            var totalAbsences = 0
            val numberOfSessions = moduleSessions.size
            for (session in moduleSessions) {
                val seanceId = session.optInt("id", -1)
                val presences = presencesMap[seanceId] ?: 0
                val absences = numberOfStudents - presences
                totalAbsences += absences
            }
            val totalPossibleAttendances = numberOfSessions * numberOfStudents
            val absencePercentage = if (totalPossibleAttendances > 0) {
                (totalAbsences.toFloat() / totalPossibleAttendances * 100).roundToInt()
            } else {
                0
            }
            entries.add(PieEntry(absencePercentage.toFloat(), "$module: $absencePercentage%"))
        }

        // Configurer le donut chart
        val dataSet = PieDataSet(entries, "Absences par module")
        dataSet.colors = listOf(
            Color.parseColor("#735557"),
            Color.parseColor("#74512D"),
            Color.parseColor("#4E1F00"),
            Color.parseColor("#B17F59"),
            Color.parseColor("#706D54")
        )
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK

        val data = PieData(dataSet)
        donutChart.data = data

        // Configuration du donut chart
        donutChart.description.isEnabled = false
        donutChart.setHoleColor(Color.WHITE)
        donutChart.holeRadius = 50f // Taille du trou pour l'effet donut
        donutChart.setDrawEntryLabels(false)
        donutChart.legend.isEnabled = true
        donutChart.legend.textColor = Color.BLACK
        donutChart.setEntryLabelColor(Color.BLACK)
        donutChart.setEntryLabelTextSize(12f) // Corrected: Use method call with parentheses
        donutChart.animateY(1000) // Animation
        donutChart.invalidate() // Rafraîchir le chart
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

    override fun onDestroyView() {
        super.onDestroyView()
        requestQueue.stop()
    }
}