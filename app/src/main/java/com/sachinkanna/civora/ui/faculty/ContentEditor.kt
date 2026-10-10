package com.sachinkanna.civora.ui.faculty

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
fun ContentEditor(profile: UserProfile,state: WorkspaceState,vm: CampusWorkspaceViewModel,back: ()->Unit) {
    var event by rememberSaveable {mutableStateOf(false)};var title by rememberSaveable {mutableStateOf("")};var body by rememberSaveable {mutableStateOf("")}
    var category by rememberSaveable {mutableStateOf("General")};var department by rememberSaveable {mutableStateOf(profile.department)}
    var venue by rememberSaveable {mutableStateOf("")};var capacity by rememberSaveable {mutableStateOf("0")}
    var start by rememberSaveable {mutableLongStateOf(0)};var end by rememberSaveable {mutableLongStateOf(0)};var expiry by rememberSaveable {mutableLongStateOf(0)}
    var image by rememberSaveable {mutableStateOf("")};var club by rememberSaveable {mutableStateOf("")}
    var documentId by rememberSaveable {mutableStateOf(java.util.UUID.randomUUID().toString())};var editing by rememberSaveable {mutableStateOf(false)}
    var preview by rememberSaveable {mutableStateOf(false)};var uploading by remember {mutableStateOf(false)}
    var deadline by rememberSaveable {mutableLongStateOf(0)}
    var contentAction by remember {mutableStateOf<Triple<String,String,String>?>(null)}
    contentAction?.let { (kind,id,action) -> AlertDialog(onDismissRequest={contentAction=null},title={Text(if(action=="CANCEL")"Cancel this event?"else if(action=="ARCHIVE")"Archive this update?"else"Restore this update?")},text={Text(if(action=="CANCEL")"Registrations and check-in will close. Registered students will receive an inbox update. Cancellation cannot be reversed."else "Archived content is hidden from discovery. Existing records remain in the audit history.")},confirmButton={TextButton(onClick={vm.action("setContentStatus",mapOf("kind" to kind,"id" to id,"action" to action),"Campus update changed");contentAction=null},enabled=!state.busy){Text("Confirm")}},dismissButton={TextButton(onClick={contentAction=null}){Text("Keep unchanged")}}) }
    fun reset() {title="";body="";venue="";capacity="0";start=0;end=0;expiry=0;deadline=0;image="";club="";editing=false;documentId=java.util.UUID.randomUUID().toString();preview=false}
    val valid=title.isNotBlank()&&body.isNotBlank()&&(!event||(venue.isNotBlank()&&start>0&&end>start&&capacity.toIntOrNull()!=null&&capacity.toIntOrNull()!!>=0))
    CampusPage(if(preview)"Preview campus update"else if(editing)"Edit campus update"else "Publish campus update",if(preview)({preview=false})else back) {
        if(preview) {
            item {CampusCard(title,body,category)}
            if(event)item {CampusCard(venue,"${dateTime(start)} · ${dateTime(end)}\n${if(capacity=="0")"No capacity limit"else"Capacity: $capacity"}")}
            else if(expiry>0)item {Text("Expires ${dateTime(expiry)}")}
            if(image.isNotBlank())item {PrivateImage(image,"Campus update photo")}
            item {Button(onClick={vm.action("publishCampusContent",mapOf("kind" to if(event)"event"else"announcement","id" to documentId,"title" to title,"body" to body,"category" to category,"priority" to if(category=="Urgent")"urgent"else"normal","department" to department,"venue" to venue,"capacity" to capacity.toIntOrNull(),"startTime" to start,"endTime" to end,"registrationDeadline" to deadline.takeIf {it>0},"expiresAt" to if(expiry>0)expiry else null,"imagePath" to image,"clubId" to club),"Campus update published") {reset()}},enabled=valid&&!state.busy&&!uploading,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text(if(state.busy)"Publishing…"else if(editing)"Save changes"else"Publish update")}}
        } else {
            item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {FilterChip(!event,{event=false;category="General";reset()},label={Text("Announcement")});FilterChip(event,{event=true;category="Workshop";reset()},label={Text("Event")})}}
            item {OutlinedTextField(title,{title=it.take(120)},label={Text("Title")},modifier=Modifier.fillMaxWidth())}
            item {OutlinedTextField(body,{body=it.take(4000)},label={Text("Details")},minLines=4,modifier=Modifier.fillMaxWidth())}
            item {SelectField("Category",category,(if(event)listOf("Workshop","Cultural","Sports","Academic","Community")else listOf("General","Academic","Department")+if(profile.role==UserRole.ADMIN)listOf("Urgent")else emptyList()).map {Choice(it,it)},{category=it})}
            if(profile.role==UserRole.ADMIN)item {SelectField("Department",department,state.values<Department>(CampusFeed.DEPARTMENTS).map {Choice(it.code,it.name)},{department=it},allowNone=true)}
            if(event) {
                item {val locations=state.values<CampusLocation>(CampusFeed.LOCATIONS);SelectField("Venue",venue,locations.map {Choice(it.name,it.name)},{venue=it})}
                item {OutlinedTextField(capacity,{capacity=it.filter(Char::isDigit).take(6)},label={Text("Capacity (0 means no limit)")},modifier=Modifier.fillMaxWidth())}
                item {DateTimeField("Starts",start,{start=it});DateTimeField("Ends",end,{end=it});Text("Times use ${java.time.ZoneId.systemDefault().id}.",style=MaterialTheme.typography.bodySmall)}
                item {DateTimeField("Registration deadline (optional)",deadline,{deadline=it});if(deadline>0)TextButton(onClick={deadline=0}) {Text("Close registration at event start")}}
                item {SelectField("Organizing club",club,state.values<Club>(CampusFeed.CLUBS).map {Choice(it.id,it.name)},{club=it},allowNone=true)}
            } else item {DateTimeField("Expiry (optional)",expiry,{expiry=it});if(expiry>0)TextButton(onClick={expiry=0}) {Text("Remove expiry")}}
            item {PhotoAttachment(image,{"campusImages/${profile.uid}/${java.util.UUID.randomUUID()}.jpg"},{image=it},{uploading=it})}
            item {Button(onClick={preview=true},enabled=valid&&!state.busy&&!uploading,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Preview update")}}
            if(editing)item {TextButton(onClick={reset()}) {Text("Create a new update")}}
            item {SectionTitle("Manage published updates")}
            if(event)items(state.values<CampusEvent>(CampusFeed.EVENTS).filter {profile.role==UserRole.ADMIN||it.createdBy==profile.uid},key={it.id}) {e->
                CampusCard(e.title,dateTime(e.startTime),if(e.cancelled)"Cancelled"else if(e.archived)"Archived"else"Edit") {documentId=e.id;editing=true;title=e.title;body=e.description;category=e.category;department=e.department;venue=e.venue;capacity=e.capacity.toString();start=e.startTime;end=e.endTime;deadline=e.registrationDeadline?:0;image=e.imagePath;club=e.clubId}
                if(!e.cancelled)TextButton(onClick={contentAction=Triple("event",e.id,"CANCEL")}) {Text("Cancel event")}
                if(!e.cancelled)TextButton(onClick={contentAction=Triple("event",e.id,if(e.archived)"RESTORE"else"ARCHIVE")}) {Text(if(e.archived)"Restore event"else"Archive event")}
            }
            else items(state.values<Announcement>(CampusFeed.ANNOUNCEMENTS).filter {profile.role==UserRole.ADMIN||it.authorId==profile.uid},key={it.id}) {n->
                CampusCard(n.title,n.category,if(n.archived)"Archived"else"Edit") {documentId=n.id;editing=true;title=n.title;body=n.body;category=n.category;department=n.department;expiry=n.expiresAt?:0;image=n.imagePath}
                TextButton(onClick={contentAction=Triple("announcement",n.id,if(n.archived)"RESTORE"else"ARCHIVE")}) {Text(if(n.archived)"Restore announcement"else"Archive announcement")}
            }
        }
    }
}

fun parseDate(text: String): Long? =
    try {
        java.time.LocalDateTime.parse(
                text,
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            )
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (e: Exception) {
        null
    }

fun editDate(time: Long): String =
    java.time.Instant.ofEpochMilli(time)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
