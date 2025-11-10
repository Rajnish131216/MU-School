package com.example.muschool

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.database.*
import kotlin.math.roundToInt

class ViewResultsActivity : AppCompatActivity() {

    private lateinit var spClass: Spinner
    private lateinit var spBatch: Spinner
    private lateinit var btnLoad: Button
    private lateinit var btnLoadAll: Button
    private lateinit var resultsContainer: LinearLayout

    private val dbResults = FirebaseDatabase.getInstance().getReference("Results")
    private val dbUsers = FirebaseDatabase.getInstance().getReference("Users")

    private var selectedClass = ""
    private var selectedBatch = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_results)

        spClass = findViewById(R.id.spClass)
        spBatch = findViewById(R.id.spBatch)
        btnLoad = findViewById(R.id.btnLoadResults)
        btnLoadAll = findViewById(R.id.btnLoadAllResults)
        resultsContainer = findViewById(R.id.resultsContainer)

        val classList = listOf("Select Class", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
        val batchList = listOf("Select Batch", "A", "B", "C", "D")

        spClass.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, classList)
        spBatch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, batchList)

        spClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedClass = if (position > 0) classList[position] else ""
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spBatch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedBatch = if (position > 0) batchList[position] else ""
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnLoad.setOnClickListener {
            if (selectedClass.isEmpty() || selectedBatch.isEmpty()) {
                Toast.makeText(this, "Please select both Class and Batch", Toast.LENGTH_SHORT).show()
            } else {
                resultsContainer.removeAllViews()
                loadClassResults(selectedClass, selectedBatch)
            }
        }

        btnLoadAll.setOnClickListener {
            resultsContainer.removeAllViews()
            loadAllResultsSequentially()
        }
    }

    /** Load one class-batch group properly */
    private fun loadClassResults(cls: String, batch: String, onComplete: (() -> Unit)? = null) {
        dbResults.child(cls).child(batch).get().addOnSuccessListener { batchSnap ->
            if (!batchSnap.exists()) {
                addText("No results found for Class $cls - Batch $batch.")
                onComplete?.invoke()
                return@addOnSuccessListener
            }

            // Create a CardView container for this batch
            val card = CardView(this).apply {
                radius = 20f
                cardElevation = 8f
                setContentPadding(24, 20, 24, 20)
                useCompatPadding = true
                setCardBackgroundColor(resources.getColor(android.R.color.background_dark))
            }

            val innerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val header = TextView(this).apply {
                text = "🏫 Class $cls - Batch $batch"
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(resources.getColor(android.R.color.holo_blue_light))
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setPadding(0, 0, 0, 16)
            }
            innerLayout.addView(header)

            val students = batchSnap.children.toList()
            var processed = 0
            var totalPercentage = 0.0
            var topperName = "N/A"
            var topperPercent = 0.0

            for (studentSnap in students) {
                val grNo = studentSnap.key ?: continue

                calculateStudentPercentage(grNo, studentSnap) { studentName, percent ->
                    val tv = TextView(this).apply {
                        text = "🎓 $studentName ($grNo) — ${percent.roundToInt()}%"
                        textSize = 16f
                        setTextColor(resources.getColor(android.R.color.holo_orange_light))
                        setPadding(12, 4, 12, 4)
                    }
                    innerLayout.addView(tv)

                    totalPercentage += percent
                    if (percent > topperPercent) {
                        topperPercent = percent
                        topperName = studentName
                    }

                    processed++
                    if (processed == students.size) {
                        val avg = if (students.isNotEmpty()) (totalPercentage / students.size).roundToInt() else 0

                        val topperText = TextView(this).apply {
                            text = "🏅 Topper: $topperName — ${topperPercent.roundToInt()}%"
                            textSize = 17f
                            setTypeface(typeface, Typeface.BOLD)
                            setTextColor(resources.getColor(android.R.color.holo_purple))
                            setPadding(8, 10, 8, 4)
                        }
                        innerLayout.addView(topperText)

                        val avgText = TextView(this).apply {
                            text = "📊 Class Average: $avg%"
                            textSize = 17f
                            setTypeface(typeface, Typeface.BOLD)
                            setTextColor(resources.getColor(android.R.color.holo_green_light))
                            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                            setPadding(0, 6, 0, 0)
                        }
                        innerLayout.addView(avgText)

                        card.addView(innerLayout)
                        resultsContainer.addView(card)

                        addSpacing()
                        onComplete?.invoke()
                    }
                }
            }
        }.addOnFailureListener {
            addText("❌ Error loading Class $cls - Batch $batch: ${it.message}")
            onComplete?.invoke()
        }
    }

    /** Sequentially load all classes and batches */
    private fun loadAllResultsSequentially() {
        dbResults.get().addOnSuccessListener { rootSnap ->
            if (!rootSnap.exists()) {
                addText("No results found.")
                return@addOnSuccessListener
            }

            val classList = rootSnap.children.toList().sortedBy { it.key?.toIntOrNull() ?: 0 }
            processNextClassBatch(classList, 0)
        }
    }

    private fun processNextClassBatch(classList: List<DataSnapshot>, index: Int) {
        if (index >= classList.size) return
        val classSnap = classList[index]
        val cls = classSnap.key ?: return processNextClassBatch(classList, index + 1)
        val batchList = classSnap.children.toList().sortedBy { it.key }

        processNextBatch(cls, batchList, 0) {
            processNextClassBatch(classList, index + 1)
        }
    }

    private fun processNextBatch(cls: String, batchList: List<DataSnapshot>, batchIndex: Int, onClassComplete: () -> Unit) {
        if (batchIndex >= batchList.size) {
            onClassComplete()
            return
        }

        val batchSnap = batchList[batchIndex]
        val batch = batchSnap.key ?: return processNextBatch(cls, batchList, batchIndex + 1, onClassComplete)

        loadClassResults(cls, batch) {
            processNextBatch(cls, batchList, batchIndex + 1, onClassComplete)
        }
    }

    /** Calculate student's percentage */
    private fun calculateStudentPercentage(
        grNo: String,
        studentSnap: DataSnapshot,
        onComplete: (String, Double) -> Unit
    ) {
        dbUsers.orderByChild("username").equalTo(grNo)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnap: DataSnapshot) {
                    var name = "Unknown"
                    if (userSnap.exists()) {
                        for (user in userSnap.children) {
                            name = user.child("name").getValue(String::class.java) ?: "Unknown"
                        }
                    }

                    var totalMarks = 0.0
                    var totalMax = 0.0
                    for (subjectSnap in studentSnap.children) {
                        for (examSnap in subjectSnap.children) {
                            val marks = examSnap.child("marksObtained").getValue(Double::class.java) ?: 0.0
                            val maxMarks = examSnap.child("maxMarks").getValue(Double::class.java) ?: 0.0
                            totalMarks += marks
                            totalMax += maxMarks
                        }
                    }

                    val percent = if (totalMax > 0) (totalMarks / totalMax) * 100 else 0.0
                    onComplete(name, percent)
                }

                override fun onCancelled(error: DatabaseError) {
                    onComplete("Unknown", 0.0)
                }
            })
    }

    /** UI Helpers */
    private fun addText(text: String) {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 16f
            setPadding(12, 8, 12, 8)
            setTextColor(resources.getColor(android.R.color.white))
        }
        resultsContainer.addView(tv)
    }

    private fun addSpacing() {
        val space = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                24
            )
        }
        resultsContainer.addView(space)
    }
}
