package com.example.muschool

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPhone: EditText
    private lateinit var etFullName: EditText
    private lateinit var etDob: EditText
    private lateinit var spinnerClass: Spinner
    private lateinit var spinnerBatch: Spinner
    private lateinit var btnRegister: Button
    private lateinit var ivLoader: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference
    private lateinit var usernameMapRef: DatabaseReference
    private lateinit var studentRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Views
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etPhone = findViewById(R.id.etPhone)
        etFullName = findViewById(R.id.etFullName)
        etDob = findViewById(R.id.etDob)
        spinnerClass = findViewById(R.id.spinnerClass)
        spinnerBatch = findViewById(R.id.spinnerBatch)
        btnRegister = findViewById(R.id.btnRegister)
        ivLoader = findViewById(R.id.ivLoader)

        // Firebase
        auth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference("Users")
        usernameMapRef = FirebaseDatabase.getInstance().getReference("Usernames")
        studentRef = FirebaseDatabase.getInstance().getReference("Students")

        // Dropdown data
        val classList = listOf(
            "Select Class", "1","2","3","4","5","6","7","8","9","10","11","12"
        )
        val batchList = listOf("Select Batch", "A", "B", "C")

        spinnerClass.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, classList)
        spinnerBatch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, batchList)

        // GR No. based visibility (Student vs Teacher/Admin)
        etUsername.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) applyRoleVisibility()
        }

        // Also update visibility when user types (optional but handy)
        etUsername.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyRoleVisibility()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // DOB picker (open on click or focus)
        etDob.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { showDobPicker() }
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDobPicker() }
        }

        // Register
        btnRegister.setOnClickListener { onRegisterClicked() }
    }

    private fun applyRoleVisibility() {
        val prefix = etUsername.text.toString().trim().uppercase(Locale.getDefault())
        when {
            prefix.startsWith("S") -> {
                etFullName.visibility = View.VISIBLE
                spinnerClass.visibility = View.VISIBLE
                spinnerBatch.visibility = View.VISIBLE
                etDob.visibility = View.VISIBLE
            }
            prefix.startsWith("T") || prefix.startsWith("A") -> {
                etFullName.visibility = View.VISIBLE
                spinnerClass.visibility = View.GONE
                spinnerBatch.visibility = View.GONE
                etDob.visibility = View.GONE
            }
            else -> {
                etFullName.visibility = View.GONE
                spinnerClass.visibility = View.GONE
                spinnerBatch.visibility = View.GONE
                etDob.visibility = View.GONE
            }
        }
    }

    private fun onRegisterClicked() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val fullName = etFullName.text.toString().trim()
        val classSelected = spinnerClass.selectedItem.toString()
        val batchSelected = spinnerBatch.selectedItem.toString()
        val dob = etDob.text.toString().trim()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
            toast("All fields are required")
            return
        }

        val role = when {
            username.startsWith("S", true) -> "Student"
            username.startsWith("T", true) -> "Teacher"
            username.startsWith("A", true) -> "Admin"
            else -> {
                toast("GR No. must start with S, T, or A")
                return
            }
        }

        if (fullName.isEmpty()) {
            toast("Please enter Full Name")
            return
        }

        // Normalize phone to digits; basic 10-digit check for India
        val digitsOnlyPhone = phone.replace("\\D".toRegex(), "")
        if (!digitsOnlyPhone.matches(Regex("^[0-9]{10}$"))) {
            toast("Enter a valid 10-digit phone number")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Enter a valid email")
            return
        }

        if (password.length < 6) {
            toast("Password must be at least 6 characters")
            return
        }

        if (role == "Student") {
            if (classSelected == "Select Class" || batchSelected == "Select Batch") {
                toast("Please select Class and Batch")
                return
            }
            if (dob.isEmpty()) {
                toast("Please select Date of Birth")
                return
            }
        }

        // Show loader GIF (Glide)
        ivLoader.visibility = View.VISIBLE
        Glide.with(this).load(R.drawable.loader).into(ivLoader)

        // Check if GR already exists
        usernameMapRef.child(username).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                ivLoader.visibility = View.GONE
                toast("GR No. already registered")
            } else {
                // Create auth user
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        val uid = auth.currentUser?.uid
                        if (uid == null) {
                            ivLoader.visibility = View.GONE
                            toast("Registration failed. Try again.")
                            return@addOnSuccessListener
                        }

                        val user = mutableMapOf<String, Any>(
                            "username" to username,
                            "email" to email,
                            "phone" to digitsOnlyPhone,
                            "role" to role,
                            "name" to fullName
                        )

                        if (role == "Student") {
                            user["class"] = classSelected
                            user["batch"] = batchSelected
                            user["dob"] = dob
                        }

                        // Save to Users/{uid}
                        userRef.child(uid).setValue(user)

                        // Map username -> email (or uid, but keeping your original)
                        usernameMapRef.child(username).setValue(email)

                        // Extra student node
                        if (role == "Student") {
                            val studentData = mapOf(
                                "name" to fullName,
                                "grNo" to username,
                                "class" to classSelected,
                                "batch" to batchSelected,
                                "dob" to dob
                            )
                            studentRef.child(username).setValue(studentData)
                        }

                        ivLoader.visibility = View.GONE
                        toast("Registered as $role")
                        startActivity(Intent(this, UsernameLoginActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        ivLoader.visibility = View.GONE
                        toast("Error: ${it.message}")
                    }
            }
        }.addOnFailureListener {
            ivLoader.visibility = View.GONE
            toast("Error: ${it.message}")
        }
    }

    private fun showDobPicker() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(
            this,
            { _, y, m, d ->
                val mm = String.format(Locale.getDefault(), "%02d", m + 1)
                val dd = String.format(Locale.getDefault(), "%02d", d)
                etDob.setText("$y-$mm-$dd") // yyyy-MM-dd
            },
            year, month, day
        )
        // No future DOBs
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
