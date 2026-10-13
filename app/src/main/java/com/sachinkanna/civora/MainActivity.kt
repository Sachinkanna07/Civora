package com.sachinkanna.civora

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.navigation.CivoraNavGraph
import com.sachinkanna.civora.ui.theme.CivoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseEnvironment.configure(this)
        enableEdgeToEdge()
        handleDestination(intent)
        setContent {
            val mode by ThemePreferences.mode.collectAsState()
            val dark=when(mode){"DARK"->true;"LIGHT"->false;else->isSystemInDarkTheme()}
            CivoraTheme(darkTheme=dark) {
                Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
                    if(com.google.firebase.FirebaseApp.getApps(this).isEmpty()) Column(Modifier.safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text("Civora",style=MaterialTheme.typography.headlineLarge)
                        Text("Campus connection is not configured. Contact your campus administrator.")
                    } else CivoraNavGraph()
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent);setIntent(intent);handleDestination(intent) }
    private fun handleDestination(intent: Intent?) {
        val expected=intent?.getStringExtra("civora_user")
        if(expected!=null&&runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()!=expected)return
        val data=intent?.data
        AppNavigation.destination.value=if(data?.scheme=="civora") AppNavigation.route(data.host,data.lastPathSegment)
            else AppNavigation.route(intent?.getStringExtra("civora_destination"),intent?.getStringExtra("civora_entity"))
    }
}
