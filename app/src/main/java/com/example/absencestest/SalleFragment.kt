package com.example.absencestest


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
import org.json.JSONArray
import org.json.JSONObject

class SalleFragment : Fragment(){

    private lateinit var spinnerFilieres: Spinner
    private lateinit var nomSalleEditText: EditText
    private lateinit var btnValiderSalle: Button

    private val TAG = "SalleFragment"

    private val filiereList = mutableListOf<String>()      // Pour afficher dans le spinner
    private val filiereIdList = mutableListOf<Int>()        // Pour stocker les id des filières

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_salle, container, false)

        spinnerFilieres = view.findViewById(R.id.spinnerFilieres)
        nomSalleEditText = view.findViewById(R.id.nomSalle)  // Modifier ton EditText pour lui ajouter un id : nomSalleEditText
        btnValiderSalle = view.findViewById(R.id.btnSalle)    // Modifier ton Button pour lui ajouter un id : btnValiderSalle

        fetchFilieres()

        btnValiderSalle.setOnClickListener {
            enregistrerSalle()
        }

        return view
    }

    private fun fetchFilieres() {
        val url = "http://100.89.160.199:5000/filieres"

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

    private fun enregistrerSalle() {
        val nomSalle = nomSalleEditText.text.toString().trim()
        val position = spinnerFilieres.selectedItemPosition

        if (nomSalle.isEmpty() || position == Spinner.INVALID_POSITION) {
            Toast.makeText(requireContext(), "Veuillez entrer un nom de Salle et choisir une filière", Toast.LENGTH_LONG).show()
            return
        }

        val filiereId = filiereIdList[position]

        val url = "http://100.89.160.199:5000/salles"

        val jsonBody = JSONObject()
        jsonBody.put("nom", nomSalle)
        jsonBody.put("filiere_id", filiereId)

        Log.d(TAG, "Envoi de la requête POST à $url avec données: $jsonBody")

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val message = response.optString("message", "Salle ajouté avec succès")
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                Log.d(TAG, "Salle ajouté avec succès: $response")
                // Nettoyer les champs
                nomSalleEditText.text.clear()
                spinnerFilieres.setSelection(0)
            },
            { error ->
                val statusCode = error.networkResponse?.statusCode
                Log.e(TAG, "Erreur Volley - Code: $statusCode, Message: ${error.message}")
                Toast.makeText(requireContext(), "Erreur lors de l'ajout du Salle", Toast.LENGTH_LONG).show()
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }
}