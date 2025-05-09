package com.example.absencestest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)

        loginButton.setOnClickListener {
            val email = username.text.toString().trim()
            val pass = password.text.toString().trim()

            Log.d(TAG, "Bouton login cliqué - email: $email, pass: $pass")

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                sendLoginRequest(email, pass)
            } else {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Champs vides détectés")
            }
        }
    }

    private fun sendLoginRequest(email: String, password: String) {
        val url = "http://192.168.0.103:5000/login"

        val jsonBody = JSONObject()
        jsonBody.put("email", email)
        jsonBody.put("password", password)

        Log.d(TAG, "Envoi requête à $url avec données: $jsonBody")

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val message = response.optString("message", "Connexion réussie")
                Log.d(TAG, "Réponse reçue: $response")
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                // Redirection vers AdministrateurActivity
                val intent = Intent(this, NavActivity::class.java)
                startActivity(intent)
                finish() // Ferme LoginActivity pour empêcher le retour
            },
            { error ->
                val statusCode = error.networkResponse?.statusCode
                val data = error.networkResponse?.data?.decodeToString()
                val errorMessage = when (statusCode) {
                    400 -> "Champs manquants"
                    401 -> "Email ou mot de passe incorrect"
                    else -> "Erreur réseau : ${error.message}"
                }

                Log.e(TAG, "Erreur Volley - Code: $statusCode, Message: ${error.message}")
                if (data != null) {
                    Log.e(TAG, "Détail erreur serveur : $data")
                }

                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }
}
