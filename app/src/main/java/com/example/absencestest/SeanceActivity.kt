package com.example.absencestest

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class SeanceActivity : AppCompatActivity() {

    private lateinit var spinnerFilieres: Spinner
    private lateinit var spinnerModules: Spinner
    private lateinit var spinnerSalles: Spinner
    private lateinit var btnSeance: Button
    private lateinit var dateEditText: EditText
    private lateinit var heureDebutEditText: EditText
    private lateinit var heureFinEditText: EditText
    private lateinit var btnCheckSalles: Button
    private lateinit var btnListSeance: Button

    private var professeurId: Int = -1
    private var selectedFiliereId: Int = -1

    private var modulesMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seance)

        professeurId = intent.getIntExtra("PROFESSEUR_ID", -1)

        spinnerFilieres = findViewById(R.id.spinnerFilieres)
        spinnerModules = findViewById(R.id.spinnerModules)
        spinnerSalles = findViewById(R.id.spinnerSalle)
        btnSeance = findViewById(R.id.btnValiderSeance)
        dateEditText = findViewById(R.id.editDateSeance)
        heureDebutEditText = findViewById(R.id.editHeureDebut)
        heureFinEditText = findViewById(R.id.editHeureFin)
        btnCheckSalles = findViewById(R.id.btnCheckSalles)

        loadFilieres()
        btnCheckSalles.setOnClickListener {
            checkSallesDisponibles()
        }

        btnSeance.setOnClickListener {
            creerSeance()
        }
    }

    private fun loadFilieres() {
        val urlFilieres = "http://192.168.134.106:5000/professeurs/$professeurId/filieres"
        val requestQueue = Volley.newRequestQueue(this)

        val requestFilieres = JsonArrayRequest(Request.Method.GET, urlFilieres, null,
            { response ->
                val filieresList = ArrayList<String>()
                val filieresIdMap = mutableMapOf<String, Int>()

                for (i in 0 until response.length()) {
                    val filiere = response.getJSONObject(i)
                    val nomFiliere = filiere.getString("nom")
                    val filiereId = filiere.getInt("id")
                    filieresList.add(nomFiliere)
                    filieresIdMap[nomFiliere] = filiereId
                }

                val adapterFilieres = ArrayAdapter(this, android.R.layout.simple_spinner_item, filieresList)
                adapterFilieres.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerFilieres.adapter = adapterFilieres

                spinnerFilieres.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val selectedFiliere = filieresList[position]
                        selectedFiliereId = filieresIdMap[selectedFiliere] ?: -1
                        loadModulesForFiliere(selectedFiliereId)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            },
            { error ->
                Toast.makeText(this, "Erreur de chargement des filières : ${error.message}", Toast.LENGTH_LONG).show()
            })

        requestQueue.add(requestFilieres)
    }

    private fun loadModulesForFiliere(filiereId: Int) {
        val urlModules = "http://192.168.134.106:5000/professeurs/$professeurId/filieres/$filiereId/modules"
        val requestQueue = Volley.newRequestQueue(this)

        val requestModules = JsonArrayRequest(Request.Method.GET, urlModules, null,
            { response ->
                val modulesList = ArrayList<String>()
                modulesMap.clear()
                for (i in 0 until response.length()) {
                    val module = response.getJSONObject(i)
                    val nomModule = module.getString("nom")
                    val idModule = module.getInt("id")
                    modulesList.add(nomModule)
                    modulesMap[nomModule] = idModule
                }

                val adapterModules = ArrayAdapter(this, android.R.layout.simple_spinner_item, modulesList)
                adapterModules.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerModules.adapter = adapterModules
            },
            { error ->
                Toast.makeText(this, "Erreur de chargement des modules : ${error.message}", Toast.LENGTH_LONG).show()
            })

        requestQueue.add(requestModules)
    }

    private fun checkSallesDisponibles() {
        val date = dateEditText.text.toString()
        val heureDebut = heureDebutEditText.text.toString()
        val heureFin = heureFinEditText.text.toString()

        if (date.isBlank() || heureDebut.isBlank() || heureFin.isBlank()) {
            Toast.makeText(this, "Veuillez remplir la date et les horaires", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://192.168.134.106:5000/salles/disponibles?date=$date&heure_debut=$heureDebut&heure_fin=$heureFin&filiere_id=$selectedFiliereId"
        Log.d("URL_DEBUG", url)

        val requestQueue = Volley.newRequestQueue(this)

        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                val sallesList = ArrayList<String>()
                for (i in 0 until response.length()) {
                    val salleObj = response.getJSONObject(i)
                    val nom = salleObj.getString("nom")  // <- Extraction uniquement du nom
                    sallesList.add(nom)
                }

                val adapterSalles = ArrayAdapter(this, android.R.layout.simple_spinner_item, sallesList)
                adapterSalles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSalles.adapter = adapterSalles
            },
            { error ->
                Toast.makeText(this, "Erreur lors de la vérification des salles : ${error.message}", Toast.LENGTH_LONG).show()
            })

        requestQueue.add(request)
    }


    private fun creerSeance() {
        val selectedModuleName = spinnerModules.selectedItem?.toString() ?: return
        val moduleId = modulesMap[selectedModuleName] ?: return
        val salle = spinnerSalles.selectedItem?.toString() ?: return
        val date = dateEditText.text.toString()
        val heureDebut = heureDebutEditText.text.toString()
        val heureFin = heureFinEditText.text.toString()

        if (date.isBlank() || heureDebut.isBlank() || heureFin.isBlank() || salle.isBlank()) {
            Toast.makeText(this, "Tous les champs doivent être remplis", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonBody = JSONObject().apply {
            put("professeur_id", professeurId)
            put("module_id", moduleId)
            put("salle", salle)
            put("date", date)
            put("heure_debut", heureDebut)
            put("heure_fin", heureFin)
        }

        val url = "http://192.168.134.106:5000/seance"
        val requestQueue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                Toast.makeText(this, "Séance créée avec succès", Toast.LENGTH_LONG).show()
            },
            { error ->
                val statusCode = error.networkResponse?.statusCode
                if (statusCode == 409) {
                    Toast.makeText(this, "Erreur : Conflit de planning", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Erreur lors de la création : ${error.message}", Toast.LENGTH_LONG).show()
                }
            })

        requestQueue.add(request)
    }
}
