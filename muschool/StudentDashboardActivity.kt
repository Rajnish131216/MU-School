package com.example.muschool

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
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
        // Optional: enable Material motion between screens if theme supports it
        // See Material Motion docs for exact setup in themes/activities
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        auth = FirebaseAuth.getInstance()

        // Bind header
        tvWelcome = findViewById(R.id.tvWelcome)
        tvStudentName = findViewById(R.id.tvStudentName)
        tvStudentClass = findViewById(R.id.tvStudentClass)
        tvStudentBatch = findViewById(R.id.tvStudentBatch)
        imgAvatar = findViewById(R.id.imgAvatar)

        // Fetch and render header data
        loadStudentHeader()

        // Cards navigation
        findViewById<CardView>(R.id.cardProfile).setOnClickListener {
            startActivity(Intent(this, StudentProfileActivity::class.java))
        }
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
        findViewById<CardView>(R.id.cardLogout).setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, UsernameLoginActivity::class.java))
            finish()
        }
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
                val userPhotoUrl = snapshot.child("profileImageUrl").getValue(String::class.java) // optional

                tvWelcome.text = "Welcome, Student"
                tvStudentName.text = "Name: ${if (name.isNotBlank()) name else "—"}"

                if (role == "Student" && username.isNotBlank()) {
                    studentRef = FirebaseDatabase.getInstance().getReference("Students").child(username)
                    studentRef?.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(studentSnap: DataSnapshot) {
                            val stdClass = studentSnap.child("class").getValue(String::class.java).orEmpty()
                            val batch = studentSnap.child("batch").getValue(String::class.java).orEmpty()
                            val studentPhotoUrl = studentSnap.child("profileImageUrl").getValue(String::class.java) // optional

                            tvStudentClass.text = "Class: ${if (stdClass.isNotBlank()) stdClass else "—"}"
                            tvStudentBatch.text = "Batch: ${if (batch.isNotBlank()) batch else "—"}"

                            // Prefer student photoUrl if present; fallback to user's photoUrl; else keep default icon
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
        if (!url.isNullOrBlank()) {
            // Load via Glide after obtaining the URL string from Firebase
            Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.baseline_person_24)
                .error(R.drawable.baseline_person_24)
                .circleCrop() // if images are square, this makes them circular inside the colored bg
                .into(imgAvatar)
        } else {
            // Keep default drawable if no URL
            imgAvatar.setImageResource(R.drawable.baseline_person_24)
        }
    }
}
