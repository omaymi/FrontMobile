package com.example.absencestest

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.absencestest.fragments.FiliereFragment
import com.example.absencestest.fragments.ModuleFragment

class AdministrateurActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_administrateur)

        // Déclarer les boutons avec findViewById
        val btnFiliere = findViewById<Button>(R.id.btnFiliere)
        val btnModule = findViewById<Button>(R.id.btnModule)

        // Charger le fragment par défaut
        loadFragment(FiliereFragment())

        // Gestion des clics
        btnFiliere.setOnClickListener { loadFragment(FiliereFragment()) }
        btnModule.setOnClickListener { loadFragment(ModuleFragment()) }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}