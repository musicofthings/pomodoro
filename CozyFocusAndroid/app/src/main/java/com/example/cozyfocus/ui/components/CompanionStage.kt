package com.cozyfocus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozyfocus.app.model.CompanionAnimal
import com.cozyfocus.app.model.Cosmetic

@Composable
fun CompanionStage(
    selectedCompanion: CompanionAnimal,
    equippedCosmetic: Cosmetic?,
    isFocusing: Boolean,
    onCompanionSelected: (CompanionAnimal) -> Unit,
    modifier: Modifier = Modifier
) {
    val companions = CompanionAnimal.entries
    val initialPage = companions.indexOf(selectedCompanion).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { companions.size })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onCompanionSelected(companions[page])
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(268.dp)
        ) { page ->
            val companion = companions[page]
            PortraitCard(
                companion = companion,
                cosmetic = if (companion == selectedCompanion) equippedCosmetic else null,
                isFocusing = isFocusing
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Swipe for another companion",
            fontSize = 13.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PortraitCard(
    companion: CompanionAnimal,
    cosmetic: Cosmetic?,
    isFocusing: Boolean
) {
    val transition = rememberInfiniteTransition(label = "companion motion")
    val breathing by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing and drift"
    )

    val breathingScale = 1f + breathing * 0.016f
    val verticalDrift = (breathing * 2.5f).dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(230.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer accent circle (254px equiv)
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .background(companion.accentColor.copy(alpha = 0.11f), CircleShape)
            )

            // Inner stroke circle (224px equiv)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .border(1.dp, companion.accentColor.copy(alpha = 0.24f), CircleShape)
            )

            // Face (198px equiv) with breathing & drift animation
            Box(
                modifier = Modifier
                    .size(175.dp)
                    .offset(y = verticalDrift)
                    .scale(breathingScale)
            ) {
                AnimalFace(
                    companion = companion,
                    cosmetic = cosmetic,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (isFocusing) "${companion.displayName} is breathing alongside you" else "${companion.displayName} is ready when you are",
            fontSize = 13.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}
