package com.example.muschool

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class CreateTimetableActivity : AppCompatActivity() {

    private lateinit var spinnerClass: Spinner
    private lateinit var spinnerBatch: Spinner
    private lateinit var spinnerDay: Spinner
    private lateinit var spinnerSubject: Spinner
    private lateinit var etStartTime: EditText
    private lateinit var etEndTime: EditText
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStudents: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_timetable)

        spinnerClass = findViewById(R.id.spinnerClass)
        spinnerBatch = findViewById(R.id.spinnerBatch)
        spinnerDay = findViewById(R.id.spinnerDay)
        spinnerSubject = findViewById(R.id.spinnerSubject)
        etStartTime = findViewById(R.id.etStartTime)
        etEndTime = findViewById(R.id.etEndTime)
        btnSave = findViewById(R.id.btnSaveTimetable)
        progressBar = findViewById(R.id.progressBar)
        tvStudents = findViewById(R.id.tvStudents)

        // Dropdown options
        val classList = listOf("Select Class", "1", "2", "3", "4","5","6","7","8","9","10","11","12")
        val batchList = listOf("Select Batch", "A", "B", "C")
        val dayList = listOf("Select Day", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val subjectList = listOf("Select Subject", "Math", "Science", "English", "Gujarati", "Computer","Socail Science","Library")

        spinnerClass.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, classList)
        spinnerBatch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, batchList)
        spinnerDay.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, dayList)
        spinnerSubject.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, subjectList)

        // Time picker setup
        etStartTime.setOnClickListener {
            showTimePicker { time -> etStartTime.setText(time) }
        }

        etEndTime.setOnClickListener {
            showTimePicker { time -> etEndTime.setText(time) }
        }

        btnSave.setOnClickListener {
            val className = spinnerClass.selectedItem.toString()
            val batch = spinnerBatch.selectedItem.toString()
            val day = spinnerDay.selectedItem.toString()
            val subject = spinnerSubject.selectedItem.toString()
            val startTime = etStartTime.text.toString().trim()
            val endTime = etEndTime.text.toString().trim()

            if (className == "Select Class" || batch == "Select Batch" ||
                day == "Select Day" || subject == "Select Subject" ||
                startTime.isEmpty() || endTime.isEmpty()
            ) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            val entry = "$subject: $startTime - $endTime"
            val dbRef = FirebaseDatabase.getInstance()
                .getReference("Timetables")
                .child(className)
                .child(batch)
                .child(day)

            dbRef.push().setValue(entry).addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Timetable saved!", Toast.LENGTH_SHORT).show()

                etStartTime.text.clear()
                etEndTime.text.clear()
                spinnerDay.setSelection(0)
                spinnerSubject.setSelection(0)

                // Optional: show feedback
                tvStudents.text = "Saved: $entry for $className $batch ($day)"
            }.addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val amPm = if (selectedHour < 12) "AM" else "PM"
            val hourFormatted = if (selectedHour % 12 == 0) 12 else selectedHour % 12
            val time = String.format("%02d:%02d %s", hourFormatted, selectedMinute, amPm)
            onTimeSelected(time)
        }, hour, minute, false).show()
    }
}
