package com.sachinkanna.civora.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.viewmodel.*

object CampusSpacing {
    val small = 8.dp
    val medium = 16.dp
    val page = 20.dp
}

@Composable
fun CampusPage(title: String, onBack: (() -> Unit)? = null, content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().imePadding().padding(horizontal = CampusSpacing.page),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            if (onBack != null) TextButton(onClick = onBack) { Text("Back") }
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
        content()
    }
}

@Composable
fun CampusCard(title: String, body: String, status: String? = null, onClick: (() -> Unit)? = null) {
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            status?.let {
                Text(
                    it.replace('_', ' '),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (onClick == null) Card(Modifier.fillMaxWidth()) { content() }
    else Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) { content() }
}

fun LazyListScope.feedState(
    feed: CampusFeed,
    state: WorkspaceState,
    vm: CampusWorkspaceViewModel,
    empty: String,
) {
    val data = state.feed(feed)
    if (data.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
    data.error?.let {
        item {
            Column {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { vm.retry(feed) }) { Text("Retry") }
            }
        }
    }
    if (!data.loading && data.error == null && data.items.isEmpty())
        item { Text(empty, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
fun ActionMessage(state: WorkspaceState, vm: CampusWorkspaceViewModel) {
    state.message?.let {
        Surface(
            color =
                if (state.actionError) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(it, Modifier.weight(1f))
                TextButton(onClick = vm::dismiss) { Text("Dismiss") }
            }
        }
    }
    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
}

fun dateTime(time: Long): String =
    if (time <= 0) "Time unavailable"
    else
        java.time.Instant.ofEpochMilli(time)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM | h:mm a"))
