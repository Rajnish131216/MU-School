package com.example.muschool

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.*

class ViewAnnouncementsActivity : AppCompatActivity() {

    private lateinit var announcementsLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var etFilterClass: EditText
    private lateinit var btnFilter: Button

    private val dbRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("Announcements")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_announcements)

        announcementsLayout = findViewById(R.id.layoutAnnouncements)
        progressBar = findViewById(R.id.progressBarAnnouncements)
        etFilterClass = findViewById(R.id.etFilterClass)
        btnFilter = findViewById(R.id.btnFilter)

        btnFilter.setOnClickListener {
            val filter = etFilterClass.text.toString().trim()
            loadAnnouncements(filter)
        }

        loadAnnouncements()
    }

    private fun loadAnnouncements(filterClass: String = "") {
        announcementsLayout.removeAllViews()
        progressBar.visibility = View.VISIBLE

        dbRef.get().addOnSuccessListener { snapshot ->
            progressBar.visibility = View.GONE
            if (snapshot.exists()) {
                for (announcement in snapshot.children) {
                    val id = announcement.key ?: continue
                    val title = announcement.child("title").getValue(String::class.java)
                    val message = announcement.child("message").getValue(String::class.java)
                    val date = announcement.child("date").getValue(String::class.java)
                    val clazz = announcement.child("class").getValue(String::class.java) ?: "All"

                    // Skip if filter applied
                    if (filterClass.isNotEmpty() && filterClass.lowercase() != clazz.lowercase()) continue

                    val container = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundResource(R.drawable.card_border)
                        setPadding(24, 16, 24, 16)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 0, 0, 24) }
                    }

                    val titleView = TextView(this).apply {
                        text = "📢 $title\n🗓️ $date\n📚 Class: $clazz\n$message"
                        textSize = 16f
                    }

                    val btnDelete = Button(this).apply {
                        text = "🗑 Delete"
                        setBackgroundColor(ContextCompat.getColor(this@ViewAnnouncementsActivity, android.R.color.holo_red_light))
                        setTextColor(ContextCompat.getColor(this@ViewAnnouncementsActivity, android.R.color.white))
                        setOnClickListener {
                            showDeleteDialog(id)
                        }
                    }

                    container.addView(titleView)
                    container.addView(btnDelete)
                    announcementsLayout.addView(container)
                }
            } else {
                announcementsLayout.addView(TextView(this).apply {
                    text = "No announcements found."
                    gravity = Gravity.CENTER
                    setPadding(8, 8, 8, 8)
                })
            }
        }.addOnFailureListener {
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Error loading announcements", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteDialog(id: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Announcement")
            .setMessage("Are you sure you want to delete this announcement?")
            .setPositiveButton("Yes") { _, _ ->
                dbRef.child(id).removeValue().addOnSuccessListener {
                    Toast.makeText(this, "Announcement deleted", Toast.LENGTH_SHORT).show()
                    loadAnnouncements()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
