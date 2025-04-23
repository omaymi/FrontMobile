package com.example.absencestest

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class LoginActivity : AppCompatActivity() {
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        username = findViewById<EditText>(R.id.username)
        password = findViewById<EditText>(R.id.password)
        loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener(View.OnClickListener {
            if (username.getText().toString() == "user" && password.getText()
                    .toString() == "1234"
            ) {
                Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@LoginActivity, "Login Failed!", Toast.LENGTH_SHORT).show()
            }
        })
    }
}