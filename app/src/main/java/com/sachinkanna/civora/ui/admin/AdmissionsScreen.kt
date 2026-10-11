package com.sachinkanna.civora.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

@Composable
fun AdmissionsScreen(state: WorkspaceState,vm: CampusWorkspaceViewModel,back: ()->Unit) {
    var email by rememberSaveable {mutableStateOf("")}
    var reference by rememberSaveable {mutableStateOf("")}
    var candidate by remember {mutableStateOf<Map<String,Any?>>(emptyMap())}
    var approve by remember {mutableStateOf<Boolean?>(null)}
    LaunchedEffect(state.lastAction,state.lastResult) {if(state.lastAction=="lookupAdmissionCandidate")candidate=state.lastResult}
    approve?.let {decision->ConfirmationDialog(if(decision)"Approve email exception?"else"Revoke email exception?","${candidate["name"]} · ${candidate["email"]}\nInstitutional approval is required. This does not verify the email or change the account's role.",if(decision)"Approve exception"else"Revoke exception",{
        vm.action("setAdmissionException",mapOf("userId" to candidate["uid"],"approve" to decision,"approvalReference" to reference),"Admission exception updated"){candidate=emptyMap();reference=""}
        approve=null
    },{approve=null})}
    CampusPage("Campus admission",back) {
        item {Text("Verified institutional emails can activate campus access. Approve outside-domain exceptions only after institutional review. Email domains never assign staff roles.")}
        item {
            OutlinedTextField(email,{email=it.take(254);candidate=emptyMap()},label={Text("Registered account email")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Button(onClick={vm.action("lookupAdmissionCandidate",mapOf("email" to email.trim()),"Account found")},enabled=!state.busy&&android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches(),modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text("Find account")}
        }
        if(candidate.isNotEmpty())item {
            CampusCard(candidate["name"] as? String ?: "Registered account","${candidate["email"]}\nRole: ${candidate["role"]}\nEmail verified: ${candidate["emailVerified"]}\nEmail exception: ${candidate["exception"]}\nCampus admission: ${candidate["admitted"]}\nSuspended: ${candidate["disabled"]}")
            OutlinedTextField(reference,{reference=it.take(300)},label={Text("Institutional approval reference")},modifier=Modifier.fillMaxWidth())
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Button(onClick={approve=true},enabled=!state.busy&&reference.isNotBlank()&&candidate["disabled"]!=true){Text("Approve exception")}
                OutlinedButton(onClick={approve=false},enabled=!state.busy&&reference.isNotBlank()){Text("Revoke exception")}
            }
        }
    }
}
