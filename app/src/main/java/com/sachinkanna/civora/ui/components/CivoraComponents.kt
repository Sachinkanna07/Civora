package com.sachinkanna.civora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Theme-aware root for authentication and onboarding, with one set of system insets. */
@Composable
fun CivoraBackground(modifier: Modifier=Modifier,content: @Composable BoxScope.()->Unit) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(),content=content)
}
