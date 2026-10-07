package com.sachinkanna.civora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.navigation.CivoraNavGraph
import com.sachinkanna.civora.ui.theme.CivoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val emulatorHost = getString(R.string.firebase_emulator_host)
        if (
            (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0 &&
                emulatorHost.isNotBlank()
        ) {
            com.google.firebase.FirebaseApp.getApps(this).firstOrNull()?.delete()
            com.google.firebase.FirebaseApp.initializeApp(
                this,
                com.google.firebase.FirebaseOptions.Builder()
                    .setProjectId("demo-civora")
                    .setApplicationId("1:123456789:android:civora")
                    .setApiKey("demo-key-not-a-secret")
                    .build(),
            )
            com.google.firebase.auth.FirebaseAuth.getInstance().useEmulator(emulatorHost, 9099)
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .useEmulator(emulatorHost, 8080)
            com.google.firebase.functions.FirebaseFunctions.getInstance()
                .useEmulator(emulatorHost, 5001)
        }
        enableEdgeToEdge()
        setContent {
            CivoraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                        androidx.compose.foundation.layout.Column(
                            Modifier.padding(innerPadding).padding(24.dp)
                        ) {
                            androidx.compose.material3.Text(
                                "Civora",
                                style =
                                    androidx.compose.material3.MaterialTheme.typography
                                        .headlineLarge,
                            )
                            androidx.compose.material3.Text(
                                "Campus connection is not configured for this build. Contact your campus administrator."
                            )
                        }
                    } else CivoraNavGraph(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
