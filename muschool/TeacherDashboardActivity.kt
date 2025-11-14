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

class TeacherDashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvTeacherName: TextView
    private lateinit var tvTeacherDept: TextView
    private lateinit var tvTeacherClasses: TextView
    private lateinit var imgAvatar: ShapeableImageView

    private lateinit var auth: FirebaseAuth
    private var userRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)

        auth = FirebaseAuth.getInstance()

        tvWelcome = findViewById(R.id.tvWelcome)
        tvTeacherName = findViewById(R.id.tvTeacherName)
        tvTeacherDept = findViewById(R.id.tvTeacherDept)
        tvTeacherClasses = findViewById(R.id.tvTeacherClasses)
        imgAvatar = findViewById(R.id.imgAvatar)

        bindCardClicks()
        loadTeacherHeader()

        // 🔹 Profile image click → popup (Profile + Logout)
        imgAvatar.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            showProfileMenu()
        }
    }

    private fun bindCardClicks() {
        findViewById<CardView>(R.id.cardManageStudents).setOnClickListener {
            startActivity(Intent(this, ManageStudentsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardUploadMaterials).setOnClickListener {
            startActivity(Intent(this, UploadMaterialsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardEnterResults).setOnClickListener {
            startActivity(Intent(this, TeacherEnterResults::class.java))
        }
        findViewById<CardView>(R.id.cardViewResults).setOnClickListener {
            startActivity(Intent(this, ViewResultsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardCreateTimetable).setOnClickListener {
            startActivity(Intent(this, CreateTimetableActivity::class.java))
        }
        findViewById<CardView>(R.id.cardPostAnnouncements).setOnClickListener {
            startActivity(Intent(this, PostAnnouncementsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardManageClasses).setOnClickListener {
            startActivity(Intent(this, ManageClassesActivity::class.java))
        }
        findViewById<CardView>(R.id.cardManageAttendance).setOnClickListener {
            startActivity(Intent(this, TeacherAttendanceActivity::class.java))
        }
        findViewById<CardView>(R.id.cardAssignments).setOnClickListener {
            startActivity(Intent(this, TeacherUploadAssignment::class.java))
        }
        findViewById<CardView>(R.id.cardSettings).setOnClickListener {
            startActivity(Intent(this, TeacherSetting::class.java))
        }
    }

    // 🔹 Popup menu for Profile + Logout
    private fun showProfileMenu() {
        val wrapper = ContextThemeWrapper(this, R.style.CustomPopupMenuStyle)
        val popup = PopupMenu(wrapper, imgAvatar)
        popup.menuInflater.inflate(R.menu.menu_profile_popup, popup.menu)

        for (i in 0 until popup.menu.size()) {
            val item = popup.menu.getItem(i)
            val icon = item.icon
            icon?.setTint(getColor(android.R.color.white))
            item.icon = icon
        }

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_view_profile -> {
                    startActivity(Intent(this, TeacherProfile::class.java))
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

    private fun loadTeacherHeader() {
        val uid = auth.currentUser?.uid ?: return
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        userRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnap: DataSnapshot) {
                if (!userSnap.exists()) {
                    Toast.makeText(this@TeacherDashboardActivity, "User not found", Toast.LENGTH_SHORT).show()
                    return
                }

                val name = userSnap.child("name").getValue(String::class.java).orEmpty()
                val dept = userSnap.child("department").getValue(String::class.java).orEmpty()
                val classes = userSnap.child("class").getValue(String::class.java).orEmpty()
                val photoUrl = userSnap.child("profileImageUrl").getValue(String::class.java)

                tvWelcome.text = "Welcome, Teacher"
                tvTeacherName.text = "Name: ${if (name.isNotBlank()) name else "—"}"
                tvTeacherDept.text = "Department: ${if (dept.isNotBlank()) dept else "—"}"
                tvTeacherClasses.text = "Classes: ${if (classes.isNotBlank()) classes else "—"}"

                loadAvatar(photoUrl)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TeacherDashboardActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
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
