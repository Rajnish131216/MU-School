package com.example.muschool

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.bumptech.glide.Glide
class ManageStudentsActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etGrNo: EditText
    private lateinit var etClass: EditText
    private lateinit var btnAddStudent: Button
    private lateinit var studentsList: LinearLayout

    private lateinit var loaderContainer: FrameLayout
    private lateinit var loaderImage: ImageView


    private val dbRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("Students")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_students)

        etName = findViewById(R.id.etStudentName)
        etGrNo = findViewById(R.id.etStudentGRNo)
        etClass = findViewById(R.id.etStudentClass)
        btnAddStudent = findViewById(R.id.btnAddStudent)
        studentsList = findViewById(R.id.layoutStudentsList)

        loaderContainer = findViewById(R.id.loaderContainer)
        loaderImage = findViewById(R.id.imageLoader)

        Glide.with(this).asGif().load(R.drawable.loader).into(loaderImage)

        btnAddStudent.setOnClickListener {
            val name = etName.text.toString().trim()
            val grNo = etGrNo.text.toString().trim()
            val stdClass = etClass.text.toString().trim()

            if (name.isEmpty() || grNo.isEmpty() || stdClass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val studentMap = mapOf(
                "name" to name,
                "grNo" to grNo,
                "class" to stdClass
            )

            dbRef.child(grNo).setValue(studentMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Student added", Toast.LENGTH_SHORT).show()
                    etName.text.clear()
                    etGrNo.text.clear()
                    etClass.text.clear()
                    loadStudents()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add student", Toast.LENGTH_SHORT).show()
                }
        }

        loadStudents()
    }

    private fun loadStudents() {
        studentsList.removeAllViews()
        showLoader(true)

        dbRef.get().addOnSuccessListener { snapshot ->
            showLoader(false)
            if (snapshot.exists()) {
                for (student in snapshot.children) {
                    val name = student.child("name").getValue(String::class.java)
                    val grNo = student.child("grNo").getValue(String::class.java)
                    val stdClass = student.child("class").getValue(String::class.java)

                    val tv = TextView(this).apply {
                        text = "👨‍🎓 $name | GR: $grNo | Class: $stdClass"
                        textSize = 16f
                        setPadding(8, 8, 8, 8)
                    }
                    studentsList.addView(tv)
                }
            } else {
                val tv = TextView(this).apply {
                    text = "No students found."
                    setPadding(8, 8, 8, 8)
                }
                studentsList.addView(tv)
            }
        }.addOnFailureListener {
            showLoader(false)
            Toast.makeText(this, "Failed to load students", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showLoader(show: Boolean) {
        loaderContainer.visibility = if (show) View.VISIBLE else View.GONE
    }
}
