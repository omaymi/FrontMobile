package com.example.absencestest

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView



class NavActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggleButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)

        // Initialisation des composants
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        toggleButton = findViewById(R.id.btn_toggle_nav)

        // Configuration de la Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        // Gestion du clic sur le bouton
        toggleButton.setOnClickListener {
            toggleNavigationDrawer()
        }

        // Configuration du listener de navigation
        navigationView.setNavigationItemSelectedListener(this)

        // Animation du bouton
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // Animation optionnelle pendant le glissement
            }

            override fun onDrawerOpened(drawerView: View) {
                toggleButton.setImageResource(R.drawable.ic_close) // Icône de fermeture
            }

            override fun onDrawerClosed(drawerView: View) {
                toggleButton.setImageResource(R.drawable.ic_menu) // Icône de menu
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })
        // Charger HomeFragment par défaut
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }
    }

    private fun toggleNavigationDrawer() {
        if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dash -> supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
            R.id.nav_home -> supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FiliereFragment())
                .commit()
            R.id.nav_settings -> supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ModuleFragment())
                .commit()
            R.id.nav_share -> supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfesseurFragment())
                .commit()
            R.id.nav_seance -> supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SeanceFragment())
                .commit()
            R.id.nav_salle -> supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SalleFragment())
                .commit()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}