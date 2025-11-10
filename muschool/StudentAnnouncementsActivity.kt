package com.example.muschool

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class StudentAnnouncementsActivity : AppCompatActivity() {

    private lateinit var layoutList: LinearLayout
    private lateinit var progressBar: ProgressBar

    private val dbRef = FirebaseDatabase.getInstance().getReference("Announcements")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_announcements)

        layoutList = findViewById(R.id.layoutAnnouncementList)
        progressBar = findViewById(R.id.progressBarAnnouncements)

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
                    val time = ann.child("timestamp").getValue(Long::class.java) ?: 0L
                    val formattedTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(time))

                    val tv = TextView(this).apply {
                        text = "📢 $title\n$message\n🕒 $formattedTime"
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
