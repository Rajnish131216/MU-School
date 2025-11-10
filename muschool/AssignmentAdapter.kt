package com.example.muschool
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Locale

class AssignmentAdapter(
    private val data: List<MaterialItem>, // Still reusing MaterialItem for assignments
    private val onView: (String) -> Unit,
    private val onDownload: (String, String) -> Unit
) : RecyclerView.Adapter<AssignmentAdapter.Holder>() {

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvAssignmentTitle)
        val tvMeta: TextView = v.findViewById(R.id.tvAssignmentMeta)
        val btnView: Button = v.findViewById(R.id.btnViewAssignment)
        val btnDownload: Button = v.findViewById(R.id.btnDownloadAssignment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_assignment_item, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val item = data[position]
        h.tvTitle.text = item.title.ifBlank { item.fileName.ifBlank { "Assignment" } }

        val timeText = if (item.uploadedAt > 0)
            DateUtils.getRelativeTimeSpanString(item.uploadedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
        else "—"

        val sizeText = if (item.sizeBytes > 0) humanSize(item.sizeBytes) else "—"
        h.tvMeta.text = "Size: $sizeText • Uploaded: $timeText"

        h.btnView.setOnClickListener { onView(item.url) }
        h.btnDownload.setOnClickListener { onDownload(item.title.ifBlank { item.fileName }, item.url) }
    }

    override fun getItemCount(): Int = data.size

    private fun humanSize(bytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.2f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.2f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}
