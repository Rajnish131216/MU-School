package com.example.muschool.model

data class AttendanceEntry(
    val studentName: String = "",
    val grNo: String = "",
    val status: String = "", // "Present" or "Absent"
    val date: String = ""
)
