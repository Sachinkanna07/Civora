package com.sachinkanna.civora.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

private enum class DataKind(val title: String, val collection: String, val fields: List<String>) {
    CANTEEN("Canteen", "canteens", listOf("name", "vendorId", "open")),
    MENU("Menu item", "menuItems", listOf("name", "canteenId", "pricePaise", "available")),
    BUS("Bus assignment", "buses", listOf("name", "routeId", "driverId")),
    ROUTE("Bus route", "busRoutes", listOf("name", "stops")),
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
    var entityId by rememberSaveable { mutableStateOf("") }
    var values by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    CampusPage("Manage campus data", back) {
        item {
            FlowRow {
                DataKind.entries.forEach { k ->
                    FilterChip(
                        kind == k,
                        {
                            kind = k
                            values = emptyMap()
                            entityId = ""
                        },
                        label = { Text(k.title) },
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                entityId,
                { entityId = it.take(128) },
                label = { Text("Campus reference ID") },
                supportingText = { Text("Use the same ID to replace an existing record.") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        kind.fields.forEach { field ->
            item {
                val label =
                    when (field) {
                        "vendorId" -> "Assigned vendor account UID"
                        "driverId" -> "Assigned driver account UID"
                        "pricePaise" -> "Price in paise"
                        "dayOfWeek" -> "Day (1 Monday | 7 Sunday)"
                        "startMinute" -> "Start minutes from midnight (09:00 = 540)"
                        "endMinute" -> "End minutes from midnight"
                        "stops" -> "Stops, one per line: name | latitude | longitude"
                        else ->
                            field.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar {
                                it.uppercase()
                            }
                    }
                if (field == "open" || field == "available")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label)
                        Switch(
                            values[field] == "true",
                            { values = values + (field to it.toString()) },
                        )
                    }
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
                    kind.fields
                        .filter { it == "open" || it == "available" }
                        .forEach { data[it] = values[it] == "true" }
                    values.forEach { (key, value) ->
                        data[key] =
                            when (key) {
                                "open",
                                "available" -> value == "true"
                                "pricePaise" -> value.toLongOrNull()
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
        item { Text("Existing reference IDs", style = MaterialTheme.typography.titleMedium) }
        item {
            val feed =
                when (kind) {
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
                        is com.sachinkanna.civora.data.model.Canteen -> "${entry.name}: ${entry.id}"
                        is com.sachinkanna.civora.data.model.Bus -> "${entry.name}: ${entry.id}"
                        is com.sachinkanna.civora.data.model.BusRoute ->
                            "${entry.name}: ${entry.id}"
                        is com.sachinkanna.civora.data.model.MenuItem ->
                            "${entry.name}: ${entry.id}"
                        is com.sachinkanna.civora.data.model.Club -> "${entry.name}: ${entry.id}"
                        is com.sachinkanna.civora.data.model.CampusLocation ->
                            "${entry.name}: ${entry.id}"
                        is com.sachinkanna.civora.data.model.TimetableEntry ->
                            "${entry.courseName}: ${entry.id}"
                        else -> ""
                    }
                )
            }
        }
    }
}
