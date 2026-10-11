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
    var mode by rememberSaveable {mutableStateOf("Today")}
    var now by remember {mutableStateOf(com.sachinkanna.civora.data.model.CampusPolicy.now())}
    LaunchedEffect(Unit) {while(true) {kotlinx.coroutines.delay(30000);now=com.sachinkanna.civora.data.model.CampusPolicy.now()}}
    var day by rememberSaveable {mutableIntStateOf(now.dayOfWeek.value)}
    val all=state.values<TimetableEntry>(CampusFeed.TIMETABLE)
    val department=all.firstOrNull()?.department.orEmpty()
    val calendar=state.values<CalendarDay>(CampusFeed.CALENDAR)
    val holidays=holidayDates(calendar,department)
    val displayDay=if(mode=="Today")now.dayOfWeek.value else day
    val date=now.toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays((displayDay-1).toLong())
    val entries=all.filter {it.dayOfWeek==displayDay}.sortedBy {it.startMinute}
    val holiday=date in holidays
    val current=if(!holiday&&date==now.toLocalDate())currentClass(entries,now)else null
    val next=nextClass(all,now,holidays)
    CampusPage("Timetable",back) {
        item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Today","Week").forEach {m->FilterChip(mode==m,{mode=m},label={Text(m)})}}}
        if(mode=="Week")item {FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {DayOfWeek.entries.forEach {d->FilterChip(day==d.value,{day=d.value},label={Text(d.name.take(3))})}}}
        item {SectionTitle(date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMM")))}
        feedState(CampusFeed.TIMETABLE,state,vm,"Complete your academic profile to find your schedule.")
        if(holiday)item {EmptyState("Campus holiday",calendar.filter {it.date==date.toString()}.joinToString {it.title})}
        else {
            val gaps=freePeriods(entries)
            val timeline=entries.map {Triple(it.startMinute,it,null as IntRange?)}+gaps.map {Triple(it.first,null as TimetableEntry?,it)}
            items(timeline.sortedBy {it.first},key={it.second?.id?:"gap${it.first}"}) {row->
                val c=row.second
                if(c!=null)CampusCard(c.courseName,"${clockTime(c.startMinute)} · ${clockTime(c.endMinute)} · ${c.room}\n${c.faculty}",if(c.id==current?.id)"Current"else if(c.id==next?.entry?.id&&date==next.date.toLocalDate())"Next"else c.courseId)
                else row.third?.let {gap->CampusCard("Free period","${clockTime(gap.first)} · ${clockTime(gap.last+1)}")}
            }
            if(entries.isEmpty()&&!state.feed(CampusFeed.TIMETABLE).loading)item {EmptyState("No scheduled classes","There are no classes on this day.")}
        }
        val upcoming=calendar.filter {runCatching {LocalDate.parse(it.date)>=now.toLocalDate()}.getOrDefault(false)&&(it.department.isBlank()||it.department==department)}.sortedBy {it.date}.take(5)
        if(upcoming.isNotEmpty())item {SectionTitle("Academic calendar")}
        items(upcoming,key={it.id}) {d->CampusCard(d.title,d.date,if(d.holiday)"Holiday"else"Campus date")}
    }
}
