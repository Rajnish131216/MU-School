package com.example.muschool

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class UsernameLoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var btnForgotPassword: TextView
    private lateinit var tvNoInternet: TextView
    private lateinit var formLayout: LinearLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var usernameMapRef: DatabaseReference
    private lateinit var userRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_username_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        btnForgotPassword = findViewById(R.id.btnForgotPassword)
        tvNoInternet = findViewById(R.id.tvNoInternet)
        formLayout = findViewById(R.id.formLayout)

        auth = FirebaseAuth.getInstance()
        usernameMapRef = FirebaseDatabase.getInstance().getReference("Usernames")
        userRef = FirebaseDatabase.getInstance().getReference("Users")

        // 🌐 Internet Check
        if (!NetworkUtils.isInternetAvailable(this)) {
            formLayout.visibility = View.GONE
            tvNoInternet.visibility = View.VISIBLE
        } else {
            formLayout.visibility = View.VISIBLE
            tvNoInternet.visibility = View.GONE
        }

        // 📝 Go to Register
        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // 🔐 Login Logic
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            usernameMapRef.child(username).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val email = snapshot.value.toString()
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = auth.currentUser?.uid
                                    if (uid != null) {
                                        userRef.child(uid).get()
                                            .addOnSuccessListener { userSnap ->
                                                val role =
                                                    userSnap.child("role").value?.toString() ?: ""

                                                val intent = when (role) {
                                                    "Student" -> Intent(
                                                        this,
                                                        StudentDashboardActivity::class.java
                                                    )

                                                    "Teacher" -> Intent(
                                                        this,
                                                        TeacherDashboardActivity::class.java
                                                    )

                                                    "Admin" -> Intent(
                                                        this,
                                                        AdminDashboardActivity::class.java
                                                    )

                                                    else -> null
                                                }

                                                if (intent != null) {
                                                    intent.addFlags(
                                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                    )
                                                    startActivity(intent)
                                                    finish()
                                                } else {
                                                    Toast.makeText(
                                                        this,
                                                        "Invalid role",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(
                                                    this,
                                                    "Error: ${it.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Login failed: user is null",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Login failed: ${task.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }

        }
    }
}
