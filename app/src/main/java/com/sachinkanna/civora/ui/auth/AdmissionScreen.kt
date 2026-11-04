package com.sachinkanna.civora.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.BuildConfig
import com.sachinkanna.civora.data.model.UserProfile
import com.sachinkanna.civora.viewmodel.AuthUiState
import com.sachinkanna.civora.viewmodel.AuthViewModel

@Composable
fun AdmissionScreen(profile: UserProfile,state: AuthUiState,vm: AuthViewModel,logout: ()->Unit) {
    var delete by remember {mutableStateOf(false)}
    var password by remember {mutableStateOf("")}
    if(delete)AlertDialog(onDismissRequest={if(!state.loading){delete=false;password=""}},title={Text("Delete this account?")},text={Column {
        Text("Confirm your password to remove your account and personal data. This cannot be undone.")
        OutlinedTextField(password,{password=it},label={Text("Password")},visualTransformation=PasswordVisualTransformation(),singleLine=true)
    }},confirmButton={TextButton(onClick={vm.deletePendingAccount(password);password="";delete=false},enabled=password.isNotBlank()&&!state.loading){Text("Delete account")}},dismissButton={TextButton(onClick={delete=false;password=""},enabled=!state.loading){Text("Keep account")}})
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("Civora",style=MaterialTheme.typography.headlineLarge)
        val institutional=com.sachinkanna.civora.data.model.CampusPolicy.institutionalEmail(profile.email)
        Text(if(!profile.emailVerified)"Verify your email"else if(institutional)"Check campus access"else"Campus approval required",style=MaterialTheme.typography.headlineMedium)
        Text(profile.email,style=MaterialTheme.typography.titleMedium)
        Text(if(!profile.emailVerified)"Confirm ownership of your email before opening campus services. Open the verification link in your inbox, then return here and check access."else if(institutional)"Check access to activate your verified campus account. If your access or role has changed, sign out and sign in again."else"This email needs an institution-approved exception before campus services become available. Contact your campus administrator to request access.")
        Text("Verified @${BuildConfig.CAMPUS_EMAIL_DOMAIN} addresses can request campus access. This domain does not determine your role; staff roles require trusted provisioning.")
        state.error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        state.notice?.let {Text(it)}
        if(state.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(!profile.emailVerified)Button(onClick={vm.sendVerificationEmail()},enabled=!state.loading&&state.verificationCooldownSeconds==0,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){
            Text(if(state.verificationCooldownSeconds>0)"Send again in ${state.verificationCooldownSeconds}s" else "Send verification email")
        }
        OutlinedButton(onClick={vm.checkAdmission()},enabled=!state.loading,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text("Check campus access")}
        Text("Independent Civora app. Institutional endorsement has not been confirmed.",style=MaterialTheme.typography.bodySmall)
        if(BuildConfig.CIVORA_SUPPORT_EMAIL.isNotBlank())Text("Civora support: ${BuildConfig.CIVORA_SUPPORT_EMAIL}")
        TextButton(onClick=logout,enabled=!state.loading){Text("Log out")}
        if(profile.role==com.sachinkanna.civora.data.model.UserRole.STUDENT)TextButton(onClick={delete=true},enabled=!state.loading){Text("Delete account",color=MaterialTheme.colorScheme.error)}
    }
}
