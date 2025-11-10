package com.example.muschool

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.*

class ManageTeachersActivity : AppCompatActivity() {

    private lateinit var layoutTeachersList: LinearLayout
    private lateinit var loaderContainer: FrameLayout
    private lateinit var loaderImage: ImageView

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etDepartment: EditText
    private lateinit var etClasses: EditText
    private lateinit var btnAddTeacher: Button

    private val dbRef by lazy { FirebaseDatabase.getInstance().getReference("Users") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_teachers)

        // Views
        layoutTeachersList = findViewById(R.id.layoutTeachersList)
        loaderContainer = findViewById(R.id.loaderContainer)
        loaderImage = findViewById(R.id.imageLoader)

        etName = findViewById(R.id.etTeacherName)
        etEmail = findViewById(R.id.etTeacherEmail)
        etDepartment = findViewById(R.id.etTeacherDepartment)
        etClasses = findViewById(R.id.etTeacherClass)
        btnAddTeacher = findViewById(R.id.btnAddTeacher)

        Glide.with(this).asGif().load(R.drawable.loader).into(loaderImage)

        btnAddTeacher.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val department = etDepartment.text.toString().trim()
            val classes = etClasses.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || department.isEmpty() || classes.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val id = dbRef.push().key ?: return@setOnClickListener
            val teacherMap = mapOf(
                "name" to name,
                "email" to email,
                "department" to department,
                "class" to classes,
                "role" to "Teacher",
                "username" to "T${(100..999).random()}"
            )

            dbRef.child(id).setValue(teacherMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Teacher added successfully", Toast.LENGTH_SHORT).show()
                    etName.text.clear()
                    etEmail.text.clear()
                    etDepartment.text.clear()
                    etClasses.text.clear()
                    loadTeachers()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add teacher: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        loadTeachers()
    }

    private fun loadTeachers() {
        layoutTeachersList.removeAllViews()
        showLoader(true)

        // ✅ safer listener instead of .get()
        dbRef.orderByChild("role").equalTo("Teacher")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    showLoader(false)
                    layoutTeachersList.removeAllViews()

                    if (!snapshot.exists()) {
                        addMessage("No teachers found.")
                        return
                    }

                    for (teacher in snapshot.children) {
                        val name = teacher.child("name").getValue(String::class.java) ?: "N/A"
                        val email = teacher.child("email").getValue(String::class.java) ?: "N/A"
                        val dept = teacher.child("department").getValue(String::class.java) ?: "N/A"
                        val cls = teacher.child("class").getValue(String::class.java) ?: "N/A"
                        val phone = teacher.child("phone").getValue(String::class.java) ?: "-"
                        val uname = teacher.child("username").getValue(String::class.java) ?: "-"

                        val info = "👨‍🏫 $name\n📧 $email\n🏫 Dept: $dept\n📚 Class: $cls\n📞 $phone\n🆔 $uname"
                        addMessage(info)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoader(false)
                    Toast.makeText(this@ManageTeachersActivity,
                        "Failed to load teachers: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun addMessage(text: String) {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 16f
            setPadding(8, 8, 8, 8)
        }
        layoutTeachersList.addView(tv)
    }

    private fun showLoader(show: Boolean) {
        loaderContainer.visibility = if (show) View.VISIBLE else View.GONE
    }
}
