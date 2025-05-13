package com.example.absencestest

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Request.Method
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class FiliereFragment : Fragment() {

    private lateinit var btnFiliere: Button
    private lateinit var nomEditText: EditText
    private val TAG = "FiliereFragment"
    private lateinit var listView: ListView
    private lateinit var adapter: FiliereAdapter
    private val filiereList = mutableListOf<Filiere>()

    data class Filiere(val id: Int, val nom: String)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_filiere, container, false)

        setupViews(view)
        setupAdapter()
        fetchFilieres()

        return view
    }

    private fun setupViews(view: View) {
        btnFiliere = view.findViewById(R.id.btnFiliere)
        nomEditText = view.findViewById(R.id.nomFiliere)
        listView = view.findViewById(R.id.listViewFilieres)

        btnFiliere.setOnClickListener {
            val nom = nomEditText.text.toString().trim()
            if (nom.isNotEmpty()) {
                sendFiliereRequest(nom)
            } else {
                showToast("Veuillez entrer un nom de filière")
            }
        }
    }

    private fun setupAdapter() {
        adapter = FiliereAdapter(filiereList)
        listView.adapter = adapter
    }
    private inner class FiliereAdapter(list: MutableList<Filiere>) :
        ArrayAdapter<Filiere>(requireContext(), R.layout.list_item_filiere, list) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // Utilisation d'un ViewHolder pour optimiser les performances
            val view: View
            val holder: ViewHolder

            if (convertView == null) {
                view = LayoutInflater.from(context).inflate(R.layout.list_item_filiere, parent, false)
                holder = ViewHolder()
                holder.tvName = view.findViewById(R.id.tvFiliereName) // Vérifiez cet ID !
                holder.btnDelete = view.findViewById(R.id.iconDelete1)
                view.tag = holder
            } else {
                view = convertView
                holder = view.tag as ViewHolder
            }

            val filiere = getItem(position)
            holder.tvName?.text = filiere?.nom ?: ""

            holder.btnDelete?.setOnClickListener {
                filiere?.let {
                    deleteFiliere(it.id, position)
                }
            }

            return view
        }

        private inner class ViewHolder {
            var tvName: TextView? = null
            var btnDelete: ImageView? = null
        }
    }

    private fun deleteFiliere(id: Int, position: Int) {
        val url = "http://192.168.43.18:5000/filieres/$id"

        val request = object : JsonObjectRequest(
            Method.DELETE, url, null,
            { _ ->
                filiereList.removeAt(position)
                adapter.notifyDataSetChanged()
                showToast("Filière supprimée avec succès")
            },
            { error ->
                handleDeleteError(error)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun fetchFilieres() {
        val url = "http://192.168.43.18:5000/filieres"

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                filiereList.clear()
                for (i in 0 until response.length()) {
                    val filiereJson = response.getJSONObject(i)
                    filiereList.add(
                        Filiere(
                            filiereJson.getInt("id"),
                            filiereJson.getString("nom")
                        )
                    )
                }
                adapter.notifyDataSetChanged()
            },
            { error ->
                showToast("Erreur de chargement des filières")
                Log.e(TAG, "Fetch error: ${error.message}")
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun sendFiliereRequest(nom: String) {
        val url = "http://192.168.43.18:5000/filieres"
        val jsonBody = JSONObject().apply { put("nom", nom) }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                nomEditText.text.clear()
                fetchFilieres() // Rafraîchir la liste
                showToast(response.optString("message", "Succès"))
            },
            { error ->
                handlePostError(error)
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun handlePostError(error: com.android.volley.VolleyError) {
        val statusCode = error.networkResponse?.statusCode
        val message = when (statusCode) {
            400 -> "Données invalides"
            else -> "Erreur réseau: ${error.message}"
        }
        showToast(message)
        Log.e(TAG, "POST Error: ${error.networkResponse?.data?.decodeToString()}")
    }

    private fun handleDeleteError(error: com.android.volley.VolleyError) {
        showToast("Échec de la suppression")
        Log.e(TAG, "DELETE Error: ${error.networkResponse?.data?.decodeToString()}")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}