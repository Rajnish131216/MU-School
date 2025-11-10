package com.example.muschool

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class StudentTimetableActivity : AppCompatActivity() {

    private lateinit var recyclerTimetable: RecyclerView
    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference
    private lateinit var timetableRef: DatabaseReference
    private val timetableList = mutableListOf<TimetableEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_timetable)

        recyclerTimetable = findViewById(R.id.recyclerTimetable)
        recyclerTimetable.layoutManager = LinearLayoutManager(this)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val studentClass = snapshot.child("class").getValue(String::class.java)
                val batch = snapshot.child("batch").getValue(String::class.java)

                Log.d("CHECK_USER_CLASS", "Class: $studentClass, Batch: $batch")

                if (!studentClass.isNullOrEmpty() && !batch.isNullOrEmpty()) {
                    loadTimetable(studentClass, batch)
                } else {
                    Toast.makeText(this@StudentTimetableActivity, "Class or Batch not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@StudentTimetableActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadTimetable(studentClass: String, batch: String) {
        Log.d("CHECK_FIREBASE", "Loading timetable for class: $studentClass, batch: $batch")

        timetableRef = FirebaseDatabase.getInstance()
            .getReference("Timetables")
            .child(studentClass)
            .child(batch)

        timetableRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                timetableList.clear()

                if (snapshot.exists()) {
                    Log.d("CHECK_FIREBASE", "Snapshot found!")

                    val dayOrder = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                    val map = mutableMapOf<String, MutableList<String>>()

                    for (daySnapshot in snapshot.children) {
                        val day = daySnapshot.key ?: continue
                        val entries = daySnapshot.children.mapNotNull { it.getValue(String::class.java) }

                        Log.d("CHECK_FIREBASE", "Day: $day, Entries: $entries")

                        map[day] = entries.toMutableList()
                    }

                    for (day in dayOrder) {
                        map[day]?.forEach { subjectTime ->
                            timetableList.add(TimetableEntry(day, subjectTime))
                        }
                    }

                    recyclerTimetable.adapter = TimetableAdapter(timetableList)
                } else {
                    Log.d("CHECK_FIREBASE", "Snapshot does not exist")
                    Toast.makeText(this@StudentTimetableActivity, "No timetable found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CHECK_FIREBASE", "Error loading timetable: ${error.message}")
                Toast.makeText(this@StudentTimetableActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
