package com.example.absencestest

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView



class NavActivityProfe : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout //conteneur du menu coulissant.
    private lateinit var navigationView: NavigationView //la vue de navigation contenant les items du menu.
    private lateinit var toggleButton: ImageButton // un bouton (icône hamburger ou croix) pour ouvrir/fermer le menu.

    private var professeurId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav_prof)
        professeurId = intent.getIntExtra("PROFESSEUR_ID", -1)
        // Initialisation des composants
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        toggleButton = findViewById(R.id.btn_toggle_nav)

        // Configuration de la Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        if (savedInstanceState == null) {
            val fragment = DefaultFragment()
            val bundle = Bundle()
            bundle.putInt("prof_id", professeurId) // tu passes ici ton idProf (int ou autre type)
            fragment.arguments = bundle

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }



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
            R.id.nav_seance -> {
                val intent = Intent(this, SeanceActivity::class.java)
                intent.putExtra("PROFESSEUR_ID", professeurId)
                startActivity(intent)
            }
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