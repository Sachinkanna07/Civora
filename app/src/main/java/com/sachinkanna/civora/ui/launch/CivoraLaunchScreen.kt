package com.sachinkanna.civora.ui.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sachinkanna.civora.data.model.UserRole
import com.sachinkanna.civora.ui.components.CivoraButton
import com.sachinkanna.civora.ui.components.CivoraCard
import com.sachinkanna.civora.ui.components.CivoraChip
import com.sachinkanna.civora.ui.components.CivoraGradientBackground
import com.sachinkanna.civora.ui.theme.Amber500
import com.sachinkanna.civora.ui.theme.CivoraTheme
import com.sachinkanna.civora.ui.theme.Cyan400
import com.sachinkanna.civora.ui.theme.Emerald500
import com.sachinkanna.civora.ui.theme.Rose500
import com.sachinkanna.civora.ui.theme.Violet400
import com.sachinkanna.civora.ui.theme.Violet500

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CivoraLaunchScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CivoraGradientBackground(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header / Brand Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
            ) {
                // App Logo Badge
                Box(
                    modifier =
                        Modifier.size(80.dp)
                            .background(
                                color = Violet500.copy(alpha = 0.2f),
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.size(56.dp)
                                .background(
                                    color = Violet500,
                                    shape = RoundedCornerShape(16.dp),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "C",
                            color = Color.White,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "CIVORA",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Everything on campus. One app.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Cyan400,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Middle Feature & Role Highlights
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CivoraCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "One Super-App for Everyone",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text =
                                "Civora connects students, faculty, admin, vendors, and drivers into a unified campus ecosystem.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Role Tags Flow Row
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            UserRole.entries.forEach { role ->
                                val color =
                                    when (role) {
                                        UserRole.STUDENT -> Cyan400
                                        UserRole.FACULTY -> Violet400
                                        UserRole.ADMIN -> Amber500
                                        UserRole.VENDOR -> Emerald500
                                        UserRole.DRIVER -> Rose500
                                    }
                                CivoraChip(
                                    text = role.displayName,
                                    accentColor = color,
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Actions
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CivoraButton(
                    text = "Get Started",
                    onClick = onGetStarted,
                    containerColor = Violet500,
                    contentColor = Color.White,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Everything on campus. One app.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CivoraLaunchScreenPreview() {
    CivoraTheme(darkTheme = true) {
        CivoraLaunchScreen(onGetStarted = {})
    }
}
