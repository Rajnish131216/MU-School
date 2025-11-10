package com.example.muschool.decorator

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.prolificinteractive.materialcalendarview.*

class AbsentDecorator(private val dates: Collection<CalendarDay>) : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean = dates.contains(day)
    override fun decorate(view: DayViewFacade) {
        view.setBackgroundDrawable(ColorDrawable(Color.RED))
    }
}
