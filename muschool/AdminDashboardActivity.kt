package com.example.muschool

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.imageview.ShapeableImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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

        tvWelcome = findViewById(R.id.tvWelcome)
        tvAdminName = findViewById(R.id.tvAdminName)
        tvAdminEmail = findViewById(R.id.tvAdminEmail)
        tvAdminRole = findViewById(R.id.tvAdminRole)
        imgAvatar = findViewById(R.id.imgAvatar)

        bindCardClicks()
        loadAdminHeader()

        // 🧑‍💼 Profile picture click → Popup (Profile + Logout)
        imgAvatar.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            showProfileMenu()
        }
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
            startActivity(Intent(this, PostAnnouncementsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardSettings).setOnClickListener {
            startActivity(Intent(this, AdminSettingsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardAdminProfile).setOnClickListener {
            startActivity(Intent(this, AdminProfileUploaderActivity::class.java))
        }
        // ✅ New: Assign Teacher Class & Subject
        findViewById<CardView>(R.id.cardAssignTeacherClass)?.setOnClickListener {
            startActivity(Intent(this, AdminAssignTeacherActivity::class.java))
        }
    }

    // 🔹 Popup Menu for Profile and Logout
    private fun showProfileMenu() {
        val wrapper = ContextThemeWrapper(this, R.style.CustomPopupMenuStyle)
        val popup = PopupMenu(wrapper, imgAvatar)
        popup.menuInflater.inflate(R.menu.menu_profile_popup, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_view_profile -> {
                    startActivity(Intent(this, AdminProfileActivity::class.java))
                    true
                }
                R.id.menu_logout -> {
                    auth.signOut()
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, UsernameLoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        popup.setForceShowIcon(true)
        popup.show()
    }

    private fun loadAdminHeader() {
        val uid = auth.currentUser?.uid ?: return
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        userRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnap: DataSnapshot) {
                if (!userSnap.exists()) {
                    Toast.makeText(this@AdminDashboardActivity, "User not found", Toast.LENGTH_SHORT).show()
                    return
                }

                val name = userSnap.child("name").getValue(String::class.java).orEmpty()
                val email = userSnap.child("email").getValue(String::class.java).orEmpty()
                val role = userSnap.child("role").getValue(String::class.java).orEmpty()
                val photoUrl = userSnap.child("profileImageUrl").getValue(String::class.java)

                tvWelcome.text = "Welcome, Admin"
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
                .circleCrop()
                .into(imgAvatar)
        } else {
            imgAvatar.setImageResource(R.drawable.baseline_person_24)
        }
    }
}
