package com.example.muschool

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class ManageClassesActivity : AppCompatActivity() {

    private lateinit var layoutClassesList: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var btnRefresh: Button
    private lateinit var tvTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_classes)

        layoutClassesList = findViewById(R.id.layoutClassesList)
        progressBar = findViewById(R.id.progressBarClasses)
        btnRefresh = findViewById(R.id.btnRefreshClasses)
        tvTitle = findViewById(R.id.tvTitle)

        btnRefresh.setOnClickListener {
            loadClasses()
        }

        loadClasses()
    }

    private fun loadClasses() {
        layoutClassesList.removeAllViews()
        progressBar.visibility = View.VISIBLE

        val studentsRef = FirebaseDatabase.getInstance().getReference("Students")

        studentsRef.get().addOnSuccessListener { snapshot ->
            progressBar.visibility = View.GONE

            if (snapshot.exists()) {
                val classSet = mutableSetOf<String>()

                for (student in snapshot.children) {
                    val className = student.child("class").getValue(String::class.java)
                    val section = student.child("batch").getValue(String::class.java)
                    val name = student.child("name").getValue(String::class.java)

                    android.util.Log.d("DEBUG_STUDENT", "Student: $name | class=$className | batch=$section")

                    if (!className.isNullOrEmpty() && !section.isNullOrEmpty()) {
                        classSet.add("$className|$section")
                    }
                }

                if (classSet.isNotEmpty()) {
                    for (cls in classSet.sorted()) {
                        val parts = cls.split("|")
                        val name = parts[0]
                        val section = parts[1]

                        val tv = TextView(this).apply {
                            text = "🏫 Class: $name | Section: $section"
                            textSize = 17f
                            setTextColor(Color.parseColor("#212121"))
                            setPadding(24, 24, 24, 24)
                            setBackgroundColor(Color.parseColor("#E3F2FD"))

                            val params = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            params.setMargins(0, 12, 0, 0)
                            layoutParams = params

                            isClickable = true
                            isFocusable = true

                            setOnClickListener {
                                val intent = Intent(this@ManageClassesActivity, ClassStudentsActivity::class.java)
                                intent.putExtra("className", name)
                                intent.putExtra("section", section)
                                startActivity(intent)
                            }
                        }

                        layoutClassesList.addView(tv)
                    }
                } else {
                    showMessage("No classes with students found.")
                }

            } else {
                showMessage("No students found in the database.")
            }

        }.addOnFailureListener {
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMessage(msg: String) {
        val tv = TextView(this).apply {
            text = msg
            textSize = 16f
            setTextColor(Color.parseColor("#616161"))
            setPadding(16, 16, 16, 16)
        }
        layoutClassesList.addView(tv)
    }
}
