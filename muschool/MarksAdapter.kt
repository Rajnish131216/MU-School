package com.example.muschool

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MarkAdapter(private val students: List<StudentMarkModel>) :
    RecyclerView.Adapter<MarkAdapter.MarkViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_marks, parent, false)
        return MarkViewHolder(view)
    }

    override fun getItemCount(): Int = students.size

    override fun onBindViewHolder(holder: MarkViewHolder, position: Int) {
        holder.bind(students[position])
    }

    inner class MarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStudentName: TextView = itemView.findViewById(R.id.tvStudentName)
        private val etMark: EditText = itemView.findViewById(R.id.etMark)

        fun bind(student: StudentMarkModel) {
            tvStudentName.text = student.studentName

            etMark.setText(student.mark?.toString() ?: "")

            etMark.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val markStr = s.toString()
                    student.mark = markStr.toIntOrNull()
                }
            })
        }
    }
}
