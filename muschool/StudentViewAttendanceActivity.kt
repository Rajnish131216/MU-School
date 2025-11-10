package com.example.muschool

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.prolificinteractive.materialcalendarview.*
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*

class StudentViewAttendanceActivity : AppCompatActivity() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvAttendancePercentage: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var studentRef: DatabaseReference

    private val presentDates = mutableSetOf<CalendarDay>()
    private val absentDates = mutableSetOf<CalendarDay>()

    private var className: String? = null
    private var batchName: String? = null
    private var grNo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_view_attendance)

        tvAttendancePercentage = findViewById(R.id.tvAttendancePercentage)
        calendarView = findViewById(R.id.calendarView)
        progressBar = findViewById(R.id.progressBar)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        // Get student's GR No
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    grNo = snapshot.child("username").getValue(String::class.java)
                    if (!grNo.isNullOrEmpty()) fetchStudentInfo(grNo!!)
                    else showToast("Student GR No not found")
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Error: ${error.message}")
                }
            })
    }

    private fun fetchStudentInfo(grNo: String) {
        studentRef = FirebaseDatabase.getInstance().getReference("Students").child(grNo)
        progressBar.visibility = View.VISIBLE

        studentRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                className = snapshot.child("class").getValue(String::class.java)
                batchName = snapshot.child("batch").getValue(String::class.java)

                if (!className.isNullOrEmpty() && !batchName.isNullOrEmpty()) {
                    fetchAttendance(grNo, className!!, batchName!!)
                } else {
                    progressBar.visibility = View.GONE
                    showToast("Student info incomplete")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                showToast("Error: ${error.message}")
            }
        })
    }

    private fun fetchAttendance(grNo: String, className: String, batchName: String) {
        val attendanceRef = FirebaseDatabase.getInstance()
            .getReference("Attendance").child(className).child(batchName)

        progressBar.visibility = View.VISIBLE
        attendanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                presentDates.clear()
                absentDates.clear()

                for (dateSnapshot in snapshot.children) {
                    val studentSnapshot = dateSnapshot.child(grNo)
                    if (studentSnapshot.exists()) {
                        val status = studentSnapshot.child("status").getValue(String::class.java) ?: continue
                        val dateStr = studentSnapshot.child("date").getValue(String::class.java) ?: continue
                        val calendarDay = parseDateToCalendarDay(dateStr)

                        if (status == "Present") presentDates.add(calendarDay)
                        else if (status == "Absent") absentDates.add(calendarDay)
                    }
                }

                applyDecorators()
                calculateAndShowPercentage()
                progressBar.visibility = View.GONE

                if (presentDates.isEmpty() && absentDates.isEmpty())
                    showToast("No attendance found")
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                showToast("Error: ${error.message}")
            }
        })
    }

    private fun parseDateToCalendarDay(dateStr: String): CalendarDay {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return CalendarDay.today()
        val calendar = Calendar.getInstance()
        calendar.time = date
        return CalendarDay.from(calendar)
    }

    private fun applyDecorators() {
        calendarView.removeDecorators()
        if (presentDates.isNotEmpty()) {
            calendarView.addDecorator(AttendanceDecorator(presentDates, Color.parseColor("#4CAF50")))
        }
        if (absentDates.isNotEmpty()) {
            calendarView.addDecorator(AttendanceDecorator(absentDates, Color.parseColor("#F44336")))
        }
    }

    private fun calculateAndShowPercentage() {
        val totalDays = presentDates.size + absentDates.size
        val percentage = if (totalDays > 0) (presentDates.size * 100 / totalDays) else 0
        tvAttendancePercentage.text = "Overall Attendance: $percentage%"
    }

    private fun showToast(message: String) {
        Toast.makeText(this@StudentViewAttendanceActivity, message, Toast.LENGTH_SHORT).show()
    }

    class AttendanceDecorator(
        private val dates: Set<CalendarDay>,
        private val color: Int
    ) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean {
            return dates.contains(day)
        }

        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(10f, color))
        }
    }
}
