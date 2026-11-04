package com.sachinkanna.civora.ui.launch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.ui.components.CivoraBackground

@Composable
fun CivoraLaunchScreen(onGetStarted: ()->Unit) {
    CivoraBackground {
        Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.primary) { Text("C",Modifier.padding(horizontal=24.dp,vertical=14.dp),style=MaterialTheme.typography.displayMedium,color=MaterialTheme.colorScheme.onPrimary) }
                Text("Civora",style=MaterialTheme.typography.displayLarge)
                Text("Everything on campus. One app.",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Text("A clearer campus day",style=MaterialTheme.typography.headlineMedium)
                Text("Your classes, campus food, transport and community — connected in one place.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(Icons.Outlined.CalendarMonth to "Stay ahead of your classes",Icons.Outlined.Restaurant to "Order, track and pick up",Icons.Outlined.DirectionsBus to "Know your route and boarding stop").forEach { (icon,label)->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {Icon(icon,null,tint=MaterialTheme.colorScheme.primary);Text(label,style=MaterialTheme.typography.bodyLarge)} }
            }
            Button(onClick=onGetStarted,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text("Get started") }
        }
    }
}
