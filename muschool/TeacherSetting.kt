package com.example.muschool

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.muschool.R
import com.google.firebase.auth.FirebaseAuth

class TeacherSetting : AppCompatActivity() {

    private lateinit var btnResetTeacherData: Button
    private lateinit var btnChangeTeacherPassword: Button
    private lateinit var btnCheckFirebaseConnection: Button
    private lateinit var tvFirebaseStatus: TextView

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_setting)

        // Initialize views
        btnResetTeacherData = findViewById(R.id.btnResetTeacherData)
        btnChangeTeacherPassword = findViewById(R.id.btnChangeTeacherPassword)
        btnCheckFirebaseConnection = findViewById(R.id.btnCheckFirebaseConnection)
        tvFirebaseStatus = findViewById(R.id.tvFirebaseStatus)

        // Button actions
        btnResetTeacherData.setOnClickListener {
            Toast.makeText(this, "This would reset your data. [Future implementation]", Toast.LENGTH_SHORT).show()
        }

        btnChangeTeacherPassword.setOnClickListener {
            Toast.makeText(this, "Password change coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnCheckFirebaseConnection.setOnClickListener {
            tvFirebaseStatus.text = auth.currentUser?.let { "✅ Connected as ${it.email}" } ?: "❌ Not connected to Firebase"
        }
    }
}