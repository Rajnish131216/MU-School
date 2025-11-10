package com.example.muschool

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class TeacherEnterResults : AppCompatActivity() {

    private lateinit var spinnerClass: Spinner
    private lateinit var spinnerBatch: Spinner
    private lateinit var spinnerSubject: Spinner
    private lateinit var spinnerExamType: Spinner
    private lateinit var recyclerStudents: RecyclerView
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar

    private val studentList = mutableListOf<StudentMarkModel>()
    private lateinit var markAdapter: MarkAdapter

    private val dbRef = FirebaseDatabase.getInstance().reference

    private val classList = listOf("Select Class", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12")
    private val batchList = listOf("Select Batch", "A", "B", "C")
    private val examTypes = listOf("Select Exam Type", "Prelims", "Main")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_marks_entry)

        spinnerClass = findViewById(R.id.spinner_class)
        spinnerBatch = findViewById(R.id.spinner_batch)
        spinnerSubject = findViewById(R.id.spinner_subject)
        spinnerExamType = findViewById(R.id.spinner_exam_type)
        recyclerStudents = findViewById(R.id.rv_students)
        btnSubmit = findViewById(R.id.btn_submit)
        progressBar = findViewById(R.id.progressBar)

        setupSpinner(spinnerClass, classList)
        setupSpinner(spinnerBatch, batchList)
        setupSpinner(spinnerExamType, examTypes)
        setupSpinner(spinnerSubject, listOf("Select Subject"))

        recyclerStudents.layoutManager = LinearLayoutManager(this)
        markAdapter = MarkAdapter(studentList)
        recyclerStudents.adapter = markAdapter

        // Load subjects based on selected class
        spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val selectedClass = classList[pos]
                if (selectedClass != "Select Class") {
                    loadSubjects(selectedClass)
                } else {
                    setupSpinner(spinnerSubject, listOf("Select Subject"))
                    clearStudents()
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // Load students when all selections are made
        val triggerStudentLoad = {
            val selectedClass = spinnerClass.selectedItem.toString()
            val selectedBatch = spinnerBatch.selectedItem.toString()
            val selectedSubject = spinnerSubject.selectedItem.toString()
            val selectedExam = spinnerExamType.selectedItem.toString()

            if (selectedClass != "Select Class" &&
                selectedBatch != "Select Batch" &&
                selectedSubject != "Select Subject" &&
                selectedSubject != "No Subjects Found" &&
                selectedExam != "Select Exam Type") {
                loadStudents(selectedClass, selectedBatch)
            } else {
                clearStudents()
            }
        }

        spinnerBatch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                triggerStudentLoad()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        spinnerSubject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                triggerStudentLoad()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        spinnerExamType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                triggerStudentLoad()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        btnSubmit.setOnClickListener {
            submitMarks()
        }
    }

    private fun setupSpinner(spinner: Spinner, items: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun loadSubjects(selectedClass: String) {
        progressBar.visibility = View.VISIBLE
        val subjectRef = dbRef.child("Subjects").child(selectedClass)

        subjectRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                progressBar.visibility = View.GONE
                val subjects = snapshot.children.mapNotNull { it.child("name").getValue(String::class.java) }

                if (subjects.isEmpty()) {
                    setupSpinner(spinnerSubject, listOf("No Subjects Found"))
                    Toast.makeText(this@TeacherEnterResults, "No subjects found for class $selectedClass", Toast.LENGTH_SHORT).show()
                } else {
                    setupSpinner(spinnerSubject, listOf("Select Subject") + subjects)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@TeacherEnterResults, "Failed to load subjects", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadStudents(className: String, batch: String) {
        progressBar.visibility = View.VISIBLE
        studentList.clear()
        markAdapter.notifyDataSetChanged()

        val studentsRef = dbRef.child("Students")

        studentsRef.orderByChild("class").equalTo(className)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    studentList.clear()
                    if (!snapshot.exists()) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@TeacherEnterResults, "No students found for class $className", Toast.LENGTH_SHORT).show()
                        return
                    }

                    for (child in snapshot.children) {
                        val studentBatch = child.child("batch").getValue(String::class.java)
                        if (studentBatch == batch) {
                            val name = child.child("name").getValue(String::class.java) ?: "Unknown"
                            val grNo = child.child("grNo").getValue(String::class.java) ?: child.key!!
                            studentList.add(StudentMarkModel(grNo, name))
                        }
                    }

                    progressBar.visibility = View.GONE
                    markAdapter.notifyDataSetChanged()

                    if (studentList.isEmpty()) {
                        Toast.makeText(this@TeacherEnterResults, "No students found for $className $batch", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@TeacherEnterResults, "Loaded ${studentList.size} students", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@TeacherEnterResults, "Failed to load students: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun submitMarks() {
        val selectedClass = spinnerClass.selectedItem.toString()
        val selectedBatch = spinnerBatch.selectedItem.toString()
        val selectedSubject = spinnerSubject.selectedItem.toString()
        val selectedExam = spinnerExamType.selectedItem.toString()

        if (selectedClass == "Select Class" || selectedBatch == "Select Batch" ||
            selectedSubject == "Select Subject" || selectedSubject == "No Subjects Found" ||
            selectedExam == "Select Exam Type") {
            Toast.makeText(this, "Please select all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val resultsRef = dbRef.child("Results").child(selectedClass).child(selectedBatch)
        progressBar.visibility = View.VISIBLE

        val updates = mutableMapOf<String, Any>()
        for (student in studentList) {
            val mark = student.mark ?: continue
            if (mark < 0 || mark > 100) {
                Toast.makeText(this, "Invalid mark for ${student.studentName}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return
            }
            updates["${student.grNo}/$selectedSubject/$selectedExam"] =
                mapOf("marksObtained" to mark, "maxMarks" to 100)
        }

        resultsRef.updateChildren(updates)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Marks submitted successfully!", Toast.LENGTH_SHORT).show()
                clearStudents()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to submit marks: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearStudents() {
        studentList.clear()
        markAdapter.notifyDataSetChanged()
    }
}
