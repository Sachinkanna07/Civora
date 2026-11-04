package com.sachinkanna.civora.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

@Composable
fun ReportsScreen(
    state: WorkspaceState,
    vm: CampusWorkspaceViewModel,
    back: () -> Unit,
    admin: Boolean = false,
    reportId: String? = null,
) {
    var selected by rememberSaveable(reportId) {mutableStateOf(reportId)}
    var mode by rememberSaveable {mutableStateOf(if(reportId==null) "Report issue" else "Track reports")}
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var image by rememberSaveable { mutableStateOf("") }
    var photoBusy by remember { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf(ReportCategory.OTHER) }
    var statusFilter by rememberSaveable { mutableStateOf<ReportStatus?>(null) }
    var categoryFilter by rememberSaveable { mutableStateOf<ReportCategory?>(null) }
    var requestId by rememberSaveable { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    CampusPage(if (admin) "Report queue" else "Campus reports", {if(selected!=null)selected=null else back()}) {
        if(!admin&&selected==null)item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Report issue","Track reports").forEach {m->FilterChip(mode==m,{mode=m},label={Text(m)})}}}
        if (!admin && selected == null && mode=="Report issue") {
            item {
                Text("Report a campus issue", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    title,
                    { title = it.take(120) },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    description,
                    { description = it.take(4000) },
                    label = { Text("Describe the issue") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
            item {
                FlowRow {
                    ReportCategory.entries.forEach { c ->
                        FilterChip(
                            category == c,
                            { category = c },
                            label = { Text(c.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    location,
                    { location = it.take(200) },
                    label = { Text("Location (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                PhotoAttachment(image, {
                    image=""; requestId=java.util.UUID.randomUUID().toString()
                    "reports/${com.google.firebase.auth.FirebaseAuth.getInstance().currentUser!!.uid}/$requestId/photo.jpg"
                }, { image=it }, { photoBusy=it })
            }
            item {
                Button(
                    onClick = {
                        vm.action(
                            "submitReport",
                            mapOf(
                                "title" to title,
                                "description" to description,
                                "category" to category.name,
                                "location" to location,
                                "imagePath" to image,
                                "requestId" to requestId,
                            ),
                            "Report submitted",
                        ) {
                            mode="Track reports"
                            title = ""
                            description = ""
                            location = ""
                            image = ""
                            requestId = java.util.UUID.randomUUID().toString()
                        }
                    },
                    enabled =
                        !state.busy && !photoBusy &&
                            title.isNotBlank() &&
                            description.isNotBlank() &&
                            true,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Submit report")
                }
            }
        }
        if (admin&&selected==null)
            item {
                FlowRow {
                    FilterChip(
                        statusFilter == null,
                        { statusFilter = null },
                        label = { Text("All states") },
                    )
                    ReportStatus.entries.forEach { s ->
                        FilterChip(
                            statusFilter == s,
                            { statusFilter = s },
                            label = { Text(s.name.replace('_', ' ')) },
                        )
                    }
                }
                FlowRow {
                    FilterChip(
                        categoryFilter == null,
                        { categoryFilter = null },
                        label = { Text("All categories") },
                    )
                    ReportCategory.entries.forEach { c ->
                        FilterChip(
                            categoryFilter == c,
                            { categoryFilter = c },
                            label = { Text(c.name) },
                        )
                    }
                }
            }
        if(admin||mode=="Track reports"||selected!=null) {
        feedState(CampusFeed.REPORTS, state, vm, "No reports to show.")
        items(
            state.values<CampusReport>(CampusFeed.REPORTS).filter {
                (selected == null || it.id == selected) &&
                    (statusFilter == null || it.status == statusFilter) &&
                    (categoryFilter == null || it.category == categoryFilter)
            },
            key = { it.id },
        ) { report ->
            if(selected==report.id)ReportCard(report, state, vm, admin)
            else CampusCard(report.title,"${report.location.ifBlank { "Location not provided" }} · ${dateTime(report.createdAt)}",report.status.name) {selected=report.id}
        }
        }
    }
}

@Composable
private fun ReportCard(
    report: CampusReport,
    state: WorkspaceState,
    vm: CampusWorkspaceViewModel,
    admin: Boolean,
) {
    var note by rememberSaveable(report.id) { mutableStateOf("") }
    var assignee by rememberSaveable(report.id) { mutableStateOf(report.assignedTo) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(report.title, style = MaterialTheme.typography.titleMedium)
            Text(report.status.name.replace('_', ' '), color = MaterialTheme.colorScheme.primary)
            Text(report.description)
            Text("${report.category} | ${report.location.ifBlank {"Location not provided"}}")
            Text(dateTime(report.createdAt))
            if (report.imagePath.isNotBlank()) PrivateImage(report.imagePath,"Issue photo")
            if (report.completionImagePath.isNotBlank()) PrivateImage(report.completionImagePath,"Completion evidence")
            if (report.resolutionNote.isNotBlank()) Text("Resolution: ${report.resolutionNote}")
            if (report.assignedTo.isNotBlank()) Text("Assigned to: ${report.assignedToName.ifBlank { "Campus maintenance" }}")
            if (report.progressNote.isNotBlank()) Text("Progress: ${report.progressNote}")
            ReportHistory(report.id)
            if (admin) {
                SelectField("Maintenance staff", assignee,
                    state.values<StaffMember>(CampusFeed.STAFF).filter { it.role=="maintenance" && it.active }.map { Choice(it.uid,it.name,"${it.department} · ${it.team}") }, { assignee=it })
                OutlinedTextField(
                    note,
                    { note = it.take(2000) },
                    label = { Text("Resolution note") },
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow {
                    ReportStatus.entries
                        .filter { report.status.canTransitionTo(it) }
                        .forEach { next ->
                            Button(
                                onClick = {
                                    vm.action(
                                        "updateReport",
                                        mapOf(
                                            "reportId" to report.id,
                                            "status" to next.name,
                                            "resolutionNote" to note,
                                            "assignedTo" to assignee,
                                        ),
                                    )
                                },
                                enabled =
                                    !state.busy &&
                                        (next != ReportStatus.ASSIGNED || assignee.isNotBlank()) &&
                                        (next !in
                                            listOf(ReportStatus.RESOLVED, ReportStatus.REJECTED) ||
                                            note.isNotBlank()),
                            ) {
                                Text(next.name.replace('_', ' '))
                            }
                        }
                }
            }
        }
    }
}
