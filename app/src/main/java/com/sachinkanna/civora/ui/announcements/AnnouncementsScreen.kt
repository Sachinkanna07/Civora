package com.sachinkanna.civora.ui.announcements

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

@Composable
fun AnnouncementsScreen(
    profile: UserProfile,
    state: WorkspaceState,
    vm: CampusWorkspaceViewModel,
    back: () -> Unit,
    noticeId: String? = null,
) {
    var selected by rememberSaveable(noticeId) { mutableStateOf(noticeId) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var departmentOnly by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable {mutableStateOf("")}
    var now by remember {mutableLongStateOf(System.currentTimeMillis())}
    LaunchedEffect(Unit) {while(true) {kotlinx.coroutines.delay(30000);now=System.currentTimeMillis()}}
    val inbox=state.values<InboxItem>(CampusFeed.INBOX)
    LaunchedEffect(selected,inbox,state.busy) {if(selected!=null&&!state.busy)inbox.firstOrNull {it.destination==Destination.ANNOUNCEMENT&&it.entityId==selected&&!it.read}?.let {vm.read(it.id)}}
    var detail by remember { mutableStateOf<Announcement?>(null) }
    var error by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(selected, retry) {
        detail = null
        error = false
        if (selected != null)
            try {
                detail = AnnouncementRepository().get(selected!!); if(detail==null)error=true
            } catch (e: Exception) {
                error = true
            }
    }
    val notices =
        state.values<Announcement>(CampusFeed.ANNOUNCEMENTS).filter {
            !it.archived &&
            (it.expiresAt == null || it.expiresAt > now) &&
                (it.department.isBlank() || it.department == profile.department) &&
                (it.audience == "students" || it.audience == "all")
        }
    val notice = notices.find { it.id == selected } ?: detail
    CampusPage(
        notice?.title ?: "Announcements",
        { if (selected != null) selected = null else back() },
    ) {
        if (selected != null) {
            if (error)
                item {
                    Text("Could not load this notice.")
                    TextButton(onClick = { retry++ }) { Text("Retry") }
                }
            else if (notice == null) item { CircularProgressIndicator() }
            notice?.let { n ->
                item {
                    Text(
                        "${if(inbox.any {it.entityId==n.id&&it.read}) "Read" else "Unread"} · ${n.category}",
                        color =
                            if (n.priority == "urgent") MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                    )
                    if(n.expiresAt!=null&&n.expiresAt<=now)StatusPill("Expired")
                    Text(n.body)
                    if(n.imagePath.isNotBlank())PrivateImage(n.imagePath,"Announcement photo")
                    Text("${n.authorName} | ${dateTime(n.createdAt)}")
                    n.expiresAt?.let { Text("Expires ${dateTime(it)}") }
                    if (n.department.isNotBlank()) Text(n.department)
                }
            }
        } else {
            item {OutlinedTextField(search,{search=it.take(200)},label={Text("Search announcements")},singleLine=true,modifier=Modifier.fillMaxWidth())}
            item {
                FlowRow {
                    FilterChip(category == null, { category = null }, label = { Text("All") })
                    listOf("General", "Academic", "Department", "Urgent").forEach { c ->
                        FilterChip(category == c, { category = c }, label = { Text(c) })
                    }
                    FilterChip(
                        departmentOnly,
                        { departmentOnly = !departmentOnly },
                        label = { Text("My department") },
                    )
                }
            }
            feedState(CampusFeed.ANNOUNCEMENTS, state, vm, "No relevant announcements.")
            val filtered = notices.filter {
                "${it.title} ${it.body}".contains(search,true) &&
                (category == null || it.category == category) &&
                    (!departmentOnly ||
                        profile.department.isNotBlank() && it.department == profile.department)
            }
            items(filtered, key = { it.id }) { n ->
                CampusCard(n.title, n.body.take(180), "${if(inbox.any {it.entityId==n.id&&it.read}) "Read" else "Unread"} · ${n.category}") {
                    selected = n.id
                }
            }
            if (filtered.isEmpty()) item { Text("No active notices match these filters.") }
        }
    }
}
