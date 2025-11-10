package com.example.muschool

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnDashboard: Button
    private lateinit var btnLogout: Button
    private lateinit var loader: MuschoolLoader
    private var userRole: String? = null
    private lateinit var networkReceiver: BroadcastReceiver
    private lateinit var connectivityCallback: ConnectivityManager.NetworkCallback


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        btnDashboard = findViewById(R.id.btnGoToDashboard)
        btnLogout = findViewById(R.id.btnLogout)
        loader = findViewById(R.id.muschool_loader)
        auth = FirebaseAuth.getInstance()

        networkReceiver = NetworkChangeReceiver()

        val uid = auth.currentUser?.uid
        if (uid == null) {
            startActivity(Intent(this, UsernameLoginActivity::class.java))
            finish()
            return
        }

        // Show loader while fetching user role
        loader.show()
        hideMainContent()

        // Fetch user role from Firebase
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
            .get()
            .addOnSuccessListener { snapshot ->
                userRole = snapshot.value.toString()

                // Hide loader and show main content after data is loaded
                Handler(Looper.getMainLooper()).postDelayed({
                    loader.hide()
                    showMainContent()
                }, 1500) // Show loader for at least 1.5 seconds for better UX
            }
            .addOnFailureListener {
                loader.hide()
                showMainContent()
                Toast.makeText(this, "Failed to load role", Toast.LENGTH_SHORT).show()
            }

        btnDashboard.setOnClickListener {
            // Show loader while navigating
            loader.show()
            hideMainContent()

            Handler(Looper.getMainLooper()).postDelayed({
                when (userRole) {
                    "Student" -> startActivity(Intent(this, StudentDashboardActivity::class.java))
                    "Teacher" -> startActivity(Intent(this, TeacherDashboardActivity::class.java))
                    "Admin" -> startActivity(Intent(this, AdminDashboardActivity::class.java))
                    else -> {
                        loader.hide()
                        showMainContent()
                        Toast.makeText(this, "Unknown role!", Toast.LENGTH_SHORT).show()
                        return@postDelayed
                    }
                }
                // Hide loader after navigation
                loader.hide()
            }, 800) // Brief loading animation
        }

        btnLogout.setOnClickListener {
            // Show loader while logging out
            loader.show()
            hideMainContent()

            Handler(Looper.getMainLooper()).postDelayed({
                auth.signOut()
                startActivity(Intent(this, UsernameLoginActivity::class.java))
                finish()
            }, 1000)
        }
    }

    private fun hideMainContent() {
        btnDashboard.visibility = android.view.View.GONE
        btnLogout.visibility = android.view.View.GONE
    }

    private fun showMainContent() {
        btnDashboard.visibility = android.view.View.VISIBLE
        btnLogout.visibility = android.view.View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Network became available
            }
            override fun onLost(network: Network) {
                // Network lost
            }
        }
        cm.registerDefaultNetworkCallback(connectivityCallback)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(networkReceiver)
    }
}