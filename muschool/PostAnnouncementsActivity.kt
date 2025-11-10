package com.example.muschool

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class PostAnnouncementsActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etMessage: EditText
    private lateinit var btnPost: Button
    private lateinit var layoutList: LinearLayout
    private lateinit var progressBar: ProgressBar

    private val dbRef = FirebaseDatabase.getInstance().getReference("Announcements")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_announcements)

        etTitle = findViewById(R.id.etAnnouncementTitle)
        etMessage = findViewById(R.id.etAnnouncementMessage)
        btnPost = findViewById(R.id.btnPostAnnouncement)
        layoutList = findViewById(R.id.layoutAnnouncementList)
        progressBar = findViewById(R.id.progressBarAnnouncements)

        btnPost.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val message = etMessage.text.toString().trim()

            if (title.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Please fill both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val id = dbRef.push().key ?: return@setOnClickListener
            val announcement = mapOf(
                "id" to id,
                "title" to title,
                "message" to message,
                "timestamp" to System.currentTimeMillis()
            )

            dbRef.child(id).setValue(announcement).addOnSuccessListener {
                Toast.makeText(this, "✅ Announcement Posted", Toast.LENGTH_SHORT).show()
                etTitle.text.clear()
                etMessage.text.clear()
                loadAnnouncements()
            }.addOnFailureListener {
                Toast.makeText(this, "❌ Failed to post", Toast.LENGTH_SHORT).show()
            }
        }

        loadAnnouncements()
    }

    private fun loadAnnouncements() {
        layoutList.removeAllViews()
        progressBar.visibility = View.VISIBLE

        dbRef.get().addOnSuccessListener { snapshot ->
            progressBar.visibility = View.GONE
            if (snapshot.exists()) {
                for (ann in snapshot.children.reversed()) {
                    val title = ann.child("title").getValue(String::class.java)
                    val message = ann.child("message").getValue(String::class.java)

                    val tv = TextView(this).apply {
                        text = "📢 $title\n$message"
                        textSize = 16f
                        setPadding(16, 16, 16, 16)
                    }
                    layoutList.addView(tv)
                }
            } else {
                layoutList.addView(TextView(this).apply {
                    text = "No announcements yet."
                    setPadding(16, 16, 16, 16)
                })
            }
        }.addOnFailureListener {
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Failed to load announcements", Toast.LENGTH_SHORT).show()
        }
    }
}
