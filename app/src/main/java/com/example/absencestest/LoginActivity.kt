package com.example.absencestest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button
    private val TAG = "LoginActivity"
    private val BASE_URL = "http://192.168.0.106:5000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)

        loginButton.setOnClickListener {
            val email = username.text.toString().trim()
            val pass = password.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                attemptAdminLogin(email, pass)
            } else {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun attemptAdminLogin(email: String, password: String) {
        val url = "$BASE_URL/login"
        val jsonBody = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                when (response.optString("role")) {
                    "admin" -> handleAdminLoginSuccess(response)
                    "professor" -> handleProfessorLoginSuccess(response)
                    else -> Toast.makeText(this, "Rôle non reconnu", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                handleLoginError(error)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun handleAdminLoginSuccess(response: JSONObject) {
        val userId = response.optInt("user_id", -1)
        if (userId != -1) {
            val intent = Intent(this, NavActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
            finish()
        }
    }

    private fun handleProfessorLoginSuccess(response: JSONObject) {
        val professeurId = response.optInt("professeur_id", -1)
        if (professeurId != -1) {
            val intent = Intent(this, NavActivityProfe::class.java)
            intent.putExtra("PROFESSEUR_ID", professeurId)
            startActivity(intent)
            finish()
        }
    }

    private fun handleLoginError(error: VolleyError) {
        val statusCode = error.networkResponse?.statusCode
        val errorMessage = when (statusCode) {
            400 -> "Données manquantes"
            401 -> "Identifiants incorrects"
            500 -> "Erreur serveur"
            else -> "Erreur de connexion (${error.networkResponse?.statusCode})"
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        Log.e(TAG, "Erreur: ${error.message}")
    }
}