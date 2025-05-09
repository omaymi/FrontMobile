package com.example.absencestest

import android.os.Bundle
import android.text.InputType
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
import org.json.JSONArray
import org.json.JSONObject

class ProfesseurFragment : Fragment() {

    private lateinit var spinnerFilieres: Spinner
    private lateinit var spinnerModules: Spinner
    private lateinit var editNomProfesseur: EditText
    private lateinit var editEmailProfesseur: EditText
    private lateinit var editMotPassProfesseur: EditText
    private lateinit var btnValiderProfesseur: Button
    private lateinit var ivTogglePassword: ImageView

    private val TAG = "ProfesseurFragment"
    private val BASE_URL = "http://192.168.228.90:5000"
    private var isPasswordVisible = false

    private val filiereList = mutableListOf<String>()
    private val filiereIdList = mutableListOf<Int>()
    private val moduleList = mutableListOf<String>()
    private val moduleIdList = mutableListOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_professeur, container, false)
        setupViews(view)
        setupListeners()
        fetchFilieres()
        return view
    }

    private fun setupViews(view: View) {
        spinnerFilieres = view.findViewById(R.id.spinnerFilieres)
        spinnerModules = view.findViewById(R.id.spinnerModules)
        editNomProfesseur = view.findViewById(R.id.editNomProfesseur)
        editEmailProfesseur = view.findViewById(R.id.editEmailProfesseur)
        editMotPassProfesseur = view.findViewById(R.id.editMotPassProfesseur)
        btnValiderProfesseur = view.findViewById(R.id.btnValiderProfesseur)
        ivTogglePassword = view.findViewById(R.id.ivTogglePassword)

        editMotPassProfesseur.apply {
            showSoftInputOnFocus = false
            setOnClickListener {
                val generatedPassword = genererMotDePasseAutomatique()
                setText(generatedPassword)
                Toast.makeText(context, "Mot de passe généré !", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        ivTogglePassword.setOnClickListener { togglePasswordVisibility() }

        spinnerFilieres.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position != Spinner.INVALID_POSITION) {
                    fetchModulesByFiliere(filiereIdList[position])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnValiderProfesseur.setOnClickListener { enregistrerProfesseur() }
    }

    private fun genererMotDePasseAutomatique(): String {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%"
        return (1..10).map { caracteres.random() }.joinToString("")
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        editMotPassProfesseur.inputType = if (isPasswordVisible) {
            ivTogglePassword.setImageResource(R.drawable.ic_visibility)
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        editMotPassProfesseur.setSelection(editMotPassProfesseur.text.length)
    }

    private fun fetchFilieres() {
        val url = "http://192.168.228.90:5000/filieres"
        

        Log.d(TAG, "Envoi de la requête GET à $url")

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d(TAG, "Réponse reçue: $response")
                handleFilieresResponse(response)
            },
            { error ->
                val statusCode = error.networkResponse?.statusCode
                Log.e(TAG, "Erreur Volley - Code: $statusCode, Message: ${error.message}")
                Toast.makeText(requireContext(), "Erreur lors de la récupération des filières", Toast.LENGTH_LONG).show()
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun fetchModulesByFiliere(filiereId: Int) {
        val url = "http://192.168.228.90:5000/modules/filiere/$filiereId"  // Mise à jour de l'URL pour correspondre à la route Flask

        Log.d(TAG, "Envoi de la requête GET à $url")

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d(TAG, "Réponse reçue: $response")
                handleModulesResponse(response)
            },
            { error ->
                val statusCode = error.networkResponse?.statusCode
                Log.e(TAG, "Erreur Volley - Code: $statusCode, Message: ${error.message}")
                Toast.makeText(requireContext(), "Erreur lors de la récupération des modules", Toast.LENGTH_LONG).show()
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }


    private fun handleFilieresResponse(response: JSONArray) {
        filiereList.clear()
        filiereIdList.clear()

        for (i in 0 until response.length()) {
            val filiere = response.getJSONObject(i)
            val id = filiere.getInt("id")
            val nom = filiere.getString("nom")
            filiereList.add(nom)
            filiereIdList.add(id)
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filiereList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerFilieres.adapter = adapter
    }

    private fun handleModulesResponse(response: JSONArray) {
        moduleList.clear()
        moduleIdList.clear()

        for (i in 0 until response.length()) {
            val module = response.getJSONObject(i)
            val id = module.getInt("id")
            val nom = module.getString("nom")
            moduleList.add(nom)
            moduleIdList.add(id)
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            moduleList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerModules.adapter = adapter
    }

    private fun enregistrerProfesseur() {
        val nomProfesseur = editNomProfesseur.text.toString().trim()
        val emailProfesseur = editEmailProfesseur.text.toString().trim()
        val MotPassProfesseur = editMotPassProfesseur.text.toString().trim()
        val positionFiliere = spinnerFilieres.selectedItemPosition
        val positionModule = spinnerModules.selectedItemPosition

        if (nomProfesseur.isEmpty() || emailProfesseur.isEmpty() || positionFiliere == Spinner.INVALID_POSITION || positionModule == Spinner.INVALID_POSITION) {
            Toast.makeText(requireContext(), "Veuillez entrer un nom, un email, et choisir une filière et un module", Toast.LENGTH_LONG).show()
            return
        }

        val filiereId = filiereIdList[positionFiliere]
        val moduleId = moduleIdList[positionModule]

        val url = "http://192.168.228.90:5000/professeurs"  // Modifiez cette URL si nécessaire

        val jsonBody = JSONObject()
        jsonBody.put("nom", nomProfesseur)
        jsonBody.put("email", emailProfesseur)
        jsonBody.put("filiere_id", filiereId)
        jsonBody.put("module_id", moduleId)
        jsonBody.put("mot_de_passe", MotPassProfesseur)

        Log.d(TAG, "Envoi de la requête POST à $url avec données: $jsonBody")

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val message = response.optString("message", "Professeur ajouté avec succès")
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                Log.d(TAG, "Professeur ajouté avec succès: $response")
                // Nettoyer les champs
                editNomProfesseur.text.clear()
                editEmailProfesseur.text.clear()
                editMotPassProfesseur.text.clear()
                spinnerFilieres.setSelection(0)
                spinnerModules.setSelection(0)
            },
            { error ->
                val statusCode = error.networkResponse?.statusCode
                Log.e(TAG, "Erreur Volley - Code: $statusCode, Message: ${error.message}")
                Toast.makeText(requireContext(), "Erreur lors de l'ajout du professeur", Toast.LENGTH_LONG).show()
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }
}