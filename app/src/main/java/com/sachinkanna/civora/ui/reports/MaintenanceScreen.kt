package com.sachinkanna.civora.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
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
fun MaintenanceScreen(profile: UserProfile,state: WorkspaceState,vm: CampusWorkspaceViewModel,onLogout: ()->Unit,reportId: String?=null,onReportClosed: ()->Unit={}) {
    var selected by rememberSaveable(reportId) { mutableStateOf(reportId) }
    var closed by rememberSaveable { mutableStateOf(false) }
    val reports=state.values<CampusReport>(CampusFeed.REPORTS)
    val report=reports.find { it.id==selected }
    CampusPage(if(selected==null) "Assigned maintenance" else "Work details",if(selected!=null)({selected=null;onReportClosed()})else null) {
        if(selected==null) {
            item { SectionTitle("Hello, ${profile.name}","Accept assignments, record progress, and request campus review.") }
            item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { FilterChip(!closed,{closed=false},label={Text("Active")});FilterChip(closed,{closed=true},label={Text("Completed")}) } }
            feedState(CampusFeed.REPORTS,state,vm,"Campus operations will assign work here.")
            val visible=reports.filter { (it.status in listOf(ReportStatus.RESOLVED,ReportStatus.REJECTED))==closed }
            if(visible.isEmpty()&&!state.feed(CampusFeed.REPORTS).loading)item { EmptyState("No ${if(closed) "completed" else "active"} work","New assignments appear here as soon as operations assign them.") }
            items(visible,key={it.id}) { r-> CampusCard(r.title,"${r.location.ifBlank { "Location not provided" }} · ${dateTime(r.createdAt)}",r.status.name) { selected=r.id } }
            item { TextButton(onClick=onLogout) {Text("Log out")} }
        } else if(report!=null) {
            item { WorkDetails(profile,report,state,vm) }
            item { ReportHistory(report.id) }
        } else item { EmptyState("Work unavailable","The assignment may have changed. Return to your queue and refresh.") }
    }
}

@Composable
private fun WorkDetails(profile: UserProfile,report: CampusReport,state: WorkspaceState,vm: CampusWorkspaceViewModel) {
    var note by rememberSaveable(report.id) { mutableStateOf("") }
    var evidence by rememberSaveable(report.id) { mutableStateOf("") }
    var uploading by remember { mutableStateOf(false) }
    Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
        StatusPill(report.status.name)
        Text(report.title,style=MaterialTheme.typography.headlineSmall)
        Text(report.description)
        Text("${report.category.name.lowercase().replace('_',' ')} · ${report.location.ifBlank { "Location not provided" }}")
        if(report.imagePath.isNotBlank())PrivateImage(report.imagePath,"Reported issue")
        if(report.progressNote.isNotBlank())CampusCard("Latest progress",report.progressNote)
        if(report.completionImagePath.isNotBlank())PrivateImage(report.completionImagePath,"Work evidence")
        if(report.status==ReportStatus.REVIEW_REQUIRED)EmptyState("Awaiting operations review","Your work has been sent for review. Operations will approve resolution or return it for follow-up.")
        if(report.status in listOf(ReportStatus.RESOLVED,ReportStatus.REJECTED))CampusCard("Operations outcome",report.resolutionNote)
        if(report.status in listOf(ReportStatus.ASSIGNED,ReportStatus.ACCEPTED,ReportStatus.IN_PROGRESS)) {
            OutlinedTextField(note,{note=it.take(2000)},label={Text("Progress or completion note")},minLines=3,modifier=Modifier.fillMaxWidth())
            if(report.status in listOf(ReportStatus.ACCEPTED,ReportStatus.IN_PROGRESS))PhotoAttachment(evidence,{ "reportEvidence/${report.id}/${profile.uid}/${java.util.UUID.randomUUID()}.jpg" },{evidence=it},{uploading=it},"Work evidence (optional)")
            val next=when(report.status) {ReportStatus.ASSIGNED->"ACCEPTED";ReportStatus.ACCEPTED->"IN_PROGRESS";else->"REVIEW_REQUIRED"}
            Button(onClick={vm.action("updateAssignedWork",mapOf("reportId" to report.id,"status" to next,"note" to note,"evidencePath" to evidence),"Work updated") {note="";evidence=""}},enabled=!state.busy&&!uploading&&(next!="REVIEW_REQUIRED"||note.isNotBlank()),modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text(when(next) {"ACCEPTED"->"Accept assignment";"IN_PROGRESS"->"Start work";else->"Submit for review"})}
            TextButton(onClick={vm.action("updateAssignedWork",mapOf("reportId" to report.id,"status" to "NOTE","note" to note,"evidencePath" to evidence),"Progress shared") {note="";evidence=""}},enabled=note.isNotBlank()&&!state.busy&&!uploading) {Text("Share progress note")}
        }
    }
}

@Composable
fun ReportHistory(reportId: String) {
    var entries by remember(reportId) { mutableStateOf<List<WorkHistory>>(emptyList()) }
    var error by remember(reportId) { mutableStateOf(false) }
    var retry by remember(reportId) { mutableIntStateOf(0) }
    DisposableEffect(reportId,retry) {
        error=false
        val listener=com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("campusReports").document(reportId).collection("history").orderBy("createdAt").addSnapshotListener { snap,e->
            error=e!=null
            if(snap!=null)entries=snap.documents.map { d->WorkHistory(d.id,d.getString("status").orEmpty(),d.getString("note").orEmpty(),d.getTimestamp("createdAt")?.toDate()?.time?:0,d.getString("evidencePath").orEmpty()) }
        }
        onDispose {listener.remove()}
    }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionTitle("Report history")
        if(error)TextButton(onClick={retry++}) {Text("History unavailable · Retry")}
        entries.forEach { h->CampusCard(h.status.replace('_',' '),"${h.note}\n${dateTime(h.createdAt)}") }
    }
}
