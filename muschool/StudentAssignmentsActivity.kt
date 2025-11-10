package com.example.muschool

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class StudentAssignmentsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var rv: RecyclerView
    private lateinit var progress: ProgressBar
    private val items = mutableListOf<MaterialItem>()
    private lateinit var adapter: MaterialsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_assignments) // Assignment XML

        auth = FirebaseAuth.getInstance()
        rv = findViewById(R.id.rvAssignments)
        progress = findViewById(R.id.progressAssignments)

        rv.layoutManager = LinearLayoutManager(this)
        adapter = MaterialsAdapter(
            data = items, // Use "data" not "items"
            onView = { title, url -> openPdfPreview(title, url) },
            onDownload = { title, url -> downloadPdf(title, url) }
        )
        rv.adapter = adapter

        loadStudentAssignments()
    }

    private fun loadStudentAssignments() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show()
            return
        }

        progress.visibility = View.VISIBLE
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnap: DataSnapshot) {
                val className = userSnap.child("class").getValue(String::class.java).orEmpty()
                val batchName = userSnap.child("batch").getValue(String::class.java).orEmpty()

                if (className.isBlank() || batchName.isBlank()) {
                    progress.visibility = View.GONE
                    Toast.makeText(
                        this@StudentAssignmentsActivity,
                        "Class or batch not set in profile",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val dbRef = FirebaseDatabase.getInstance()
                    .getReference("Assignments") // changed from StudyMaterials
                    .child(className)
                    .child(batchName)

                dbRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        items.clear()
                        for (child in snapshot.children) {
                            val item = MaterialItem(
                                id = child.child("id").getValue(String::class.java).orEmpty(),
                                title = child.child("title").getValue(String::class.java).orEmpty(),
                                fileName = child.child("fileName").getValue(String::class.java).orEmpty(),
                                url = child.child("url").getValue(String::class.java).orEmpty(),
                                provider = child.child("provider").getValue(String::class.java).orEmpty(),
                                `class` = className,
                                batch = batchName,
                                uploadedAt = child.child("uploadedAt").getValue(Long::class.java) ?: 0L,
                                sizeBytes = child.child("sizeBytes").getValue(Long::class.java) ?: 0L
                            )
                            if (item.url.isNotBlank()) items.add(item)
                        }

                        items.sortByDescending { it.uploadedAt }
                        adapter.notifyDataSetChanged()
                        progress.visibility = View.GONE
                    }

                    override fun onCancelled(error: DatabaseError) {
                        progress.visibility = View.GONE
                        Toast.makeText(
                            this@StudentAssignmentsActivity,
                            "Error: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                progress.visibility = View.GONE
                Toast.makeText(
                    this@StudentAssignmentsActivity,
                    "Error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun openPdfPreview(title: String, url: String) {
        if (url.isBlank()) {
            Toast.makeText(this, "Invalid PDF URL", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, PDFPreviewActivity::class.java)
        intent.putExtra("title", title)
        intent.putExtra("url", url)
        startActivity(intent)
    }

    private fun String.toDevicePdfUrl(): String {
        var u = this
        if (contains("cloudinary", ignoreCase = true)) {
            u = u.replace("/image/upload/", "/raw/upload/")
                .replace("/video/upload/", "/raw/upload/")
        }
        return if (u.lowercase().endsWith(".pdf")) u else "$u?ext=pdf"
    }

    private fun downloadPdf(title: String, originalUrl: String) {
        val url = originalUrl.toDevicePdfUrl()
        try {
            val safeTitle = title.ifBlank { "assignment" }
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val fileName = "$safeTitle.pdf"

            val req = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Downloading assignment")
                .setMimeType("application/pdf")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(req)

            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("Assignments", "Download failed: ${e.message}")
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
