package com.example.muschool

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class StudentResultsActivity : AppCompatActivity() {

    private lateinit var recyclerResults: RecyclerView
    private lateinit var adapter: ResultAdapter
    private lateinit var tvSummary: TextView
    private val resultList = mutableListOf<ResultEntry>()

    private val dbResults = FirebaseDatabase.getInstance().getReference("Results")
    private val dbUsers = FirebaseDatabase.getInstance().getReference("Users")
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_view_results)

        recyclerResults = findViewById(R.id.recyclerResults)
        recyclerResults.layoutManager = LinearLayoutManager(this)
        adapter = ResultAdapter(resultList)
        recyclerResults.adapter = adapter

        tvSummary = findViewById(R.id.tvSummary)

        fetchStudentInfoAndResults()
    }

    /**
     * ✅ Automatically fetch logged-in student's GR number, class, and batch
     */
    private fun fetchStudentInfoAndResults() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = user.uid

        dbUsers.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@StudentResultsActivity, "User data not found!", Toast.LENGTH_SHORT).show()
                    return
                }

                val grNo = snapshot.child("username").getValue(String::class.java) ?: ""
                val studentClass = snapshot.child("class").getValue(String::class.java) ?: ""
                val batch = snapshot.child("batch").getValue(String::class.java) ?: ""

                if (grNo.isEmpty() || studentClass.isEmpty() || batch.isEmpty()) {
                    Toast.makeText(this@StudentResultsActivity, "Incomplete user data.", Toast.LENGTH_SHORT).show()
                    return
                }

                loadResults(studentClass, batch, grNo)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@StudentResultsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * ✅ Load results dynamically using class, batch, and GR number
     */
    private fun loadResults(studentClass: String, batch: String, grNo: String) {
        val resultsRef = dbResults.child(studentClass).child(batch).child(grNo)

        resultsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                resultList.clear()

                if (!snapshot.exists()) {
                    tvSummary.text = "No results found yet."
                    tvSummary.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()
                    return
                }

                var totalMarks = 0
                var totalMax = 0

                for (subjectSnap in snapshot.children) {
                    val subjectName = subjectSnap.key ?: continue

                    for (examSnap in subjectSnap.children) {
                        val examType = examSnap.key ?: continue
                        val marksObtained = examSnap.child("marksObtained").getValue(Int::class.java) ?: 0
                        val maxMarks = examSnap.child("maxMarks").getValue(Int::class.java) ?: 0

                        resultList.add(
                            ResultEntry(
                                subjectName = subjectName,
                                examType = examType,
                                marksObtained = marksObtained,
                                maxMarks = maxMarks
                            )
                        )

                        totalMarks += marksObtained
                        totalMax += maxMarks
                    }
                }

                adapter.notifyDataSetChanged()

                if (totalMax > 0) {
                    val percent = (totalMarks.toDouble() / totalMax) * 100
                    tvSummary.text = """
                        📊 Summary
                        Class: $studentClass | Batch: $batch
                        
                        Total Marks: $totalMarks / $totalMax
                        Overall Percentage: ${"%.2f".format(percent)}%
                    """.trimIndent()
                } else {
                    tvSummary.text = "No valid marks found."
                }

                tvSummary.visibility = View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RESULTS_DEBUG", "Error loading results: ${error.message}")
                tvSummary.text = "Error loading results."
                tvSummary.visibility = View.VISIBLE
            }
        })
    }
}
