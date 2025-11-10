package com.example.muschool

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class ClassStudentsActivity : AppCompatActivity() {

    private lateinit var layoutStudentsList: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_students)

        layoutStudentsList = findViewById(R.id.layoutStudentsList)
        progressBar = findViewById(R.id.progressBarStudents)
        tvTitle = findViewById(R.id.tvClassTitle)

        val className = intent.getStringExtra("className") ?: ""
        val section = intent.getStringExtra("section") ?: ""

        tvTitle.text = "👩‍🎓 Students - Class $className | Section: $section"

        loadStudents(className, section)
    }

    private fun loadStudents(className: String, section: String) {
        layoutStudentsList.removeAllViews()
        progressBar.visibility = View.VISIBLE

        val studentsRef = FirebaseDatabase.getInstance().getReference("Students")

        studentsRef.get().addOnSuccessListener { snapshot ->
            progressBar.visibility = View.GONE

            if (snapshot.exists()) {
                var found = false
                for (student in snapshot.children) {
                    val sClass = student.child("class").getValue(String::class.java)
                    val sSection = student.child("batch").getValue(String::class.java)
                    val sName = student.child("name").getValue(String::class.java)
                    val grNo = student.child("grNo").getValue(String::class.java)

                    if (sClass == className && sSection == section) {
                        val tv = TextView(this).apply {
                            text = "👤 $sName (GR No: $grNo)"
                            textSize = 16f
                            setTextColor(Color.parseColor("#424242"))
                            setPadding(20, 20, 20, 20)
                            setBackgroundColor(Color.parseColor("#E8F5E9"))

                            val params = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            params.setMargins(0, 10, 0, 0)
                            layoutParams = params
                        }
                        layoutStudentsList.addView(tv)
                        found = true
                    }
                }

                if (!found) {
                    showMessage("No students found in this class.")
                }

            } else {
                showMessage("No students found in database.")
            }

        }.addOnFailureListener {
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Failed to load students", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMessage(msg: String) {
        val tv = TextView(this).apply {
            text = msg
            textSize = 16f
            setTextColor(Color.parseColor("#757575"))
            setPadding(16, 16, 16, 16)
        }
        layoutStudentsList.addView(tv)
    }
}
