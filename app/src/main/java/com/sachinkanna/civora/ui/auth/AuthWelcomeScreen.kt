package com.sachinkanna.civora.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.ui.components.CivoraBackground

@Composable
fun AuthWelcomeScreen(onSignInClick: ()->Unit,onCreateAccountClick: ()->Unit,modifier: Modifier=Modifier) {
    CivoraBackground(modifier) {
        Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center) {
            Text("Welcome to Civora",style=MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            Text("Sign in to your campus account.",color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            Button(onClick=onSignInClick,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text("Sign In") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick=onCreateAccountClick,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text("Create student account") }
            Spacer(Modifier.height(24.dp))
            Text("Campus staff accounts are provided by your administrator.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
