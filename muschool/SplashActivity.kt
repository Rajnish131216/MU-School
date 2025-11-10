package com.example.muschool

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var loader: MuschoolLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // Loader layout

        loader = findViewById(R.id.muschool_loader)
        loader.show() // Start loader animation immediately

        auth = FirebaseAuth.getInstance()

        // Check user status right away
        val currentUser = auth.currentUser
        if (currentUser != null) {
            redirectToDashboard()
        } else {
            loader.hide()
            startActivity(Intent(this, UsernameLoginActivity::class.java))
            finish()
        }
    }

    private fun redirectToDashboard() {
        val uid = auth.currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance()
            .getReference("Users").child(uid)

        // Fetch role from Firebase
        userRef.get().addOnSuccessListener { snapshot ->
            val role = snapshot.child("role").value?.toString() ?: ""

            loader.hide() // Stop loader before navigation

            when (role) {
                "Student" -> startActivity(Intent(this, StudentDashboardActivity::class.java))
                "Teacher" -> startActivity(Intent(this, TeacherDashboardActivity::class.java))
                "Admin" -> startActivity(Intent(this, AdminDashboardActivity::class.java))
                else -> startActivity(Intent(this, UsernameLoginActivity::class.java))
            }

            finish()
        }.addOnFailureListener {
            loader.hide()
            startActivity(Intent(this, UsernameLoginActivity::class.java))
            finish()
        }
    }
}
