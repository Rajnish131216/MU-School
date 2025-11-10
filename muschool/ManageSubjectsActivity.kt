package com.example.muschool

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.util.*

class ManageSubjectsActivity : AppCompatActivity() {

    private lateinit var spinnerClass: Spinner
    private lateinit var etSubjectName: EditText
    private lateinit var btnAddSubject: Button
    private lateinit var listViewSubjects: ListView

    private val dbRef = FirebaseDatabase.getInstance().getReference("Subjects")
    private val subjectList = mutableListOf<Subject>()
    private lateinit var adapter: ArrayAdapter<String>
    private var selectedClass = "9"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_subjects)

        spinnerClass = findViewById(R.id.spinnerClass)
        etSubjectName = findViewById(R.id.etSubjectName)
        btnAddSubject = findViewById(R.id.btnAddSubject)
        listViewSubjects = findViewById(R.id.listViewSubjects)

        spinnerClass.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("1", "2","3","4","5","6","7","8","9","10", "11", "12")
        )

        spinnerClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedClass = parent?.getItemAtPosition(position).toString()
                loadSubjects()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listViewSubjects.adapter = adapter

        btnAddSubject.setOnClickListener {
            val name = etSubjectName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter subject name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addSubject(name)
        }

        listViewSubjects.setOnItemClickListener { _, _, position, _ ->
            val subject = subjectList[position]
            showEditRemoveDialog(subject)
        }
    }

    private fun loadSubjects() {
        dbRef.child(selectedClass).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                subjectList.clear()
                val names = mutableListOf<String>()
                for (child in snapshot.children) {
                    val subject = child.getValue(Subject::class.java)
                    if (subject != null) {
                        subjectList.add(subject)
                        names.add(subject.name)
                    }
                }
                adapter.clear()
                adapter.addAll(names)
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ManageSubjectsActivity, "Failed to load subjects: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun addSubject(name: String) {
        val id = dbRef.child(selectedClass).push().key ?: UUID.randomUUID().toString()
        val subject = Subject(id, name)
        dbRef.child(selectedClass).child(id).setValue(subject)
            .addOnSuccessListener {
                Toast.makeText(this, "Subject added", Toast.LENGTH_SHORT).show()
                etSubjectName.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showEditRemoveDialog(subject: Subject) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit or Remove Subject")

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_subject, null)
        val etEditName = view.findViewById<EditText>(R.id.etEditSubjectName)
        etEditName.setText(subject.name)

        builder.setView(view)

        builder.setPositiveButton("Update") { _, _ ->
            val newName = etEditName.text.toString().trim()
            if (newName.isNotEmpty()) {
                dbRef.child(selectedClass).child(subject.id).child("name").setValue(newName)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Subject updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }

        builder.setNegativeButton("Remove") { _, _ ->
            dbRef.child(selectedClass).child(subject.id).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Subject removed", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Remove failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        builder.setNeutralButton("Cancel", null)
        builder.show()
    }

    data class Subject(
        var id: String = "",
        var name: String = ""
    )
}
