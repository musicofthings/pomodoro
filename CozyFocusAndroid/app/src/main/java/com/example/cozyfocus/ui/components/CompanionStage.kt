package com.example.cozyfocus.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cozyfocus.model.CompanionAnimal
import com.example.cozyfocus.model.Cosmetic
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.sin

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
    val animationTime by produceState(0f) {
        val startTime = System.currentTimeMillis()
        while (true) {
            awaitFrame()
            value = (System.currentTimeMillis() - startTime) / 1000f
        }
    }

    val breathingScale = 1f + sin(animationTime * 1.5f) * 0.016f
    val verticalDrift = (sin(animationTime * 0.7f) * 2.5f).dp

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
