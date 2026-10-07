package com.sachinkanna.civora.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.ui.components.CivoraButton
import com.sachinkanna.civora.ui.components.CivoraGradientBackground
import com.sachinkanna.civora.ui.theme.Violet400
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = OnboardingPages.list
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    CivoraGradientBackground(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Bar with Skip Button
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isLastPage) {
                    TextButton(onClick = onFinishOnboarding) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            color = Violet400,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Middle Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) { pageIndex ->
                OnboardingPageContent(pageData = pages[pageIndex])
            }

            // Bottom Navigation & Indicator Controls
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Animated Page Indicator Dots
                PageIndicatorDots(
                    pageCount = pages.size,
                    currentPage = pagerState.currentPage,
                    activeColor = pages[pagerState.currentPage].accentColor,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Back Button
                    if (pagerState.currentPage > 0) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        ) {
                            Text(
                                text = "Back",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(64.dp))
                    }

                    // Next / Continue Button
                    CivoraButton(
                        text = if (isLastPage) "Continue" else "Next",
                        onClick = {
                            if (isLastPage) {
                                onFinishOnboarding()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier.width(160.dp),
                        containerColor = pages[pagerState.currentPage].accentColor,
                        contentColor = Color.White,
                    )
                }
            }
        }
    }
}

/** Custom animated page indicator dots. */
@Composable
private fun PageIndicatorDots(
    pageCount: Int,
    currentPage: Int,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by
                animateDpAsState(
                    targetValue = if (isSelected) 28.dp else 8.dp,
                    animationSpec = tween(durationMillis = 300),
                    label = "dotWidth",
                )
            val color by
                animateColorAsState(
                    targetValue = if (isSelected) activeColor else Color.White.copy(alpha = 0.3f),
                    animationSpec = tween(durationMillis = 300),
                    label = "dotColor",
                )

            Box(modifier = Modifier.height(8.dp).width(width).clip(CircleShape).background(color))
        }
    }
}
