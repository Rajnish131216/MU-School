package com.example.muschool

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class AssignedListAdapter(
    private val assignedItems: MutableList<String>,
    private val teacherId: String,
    private val onRefresh: () -> Unit
) : RecyclerView.Adapter<AssignedListAdapter.AssignedViewHolder>() {

    inner class AssignedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvClassBatch: TextView = itemView.findViewById(R.id.tvClassBatch)
        val tvSubject: TextView = itemView.findViewById(R.id.tvSubject)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssignedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_assignment, parent, false)
        return AssignedViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssignedViewHolder, position: Int) {
        val item = assignedItems[position]
        // Example: "9A - Maths"
        val parts = item.split(" - ")
        val classBatch = parts.getOrNull(0) ?: "N/A"
        val subject = parts.getOrNull(1) ?: "Unknown Subject"

        holder.tvClassBatch.text = "Class $classBatch"
        holder.tvSubject.text = "Subject: $subject"

        holder.btnDelete.setOnClickListener {
            val context = holder.itemView.context
            AlertDialog.Builder(context)
                .setTitle("Remove Assignment")
                .setMessage("Do you want to unassign '$classBatch - $subject' from this teacher?")
                .setPositiveButton("Yes") { _, _ ->
                    val dbRef = FirebaseDatabase.getInstance()
                        .getReference("assignclbtsj")
                        .child(teacherId)
                        .child(item.replace(" - ", "_"))

                    dbRef.removeValue()
                        .addOnSuccessListener {
                            assignedItems.removeAt(position)
                            notifyItemRemoved(position)
                            onRefresh()
                        }
                        .addOnFailureListener {
                            android.widget.Toast.makeText(
                                context,
                                "Failed to remove: ${it.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun getItemCount(): Int = assignedItems.size
}
