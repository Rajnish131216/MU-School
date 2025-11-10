package com.example.muschool

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvAdminName: TextView
    private lateinit var tvAdminEmail: TextView
    private lateinit var tvAdminRole: TextView
    private lateinit var imgAvatar: ShapeableImageView

    private lateinit var auth: FirebaseAuth
    private var userRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        auth = FirebaseAuth.getInstance()

        // Header views
        tvWelcome = findViewById(R.id.tvWelcome)
        tvAdminName = findViewById(R.id.tvAdminName)
        tvAdminEmail = findViewById(R.id.tvAdminEmail)
        tvAdminRole = findViewById(R.id.tvAdminRole)
        imgAvatar = findViewById(R.id.imgAvatar)

        bindCardClicks()
        loadAdminHeader()
    }

    private fun bindCardClicks() {
        findViewById<CardView>(R.id.cardManageTeachers).setOnClickListener {
            startActivity(Intent(this, ManageTeachersActivity::class.java))
        }

        findViewById<CardView>(R.id.cardManageStudents).setOnClickListener {
            startActivity(Intent(this, ManageStudentsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardViewResults).setOnClickListener {
            startActivity(Intent(this, ViewResultsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardManageSubjects).setOnClickListener {
            startActivity(Intent(this, ManageSubjectsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardManageClasses).setOnClickListener {
            startActivity(Intent(this, ManageClassesActivity::class.java))
        }

        findViewById<CardView>(R.id.cardAnnouncements).setOnClickListener {
            startActivity(Intent(this, ViewAnnouncementsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardAdminProfile).setOnClickListener {
            startActivity(Intent(this, AdminProfileUploaderActivity::class.java))
        }

        findViewById<CardView>(R.id.cardSettings).setOnClickListener {
            startActivity(Intent(this, AdminSettingsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, UsernameLoginActivity::class.java))
            finish()
        }
    }

    private fun loadAdminHeader() {
        val uid = auth.currentUser?.uid ?: return
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        userRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@AdminDashboardActivity, "Admin profile not found", Toast.LENGTH_SHORT).show()
                    return
                }

                val name = snapshot.child("name").getValue(String::class.java).orEmpty()
                val email = snapshot.child("email").getValue(String::class.java).orEmpty()
                val role = snapshot.child("role").getValue(String::class.java).orEmpty()
                val photoUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                tvWelcome.text = "Welcome, ${role.ifBlank { "Admin" }}"
                tvAdminName.text = "Name: ${if (name.isNotBlank()) name else "—"}"
                tvAdminEmail.text = "Email: ${if (email.isNotBlank()) email else "—"}"
                tvAdminRole.text = "Role: ${if (role.isNotBlank()) role else "Admin"}"

                loadAvatar(photoUrl)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminDashboardActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAvatar(url: String?) {
        if (!url.isNullOrBlank()) {
            Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.baseline_person_24)
                .error(R.drawable.baseline_person_24)
                .into(imgAvatar)
        } else {
            imgAvatar.setImageResource(R.drawable.baseline_person_24)
        }
    }
}
