package com.example.absencestest

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.Calendar

class SeanceFragment : Fragment() {

    private lateinit var btnSeance: Button
    private lateinit var dateEditText: EditText
    private lateinit var heureDebutEditText: EditText
    private lateinit var heureFinEditText: EditText
    private lateinit var spinnerFilieres: Spinner
    private lateinit var spinnerProfesseur: Spinner
    private lateinit var spinnerModules: Spinner
    private lateinit var spinnerSalles: Spinner
    private lateinit var btnCheckSalles: Button
    private lateinit var btnListSeance: Button
    private lateinit var requestQueue: RequestQueue

    private val filiereList = mutableListOf<String>()
    private val filiereIdList = mutableListOf<Int>()
    private val moduleList = mutableListOf<String>()
    private val moduleIdList = mutableListOf<Int>()
    private val professeurList = mutableListOf<String>()
    private val professeurIdList = mutableListOf<Int>()
    private val salleList = mutableListOf<String>()

    private var selectedSalle: String? = null
    private val TAG = "SeanceFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_seance, container, false)
        requestQueue = Volley.newRequestQueue(requireContext())

        // Initialisation des vues
        initViews(view)
        setupDateTimePickers()
        setupListeners()
        fetchFilieres()

        return view
    }

    private fun initViews(view: View) {
        btnSeance = view.findViewById(R.id.btnValiderSeance)
        dateEditText = view.findViewById(R.id.editDateSeance)
        heureDebutEditText = view.findViewById(R.id.editHeureDebut)
        heureFinEditText = view.findViewById(R.id.editHeureFin)
        spinnerFilieres = view.findViewById(R.id.spinnerFilieres)
        spinnerModules = view.findViewById(R.id.spinnerModules)
        spinnerProfesseur = view.findViewById(R.id.spinnerProfessor)
        spinnerSalles = view.findViewById(R.id.spinnerSalle)
        btnCheckSalles = view.findViewById(R.id.btnCheckSalles)
        btnListSeance = view.findViewById(R.id.btnListSeances)
    }

    private fun setupDateTimePickers() {
        dateEditText.setOnClickListener { showDatePicker() }
        heureDebutEditText.setOnClickListener { showTimePicker(heureDebutEditText) }
        heureFinEditText.setOnClickListener { showTimePicker(heureFinEditText) }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Format: AAAA-MM-JJ
                val formattedDate = String.format(
                    "%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1, // Les mois commencent à 0
                    selectedDay
                )
                dateEditText.setText(formattedDate)
            },
            year,
            month,
            day
        ).show()
    }

    private fun showTimePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                // Format: HH:MM:SS (secondes fixes à 00)
                val formattedTime = String.format(
                    "%02d:%02d:00", // Secondes fixées à 00
                    selectedHour,
                    selectedMinute
                )
                editText.setText(formattedTime)
            },
            hour,
            minute,
            true // Format 24h
        ).show()
    }

    private fun setupListeners() {
        spinnerFilieres.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                fetchModulesByFiliere(filiereIdList[pos])
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        spinnerModules.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos != Spinner.INVALID_POSITION) {
                    val filiereId = filiereIdList[spinnerFilieres.selectedItemPosition]
                    val moduleId = moduleIdList[pos]
                    fetchProfesseurs(filiereId, moduleId)
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        spinnerSalles.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedSalle = salleList.getOrNull(pos)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnCheckSalles.setOnClickListener { checkSallesDisponibles() }
        btnSeance.setOnClickListener { creerSeance() }
        btnListSeance.setOnClickListener { lancerListeSeances() }
    }

    private fun fetchFilieres() {
        val url = "http://192.168.43.18:5000/filieres"
        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                filiereList.clear()
                filiereIdList.clear()
                for (i in 0 until response.length()) {
                    val filiere = response.getJSONObject(i)
                    filiereList.add(filiere.getString("nom"))
                    filiereIdList.add(filiere.getInt("id"))
                }
                spinnerFilieres.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filiereList)
            },
            { error -> Log.e(TAG, "Erreur filières: ${error.message}") }
        )
        requestQueue.add(request)
    }

    private fun fetchModulesByFiliere(filiereId: Int) {
        val url = "http://192.168.43.18:5000/modules/filiere/$filiereId"
        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                moduleList.clear()
                moduleIdList.clear()
                for (i in 0 until response.length()) {
                    val module = response.getJSONObject(i)
                    moduleList.add(module.getString("nom"))
                    moduleIdList.add(module.getInt("id"))
                }
                spinnerModules.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, moduleList)
            },
            { error -> Log.e(TAG, "Erreur modules: ${error.message}") }
        )
        requestQueue.add(request)
    }

    private fun fetchProfesseurs(filiereId: Int, moduleId: Int) {
        val url = "http://192.168.43.18:5000/professeurs?filiere_id=$filiereId&module_id=$moduleId"
        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                professeurList.clear()
                professeurIdList.clear()

                for (i in 0 until response.length()) {
                    try {
                        val prof = response.getJSONObject(i)
                        professeurList.add(prof.getString("nom"))
                        professeurIdList.add(prof.getInt("id"))
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur parsing professeur: ${e.message}")
                    }
                }

                // Forcer le rafraîchissement de l'UI
                activity?.runOnUiThread {
                    spinnerProfesseur.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        professeurList
                    ).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }

                    if (professeurList.isNotEmpty()) {
                        spinnerProfesseur.setSelection(0)
                    } else {
                        Toast.makeText(requireContext(), "Aucun professeur trouvé", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            { error ->
                Log.e(TAG, "Erreur professeurs: ${error.message}")
                Toast.makeText(requireContext(), "Erreur de chargement des professeurs", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(request)

    }

    private fun checkSallesDisponibles() {
        val filiereId = filiereIdList.getOrNull(spinnerFilieres.selectedItemPosition)
        val date = dateEditText.text.toString()
        val debut = heureDebutEditText.text.toString()
        val fin = heureFinEditText.text.toString()

        if (filiereId == null || date.isEmpty() || debut.isEmpty() || fin.isEmpty()) {
            Toast.makeText(requireContext(), "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://192.168.43.18:5000/salles/disponibles?filiere_id=$filiereId&date=$date&heure_debut=$debut&heure_fin=$fin"
        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                salleList.clear()
                for (i in 0 until response.length()) {
                    salleList.add(response.getJSONObject(i).getString("nom"))
                }

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, salleList)
                spinnerSalles.adapter = adapter

                if (salleList.isNotEmpty()) {
                    spinnerSalles.setSelection(0)
                    selectedSalle = salleList[0]
                } else {
                    selectedSalle = null
                }
            },
            { error ->
                Log.e(TAG, "Erreur récupération salles: ${error.message}")
                Toast.makeText(requireContext(), "Erreur récupération salles", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(request)
    }

    private fun creerSeance() {
        val professeurId = professeurIdList.getOrNull(spinnerProfesseur.selectedItemPosition)
        val moduleId = moduleIdList.getOrNull(spinnerModules.selectedItemPosition)
        val date = dateEditText.text.toString()
        val debut = heureDebutEditText.text.toString()
        val fin = heureFinEditText.text.toString()
        val salle = selectedSalle

        if (professeurId == null || moduleId == null || salle == null || date.isEmpty() || debut.isEmpty() || fin.isEmpty()) {
            Toast.makeText(requireContext(), "Veuillez compléter tous les champs.", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonBody = JSONObject().apply {
            put("professeur_id", professeurId)
            put("module_id", moduleId)
            put("salle", salle)
            put("date", date)
            put("heure_debut", debut)
            put("heure_fin", fin)
        }

        val url = "http://192.168.43.18:5000/seance"
        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                Toast.makeText(requireContext(), "Séance ajoutée avec succès", Toast.LENGTH_SHORT).show()
                reinitialiserFormulaire()
            },
            { error ->
                Log.e(TAG, "Erreur POST séance: ${error.message}")
                Toast.makeText(requireContext(), "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(request)
    }

    private fun reinitialiserFormulaire() {
        dateEditText.text.clear()
        heureDebutEditText.text.clear()
        heureFinEditText.text.clear()
        spinnerFilieres.setSelection(0)
        spinnerSalles.adapter = null
        selectedSalle = null
    }

    private fun lancerListeSeances() {
        val intent = Intent(requireContext(), ListSeanceScanActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requestQueue.stop()
    }
}