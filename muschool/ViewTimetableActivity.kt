package com.example.muschool

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class ViewTimetableActivity : AppCompatActivity() {

    private lateinit var spinnerClass: Spinner
    private lateinit var spinnerBatch: Spinner
    private lateinit var btnLoad: Button
    private lateinit var tvResult: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_timetable)

        spinnerClass = findViewById(R.id.spinnerClass)
        spinnerBatch = findViewById(R.id.spinnerBatch)
        btnLoad = findViewById(R.id.btnLoadTimetable)
        tvResult = findViewById(R.id.tvTimetableResult)
        progressBar = findViewById(R.id.progressBar)

        dbRef = FirebaseDatabase.getInstance().getReference("Timetables")

        val classList = listOf("Select Class", "10A", "11A", "12B")
        val batchList = listOf("Select Batch", "Batch A", "Batch B", "Batch C")

        spinnerClass.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, classList)
        spinnerBatch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, batchList)

        btnLoad.setOnClickListener {
            val selectedClass = spinnerClass.selectedItem.toString()
            val selectedBatch = spinnerBatch.selectedItem.toString()

            if (selectedClass == "Select Class" || selectedBatch == "Select Batch") {
                Toast.makeText(this, "Please select class and batch", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = ProgressBar.VISIBLE
            tvResult.text = ""

            val timetablePath = "$selectedClass/$selectedBatch"
            dbRef.child(timetablePath).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    progressBar.visibility = ProgressBar.GONE
                    if (snapshot.exists()) {
                        val result = StringBuilder("🗓️ Timetable for $selectedClass - $selectedBatch\n\n")
                        for (daySnap in snapshot.children) {
                            result.append("📌 ${daySnap.key}:\n")
                            for (entry in daySnap.children) {
                                result.append("   • ${entry.getValue(String::class.java)}\n")
                            }
                            result.append("\n")
                        }
                        tvResult.text = result.toString()
                    } else {
                        tvResult.text = "No timetable found for this class and batch."
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(this@ViewTimetableActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
