package com.example.absencestest

import android.content.Context
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
import kotlin.random.Random

class ProfesseurFragment : Fragment() {

    private lateinit var spinnerFilieres: Spinner
    private lateinit var spinnerModules: Spinner
    private lateinit var editNomProfesseur: EditText
    private lateinit var editEmailProfesseur: EditText
    private lateinit var editMotPassProfesseur: EditText
    private lateinit var btnValiderProfesseur: Button
    private lateinit var listViewProfesseurs: ListView
    private lateinit var ivTogglePassword: ImageView
    private var isPasswordVisible = false

    private val TAG = "ProfesseurFragment"
    private val BASE_URL = "http://192.168.43.18:5000"

    private val filiereList = mutableListOf<String>()
    private val filiereIdList = mutableListOf<Int>()
    private val moduleList = mutableListOf<String>()
    private val moduleIdList = mutableListOf<Int>()
    private lateinit var professeurAdapter: ProfesseurAdapter
    private val professeurList = mutableListOf<Professeur>()

    data class Professeur(val id: Int, val nom: String, val filiereId: Int, val moduleId: Int)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_professeur, container, false)

        initViews(view)
        setupAdapters()
        setupListeners()
        fetchFilieres()

        return view
    }

    private fun initViews(view: View) {
        spinnerFilieres = view.findViewById(R.id.spinnerFilieres)
        spinnerModules = view.findViewById(R.id.spinnerModules)
        editNomProfesseur = view.findViewById(R.id.editNomProfesseur)
        editEmailProfesseur = view.findViewById(R.id.editEmailProfesseur)
        editMotPassProfesseur = view.findViewById(R.id.editMotPassProfesseur)
        btnValiderProfesseur = view.findViewById(R.id.btnValiderProfesseur)
        ivTogglePassword = view.findViewById(R.id.ivTogglePassword)
        editMotPassProfesseur = view.findViewById(R.id.editMotPassProfesseur)
        listViewProfesseurs = view.findViewById(R.id.listeProfesseurs)

        editMotPassProfesseur.apply {
            showSoftInputOnFocus = false
            setOnClickListener {
                val generatedPassword = genererMotDePasseAutomatique()
                setText(generatedPassword)
                Toast.makeText(context, "Mot de passe généré !", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun setupAdapters() {
        professeurAdapter = ProfesseurAdapter(requireContext(), professeurList)
        listViewProfesseurs.adapter = professeurAdapter

        val filiereAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filiereList
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerFilieres.adapter = filiereAdapter

        val moduleAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            moduleList
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerModules.adapter = moduleAdapter
    }

    private fun setupListeners() {
        btnValiderProfesseur.setOnClickListener { enregistrerProfesseur() }
        ivTogglePassword.setOnClickListener { togglePasswordVisibility() }

        spinnerFilieres.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != Spinner.INVALID_POSITION) {
                    fetchModules(filiereIdList[position])
                    fetchProfesseurs(filiereIdList[position])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
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
        val url = "$BASE_URL/filieres"

        Volley.newRequestQueue(requireContext()).add(
            JsonArrayRequest(
                Request.Method.GET, url, null,
                { response ->
                    filiereList.clear()
                    filiereIdList.clear()

                    for (i in 0 until response.length()) {
                        val filiere = response.getJSONObject(i)
                        filiereList.add(filiere.getString("nom"))
                        filiereIdList.add(filiere.getInt("id"))
                    }

                    (spinnerFilieres.adapter as ArrayAdapter<*>).notifyDataSetChanged()
                },
                { error ->
                    showToast("Erreur de chargement des filières")
                    Log.e(TAG, "Fetch filières error: ${error.message}")
                }
            )
        )
    }

    private fun fetchModules(filiereId: Int) {
        val url = "$BASE_URL/modules/filiere/$filiereId"

        Volley.newRequestQueue(requireContext()).add(
            JsonArrayRequest(
                Request.Method.GET, url, null,
                { response ->
                    moduleList.clear()
                    moduleIdList.clear()

                    for (i in 0 until response.length()) {
                        val module = response.getJSONObject(i)
                        moduleList.add(module.getString("nom"))
                        moduleIdList.add(module.getInt("id"))
                    }

                    (spinnerModules.adapter as ArrayAdapter<*>).notifyDataSetChanged()
                },
                { error ->
                    showToast("Erreur de chargement des modules")
                    Log.e(TAG, "Fetch modules error: ${error.message}")
                }
            )
        )
    }

    private fun fetchProfesseurs(filiereId: Int) {
        val url = "$BASE_URL/professeurs/filiere/$filiereId"

        Volley.newRequestQueue(requireContext()).add(
            JsonArrayRequest(
                Request.Method.GET, url, null,
                { response ->
                    professeurList.clear()
                    for (i in 0 until response.length()) {
                        val professeur = response.getJSONObject(i)
                        professeurList.add(
                            Professeur(
                                professeur.getInt("id"),
                                professeur.getString("nom"),
                                filiereId,
                                professeur.getInt("module_id")
                            )
                        )
                    }
                    professeurAdapter.notifyDataSetChanged()
                },
                { error ->
                    showToast("Erreur de chargement des professeurs")
                    Log.e(TAG, "Fetch professeurs error: ${error.message}")
                }
            )
        )
    }

    private fun enregistrerProfesseur() {
        val nom = editNomProfesseur.text.toString().trim()
        val email = editEmailProfesseur.text.toString().trim()
        val motDePasse = editMotPassProfesseur.text.toString().trim()
        val filierePosition = spinnerFilieres.selectedItemPosition
        val modulePosition = spinnerModules.selectedItemPosition

        if (nom.isEmpty() || email.isEmpty() || motDePasse.isEmpty() ||
            filierePosition == Spinner.INVALID_POSITION || modulePosition == Spinner.INVALID_POSITION) {
            showToast("Tous les champs sont requis")
            return
        }

        val filiereId = filiereIdList[filierePosition]
        val moduleId = moduleIdList[modulePosition]
        val url = "$BASE_URL/professeurs"
        val jsonBody = JSONObject().apply {
            put("nom", nom)
            put("email", email)
            put("mot_de_passe", motDePasse)
            put("filiere_id", filiereId)
            put("module_id", moduleId)
        }

        Volley.newRequestQueue(requireContext()).add(
            JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                { response ->
                    editNomProfesseur.text.clear()
                    editEmailProfesseur.text.clear()
                    editMotPassProfesseur.text.clear()
                    fetchProfesseurs(filiereId)
                    showToast("Professeur ajouté avec succès")
                },
                { error ->
                    handlePostError(error)
                }
            )
        )
    }

    private fun deleteProfesseur(professeurId: Int, position: Int) {
        val url = "$BASE_URL/professeurs/$professeurId"

        Volley.newRequestQueue(requireContext()).add(
            object : JsonObjectRequest(
                Request.Method.DELETE, url, null,
                { _ ->
                    professeurList.removeAt(position)
                    professeurAdapter.notifyDataSetChanged()
                    showToast("Professeur supprimé")
                },
                { error ->
                    showToast("Échec de la suppression")
                    Log.e(TAG, "Delete error: ${error.message}")
                }
            ) {
                override fun getHeaders() = hashMapOf("Content-Type" to "application/json")
            }
        )
    }

    private inner class ProfesseurAdapter(
        context: Context,
        private val items: List<Professeur>
    ) : ArrayAdapter<Professeur>(context, R.layout.list_item_professeur, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val viewHolder: ViewHolder
            val view = convertView ?: LayoutInflater.from(context).inflate(
                R.layout.list_item_professeur,
                parent,
                false
            ).also {
                viewHolder = ViewHolder(
                    it.findViewById(R.id.tvProfesseurName),
                    it.findViewById(R.id.iconDelete)
                )
                it.tag = viewHolder
            } ?: throw IllegalStateException("View cannot be null")

            val holder = view.tag as ViewHolder
            val professeur = getItem(position)

            holder.tvName.text = professeur?.nom ?: ""
            holder.btnDelete.setOnClickListener {
                professeur?.let { deleteProfesseur(it.id, position) }
            }

            return view
        }

        private inner class ViewHolder(
            val tvName: TextView,
            val btnDelete: ImageView
        )
    }

    private fun handlePostError(error: com.android.volley.VolleyError) {
        val message = when (error.networkResponse?.statusCode) {
            400 -> "Données invalides"
            else -> "Erreur réseau: ${error.message}"
        }
        showToast(message)
        Log.e(TAG, "POST Error: ${error.networkResponse?.data?.decodeToString()}")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}