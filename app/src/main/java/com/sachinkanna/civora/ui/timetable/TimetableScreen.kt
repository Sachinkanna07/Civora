package com.sachinkanna.civora.ui.timetable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun TimetableScreen(state: WorkspaceState, vm: CampusWorkspaceViewModel, back: () -> Unit) {
    var day by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfWeek.value) }
    val entries =
        state
            .values<TimetableEntry>(CampusFeed.TIMETABLE)
            .filter { it.dayOfWeek == day }
            .sortedBy { it.startMinute }
    CampusPage("Timetable", back) {
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { d ->
                    FilterChip(day == d.value, { day = d.value }, label = { Text(d.name.take(3)) })
                }
            }
        }
        feedState(
            CampusFeed.TIMETABLE,
            state,
            vm,
            "No timetable assigned. Complete your academic profile.",
        )
        items(entries, key = { it.id }) {
            CampusCard(
                it.courseName,
                "${clockTime(it.startMinute)}-${clockTime(it.endMinute)} | ${it.room}\n${it.faculty}",
                it.courseId,
            )
        }
        if (entries.isEmpty() && !state.feed(CampusFeed.TIMETABLE).loading)
            item { Text("No scheduled classes on this day.") }
        items(freePeriods(entries)) { gap ->
            CampusCard("Free period", "${clockTime(gap.first)} - ${clockTime(gap.last+1)}")
        }
    }
}
