package com.example.absencestest

import android.content.Context
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

class SalleFragment : Fragment() {

    private lateinit var spinnerFilieres: Spinner
    private lateinit var nomSalleEditText: EditText
    private lateinit var btnValiderSalle: Button
    private lateinit var listViewSalles: ListView

    private val TAG = "SalleFragment"
    private val BASE_URL = "http://100.70.32.157:5000"

    private val filiereList = mutableListOf<String>()
    private val filiereIdList = mutableListOf<Int>()
    private val salleList = mutableListOf<Salle>()
    private lateinit var salleAdapter: SalleAdapter

    data class Salle(val id: Int, val nom: String, val filiereId: Int)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_salle, container, false)

        initViews(view)
        setupAdapters()
        setupListeners()
        fetchFilieres()

        return view
    }

    private fun initViews(view: View) {
        spinnerFilieres = view.findViewById(R.id.spinnerFilieres)
        nomSalleEditText = view.findViewById(R.id.nomSalle)
        btnValiderSalle = view.findViewById(R.id.btnSalle)
        listViewSalles = view.findViewById(R.id.listeSalles)
    }

    private fun setupAdapters() {
        salleAdapter = SalleAdapter(requireContext(), salleList)
        listViewSalles.adapter = salleAdapter

        val filiereAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filiereList
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerFilieres.adapter = filiereAdapter
    }

    private fun setupListeners() {
        btnValiderSalle.setOnClickListener { enregistrerSalle() }

        spinnerFilieres.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != Spinner.INVALID_POSITION) {
                    fetchSalles(filiereIdList[position])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
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

    private fun fetchSalles(filiereId: Int) {
        val url = "$BASE_URL/salles/filiere/$filiereId"

        Volley.newRequestQueue(requireContext()).add(
            JsonArrayRequest(
                Request.Method.GET, url, null,
                { response ->
                    salleList.clear()
                    for (i in 0 until response.length()) {
                        val salle = response.getJSONObject(i)
                        salleList.add(
                            Salle(
                                salle.getInt("id"),
                                salle.getString("nom"),
                                filiereId
                            )
                        )
                    }
                    salleAdapter.notifyDataSetChanged()
                },
                { error ->
                    showToast("Erreur de chargement des salles")
                    Log.e(TAG, "Fetch salles error: ${error.message}")
                }
            )
        )
    }

    private fun enregistrerSalle() {
        val nomSalle = nomSalleEditText.text.toString().trim()
        val position = spinnerFilieres.selectedItemPosition

        if (nomSalle.isEmpty() || position == Spinner.INVALID_POSITION) {
            showToast("Veuillez remplir tous les champs")
            return
        }

        val filiereId = filiereIdList[position]
        val url = "$BASE_URL/salles"
        val jsonBody = JSONObject().apply {
            put("nom", nomSalle)
            put("filiere_id", filiereId)
        }

        Volley.newRequestQueue(requireContext()).add(
            JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                { response ->
                    nomSalleEditText.text.clear()
                    fetchSalles(filiereId)
                    showToast("Salle ajoutée avec succès")
                },
                { error ->
                    handlePostError(error)
                }
            )
        )
    }

    private fun deleteSalle(salleId: Int, position: Int) {
        val url = "$BASE_URL/salles/$salleId"

        Volley.newRequestQueue(requireContext()).add(
            object : JsonObjectRequest(
                Request.Method.DELETE, url, null,
                { _ ->
                    salleList.removeAt(position)
                    salleAdapter.notifyDataSetChanged()
                    showToast("Salle supprimée")
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

    private inner class SalleAdapter(
        context: Context,
        private val items: List<Salle>
    ) : ArrayAdapter<Salle>(context, R.layout.ist_item_salle, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val viewHolder: ViewHolder
            val view = convertView ?: LayoutInflater.from(context).inflate(
                R.layout.ist_item_salle,
                parent,
                false
            ).also {
                viewHolder = ViewHolder(
                    it.findViewById(R.id.tvSalleName),
                    it.findViewById(R.id.iconDelete)
                )
                it.tag = viewHolder
            } ?: throw IllegalStateException("View cannot be null")

            val holder = view.tag as ViewHolder
            val salle = getItem(position)

            holder.tvName.text = salle?.nom ?: ""
            holder.btnDelete.setOnClickListener {
                salle?.let { deleteSalle(it.id, position) }
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