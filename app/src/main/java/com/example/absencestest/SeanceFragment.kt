package com.example.absencestest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

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

        // Initialisation des vues
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
        // Récupération des filières
        fun fetchFilieres() {
            val url = "http://192.168.134.106:5000/filieres"
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
            Volley.newRequestQueue(requireContext()).add(request)
        }

        fun fetchModulesByFiliere(filiereId: Int) {
            val url = "http://192.168.134.106:5000/modules/filiere/$filiereId"
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
            Volley.newRequestQueue(requireContext()).add(request)
        }

        fun fetchProfesseurs(filiereId: Int, moduleId: Int) {
            val url = "http://192.168.134.106:5000/professeurs?filiere_id=$filiereId&module_id=$moduleId"
            val request = JsonArrayRequest(Request.Method.GET, url, null,
                { response ->
                    professeurList.clear()
                    professeurIdList.clear()
                    for (i in 0 until response.length()) {
                        val prof = response.getJSONObject(i)
                        professeurList.add(prof.getString("nom"))
                        professeurIdList.add(prof.getInt("id"))
                    }
                    spinnerProfesseur.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, professeurList)
                },
                { error -> Log.e(TAG, "Erreur professeurs: ${error.message}") }
            )
            Volley.newRequestQueue(requireContext()).add(request)
        }

        // Sélections
        spinnerFilieres.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                fetchModulesByFiliere(filiereIdList[pos])
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        spinnerModules.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val filiereId = filiereIdList[spinnerFilieres.selectedItemPosition]
                fetchProfesseurs(filiereId, moduleIdList[pos])
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        spinnerSalles.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedSalle = salleList.getOrNull(pos)
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnCheckSalles.setOnClickListener {
            val filiereId = filiereIdList.getOrNull(spinnerFilieres.selectedItemPosition)
            val date = dateEditText.text.toString()
            val debut = heureDebutEditText.text.toString()
            val fin = heureFinEditText.text.toString()

            if (filiereId == null || date.isEmpty() || debut.isEmpty() || fin.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val url = "http://192.168.134.106:5000/salles/disponibles?filiere_id=$filiereId&date=$date&heure_debut=$debut&heure_fin=$fin"
            val request = JsonArrayRequest(Request.Method.GET, url, null,
                { response ->
                    salleList.clear()
                    for (i in 0 until response.length()) {
                        salleList.add(response.getJSONObject(i).getString("nom"))
                    }

                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, salleList)
                    spinnerSalles.adapter = adapter

                    // 🟢 Fix : Initialiser selectedSalle automatiquement
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
            Volley.newRequestQueue(requireContext()).add(request)
        }

        btnSeance.setOnClickListener {
            val professeurId = professeurIdList.getOrNull(spinnerProfesseur.selectedItemPosition)
            val moduleId = moduleIdList.getOrNull(spinnerModules.selectedItemPosition)
            val date = dateEditText.text.toString()
            val debut = heureDebutEditText.text.toString()
            val fin = heureFinEditText.text.toString()
            val salle = selectedSalle

            if (professeurId == null || moduleId == null || salle == null || date.isEmpty() || debut.isEmpty() || fin.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez compléter tous les champs.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val jsonBody = JSONObject().apply {
                put("professeur_id", professeurId)
                put("module_id", moduleId)
                put("salle", salle)
                put("date", date)
                put("heure_debut", debut)
                put("heure_fin", fin)
            }

            val url = "http://192.168.134.106:5000/seance"
            val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
                { response ->
                    Toast.makeText(requireContext(), "Séance ajoutée avec succès", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Réponse ajout séance : $response")
                },
                { error ->
                    Log.e(TAG, "Erreur POST séance: ${error.message}")
                    Toast.makeText(requireContext(), "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show()
                }
            )
            Volley.newRequestQueue(requireContext()).add(request)
        }


        btnListSeance.setOnClickListener {
            val intent = Intent(requireContext(), ListSeanceScanActivity::class.java)
            startActivity(intent)
        }

        fetchFilieres()
        return view
    }
}
