package com.example.muschool

import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*

data class TeacherModel(
    val key: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val username: String = ""
)

class AdminAssignTeacherActivity : AppCompatActivity() {

    private lateinit var spTeacher: Spinner
    private lateinit var tvTeacherName: TextView
    private lateinit var tvTeacherEmail: TextView
    private lateinit var tvTeacherPhone: TextView
    private lateinit var layoutTeacherInfo: LinearLayout
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var rvAssignedList: RecyclerView

    private lateinit var dbUsers: DatabaseReference
    private lateinit var dbAssignClBtSj: DatabaseReference
    private lateinit var dbSubjects: DatabaseReference

    private val teacherList = mutableListOf<TeacherModel>()
    private val teacherNames = mutableListOf("Select Teacher")
    private val assignedList = mutableListOf<String>()
    private lateinit var adapter: AssignedListAdapter

    private var selectedTeacherKey: String? = null
    private var selectedClass: String? = null
    private var selectedBatch: String? = null
    private var selectedSubject: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_assign_teacher)

        // Initialize views
        spTeacher = findViewById(R.id.spTeacher)
        tvTeacherName = findViewById(R.id.tvTeacherName)
        tvTeacherEmail = findViewById(R.id.tvTeacherEmail)
        tvTeacherPhone = findViewById(R.id.tvTeacherPhone)
        layoutTeacherInfo = findViewById(R.id.layoutTeacherInfo)
        fabAdd = findViewById(R.id.fabAdd)
        rvAssignedList = findViewById(R.id.rvAssignments)

        // Firebase references
        dbUsers = FirebaseDatabase.getInstance().getReference("Users")
        dbAssignClBtSj = FirebaseDatabase.getInstance().getReference("assignclbtsj")
        dbSubjects = FirebaseDatabase.getInstance().getReference("Subjects")

        // RecyclerView setup
        rvAssignedList.layoutManager = LinearLayoutManager(this)
        adapter = AssignedListAdapter(assignedList, "", onRefresh = {})
        rvAssignedList.adapter = adapter

        // Add swipe to delete
        addSwipeToDelete()

        // Spinner setup
        spTeacher.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, teacherNames)
        loadTeachers()

        spTeacher.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val teacher = teacherList[position - 1]
                    selectedTeacherKey = teacher.key
                    tvTeacherName.text = "Name: ${teacher.name}"
                    tvTeacherEmail.text = "Email: ${teacher.email}"
                    tvTeacherPhone.text = "Phone: ${teacher.phone}"
                    layoutTeacherInfo.visibility = View.VISIBLE
                    loadAssignedData(teacher.key)
                } else {
                    layoutTeacherInfo.visibility = View.GONE
                    assignedList.clear()
                    adapter.notifyDataSetChanged()
                    selectedTeacherKey = null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Floating Action Button → Assign Dialog
        fabAdd.setOnClickListener {
            if (selectedTeacherKey == null) {
                Toast.makeText(this, "Please select a teacher first!", Toast.LENGTH_SHORT).show()
            } else {
                showAssignDialog()
            }
        }
    }

    private fun loadTeachers() {
        dbUsers.orderByChild("role").equalTo("Teacher")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    teacherList.clear()
                    teacherNames.clear()
                    teacherNames.add("Select Teacher")

                    for (snap in snapshot.children) {
                        val key = snap.key ?: continue
                        val name = snap.child("name").value?.toString() ?: "N/A"
                        val email = snap.child("email").value?.toString() ?: "N/A"
                        val phone = snap.child("phone").value?.toString() ?: "N/A"
                        val username = snap.child("username").value?.toString() ?: "N/A"
                        teacherList.add(TeacherModel(key, name, email, phone, username))
                        teacherNames.add("$name ($username)")
                    }

                    val adapter = ArrayAdapter(this@AdminAssignTeacherActivity, android.R.layout.simple_spinner_dropdown_item, teacherNames)
                    spTeacher.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AdminAssignTeacherActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showAssignDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_assign_class, null)
        val spClass = dialogView.findViewById<Spinner>(R.id.spClass)
        val spBatch = dialogView.findViewById<Spinner>(R.id.spBatch)
        val spSubject = dialogView.findViewById<Spinner>(R.id.spSubject)

        val classList = Array(13) { i -> if (i == 0) "Select Class" else i.toString() }
        val batchList = arrayOf("Select Batch", "A", "B", "C")
        val subjectList = mutableListOf("Select Subject")

        spClass.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, classList)
        spBatch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, batchList)
        spSubject.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, subjectList)

        spClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (pos > 0) loadSubjectsForClass(classList[pos], spSubject, subjectList)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        AlertDialog.Builder(this)
            .setTitle("Assign Class, Batch & Subject")
            .setView(dialogView)
            .setPositiveButton("Assign") { _, _ ->
                selectedClass = spClass.selectedItem.toString()
                selectedBatch = spBatch.selectedItem.toString()
                selectedSubject = spSubject.selectedItem.toString()

                if (selectedClass == "Select Class" || selectedBatch == "Select Batch" || selectedSubject == "Select Subject") {
                    Toast.makeText(this, "Please select valid options", Toast.LENGTH_SHORT).show()
                } else {
                    saveAssignment()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadSubjectsForClass(selectedClass: String, spinner: Spinner, list: MutableList<String>) {
        dbSubjects.child(selectedClass)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    list.clear()
                    list.add("Select Subject")
                    if (snapshot.exists()) {
                        for (snap in snapshot.children) {
                            val name = snap.child("name").value?.toString()
                            if (!name.isNullOrEmpty()) list.add(name)
                        }
                    }
                    val adapter = ArrayAdapter(this@AdminAssignTeacherActivity, android.R.layout.simple_spinner_dropdown_item, list)
                    spinner.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun saveAssignment() {
        val teacherId = selectedTeacherKey ?: return
        val key = "${selectedClass}${selectedBatch}_${selectedSubject}"

        dbAssignClBtSj.child(teacherId).child(key).setValue(true)
            .addOnSuccessListener {
                Toast.makeText(this, "Assigned Successfully!", Toast.LENGTH_SHORT).show()
                loadAssignedData(teacherId)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAssignedData(teacherId: String) {
        dbAssignClBtSj.child(teacherId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    assignedList.clear()
                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            val key = child.key ?: continue
                            assignedList.add(key.replace("_", " - "))
                        }
                    }

                    adapter = AssignedListAdapter(assignedList, teacherId) {
                        loadAssignedData(teacherId)
                    }
                    rvAssignedList.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun addSwipeToDelete() {
        val paint = Paint()
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val teacherId = selectedTeacherKey ?: return
                val item = assignedList[position]

                AlertDialog.Builder(this@AdminAssignTeacherActivity)
                    .setTitle("Remove Assignment")
                    .setMessage("Do you want to unassign '$item' from this teacher?")
                    .setPositiveButton("Yes") { _, _ ->
                        FirebaseDatabase.getInstance()
                            .getReference("assignclbtsj")
                            .child(teacherId)
                            .child(item.replace(" - ", "_"))
                            .removeValue()
                            .addOnSuccessListener {
                                assignedList.removeAt(position)
                                adapter.notifyItemRemoved(position)
                            }
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
                val itemView = vh.itemView
                val backgroundColor = Color.parseColor("#FF5252")

                paint.color = backgroundColor
                if (dX > 0) { // Swipe right
                    c.drawRect(
                        itemView.left.toFloat(),
                        itemView.top.toFloat(),
                        itemView.left + dX,
                        itemView.bottom.toFloat(),
                        paint
                    )
                } else if (dX < 0) { // Swipe left
                    c.drawRect(
                        itemView.right + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat(),
                        paint
                    )
                }
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rvAssignedList)
    }
}
