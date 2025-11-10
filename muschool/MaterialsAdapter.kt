package com.example.muschool

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale


class MaterialsAdapter(
    private val data: List<MaterialItem>,
    private val onView: (String, String) -> Unit,
    private val onDownload: (String, String) -> Unit
) : RecyclerView.Adapter<MaterialsAdapter.Holder>() {

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvMeta: TextView = v.findViewById(R.id.tvMeta)
        val tvProvider: TextView = v.findViewById(R.id.tvProvider)
        val btnDownload: Button = v.findViewById(R.id.btnDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_material_item, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val item = data[position]
        val titleText = item.title.ifBlank { item.fileName.ifBlank { "Study Material" } }

        // --- Title ---
        h.tvTitle.text = titleText

        // --- File Info ---
        val sizeText = if (item.sizeBytes > 0) humanSize(item.sizeBytes) else "—"
        val timeText = if (item.uploadedAt > 0)
            DateUtils.getRelativeTimeSpanString(
                item.uploadedAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        else "—"

        h.tvMeta.text = "Size: $sizeText • Uploaded: $timeText"

        // --- Provider Info ---
        val providerText = when (item.provider.lowercase(Locale.getDefault())) {
            "drive" -> "Source: Google Drive"
            "cloudinary" -> "Source: Cloudinary"
            else -> "Source: External"
        }
        h.tvProvider.text = providerText

        // --- Click Handlers ---
        // Tap anywhere on card to view
        h.itemView.setOnClickListener {
            onView(titleText, item.url)
        }

        // Download button
        h.btnDownload.setOnClickListener {
            onDownload(titleText, item.url)
        }
    }

    override fun getItemCount(): Int = data.size

    // --- Human-readable file size ---
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
