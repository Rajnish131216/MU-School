package com.example.muschool

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.muschool.adapter.AttendanceAdapter
import com.example.muschool.model.AttendanceEntry
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class TeacherAttendanceActivity : AppCompatActivity() {

    private lateinit var spinnerClass: Spinner
    private lateinit var spinnerBatch: Spinner
    private lateinit var btnLoad: Button
    private lateinit var btnSave: Button
    private lateinit var recyclerAttendance: RecyclerView
    private lateinit var progressBar: ProgressBar

    private val attendanceList = mutableListOf<AttendanceEntry>()
    private lateinit var adapter: AttendanceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_attendance)

        spinnerClass = findViewById(R.id.spinnerClass)
        spinnerBatch = findViewById(R.id.spinnerBatch)
        btnLoad = findViewById(R.id.btnLoadStudents)
        btnSave = findViewById(R.id.btnSaveAttendance)
        recyclerAttendance = findViewById(R.id.recyclerAttendance)
        progressBar = findViewById(R.id.progressBar)

        val classList = listOf("Select Class", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
        val batchList = listOf("Select Batch", "A", "B", "C")

        spinnerClass.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, classList)
        spinnerBatch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, batchList)

        recyclerAttendance.layoutManager = LinearLayoutManager(this)

        adapter = AttendanceAdapter(attendanceList) { position, status ->
            attendanceList[position] = attendanceList[position].copy(status = status)
        }

        recyclerAttendance.adapter = adapter

        btnLoad.setOnClickListener {
            val selectedClass = spinnerClass.selectedItem.toString()
            val selectedBatch = spinnerBatch.selectedItem.toString()

            if (selectedClass == "Select Class" || selectedBatch == "Select Batch") {
                Toast.makeText(this, "Please select class and batch", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadStudents(selectedClass, selectedBatch)
        }

        btnSave.setOnClickListener {
            val selectedClass = spinnerClass.selectedItem.toString()
            val selectedBatch = spinnerBatch.selectedItem.toString()
            saveAttendance(selectedClass, selectedBatch)
        }
    }

    private fun loadStudents(className: String, batch: String) {
        progressBar.visibility = View.VISIBLE
        val dbRef = FirebaseDatabase.getInstance().getReference("Students")

        dbRef.get().addOnSuccessListener { snapshot ->
            attendanceList.clear()

            for (child in snapshot.children) {
                val studentClass = child.child("class").value.toString()
                val studentBatch = child.child("batch").value.toString()

                if (studentClass == className && studentBatch == batch) {
                    val name = child.child("name").value.toString()
                    val grNo = child.child("grNo").value.toString()
                    attendanceList.add(AttendanceEntry(name, grNo, "Present", getTodayDate()))
                }
            }

            adapter.notifyDataSetChanged()
            progressBar.visibility = View.GONE

            if (attendanceList.isEmpty()) {
                Toast.makeText(this, "No students found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Failed to load students: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAttendance(className: String, batch: String) {
        if (attendanceList.isEmpty()) {
            Toast.makeText(this, "No students to save", Toast.LENGTH_SHORT).show()
            return
        }

        val date = getTodayDate()
        val dbRef = FirebaseDatabase.getInstance()
            .getReference("Attendance")
            .child(className)
            .child(batch)
            .child(date)

        val updates = mutableMapOf<String, Any>()
        for (student in attendanceList) {
            updates[student.grNo] = student
        }

        dbRef.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Attendance saved", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to save attendance", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ Fix: Force time zone to Asia/Kolkata (IST)
    private fun getTodayDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("Asia/Kolkata") // Indian Standard Time
        val today = formatter.format(Date())
        Log.d("ATTENDANCE", "Current IST date used: $today")
        return today
    }
}
