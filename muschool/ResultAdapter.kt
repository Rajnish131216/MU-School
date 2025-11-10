package com.example.muschool

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ResultAdapter(private val results: List<ResultEntry>) :
    RecyclerView.Adapter<ResultAdapter.ResultViewHolder>() {

    class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSubject: TextView = itemView.findViewById(R.id.tvSubject)
        val tvExam: TextView = itemView.findViewById(R.id.tvExam)
        val tvMarks: TextView = itemView.findViewById(R.id.tvMarks)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val item = results[position]
        holder.tvSubject.text = "📘 ${item.subjectName}"
        holder.tvExam.text = "🧾 ${item.examType}"
        holder.tvMarks.text = "Marks: ${item.marksObtained}/${item.maxMarks}"

        val percent =
            if (item.maxMarks > 0) (item.marksObtained.toFloat() / item.maxMarks * 100).toInt() else 0
        holder.progressBar.progress = percent
    }

    override fun getItemCount(): Int = results.size
}
