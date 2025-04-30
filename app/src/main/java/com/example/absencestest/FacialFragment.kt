package com.example.absencestest

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class FacialFragement : Fragment() {

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_facial, container, false)

        val btnDetection: Button = view.findViewById(R.id.btnDetection)

        btnDetection.setOnClickListener {
            // Intent pour démarrer CameraActivity
            val intent = Intent(requireContext(), CameraActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}