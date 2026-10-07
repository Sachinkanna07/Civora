package com.sachinkanna.civora.ui.transport

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

@Composable
fun CampusLocationsScreen(state: WorkspaceState, vm: CampusWorkspaceViewModel, back: () -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    CampusPage("Campus services", back) {
        item {
            OutlinedTextField(
                search,
                { search = it },
                label = { Text("Find a place") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        feedState(CampusFeed.LOCATIONS, state, vm, "Campus locations have not been published.")
        items(
            state.values<CampusLocation>(CampusFeed.LOCATIONS).filter {
                "${it.name} ${it.category}".contains(search, true)
            },
            key = { it.id },
        ) { place ->
            CampusCard(place.name, place.description, place.category)
            if (place.latitude != null && place.longitude != null)
                TextButton(
                    onClick = {
                        openMap(context, place.latitude, place.longitude, place.name)
                    }
                ) {
                    Text("Open directions")
                }
        }
    }
}
