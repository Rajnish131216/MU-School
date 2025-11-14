package com.example.muschool

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Header views
    private lateinit var tvWelcome: TextView
    private lateinit var tvStudentName: TextView
    private lateinit var tvStudentClass: TextView
    private lateinit var tvStudentBatch: TextView
    private lateinit var imgAvatar: ImageView

    private var userRef: DatabaseReference? = null
    private var studentRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        auth = FirebaseAuth.getInstance()

        // Bind header views
        tvWelcome = findViewById(R.id.tvWelcome)
        tvStudentName = findViewById(R.id.tvStudentName)
        tvStudentClass = findViewById(R.id.tvStudentClass)
        tvStudentBatch = findViewById(R.id.tvStudentBatch)
        imgAvatar = findViewById(R.id.imgAvatar)

        // Check session
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, UsernameLoginActivity::class.java))
            finish()
            return
        }

        // Load user data
        loadStudentHeader()

        // Profile image click → Gradient popup (Profile + Logout)
        imgAvatar.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            showProfileMenu()
        }

        // Dashboard cards
        findViewById<CardView>(R.id.cardTimetable).setOnClickListener {
            startActivity(Intent(this, StudentTimetableActivity::class.java))
        }

        findViewById<CardView>(R.id.cardAssignments).setOnClickListener {
            startActivity(Intent(this, StudentAssignmentsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardResults).setOnClickListener {
            startActivity(Intent(this, StudentResultsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardAttendance).setOnClickListener {
            startActivity(Intent(this, StudentViewAttendanceActivity::class.java))
        }

        findViewById<CardView>(R.id.cardMaterials).setOnClickListener {
            startActivity(Intent(this, StudentMaterialsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardAnnouncements).setOnClickListener {
            startActivity(Intent(this, StudentAnnouncementsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardSettings).setOnClickListener {
            startActivity(Intent(this, StudentSetting::class.java))
        }
    }

    // 🔹 Gradient popup menu (Profile + Logout)
    private fun showProfileMenu() {
        val wrapper = ContextThemeWrapper(this, R.style.CustomPopupMenuStyle)
        val popup = PopupMenu(wrapper, imgAvatar)
        popup.menuInflater.inflate(R.menu.menu_profile_popup, popup.menu)

        // Set icon tint white
        for (i in 0 until popup.menu.size()) {
            val item = popup.menu.getItem(i)
            val icon = item.icon
            icon?.setTint(getColor(android.R.color.white))
            item.icon = icon
        }

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_view_profile -> {
                    startActivity(Intent(this, StudentProfileActivity::class.java))
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

    private fun loadStudentHeader() {
        val uid = auth.currentUser?.uid ?: return
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        userRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@StudentDashboardActivity, "User not found", Toast.LENGTH_SHORT).show()
                    return
                }

                val name = snapshot.child("name").getValue(String::class.java).orEmpty()
                val username = snapshot.child("username").getValue(String::class.java).orEmpty()
                val role = snapshot.child("role").getValue(String::class.java).orEmpty()
                val userPhotoUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                tvWelcome.text = "Welcome, Student"
                tvStudentName.text = "Name: ${if (name.isNotBlank()) name else "—"}"

                if (role.equals("Student", ignoreCase = true) && username.isNotBlank()) {
                    studentRef = FirebaseDatabase.getInstance().getReference("Students").child(username)

                    studentRef?.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(studentSnap: DataSnapshot) {
                            if (!studentSnap.exists()) {
                                tvStudentClass.text = "Class: —"
                                tvStudentBatch.text = "Batch: —"
                                loadAvatar(userPhotoUrl)
                                return
                            }

                            val stdClass = studentSnap.child("class").getValue(String::class.java).orEmpty()
                            val batch = studentSnap.child("batch").getValue(String::class.java).orEmpty()
                            val studentPhotoUrl = studentSnap.child("profileImageUrl").getValue(String::class.java)

                            tvStudentClass.text = "Class: ${if (stdClass.isNotBlank()) stdClass else "—"}"
                            tvStudentBatch.text = "Batch: ${if (batch.isNotBlank()) batch else "—"}"

                            val finalUrl = when {
                                !studentPhotoUrl.isNullOrBlank() -> studentPhotoUrl
                                !userPhotoUrl.isNullOrBlank() -> userPhotoUrl
                                else -> null
                            }
                            loadAvatar(finalUrl)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            tvStudentClass.text = "Class: —"
                            tvStudentBatch.text = "Batch: —"
                            loadAvatar(userPhotoUrl)
                        }
                    })
                } else {
                    tvStudentClass.text = "Class: —"
                    tvStudentBatch.text = "Batch: —"
                    loadAvatar(userPhotoUrl)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@StudentDashboardActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAvatar(url: String?) {
        try {
            if (!url.isNullOrBlank()) {
                Glide.with(this@StudentDashboardActivity)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .circleCrop()
                    .into(imgAvatar)
            } else {
                imgAvatar.setImageResource(R.drawable.baseline_person_24)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            imgAvatar.setImageResource(R.drawable.baseline_person_24)
        }
    }
}
