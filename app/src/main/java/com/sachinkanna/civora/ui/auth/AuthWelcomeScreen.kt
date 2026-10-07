package com.sachinkanna.civora.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.sachinkanna.civora.ui.components.CivoraButton
import com.sachinkanna.civora.ui.components.CivoraCard
import com.sachinkanna.civora.ui.components.CivoraGradientBackground
import com.sachinkanna.civora.ui.theme.CivoraTheme
import com.sachinkanna.civora.ui.theme.Cyan400
import com.sachinkanna.civora.ui.theme.Navy800
import com.sachinkanna.civora.ui.theme.Violet400
import com.sachinkanna.civora.ui.theme.Violet500

@Composable
fun AuthWelcomeScreen(
    onSignInClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
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

            // Middle Card
            CivoraCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Your campus day starts here",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text =
                            "Sign in to your campus account, or create a student account to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Bottom Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CivoraButton(
                    text = "Sign In",
                    onClick = onSignInClick,
                    containerColor = Violet500,
                    contentColor = Color.White,
                )

                Spacer(modifier = Modifier.height(12.dp))

                CivoraButton(
                    text = "Create Account",
                    onClick = onCreateAccountClick,
                    containerColor = Navy800,
                    contentColor = Violet400,
                )

                Spacer(modifier = Modifier.height(16.dp))

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
fun AuthWelcomeScreenPreview() {
    CivoraTheme(darkTheme = true) {
        AuthWelcomeScreen(
            onSignInClick = {},
            onCreateAccountClick = {},
        )
    }
}
