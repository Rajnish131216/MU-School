package com.example.muschool.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.muschool.R
import com.example.muschool.model.AttendanceEntry

class AttendanceAdapter(
    private val studentList: List<AttendanceEntry>,
    private val onStatusChanged: (Int, String) -> Unit = { _, _ -> }  // 👈 default lambda
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    inner class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStudentName: TextView = itemView.findViewById(R.id.tvStudentName)
        val radioGroup: RadioGroup = itemView.findViewById(R.id.radioGroupStatus)
        val radioPresent: RadioButton = itemView.findViewById(R.id.radioPresent)
        val radioAbsent: RadioButton = itemView.findViewById(R.id.radioAbsent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_student, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val student = studentList[position]
        holder.tvStudentName.text = "${student.studentName} (${student.grNo})"

        holder.radioGroup.setOnCheckedChangeListener(null)  // Prevent state change on scroll

        // Pre-select radio button based on status
        when (student.status) {
            "Present" -> holder.radioPresent.isChecked = true
            "Absent" -> holder.radioAbsent.isChecked = true
        }

        holder.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val status = when (checkedId) {
                R.id.radioPresent -> "Present"
                R.id.radioAbsent -> "Absent"
                else -> ""
            }
            onStatusChanged(position, status)
        }
    }

    override fun getItemCount(): Int = studentList.size
}
