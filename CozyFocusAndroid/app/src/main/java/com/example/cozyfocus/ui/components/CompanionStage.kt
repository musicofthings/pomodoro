package com.example.cozyfocus.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
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
import com.example.cozyfocus.model.CompanionAnimal
import com.example.cozyfocus.model.Cosmetic

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
            modifier = Modifier.fillMaxWidth().height(220.dp)
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
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .background(companion.accentColor.copy(alpha = 0.11f), CircleShape)
                .border(1.dp, companion.accentColor.copy(alpha = 0.24f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(breathingScale)
            ) {
                if (cosmetic != null) {
                    Text(text = cosmetic.mark, fontSize = 28.sp)
                }
                Text(text = companion.symbol, fontSize = 100.sp)
            }
        }
    }
}
