package com.example.muschool

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import com.google.firebase.database.*
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class ViewResultsActivity : AppCompatActivity() {

    private lateinit var spClass: Spinner
    private lateinit var spBatch: Spinner
    private lateinit var spExamType: Spinner
    private lateinit var btnLoad: Button
    private lateinit var btnLoadAll: Button
    private lateinit var btnGeneratePDF: Button
    private lateinit var btnGenerateExcel: Button
    private lateinit var resultsContainer: LinearLayout

    private val dbResults = FirebaseDatabase.getInstance().getReference("Results")
    private val dbUsers = FirebaseDatabase.getInstance().getReference("Users")

    private var selectedClass = ""
    private var selectedBatch = ""
    private var selectedExamType = "Both"
    private var isLoadAllMode = false
    private var isLoading = false

    private val pdfData: MutableList<ResultModel> = mutableListOf()
    private val allPdfData: MutableList<ResultModel> = mutableListOf()

    private var progressBar: ProgressBar? = null

    data class ResultModel(
        val className: String,
        val batchName: String,
        val name: String,
        val grNo: String,
        val prelim: Int,
        val main: Int,
        val final: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_results)

        spClass = findViewById(R.id.spClass)
        spBatch = findViewById(R.id.spBatch)
        spExamType = findViewById(R.id.spExamType)
        btnLoad = findViewById(R.id.btnLoadResults)
        btnLoadAll = findViewById(R.id.btnLoadAllResults)
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF)
        btnGenerateExcel = findViewById(R.id.btnGenerateExcel)
        resultsContainer = findViewById(R.id.resultsContainer)

        val classList = listOf("Select Class", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
        val batchList = listOf("Select Batch", "A", "B", "C")
        val examList = listOf("Prelim", "Main", "Both")

        spClass.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, classList)
        spBatch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, batchList)
        spExamType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, examList)

        spClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                selectedClass = if (position > 0) classList[position] else ""
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        spBatch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                selectedBatch = if (position > 0) batchList[position] else ""
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        spExamType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                selectedExamType = examList[position]
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        btnLoad.setOnClickListener {
            isLoadAllMode = false
            if (selectedClass.isEmpty() || selectedBatch.isEmpty()) {
                Toast.makeText(this, "Please select both Class and Batch", Toast.LENGTH_SHORT).show()
            } else {
                resultsContainer.removeAllViews()
                pdfData.clear()
                showLoading()
                loadClassResults(selectedClass, selectedBatch) {
                    hideLoading()
                }
            }
        }

        btnLoadAll.setOnClickListener {
            isLoadAllMode = true
            resultsContainer.removeAllViews()
            pdfData.clear()
            allPdfData.clear()
            showLoading()
            loadAllResultsSequentially { hideLoading() }
        }

        btnGeneratePDF.setOnClickListener {
            val dataList = if (isLoadAllMode) allPdfData else pdfData
            if (dataList.isEmpty()) {
                Toast.makeText(this, "Please load results first", Toast.LENGTH_SHORT).show()
            } else {
                // Show a small loader while PDF is being generated
                showLoading()
                Thread {
                    try {
                        generatePDFInternal()
                    } finally {
                        runOnUiThread { hideLoading() }
                    }
                }.start()
            }
        }

        btnGenerateExcel.setOnClickListener {
            val dataList = if (isLoadAllMode) allPdfData else pdfData
            if (dataList.isEmpty()) {
                Toast.makeText(this, "Please load results first", Toast.LENGTH_SHORT).show()
            } else {
                showLoading()
                // Run Excel generation on background thread to avoid UI freeze / possible process kill
                Thread {
                    try {
                        generateExcelInternal()
                    } finally {
                        runOnUiThread { hideLoading() }
                    }
                }.start()
            }
        }
    }

    private fun showLoading() {
        if (isLoading) return
        isLoading = true
        progressBar = ProgressBar(this).apply { isIndeterminate = true }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.CENTER
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
            addView(progressBar, lp)
        }
        runOnUiThread { resultsContainer.addView(container, 0) }
    }

    private fun hideLoading() {
        if (!isLoading) return
        isLoading = false
        runOnUiThread {
            if (resultsContainer.childCount > 0) {
                val first = resultsContainer.getChildAt(0)
                if (first is LinearLayout) {
                    var containsProgress = false
                    for (i in 0 until first.childCount) if (first.getChildAt(i) is ProgressBar) containsProgress = true
                    if (containsProgress) resultsContainer.removeViewAt(0)
                }
            }
        }
    }

    private fun loadClassResults(cls: String, batch: String, onComplete: (() -> Unit)? = null) {
        dbResults.child(cls).child(batch).get().addOnSuccessListener { batchSnap ->
            if (!batchSnap.exists()) {
                addText("No results found for Class $cls - Batch $batch.")
                onComplete?.invoke()
                return@addOnSuccessListener
            }

            val card = CardView(this).apply {
                radius = 20f
                cardElevation = 8f
                setContentPadding(24, 20, 24, 20)
                useCompatPadding = true
                setCardBackgroundColor(resources.getColor(android.R.color.background_dark))
            }

            val innerLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

            val header = TextView(this).apply {
                text = "🏫 Class $cls - Batch $batch"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(resources.getColor(android.R.color.holo_blue_light))
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setPadding(0, 0, 0, 16)
            }
            innerLayout.addView(header)

            val students = batchSnap.children.toList()
            if (students.isEmpty()) {
                val tv = TextView(this).apply {
                    text = "No students found in Class $cls - Batch $batch."
                    textSize = 16f
                    setPadding(12, 8, 12, 8)
                    setTextColor(resources.getColor(android.R.color.holo_red_light))
                }
                innerLayout.addView(tv)
                card.addView(innerLayout)
                resultsContainer.addView(card)
                addSpacing()
                onComplete?.invoke()
                return@addOnSuccessListener
            }

            var processed = 0
            var totalPercent = 0.0
            var countIncluded = 0
            var topper: ResultModel? = null

            fun finalizeCard() {
                if (countIncluded == 0) {
                    displaySummary(innerLayout, null, 0.0, 0)
                } else {
                    displaySummary(innerLayout, topper, totalPercent, countIncluded)
                }
                card.addView(innerLayout)
                resultsContainer.addView(card)
                addSpacing()
                onComplete?.invoke()
            }

            for (studentSnap in students) {

                val grNo = studentSnap.key
                if (grNo.isNullOrEmpty()) {
                    processed++
                    if (processed == students.size) finalizeCard()
                    continue
                }

                calculateStudentPercentage(grNo, studentSnap) { name, prelim, main, final, valid ->
                    if (!valid) {
                        processed++
                        if (processed == students.size) finalizeCard()
                        return@calculateStudentPercentage
                    }

                    val tv = TextView(this).apply {
                        text = when (selectedExamType) {
                            "Prelim" -> "🎓 $name ($grNo)\n   Prelim: ${prelim.roundToInt()}%"
                            "Main" -> "🎓 $name ($grNo)\n   Main: ${main.roundToInt()}%"
                            else -> "🎓 $name ($grNo)\n   Prelim: ${prelim.roundToInt()}% | Main: ${main.roundToInt()}% | Final: ${final.roundToInt()}%"
                        }
                        textSize = 15f
                        setTextColor(resources.getColor(android.R.color.holo_orange_light))
                        setPadding(12, 4, 12, 4)
                    }
                    innerLayout.addView(tv)

                    val model = ResultModel(cls, batch, name, grNo, prelim.roundToInt(), main.roundToInt(), final.roundToInt())
                    pdfData.add(model)
                    if (isLoadAllMode) allPdfData.add(model)

                    totalPercent += when (selectedExamType) {
                        "Prelim" -> prelim
                        "Main" -> main
                        else -> final
                    }

                    if (topper == null || model.final > (topper?.final ?: -1)) {
                        topper = model
                    }

                    processed++
                    countIncluded++
                    if (processed == students.size) finalizeCard()
                }
            }

        }.addOnFailureListener {
            addText("❌ Error loading Class $cls - Batch $batch: ${it.message}")
            onComplete?.invoke()
        }
    }

    private fun findExamNodeForSubject(subjectSnap: DataSnapshot, token: String): DataSnapshot? {
        if (subjectSnap.hasChild(token)) return subjectSnap.child(token)
        val tokenLower = token.lowercase(Locale.getDefault())
        for (child in subjectSnap.children) {
            val key = child.key ?: continue
            if (key.lowercase(Locale.getDefault()).contains(tokenLower)) return child
        }
        return null
    }

    private fun calculateStudentPercentage(
        grNo: String,
        studentSnap: DataSnapshot,
        onComplete: (String, Double, Double, Double, Boolean) -> Unit
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

                    fun percentForExamToken(token: String): Double? {
                        var marks = 0.0
                        var max = 0.0
                        var foundAny = false
                        for (subjectSnap in studentSnap.children) {
                            val examNode = findExamNodeForSubject(subjectSnap, token)
                            if (examNode != null && examNode.exists()) {
                                val m = examNode.child("marksObtained").getValue(Double::class.java) ?: 0.0
                                val x = examNode.child("maxMarks").getValue(Double::class.java) ?: 0.0
                                if (x > 0.0) {
                                    marks += m
                                    max += x
                                    foundAny = true
                                }
                            }
                        }
                        return if (!foundAny || max <= 0.0) null else (marks / max) * 100.0
                    }

                    val prelimPercent = percentForExamToken("prelim")
                    val mainPercent = percentForExamToken("main")

                    val hasPrelim = prelimPercent != null
                    val hasMain = mainPercent != null

                    when (selectedExamType) {
                        "Prelim" -> {
                            if (hasPrelim) onComplete(name, prelimPercent!!, 0.0, prelimPercent, true)
                            else onComplete(name, 0.0, 0.0, 0.0, false)
                        }
                        "Main" -> {
                            if (hasMain) onComplete(name, 0.0, mainPercent!!, mainPercent, true)
                            else onComplete(name, 0.0, 0.0, 0.0, false)
                        }
                        else -> {
                            if (hasPrelim && hasMain) {
                                val finalPct = (prelimPercent!! + mainPercent!!) / 2.0
                                onComplete(name, prelimPercent, mainPercent, finalPct, true)
                            } else {
                                onComplete(name, 0.0, 0.0, 0.0, false)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onComplete("Unknown", 0.0, 0.0, 0.0, false)
                }
            })
    }

    private fun displaySummary(innerLayout: LinearLayout, topper: ResultModel?, totalPercent: Double, count: Int) {
        if (count == 0) {
            val tv = TextView(this).apply {
                text = "No students with selected exam data."
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.holo_red_light))
                setPadding(12, 8, 12, 8)
            }
            innerLayout.addView(tv)
            return
        }
        val avg = (totalPercent / count).roundToInt()
        val topperText = TextView(this).apply {
            text = if (topper != null) "🏅 Topper: ${topper.name} (${topper.className}-${topper.batchName}) — ${topper.final}%" else "🏅 Topper: N/A"
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(android.R.color.holo_purple))
            setPadding(8, 10, 8, 4)
        }
        val avgText = TextView(this).apply {
            text = "📊 Class Average: $avg%"
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(android.R.color.holo_green_light))
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setPadding(0, 6, 0, 0)
        }
        innerLayout.addView(topperText)
        innerLayout.addView(avgText)
    }

    // --- PDF generation moved to a dedicated internal function for background use ---
    private fun generatePDFInternal() {
        try {
            val dataList = if (isLoadAllMode) allPdfData else pdfData
            if (dataList.isEmpty()) {
                runOnUiThread { Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show() }
                return
            }

            val pdfDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "ResultsPDFs")
            if (!pdfDir.exists()) pdfDir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val fileName = if (isLoadAllMode) {
                "Result_All_${selectedExamType}_$timestamp.pdf"
            } else {
                val safeClass = if (selectedClass.isNotEmpty()) selectedClass else "Class"
                val safeBatch = if (selectedBatch.isNotEmpty()) selectedBatch else "Batch"
                "Result_${safeClass}_${safeBatch}_${selectedExamType}_$timestamp.pdf"
            }

            val file = File(pdfDir, fileName)
            val document = Document(PageSize.A4.rotate())
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()

            val titleFont = Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD, BaseColor(0, 102, 204))
            val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK)

            val bmp = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            bmp?.let {
                val stream = ByteArrayOutputStream()
                it.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                val logo = Image.getInstance(stream.toByteArray())
                logo.alignment = Image.ALIGN_CENTER
                logo.scaleToFit(60f, 60f)
                document.add(logo)
            }

            val schoolName = "MU SCHOOL"
            val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            document.add(Paragraph("$schoolName | Exam: $selectedExamType | Date: $date", titleFont))
            document.add(Paragraph("\n"))

            val headers = mutableListOf("Class", "Batch", "Student Name", "GR No")
            when (selectedExamType) {
                "Prelim" -> headers.add("Prelim %")
                "Main" -> headers.add("Main %")
                else -> headers.addAll(listOf("Prelim %", "Main %", "Final %"))
            }

            val table = PdfPTable(headers.size)
            table.widthPercentage = 100f
            headers.forEach { table.addCell(PdfPCell(Paragraph(it, normalFont))) }

            dataList.forEach {
                table.addCell(Paragraph(it.className, normalFont))
                table.addCell(Paragraph(it.batchName, normalFont))
                table.addCell(Paragraph(it.name, normalFont))
                table.addCell(Paragraph(it.grNo, normalFont))
                when (selectedExamType) {
                    "Prelim" -> table.addCell(Paragraph("${it.prelim}%", normalFont))
                    "Main" -> table.addCell(Paragraph("${it.main}%", normalFont))
                    else -> {
                        table.addCell(Paragraph("${it.prelim}%", normalFont))
                        table.addCell(Paragraph("${it.main}%", normalFont))
                        table.addCell(Paragraph("${it.final}%", normalFont))
                    }
                }
            }

            document.add(table)
            document.close()
            runOnUiThread {
                Toast.makeText(this, "✅ PDF Generated: $fileName", Toast.LENGTH_SHORT).show()
                shareFile(file, "application/pdf")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread { Toast.makeText(this, "❌ PDF Error: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }

    // --- Excel generation moved to a background-safe internal function ---
    private fun generateExcelInternal() {
        try {
            val dataList = if (isLoadAllMode) allPdfData else pdfData
            if (dataList.isEmpty()) {
                runOnUiThread { Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show() }
                return
            }

            val timestamp = System.currentTimeMillis()
            val fileName = if (isLoadAllMode) {
                "Result_All_${selectedExamType}_$timestamp.xlsx"
            } else {
                val safeClass = if (selectedClass.isNotEmpty()) selectedClass else "Class"
                val safeBatch = if (selectedBatch.isNotEmpty()) selectedBatch else "Batch"
                "Result_${safeClass}_${safeBatch}_${selectedExamType}_$timestamp.xlsx"
            }

            // Create directory (ensure it exists)
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "ResultsExcel")
            if (!dir.exists()) {
                val made = dir.mkdirs()
                if (!made && !dir.exists()) {
                    // failed to create folder
                    runOnUiThread {
                        Toast.makeText(this, "Failed to create ResultsExcel directory", Toast.LENGTH_LONG).show()
                    }
                    return
                }
            }

            val file = File(dir, fileName)

            // Use use{} to ensure workbook is closed properly
            XSSFWorkbook().use { workbook ->
                val sheet = workbook.createSheet("Results")

                val headers = mutableListOf("Class", "Batch", "Student Name", "GR No")
                when (selectedExamType) {
                    "Prelim" -> headers.add("Prelim %")
                    "Main" -> headers.add("Main %")
                    else -> headers.addAll(listOf("Prelim %", "Main %", "Final %"))
                }

                val headerRow = sheet.createRow(0)
                headers.forEachIndexed { i, h -> headerRow.createCell(i).setCellValue(h) }

                var rowIndex = 1
                dataList.forEach { model ->
                    val row = sheet.createRow(rowIndex++)
                    // Safely set values (never write null)
                    row.createCell(0).setCellValue(model.className ?: "")
                    row.createCell(1).setCellValue(model.batchName ?: "")
                    row.createCell(2).setCellValue(model.name ?: "")
                    row.createCell(3).setCellValue(model.grNo ?: "")

                    when (selectedExamType) {
                        "Prelim" -> row.createCell(4).setCellValue(model.prelim.toDouble())
                        "Main" -> row.createCell(4).setCellValue(model.main.toDouble())
                        else -> {
                            row.createCell(4).setCellValue(model.prelim.toDouble())
                            row.createCell(5).setCellValue(model.main.toDouble())
                            row.createCell(6).setCellValue(model.final.toDouble())
                        }
                    }
                }

                // autosize columns (best-effort)
                for (i in headers.indices) {
                    try { sheet.autoSizeColumn(i) } catch (_: Exception) {}
                }

                // Write workbook to file safely
                try {
                    FileOutputStream(file).use { fos ->
                        workbook.write(fos)
                    }
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this, "Failed to write Excel file: ${ioe.message}", Toast.LENGTH_LONG).show()
                    }
                    return
                }
            } // workbook closed here

            runOnUiThread {
                Toast.makeText(this, "✅ Excel Generated: $fileName", Toast.LENGTH_SHORT).show()
                shareFile(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread { Toast.makeText(this, "❌ Excel Error: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }

    private fun shareFile(file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Result File"))
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread { Toast.makeText(this, "Share Error: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }

    private fun addText(text: String) {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 16f
            setPadding(12, 8, 12, 8)
            setTextColor(resources.getColor(android.R.color.white))
        }
        runOnUiThread { resultsContainer.addView(tv) }
    }

    private fun addSpacing() {
        val space = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 24)
        }
        runOnUiThread { resultsContainer.addView(space) }
    }

    private fun loadAllResultsSequentially(done: () -> Unit) {
        dbResults.get().addOnSuccessListener { rootSnap ->
            if (!rootSnap.exists()) {
                addText("No results found.")
                done()
                return@addOnSuccessListener
            }
            val classes = rootSnap.children.toList().sortedBy { it.key?.toIntOrNull() ?: 0 }
            processClassBatch(classes, 0, done)
        }.addOnFailureListener {
            addText("Error loading all results: ${it.message}")
            done()
        }
    }

    private fun processClassBatch(classes: List<DataSnapshot>, index: Int, done: () -> Unit) {
        if (index >= classes.size) {
            done()
            return
        }
        val cls = classes[index].key ?: return processClassBatch(classes, index + 1, done)
        val batches = classes[index].children.toList()
        processBatch(cls, batches, 0) { processClassBatch(classes, index + 1, done) }
    }

    private fun processBatch(cls: String, batches: List<DataSnapshot>, idx: Int, done: () -> Unit) {
        if (idx >= batches.size) {
            done()
            return
        }
        val batch = batches[idx].key ?: return processBatch(cls, batches, idx + 1, done)
        loadClassResults(cls, batch) { processBatch(cls, batches, idx + 1, done) }
    }
}
