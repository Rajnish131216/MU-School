package com.example.muschool

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class AdminSettingsActivity : AppCompatActivity() {

    private lateinit var btnResetAdminData: Button
    private lateinit var btnChangeAdminPassword: Button
    private lateinit var btnCheckFirebaseConnection: Button
    private lateinit var tvFirebaseStatus: TextView

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_settings)

        // Initialize views
        btnResetAdminData = findViewById(R.id.btnResetAdminData)
        btnChangeAdminPassword = findViewById(R.id.btnChangeAdminPassword)
        btnCheckFirebaseConnection = findViewById(R.id.btnCheckFirebaseConnection)
        tvFirebaseStatus = findViewById(R.id.tvFirebaseStatus)

        // Button actions (same as Teacher)
        btnResetAdminData.setOnClickListener {
            Toast.makeText(this, "This would reset your data. [Future implementation]", Toast.LENGTH_SHORT).show()
        }

        btnChangeAdminPassword.setOnClickListener {
            Toast.makeText(this, "Password change coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnCheckFirebaseConnection.setOnClickListener {
            tvFirebaseStatus.text = auth.currentUser?.let { "✅ Connected as ${it.email}" }
                ?: "❌ Not connected to Firebase"
        }
    }
}
