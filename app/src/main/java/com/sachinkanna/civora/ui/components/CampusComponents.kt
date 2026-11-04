package com.sachinkanna.civora.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.viewmodel.*

object CampusSpacing { val small=8.dp; val medium=16.dp; val page=20.dp; val large=24.dp }
val LocalAccountAction = staticCompositionLocalOf<(() -> Unit)?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusPage(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}, content: LazyListScope.() -> Unit) {
    val account = LocalAccountAction.current
    BackHandler(onBack != null) { onBack?.invoke() }
    Scaffold(
        topBar = { TopAppBar(title={ Text(title, style=MaterialTheme.typography.titleLarge) }, navigationIcon={ if(onBack!=null) IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back") } }, actions={ actions(); if(account != null) IconButton(onClick=account) {Icon(Icons.Outlined.AccountCircle,"Account and settings")} }, colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background)) },
        containerColor=MaterialTheme.colorScheme.background,
        contentWindowInsets=WindowInsets.navigationBars,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentPadding=PaddingValues(start=CampusSpacing.page,end=CampusSpacing.page,top=12.dp,bottom=24.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) { content() }
    }
}

@Composable
fun StatusPill(status: String, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.red < .2f
    val normalized=status.uppercase()
    val success=listOf("READY","COMPLETED","RESOLVED","ATTENDED","CHECKED IN","OPEN","LIVE").any { normalized == it }
    val danger=listOf("REJECTED","CANCELLED","URGENT","BREAKDOWN","ERROR").any { normalized.contains(it) }
    val warning=listOf("STALE","PAUSED","DELAY","REVIEW REQUIRED","CLOSED").any { normalized.contains(it) }
    val bg=when { success -> if(dark) Color(0xFF173D31) else Color(0xFFE0F3E9); danger->MaterialTheme.colorScheme.errorContainer; warning->if(dark)Color(0xFF493719)else Color(0xFFFFF0CB);else->MaterialTheme.colorScheme.primaryContainer }
    val fg=when { success->if(dark)Color(0xFF9FE0BE)else Color(0xFF185C3B);danger->MaterialTheme.colorScheme.onErrorContainer;warning->if(dark)Color(0xFFFFD782)else Color(0xFF76500B);else->MaterialTheme.colorScheme.onPrimaryContainer }
    Surface(modifier=modifier,shape=RoundedCornerShape(8.dp),color=bg) { Text(status.replace('_',' ').lowercase().replaceFirstChar { it.uppercase() },Modifier.padding(horizontal=10.dp,vertical=6.dp),color=fg,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.SemiBold) }
}

@Composable
fun CampusCard(title: String, body: String, status: String? = null, onClick: (() -> Unit)? = null) {
    val contents: @Composable () -> Unit = {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            status?.takeIf { it.isNotBlank() }?.let { StatusPill(it) }
            Text(title,style=MaterialTheme.typography.titleMedium)
            if(body.isNotBlank()) Text(body,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
        }
    }
    val colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)
    if(onClick==null) OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=colors) { contents() }
    else OutlinedCard(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=colors) { contents() }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) { Text(title,style=MaterialTheme.typography.titleMedium); subtitle?.let { Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
}

@Composable
fun EmptyState(title: String, body: String, icon: ImageVector = Icons.Outlined.Inbox, action: String? = null, onAction: (() -> Unit)? = null) {
    OutlinedCard(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),shape=RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(icon,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(28.dp))
            Text(title,style=MaterialTheme.typography.titleMedium)
            Text(body,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(action!=null&&onAction!=null) TextButton(onClick=onAction) { Text(action) }
        }
    }
}

@Composable
fun LoadingPlaceholder() {
    Column(Modifier.fillMaxWidth().semantics { contentDescription="Loading campus information" },verticalArrangement=Arrangement.spacedBy(8.dp)) {
        repeat(2) { Box(Modifier.fillMaxWidth().height(64.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))) }
    }
}

fun LazyListScope.feedState(feed: CampusFeed, state: WorkspaceState, vm: CampusWorkspaceViewModel, empty: String) {
    val data=state.feed(feed)
    if(data.loading&&data.items.isEmpty()) item { LoadingPlaceholder() }
    data.error?.let { item { EmptyState(if(data.items.isEmpty()) "Information unavailable" else "Showing last available information",it,Icons.Outlined.CloudOff,"Try again") { vm.retry(feed) } } }
    if(!data.loading&&data.error==null&&data.items.isEmpty()) item { EmptyState("Nothing here yet",empty) }
}

@Composable
fun ActionMessage(state: WorkspaceState, vm: CampusWorkspaceViewModel) {
    val host=remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { host.showSnackbar(it,actionLabel="Dismiss",duration=SnackbarDuration.Short); if(vm.state.value.message==it)vm.dismiss() } }
    SnackbarHost(host,Modifier.fillMaxWidth().padding(16.dp))
}

// Values are opaque internally; the picker shows names and useful staff/route context.
data class Choice(val key: String,val label: String,val subtitle: String="")
@Composable
fun SelectField(label: String,value: String,choices: List<Choice>,onSelect: (String)->Unit,modifier: Modifier=Modifier,allowNone: Boolean=false) {
    var show by rememberSaveable { mutableStateOf(false) };var search by rememberSaveable { mutableStateOf("") }
    OutlinedButton(onClick={show=true},modifier=modifier.fillMaxWidth().heightIn(min=64.dp),shape=RoundedCornerShape(12.dp),enabled=choices.isNotEmpty()||allowNone,contentPadding=PaddingValues(14.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) { Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(choices.find { it.key==value }?.label ?: if(value.isBlank()) "Choose $label" else "Selection unavailable",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurface) }
        Icon(Icons.Outlined.ExpandMore,null)
    }
    if(show) AlertDialog(onDismissRequest={show=false},title={Text(label)},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(search,{search=it},label={Text("Search $label")},singleLine=true,modifier=Modifier.fillMaxWidth())
        LazyColumn(Modifier.heightIn(max=360.dp)) {
            if(allowNone)item { TextButton(onClick={onSelect("");show=false;search=""},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("None") } }
            items(choices.filter { "${it.label} ${it.subtitle}".contains(search,true) },key={it.key}) { choice ->
                TextButton(onClick={onSelect(choice.key);show=false;search=""},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) { Column(Modifier.fillMaxWidth()) { Text(choice.label,color=MaterialTheme.colorScheme.onSurface);if(choice.subtitle.isNotBlank())Text(choice.subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
        }
    }},confirmButton={TextButton(onClick={show=false}) { Text("Done") }})
}

@Composable
fun DateTimeField(label: String,value: Long,onChange: (Long)->Unit,modifier: Modifier=Modifier) {
    val context=androidx.compose.ui.platform.LocalContext.current
    OutlinedButton(onClick={
        val initial=if(value>0)java.time.Instant.ofEpochMilli(value).atZone(com.sachinkanna.civora.data.model.CampusPolicy.zone)else java.time.ZonedDateTime.now(com.sachinkanna.civora.data.model.CampusPolicy.zone)
        android.app.DatePickerDialog(context,{_,year,month,day->
            android.app.TimePickerDialog(context,{_,hour,minute->onChange(java.time.LocalDate.of(year,month+1,day).atTime(hour,minute).atZone(com.sachinkanna.civora.data.model.CampusPolicy.zone).toInstant().toEpochMilli())},initial.hour,initial.minute,false).show()
        },initial.year,initial.monthValue-1,initial.dayOfMonth).show()
    },modifier=modifier.fillMaxWidth().heightIn(min=60.dp),shape=RoundedCornerShape(12.dp)) { Icon(Icons.Outlined.CalendarMonth,null);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)) { Text(label,style=MaterialTheme.typography.labelMedium);Text(if(value>0)dateTime(value)else "Choose date and time",style=MaterialTheme.typography.bodyMedium) } }
}

@Composable
fun ConfirmationDialog(title: String,body: String,confirm: String,onConfirm: ()->Unit,onDismiss: ()->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Text(body)},confirmButton={TextButton(onClick=onConfirm) { Text(confirm) }},dismissButton={TextButton(onClick=onDismiss) { Text("Keep editing") }})
}
fun dateTime(time: Long): String=if(time<=0)"Time unavailable" else java.time.Instant.ofEpochMilli(time).atZone(com.sachinkanna.civora.data.model.CampusPolicy.zone).format(java.time.format.DateTimeFormatter.ofPattern("d MMM · h:mm a"))
fun elapsedLabel(time: Long,now: Long=System.currentTimeMillis()): String {
    if(time<=0)return "Time unavailable"
    val minutes=((now-time).coerceAtLeast(0)/60000)
    return when {minutes<1->"Just now";minutes<60->"$minutes min ago";minutes<1440->"${minutes/60} hr ago";else->dateTime(time)}
}
