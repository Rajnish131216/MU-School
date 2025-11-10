package com.example.muschool

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class TeacherUploadAssignment : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var spinnerClass: Spinner
    private lateinit var spinnerBatch: Spinner
    private lateinit var etDriveLink: EditText
    private lateinit var btnSelectFile: Button
    private lateinit var btnUpload: Button
    private lateinit var tvSelectedFile: TextView
    private var fileUri: Uri? = null

    private val dbRef = FirebaseDatabase.getInstance().getReference("Assignments")

    private val cloudName = "dq8qxywz2"  // your Cloudinary cloud name
    private val unsignedPreset = "Assignment"  // your unsigned preset

    private var progressDialog: AlertDialog? = null

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                fileUri = uri
                tvSelectedFile.text = "📄 Selected: ${queryDisplayName(uri) ?: uri.lastPathSegment}"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_upload_assignment)

        initCloudinary()

        etTitle = findViewById(R.id.etAssignmentTitle)
        spinnerClass = findViewById(R.id.spinnerClass)
        spinnerBatch = findViewById(R.id.spinnerBatch)
        etDriveLink = findViewById(R.id.etDriveLink)
        btnSelectFile = findViewById(R.id.btnSelectFile)
        btnUpload = findViewById(R.id.btnUpload)
        tvSelectedFile = findViewById(R.id.tvSelectedFile)

        spinnerClass.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("1","2","3","4","5","6","7","8","9", "10", "11", "12"))
        spinnerBatch.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("A", "B", "C"))

        btnSelectFile.setOnClickListener { pickFileLauncher.launch("application/pdf") }

        btnUpload.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val className = spinnerClass.selectedItem.toString()
            val batchName = spinnerBatch.selectedItem.toString()
            val driveLink = etDriveLink.text.toString().trim()
            val uri = fileUri

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter assignment title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // If user pasted Google Drive link, skip Cloudinary
            if (driveLink.isNotEmpty()) {
                if (!driveLink.startsWith("http")) {
                    Toast.makeText(this, "Invalid link format", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                saveToFirebase(
                    title = title,
                    fileName = "Drive link",
                    className = className,
                    batchName = batchName,
                    url = driveLink,
                    sizeBytes = 0L,
                    provider = "drive"
                )
                return@setOnClickListener
            }

            // Otherwise, upload to Cloudinary
            if (uri == null) {
                Toast.makeText(this, "Please select a file or paste a Drive link", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showProgress("Uploading...")
            uploadToCloudinary(uri, title, className, batchName)
        }
    }

    private fun initCloudinary() {
        val config = mapOf("cloud_name" to cloudName)
        try {
            MediaManager.init(applicationContext, config)
        } catch (_: Exception) {
            // Already initialized
        }
    }

    private fun showProgress(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(message)
        val progressBar = ProgressBar(this)
        val padding = (16 * resources.displayMetrics.density).toInt()
        progressBar.setPadding(padding, padding, padding, padding)
        builder.setView(progressBar)
        builder.setCancelable(false)
        progressDialog = builder.create()
        progressDialog?.show()
    }

    private fun updateProgress(message: String) {
        progressDialog?.setTitle(message)
    }

    private fun hideProgress() {
        progressDialog?.dismiss()
    }

    private fun uploadToCloudinary(uri: Uri, title: String, className: String, batchName: String) {
        val displayName = queryDisplayName(uri) ?: "assignment_${System.currentTimeMillis()}.pdf"
        val publicId = "assignments/$className/$batchName/${UUID.randomUUID()}"

        MediaManager.get().upload(uri)
            .unsigned(unsignedPreset)
            .option("public_id", publicId)
            .option("folder", "Assignments")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val percent = if (totalBytes > 0) (bytes * 100 / totalBytes).toInt() else 0
                    runOnUiThread { updateProgress("Uploading $percent%") }
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String
                    val size = (resultData["bytes"] as? Number)?.toLong() ?: 0L

                    if (url.isNullOrBlank()) {
                        onError(requestId, ErrorInfo(-1, "No URL received"))
                        return
                    }

                    saveToFirebase(title, displayName, className, batchName, url, size, "cloudinary")
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    runOnUiThread {
                        hideProgress()
                        Toast.makeText(
                            this@TeacherUploadAssignment,
                            "Upload failed: ${error.description}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    onError(requestId, error)
                }
            })
            .dispatch()
    }

    private fun saveToFirebase(
        title: String,
        fileName: String,
        className: String,
        batchName: String,
        url: String,
        sizeBytes: Long,
        provider: String
    ) {
        val id = dbRef.child(className).child(batchName).push().key ?: UUID.randomUUID().toString()
        val assignment = mapOf(
            "id" to id,
            "title" to title,
            "fileName" to fileName,
            "url" to url,
            "class" to className,
            "batch" to batchName,
            "uploadedAt" to System.currentTimeMillis(),
            "sizeBytes" to sizeBytes,
            "provider" to provider
        )

        dbRef.child(className).child(batchName).child(id).setValue(assignment)
            .addOnSuccessListener {
                hideProgress()
                Toast.makeText(this, "Assignment uploaded successfully", Toast.LENGTH_SHORT).show()
                etTitle.text.clear()
                etDriveLink.text.clear()
                tvSelectedFile.text = ""
                fileUri = null
            }
            .addOnFailureListener {
                hideProgress()
                Toast.makeText(this, "Firebase save failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (_: Exception) {
            null
        }
    }
}
