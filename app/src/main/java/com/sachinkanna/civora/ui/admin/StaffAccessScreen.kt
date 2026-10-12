package com.sachinkanna.civora.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.data.model.StaffMember
import com.sachinkanna.civora.data.repository.CampusFeed
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

@Composable
fun StaffAccessScreen(state:WorkspaceState,vm:CampusWorkspaceViewModel,back:()->Unit){
    val directory=state.values<StaffMember>(CampusFeed.STAFF);val staff=directory.filter {it.active};val inactive=directory.filter {!it.active}
    var target by rememberSaveable {mutableStateOf("")};var replacement by rememberSaveable {mutableStateOf("")};var phrase by rememberSaveable {mutableStateOf("")}
    var deletionTarget by rememberSaveable {mutableStateOf("")};var deletionPhrase by rememberSaveable {mutableStateOf("")}
    val selected=staff.find {it.uid==target};val replacements=staff.filter {it.uid!=target&&it.role==selected?.role}
    val preview=if(state.lastAction=="previewStaffDeactivation"&&state.lastResult["targetUid"]==target)state.lastResult else emptyMap()
    CampusPage("Staff access and transfer",back){
        item {Text("Deactivate an account only after transferring every active responsibility. Historical audit records are retained.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        item {SelectField("Staff account",target,staff.map {Choice(it.uid,it.name,"${it.role} · ${it.department}")},{target=it;replacement="";phrase=""})}
        if(selected!=null)item {SelectField("Replacement ${selected.role}",replacement,replacements.map {Choice(it.uid,it.name,it.department)},{replacement=it})}
        item {OutlinedButton(onClick={vm.action("previewStaffDeactivation",mapOf("targetUid" to target),"Responsibilities checked")},enabled=target.isNotBlank()&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text("Review responsibilities")}}
        if(preview.isNotEmpty())item {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            CampusCard("Transfer summary","${preview["reports"]} maintenance assignments · ${preview["orders"]} active orders\n${preview["buses"]} vehicles · ${preview["trips"]} active trips · ${preview["events"]} events\n${preview["registrations"]} registrations · ${preview["canteens"]} canteens · ${preview["messHalls"]} mess halls","REVIEW REQUIRED")
            Text("Active trips assigned to transferred vehicles will be stopped. The replacement driver must start a new trip.",style=MaterialTheme.typography.bodySmall)
        }}
        if(preview.isNotEmpty())item {OutlinedTextField(phrase,{phrase=it.take(10)},label={Text("Type TRANSFER to confirm")},singleLine=true,modifier=Modifier.fillMaxWidth())}
        if(preview.isNotEmpty())item {Button(onClick={vm.action("deactivateStaffAccount",mapOf("targetUid" to target,"replacementUid" to replacement,"confirm" to true),"Responsibilities transferred and account deactivated"){target="";replacement="";phrase=""}},enabled=replacement.isNotBlank()&&phrase=="TRANSFER"&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)){Text("Transfer and deactivate")}}
        if(inactive.isNotEmpty())item {HorizontalDivider();Text("Permanent account deletion",style=MaterialTheme.typography.titleMedium);Text("Available only after approved retention policy activation. Operational and security audit records remain retained.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(inactive.isNotEmpty())item {SelectField("Deactivated staff account",deletionTarget,inactive.map {Choice(it.uid,it.name.ifBlank {"Former ${it.role}"},it.role)},{deletionTarget=it;deletionPhrase=""})}
        if(deletionTarget.isNotBlank())item {OutlinedTextField(deletionPhrase,{deletionPhrase=it.take(6)},label={Text("Type DELETE to confirm")},singleLine=true,modifier=Modifier.fillMaxWidth())}
        if(deletionTarget.isNotBlank())item {Button(onClick={vm.action("deleteDeactivatedStaffAccount",mapOf("targetUid" to deletionTarget,"confirmPhrase" to "DELETE"),"Staff account deleted; audit records retained"){deletionTarget="";deletionPhrase=""}},enabled=deletionPhrase=="DELETE"&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)){Text("Permanently delete account")}}
    }
}
