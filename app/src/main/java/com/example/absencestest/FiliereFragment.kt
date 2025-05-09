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

class FiliereFragment : Fragment() {

    private lateinit var btnFiliere: Button
    private lateinit var nomEditText: EditText
    private val TAG = "FiliereFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_filiere, container, false)

        btnFiliere = view.findViewById(R.id.btnFiliere)
        nomEditText = view.findViewById(R.id.nomFiliere)

        btnFiliere.setOnClickListener {
            val nom = nomEditText.text.toString().trim()

            if (nom.isNotEmpty()) {
                sendFiliereRequest(nom)
            } else {
                Toast.makeText(requireContext(), "Veuillez entrer un nom de filière", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Champs nom vide")
            }
        }

        return view
    }

    private fun sendFiliereRequest(nom: String) {
        val url = "http://192.168.228.90:5000/filieres"

        val jsonBody = JSONObject()
        jsonBody.put("nom", nom)

        Log.d(TAG, "Envoi requête à $url avec données: $jsonBody")

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val message = response.optString("message", "Filière ajoutée avec succès")
                Log.d(TAG, "Réponse reçue: $response")
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                // Optionnel : vider le champ après succès
                nomEditText.text.clear()
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
