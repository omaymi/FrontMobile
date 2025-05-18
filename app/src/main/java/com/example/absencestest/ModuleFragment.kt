package com.example.absencestest

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Request.Method
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class ModuleFragment : Fragment() {

    private lateinit var spinnerFilieres: Spinner
    private lateinit var nomModuleEditText: EditText
    private lateinit var btnValiderModule: Button
    private lateinit var listViewModules: ListView

    private val TAG = "ModuleFragment"
    private val filiereList = mutableListOf<String>()
    private val filiereIdList = mutableListOf<Int>()
    private lateinit var moduleAdapter: ModuleAdapter
    private val moduleList = mutableListOf<Module>()

    data class Module(val id: Int, val nom: String, val filiereId: Int)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_module, container, false)

        initViews(view)
        setupAdapters()
        setupListeners()
        fetchFilieres()

        return view
    }

    private fun initViews(view: View) {
        spinnerFilieres = view.findViewById(R.id.spinnerFilieres)
        nomModuleEditText = view.findViewById(R.id.nomModule)
        btnValiderModule = view.findViewById(R.id.btnModule)
        listViewModules = view.findViewById(R.id.listeModules)
    }

    private fun setupAdapters() {
        moduleAdapter = ModuleAdapter(requireContext(), moduleList)
        listViewModules.adapter = moduleAdapter

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filiereList
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerFilieres.adapter = spinnerAdapter
    }

    private fun setupListeners() {
        btnValiderModule.setOnClickListener { enregistrerModule() }

        spinnerFilieres.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != Spinner.INVALID_POSITION) {
                    fetchModules(filiereIdList[position])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun fetchFilieres() {
        val url = "http://100.70.32.157:5000/filieres"

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
        val url = "http://100.70.32.157:5000/modules/filiere/$filiereId"

        Volley.newRequestQueue(requireContext()).add(
            JsonArrayRequest(
                Request.Method.GET, url, null,
                { response ->
                    moduleList.clear()
                    for (i in 0 until response.length()) {
                        val module = response.getJSONObject(i)
                        moduleList.add(
                            Module(
                                module.getInt("id"),
                                module.getString("nom"),
                                filiereId
                            )
                        )
                    }
                    moduleAdapter.notifyDataSetChanged()
                },
                { error ->
                    showToast("Erreur de chargement des modules")
                    Log.e(TAG, "Fetch modules error: ${error.message}")
                }
            )
        )
    }

    private fun enregistrerModule() {
        val nom = nomModuleEditText.text.toString().trim()
        val position = spinnerFilieres.selectedItemPosition

        if (nom.isEmpty() || position == Spinner.INVALID_POSITION) {
            showToast("Tous les champs sont requis")
            return
        }

        val filiereId = filiereIdList[position]
        val url = "http://100.70.32.157:5000/modules"
        val jsonBody = JSONObject().apply {
            put("nom", nom)
            put("filiere_id", filiereId)
        }

        Volley.newRequestQueue(requireContext()).add(
            JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                { response ->
                    nomModuleEditText.text.clear()
                    fetchModules(filiereId)
                    showToast("Module ajouté avec succès")
                },
                { error ->
                    handlePostError(error)
                }
            )
        )
    }

    private fun deleteModule(moduleId: Int, position: Int) {
        val url = "http://100.70.32.157:5000/modules/$moduleId"

        Volley.newRequestQueue(requireContext()).add(
            object : JsonObjectRequest(
                Method.DELETE, url, null,
                { _ ->
                    moduleList.removeAt(position)
                    moduleAdapter.notifyDataSetChanged()
                    showToast("Module supprimé")
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

    private inner class ModuleAdapter(
        context: Context,
        private val items: List<Module>
    ) : ArrayAdapter<Module>(context, R.layout.list_item_module, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val viewHolder: ViewHolder
            val view = convertView ?: LayoutInflater.from(context).inflate(
                R.layout.list_item_module,
                parent,
                false
            ).also {
                viewHolder = ViewHolder(
                    it.findViewById(R.id.tvModuleName),
                    it.findViewById(R.id.iconDelete2)
                )
                it.tag = viewHolder
            } ?: throw IllegalStateException("View cannot be null")

            val holder = view.tag as ViewHolder
            val module = getItem(position)

            holder.tvName.text = module?.nom ?: ""
            holder.btnDelete.setOnClickListener {
                module?.let { deleteModule(it.id, position) }
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