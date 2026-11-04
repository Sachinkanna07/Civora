package com.sachinkanna.civora.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.ui.components.*

@Composable
fun OnboardingScreen(onFinishOnboarding: ()->Unit) {
    CivoraBackground {
        Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Text("Your campus, connected",style=MaterialTheme.typography.headlineLarge)
                Text("A useful daily overview and straightforward services.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                CampusCard("Start with Today","See your next class, upcoming event and active order.")
                CampusCard("Find it in Services","Food, transport, timetable, events and campus help.")
                CampusCard("Stay in the loop","Your private inbox keeps updates and activity together.")
                Text("Location is shared only when a driver explicitly starts a trip. Photos attached to reports remain private.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick=onFinishOnboarding,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text("Continue") }
        }
    }
}
