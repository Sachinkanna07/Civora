package com.sachinkanna.civora.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.data.model.*
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

private enum class DataKind(val title: String, val collection: String, val fields: List<String>) {
    DEPARTMENT("Department", "departments", listOf("name","code")),
    CALENDAR("Academic calendar", "academicCalendar", listOf("title","date","department","holiday")),
    OVERRIDE("Date override", "academicOverrides", listOf("date","kind","baseCourseId","courseId","courseName","faculty","department","year","section","startMinute","endMinute","room","reason")),
    CANTEEN("Canteen", "canteens", listOf("name", "vendorId", "open")),
    MENU("Menu item", "menuItems", listOf("name", "canteenId", "pricePaise", "available")),
    BUS("Bus assignment", "buses", listOf("name", "routeId", "driverId")),
    ROUTE("Bus route", "busRoutes", listOf("name", "contactPhone", "stops")),
    TIMETABLE(
        "Timetable class",
        "timetables",
        listOf(
            "courseId",
            "courseName",
            "faculty",
            "department",
            "year",
            "section",
            "dayOfWeek",
            "startMinute",
            "endMinute",
            "room",
            "effectiveFrom",
            "effectiveTo",
        ),
    ),
    CLUB("Club", "clubs", listOf("name", "description", "category")),
    LOCATION(
        "Campus place",
        "campusLocations",
        listOf("name", "description", "category", "latitude", "longitude"),
    ),
}

@Composable
fun CampusDataScreen(state: WorkspaceState, vm: CampusWorkspaceViewModel, back: () -> Unit) {
    var kind by rememberSaveable { mutableStateOf(DataKind.CANTEEN) }
    var entityId by rememberSaveable { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var values by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val feed=when(kind) {DataKind.DEPARTMENT->CampusFeed.DEPARTMENTS;DataKind.CALENDAR->CampusFeed.CALENDAR;DataKind.OVERRIDE->CampusFeed.ACADEMIC_OVERRIDES;DataKind.CANTEEN->CampusFeed.CANTEENS;DataKind.MENU->CampusFeed.MENU;DataKind.BUS->CampusFeed.BUSES;DataKind.ROUTE->CampusFeed.ROUTES;DataKind.TIMETABLE->CampusFeed.TIMETABLE;DataKind.CLUB->CampusFeed.CLUBS;DataKind.LOCATION->CampusFeed.LOCATIONS}
    val records=state.feed(feed).items.mapNotNull(::editorRecord)
    CampusPage("Manage campus data", back) {
        item {
            FlowRow {
                DataKind.entries.forEach { k ->
                    FilterChip(
                        kind == k,
                        {
                            kind = k
                            values = emptyMap()
                            entityId = java.util.UUID.randomUUID().toString()
                        },
                        label = { Text(k.title) },
                    )
                }
            }
        }
        item {
            SelectField("Existing record",if(records.any {it.first==entityId})entityId else "",records.map {Choice(it.first,it.second["name"]?:it.second["courseName"]?:it.second["title"]?:"Campus record")},{id->records.find {it.first==id}?.let {entityId=id;values=it.second}},allowNone=true)
            Text("Create a campus record or choose an existing one to edit.")
            TextButton(onClick={entityId=java.util.UUID.randomUUID().toString();values=emptyMap()}) {Text("Start new record")}
        }
        kind.fields.forEach { field ->
            item {
                val label =
                    when (field) {
                        "vendorId" -> "Assigned vendor account UID"
                        "driverId" -> "Assigned driver account UID"
                        "pricePaise" -> "Price in INR"
                        "dayOfWeek" -> "Day (1 Monday | 7 Sunday)"
                        "startMinute" -> "Start minutes from midnight (09:00 = 540)"
                        "endMinute" -> "End minutes from midnight"
                        "stops" -> "Stops, one per line: name | latitude | longitude"
                        else ->
                            field.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar {
                                it.uppercase()
                            }
                    }
                if (field in listOf("open","available","holiday"))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label)
                        Switch(
                            values[field] == "true",
                            { values = values + (field to it.toString()) },
                        )
                    }
                else if(field in listOf("vendorId","driverId","canteenId","routeId","department","faculty","room","dayOfWeek","year","section","kind","baseCourseId")) {
                    val choices=when(field) {
                        "vendorId","driverId","faculty"->state.values<StaffMember>(CampusFeed.STAFF).filter {it.active&&it.role==when(field) {"vendorId"->"vendor";"driverId"->"driver";else->"faculty"}}.map {Choice(if(field=="faculty")it.name else it.uid,it.name,it.department)}
                        "canteenId"->state.values<Canteen>(CampusFeed.CANTEENS).map {Choice(it.id,it.name)}
                        "routeId"->state.values<BusRoute>(CampusFeed.ROUTES).map {Choice(it.id,it.name)}
                        "department"->state.values<Department>(CampusFeed.DEPARTMENTS).map {Choice(it.code,it.name)}
                        "room"->state.values<CampusLocation>(CampusFeed.LOCATIONS).map {Choice(it.name,it.name)}
                        "dayOfWeek"->java.time.DayOfWeek.entries.map {Choice(it.value.toString(),it.name.lowercase().replaceFirstChar {it.uppercase()})}
                        "year"->listOf("1","2","3","4").map {Choice(it,"Year $it")}
                        "kind"->listOf(Choice("CLASS","Add or replace a class"),Choice("CANCEL","Cancel a recurring class"))
                        "baseCourseId"->state.values<TimetableEntry>(CampusFeed.TIMETABLE).distinctBy {it.courseId}.map {Choice(it.courseId,it.courseName,it.courseId)}
                        else->listOf("A","B").map {Choice(it,"Section $it")}
                    }
                    SelectField(when(field) {"vendorId"->"Kitchen operator";"driverId"->"Driver";"canteenId"->"Canteen";"routeId"->"Route";"dayOfWeek"->"Day";else->label},values[field].orEmpty(),choices,{values=values+(field to it)},allowNone=field=="department"&&kind==DataKind.CALENDAR)
                } else if(field in listOf("startMinute","endMinute")) {
                    val context=androidx.compose.ui.platform.LocalContext.current
                    val minute=values[field]?.toIntOrNull()?:540
                    OutlinedButton(onClick={android.app.TimePickerDialog(context,{_,h,m->values=values+(field to (h*60+m).toString())},minute/60,minute%60,false).show()},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("${if(field=="startMinute") "Starts" else "Ends"}: ${clockTime(minute)}")}
                } else if(field in listOf("date","effectiveFrom","effectiveTo")) {
                    val context=androidx.compose.ui.platform.LocalContext.current
                    val date=runCatching {java.time.LocalDate.parse(values[field])}.getOrDefault(java.time.LocalDate.now(com.sachinkanna.civora.data.model.CampusPolicy.zone))
                    OutlinedButton(onClick={android.app.DatePickerDialog(context,{_,y,m,d->values=values+(field to java.time.LocalDate.of(y,m+1,d).toString())},date.year,date.monthValue-1,date.dayOfMonth).show()},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("${label}: ${values[field]?:"Choose date"}")}
                } else if(field=="stops") StopEditor(values[field].orEmpty()) {values=values+(field to it)}
                else
                    OutlinedTextField(
                        values[field].orEmpty(),
                        {
                            values =
                                values +
                                    (field to
                                        it.take(
                                            if (field in listOf("description", "stops")) 4000
                                            else 200
                                        ))
                        },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = if (field == "stops") 3 else 1,
                    )
            }
        }
        item {
            Button(
                onClick = {
                    val data =
                        mutableMapOf<String, Any?>(
                            "collection" to kind.collection,
                            "id" to entityId,
                        )
                    kind.fields.forEach { data[it] = values[it].orEmpty() }
                    if(kind in listOf(DataKind.TIMETABLE,DataKind.OVERRIDE)) {data["startMinute"]=values["startMinute"]?.toIntOrNull()?:540;data["endMinute"]=values["endMinute"]?.toIntOrNull()?:600}
                    kind.fields
                        .filter { it in listOf("open","available","holiday") }
                        .forEach { data[it] = values[it] == "true" }
                    values.forEach { (key, value) ->
                        data[key] =
                            when (key) {
                                "open",
                                "available",
                                "holiday" -> value == "true"
                                "pricePaise" -> runCatching {java.math.BigDecimal(value).movePointRight(2).longValueExact()}.getOrNull()
                                "startMinute",
                                "endMinute",
                                "dayOfWeek" -> value.toIntOrNull()
                                "latitude",
                                "longitude" -> value.toDoubleOrNull()
                                "stops" ->
                                    value
                                        .lineSequence()
                                        .filter { it.isNotBlank() }
                                        .map { line ->
                                            val parts = line.split('|').map { it.trim() }
                                            mapOf(
                                                "name" to parts.getOrNull(0),
                                                "latitude" to parts.getOrNull(1)?.toDoubleOrNull(),
                                                "longitude" to parts.getOrNull(2)?.toDoubleOrNull(),
                                            )
                                        }
                                        .toList()
                                else -> value
                            }
                    }
                    vm.action("manageCampusData", data, "Campus record saved")
                },
                enabled = !state.busy && entityId.matches(Regex("[A-Za-z0-9_-]+")),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save campus record")
            }
        }
        item { Text("Existing records", style = MaterialTheme.typography.titleMedium) }
        item {
            val feed =
                when (kind) {
                    DataKind.DEPARTMENT -> CampusFeed.DEPARTMENTS
                    DataKind.CALENDAR -> CampusFeed.CALENDAR
                    DataKind.OVERRIDE -> CampusFeed.ACADEMIC_OVERRIDES
                    DataKind.CANTEEN -> CampusFeed.CANTEENS
                    DataKind.MENU -> CampusFeed.MENU
                    DataKind.BUS -> CampusFeed.BUSES
                    DataKind.ROUTE -> CampusFeed.ROUTES
                    DataKind.CLUB -> CampusFeed.CLUBS
                    DataKind.LOCATION -> CampusFeed.LOCATIONS
                    DataKind.TIMETABLE -> CampusFeed.TIMETABLE
                }
            state.feed(feed).items.forEach { entry ->
                Text(
                    when (entry) {
                        is com.sachinkanna.civora.data.model.AcademicOverride -> "${entry.date} · ${entry.courseName.ifBlank {entry.reason}}"
                        is com.sachinkanna.civora.data.model.Canteen -> entry.name
                        is com.sachinkanna.civora.data.model.Bus -> entry.name
                        is com.sachinkanna.civora.data.model.BusRoute ->
                            entry.name
                        is com.sachinkanna.civora.data.model.MenuItem ->
                            entry.name
                        is com.sachinkanna.civora.data.model.Club -> entry.name
                        is com.sachinkanna.civora.data.model.CampusLocation ->
                            entry.name
                        is com.sachinkanna.civora.data.model.TimetableEntry ->
                            entry.courseName
                        is Department -> entry.name
                        is CalendarDay -> "${entry.title} · ${entry.date}"
                        else -> ""
                    }
                )
            }
        }
    }
}

private fun editorRecord(entry: Any): Pair<String,Map<String,String>>?=when(entry) {
    is Canteen->entry.id to mapOf("name" to entry.name,"vendorId" to entry.vendorId,"open" to entry.open.toString())
    is MenuItem->entry.id to mapOf("name" to entry.name,"canteenId" to entry.canteenId,"pricePaise" to java.math.BigDecimal.valueOf(entry.pricePaise,2).toPlainString(),"available" to entry.available.toString())
    is Bus->entry.id to mapOf("name" to entry.name,"routeId" to entry.routeId,"driverId" to entry.driverId)
    is BusRoute->entry.id to mapOf("name" to entry.name,"contactPhone" to entry.contactPhone,"stops" to entry.stops.joinToString("\n") {"${it.name}|${it.latitude}|${it.longitude}"})
    is Department->entry.id to mapOf("name" to entry.name,"code" to entry.code)
    is CalendarDay->entry.id to mapOf("title" to entry.title,"date" to entry.date,"department" to entry.department,"holiday" to entry.holiday.toString())
    is AcademicOverride->entry.id to mapOf("date" to entry.date,"kind" to entry.kind,"baseCourseId" to entry.baseCourseId,"courseId" to entry.courseId,"courseName" to entry.courseName,"faculty" to entry.faculty,"department" to entry.department,"year" to entry.year,"section" to entry.section,"startMinute" to entry.startMinute.toString(),"endMinute" to entry.endMinute.toString(),"room" to entry.room,"reason" to entry.reason)
    is Club->entry.id to mapOf("name" to entry.name,"description" to entry.description,"category" to entry.category)
    is CampusLocation->entry.id to mapOf("name" to entry.name,"description" to entry.description,"category" to entry.category,"latitude" to entry.latitude.toString(),"longitude" to entry.longitude.toString())
    is TimetableEntry->entry.id to mapOf("courseId" to entry.courseId,"courseName" to entry.courseName,"faculty" to entry.faculty,"department" to entry.department,"year" to entry.year,"section" to entry.section,"dayOfWeek" to entry.dayOfWeek.toString(),"startMinute" to entry.startMinute.toString(),"endMinute" to entry.endMinute.toString(),"room" to entry.room,"effectiveFrom" to entry.effectiveFrom,"effectiveTo" to entry.effectiveTo)
    else->null
}

@Composable
private fun StopEditor(value: String,onChange: (String)->Unit) {
    var name by rememberSaveable {mutableStateOf("")};var latitude by rememberSaveable {mutableStateOf("")};var longitude by rememberSaveable {mutableStateOf("")}
    val stops=value.lines().filter {it.isNotBlank()}
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        SectionTitle("Route stops","Add stops in the order the bus visits them.")
        stops.forEachIndexed {index,line->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text("${index+1}. ${line.substringBefore('|')}",Modifier.weight(1f));TextButton(onClick={onChange(stops.filterIndexed {i,_->i!=index}.joinToString("\n"))}) {Text("Remove")}}}
        OutlinedTextField(name,{name=it.replace('|',' ').take(100)},label={Text("Stop name")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(latitude,{latitude=it.take(15)},label={Text("Latitude")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(longitude,{longitude=it.take(15)},label={Text("Longitude")},modifier=Modifier.fillMaxWidth())
        val lat=latitude.toDoubleOrNull();val lng=longitude.toDoubleOrNull()
        OutlinedButton(onClick={onChange((stops+"${name.trim()}|$latitude|$longitude").joinToString("\n"));name="";latitude="";longitude=""},enabled=name.isNotBlank()&&lat!=null&&lat.isFinite()&&lat in -90.0..90.0&&lng!=null&&lng.isFinite()&&lng in -180.0..180.0&&stops.size<60) {Text("Add stop")}
    }
}
