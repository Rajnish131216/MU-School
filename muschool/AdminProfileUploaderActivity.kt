package com.example.muschool

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody.Part
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class AdminProfileUploaderActivity : AppCompatActivity() {

    private lateinit var spUserType: Spinner
    private lateinit var spUser: Spinner
    private lateinit var ivPreview: ImageView
    private lateinit var btnPick: Button
    private lateinit var btnUpload: Button
    private lateinit var tvStatus: TextView

    private val db: FirebaseDatabase by lazy { Firebase.database }
    private val usersRef: DatabaseReference by lazy { db.getReference("Users") }

    private var pickedUri: Uri? = null
    private var cameraPhotoUri: Uri? = null
    private var selectedUserId: String? = null
    private var selectedRole: String = "Student"

    // Cloudinary config
    private val CLOUDINARY_CLOUD_NAME = "dq8qxywz2"
    private val CLOUDINARY_UPLOAD_PRESET = "ProfilePicture"

    // Permission handling
    private val requestCameraPermission =
        registerForActivityResult(RequestPermission()) { granted ->
            if (granted) launchCamera() else toast("Camera permission denied")
        }

    // Capture photo
    private val captureImage = registerForActivityResult(TakePicture()) { success ->
        if (success && cameraPhotoUri != null) {
            pickedUri = cameraPhotoUri
            Glide.with(this).load(pickedUri).centerCrop().into(ivPreview)
            tvStatus.text = "📸 Photo captured"
        } else {
            tvStatus.text = "❌ Capture cancelled"
        }
    }

    // Pick from gallery
    private val pickFromGallery = registerForActivityResult(GetContent()) { uri ->
        if (uri != null) {
            pickedUri = uri
            Glide.with(this).load(uri).centerCrop().into(ivPreview)
            tvStatus.text = "🖼️ Image selected"
        }
    }

    data class SimpleUser(
        val id: String,
        val name: String? = null,
        val email: String? = null,
        val role: String? = null
    )

    private val userList = mutableListOf<SimpleUser>()
    private val userDisplayList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_profile_uploader)

        // Initialize views
        spUserType = findViewById(R.id.spUserType)
        spUser = findViewById(R.id.spUser)
        ivPreview = findViewById(R.id.ivPreview)
        btnPick = findViewById(R.id.btnPick)
        btnUpload = findViewById(R.id.btnUpload)
        tvStatus = findViewById(R.id.tvStatus)

        // Add "Admin" role
        val roles = listOf("Student", "Teacher", "Admin")
        spUserType.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)

        spUserType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long
            ) {
                selectedRole = roles[pos]
                loadUsersByRole(selectedRole)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spUser.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long
            ) {
                if (pos in userList.indices) selectedUserId = userList[pos].id
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Pick image
        btnPick.setOnClickListener { showImageSourceDialog() }

        // Upload image
        btnUpload.setOnClickListener {
            val uid = selectedUserId
            val uri = pickedUri
            if (uid.isNullOrEmpty()) {
                toast("⚠️ Please select a user first")
                return@setOnClickListener
            }
            if (uri == null) {
                toast("⚠️ Please choose an image")
                return@setOnClickListener
            }

            tvStatus.text = "☁️ Uploading to Cloudinary..."
            uploadToCloudinary(uri) { success, urlOrMsg ->
                tvStatus.text =
                    if (success) "Saving image URL to Firebase..." else "❌ Upload failed: $urlOrMsg"
                if (success) {
                    saveUrlToFirebase(uid, urlOrMsg) { ok, msg ->
                        tvStatus.text =
                            if (ok) "✅ Profile image updated successfully" else "❌ Failed: $msg"
                        if (ok) toast("Profile image updated for $selectedRole")
                    }
                }
            }
        }
    }

    // --- Image Source Selection ---
    private fun showImageSourceDialog() {
        val items = arrayOf("📷 Take Photo", "🖼️ Choose from Gallery")
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Image Source")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> checkCameraAndOpen()
                    1 -> pickFromGallery.launch("image/*")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- Camera Handling ---
    private fun checkCameraAndOpen() {
        val granted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        try {
            val photoFile = File.createTempFile("capture_", ".jpg", cacheDir)
            cameraPhotoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )
            cameraPhotoUri?.let { captureImage.launch(it) } ?: toast("❌ Cannot create URI")
        } catch (e: Exception) {
            toast("⚠️ Cannot open camera: ${e.message}")
        }
    }

    // --- Firebase: Load Users by Role ---
    private fun loadUsersByRole(role: String) {
        tvStatus.text = "📡 Loading $role list..."
        usersRef.orderByChild("role").equalTo(role)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userList.clear()
                    userDisplayList.clear()
                    for (child in snapshot.children) {
                        val id = child.key ?: continue
                        val name = child.child("name").getValue(String::class.java)
                        val email = child.child("email").getValue(String::class.java)
                        val roleVal = child.child("role").getValue(String::class.java)
                        userList.add(SimpleUser(id, name, email, roleVal))
                        userDisplayList.add("${name ?: "No Name"} • ${email ?: ""}")
                    }

                    spUser.adapter = ArrayAdapter(
                        this@AdminProfileUploaderActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        if (userDisplayList.isEmpty()) listOf("No $role users found") else userDisplayList
                    )

                    selectedUserId = userList.firstOrNull()?.id
                    tvStatus.text = "✅ Loaded ${userList.size} $role user(s)"
                }

                override fun onCancelled(error: DatabaseError) {
                    tvStatus.text = "❌ Load failed: ${error.message}"
                }
            })
    }

    // --- Cloudinary Upload ---
    private fun uploadToCloudinary(uri: Uri, callback: (Boolean, String) -> Unit) {
        try {
            val tempFile = createTempImageFromUri(uri) ?: run {
                callback(false, "Cannot read image file")
                return
            }

            val url = "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload"
            val mime = contentResolver.getType(uri) ?: "image/jpeg"
            val reqBody = tempFile.asRequestBody(mime.toMediaTypeOrNull())

            val body = Part.createFormData("file", tempFile.name, reqBody)
            val uploadPreset = Part.createFormData("upload_preset", CLOUDINARY_UPLOAD_PRESET)

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(body)
                .addPart(uploadPreset)
                .build()

            val request = Request.Builder().url(url).post(multipart).build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    runOnUiThread { callback(false, e.message ?: "Network error") }
                }

                override fun onResponse(call: Call, response: Response) {
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        runOnUiThread { callback(false, "HTTP ${response.code}: $bodyStr") }
                        return
                    }
                    val json = JSONObject(bodyStr)
                    val secureUrl = json.optString("secure_url")
                    runOnUiThread {
                        if (secureUrl.isNullOrEmpty()) callback(false, "No URL in response")
                        else callback(true, secureUrl)
                    }
                }
            })
        } catch (e: Exception) {
            callback(false, e.message ?: "Upload error")
        }
    }

    // --- Save Cloudinary URL to Firebase ---
    private fun saveUrlToFirebase(userId: String, url: String, cb: (Boolean, String) -> Unit) {
        usersRef.child(userId).child("profileImageUrl").setValue(url)
            .addOnSuccessListener { cb(true, "ok") }
            .addOnFailureListener { cb(false, it.message ?: "Database error") }
    }

    // --- Temp File Helper ---
    private fun createTempImageFromUri(uri: Uri): File? {
        return try {
            val input = contentResolver.openInputStream(uri) ?: return null
            val suffix = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(contentResolver.getType(uri)) ?: "jpg"
            val file = File.createTempFile("upload_", ".$suffix", cacheDir)
            FileOutputStream(file).use { out -> input.copyTo(out) }
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
