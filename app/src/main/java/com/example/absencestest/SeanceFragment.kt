package com.example.absencestest

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class SeanceFragment : Fragment() {

    private lateinit var btnSeance: Button
    private lateinit var dateEditText: EditText
    private lateinit var heureDebutEditText: EditText
    private lateinit var heureFinEditText: EditText

    private val TAG = "SeanceFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_seance, container, false)

        btnSeance = view.findViewById(R.id.btnValiderSeance)
        dateEditText = view.findViewById(R.id.editDateSeance)
        heureDebutEditText = view.findViewById(R.id.editHeureDebut)
        heureFinEditText = view.findViewById(R.id.editHeureFin)
        spinnerFilieres = view.findViewById(R.id.spinnerFilieres)
        spinnerModules = view.findViewById(R.id.spinnerModules)
        spinnerProfesseur = view.findViewById(R.id.spinnerProfesseur)

        btnSeance.setOnClickListener {
            val date = dateEditText.text.toString().trim()
            val heureDebut = heureDebutEditText.text.toString().trim()
            val heureFin = heureFinEditText.text.toString().trim()

            if (date.isNotEmpty() && heureDebut.isNotEmpty() && heureFin.isNotEmpty()) {
                sendSeanceRequest(date, heureDebut, heureFin)
            } else {
                Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Champs manquants")
            }
        }

        return view
    }

    private fun sendSeanceRequest(date: String, heureDebut: String, heureFin: String) {
        val url = "http://100.89.160.188:5000/seances"

        val jsonBody = JSONObject().apply {
            put("date", date)
            put("heure_debut", heureDebut)
            put("heure_fin", heureFin)
        }

        Log.d(TAG, "Envoi requête à $url avec données: $jsonBody")

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val message = response.optString("message", "Séance ajoutée avec succès")
                Log.d(TAG, "Réponse reçue: $response")
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

                // Vider les champs après succès
                dateEditText.text.clear()
                heureDebutEditText.text.clear()
                heureFinEditText.text.clear()
            },
            { error ->
                val statusCode = error.networkResponse?.statusCode
                val data = error.networkResponse?.data?.decodeToString()
                val errorMessage = when (statusCode) {
                    400 -> "Champs manquants"
                    else -> "Erreur réseau : ${error.message}"
                }

                Log.e(TAG, "Erreur Volley - Code: $statusCode, Message: ${error.message}")
                if (data != null) {
                    Log.e(TAG, "Détail erreur serveur : $data")
                }

                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }
}
