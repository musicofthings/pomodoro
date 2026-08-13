package com.example.cozyfocus.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cozyfocus.model.CompanionAnimal
import com.example.cozyfocus.model.Cosmetic
import com.example.cozyfocus.ui.components.CompanionStage
import java.util.Calendar

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessions by viewModel.repository.sessions.collectAsState(initial = emptyList())
    val inventory by viewModel.inventory.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White.copy(alpha = 0.9f)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("🏠", fontSize = 20.sp) },
                    label = { Text("Focus") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("🗓️", fontSize = 20.sp) },
                    label = { Text("Journey") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Text("⭐", fontSize = 20.sp) },
                    label = { Text("Den") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F6F0))
        ) {
            when (selectedTab) {
                0 -> FocusTabContent(uiState = uiState, viewModel = viewModel)
                1 -> JourneyTabContent(sessionsCount = sessions.size, totalMinutes = (sessions.sumOf { it.durationSeconds } / 60).toInt())
                2 -> DenTabContent(
                    coinBalance = uiState.coinBalance,
                    equippedCosmetic = uiState.equippedCosmetic,
                    ownedCosmetics = inventory.map { it.cosmeticRaw },
                    onPurchase = { viewModel.purchaseCosmetic(it) },
                    onEquip = { viewModel.equipCosmetic(it) }
                )
            }

            AnimatedVisibility(
                visible = uiState.completionToastMessage != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
            ) {
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Text(
                        text = uiState.completionToastMessage ?: "",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusTabContent(
    uiState: MainUiState,
    viewModel: MainScreenViewModel
) {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    val minutes = (uiState.remainingSeconds / 60).toInt()
    val seconds = (uiState.remainingSeconds % 60).toInt()
    val timeText = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = greeting, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFF3E0), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "🪙 ${uiState.coinBalance}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
            }
        }

        CompanionStage(
            selectedCompanion = uiState.selectedCompanion,
            equippedCosmetic = uiState.equippedCosmetic,
            isFocusing = uiState.isRunning,
            onCompanionSelected = { companion ->
                if (companion != uiState.selectedCompanion) {
                    viewModel.selectCompanion(companion)
                }
            }
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (uiState.isComplete) "Lovely work" else if (uiState.isRunning) "Focus gently" else "A ${(viewModel.currentSessionDuration / 60)}-minute moment",
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeText,
                fontSize = 58.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.toggleTimer() },
                modifier = Modifier.weight(1f).height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = if (uiState.isRunning) "⏸  Pause gently" else if (uiState.isComplete) "▶  Begin another focus" else "▶  Begin focus",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color.White, CircleShape)
                    .clickable { viewModel.stopTimer() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "↺", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Choose your time", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${(viewModel.currentSessionDuration / 60)} minutes",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
                Slider(
                    value = uiState.durationIndex.toFloat(),
                    onValueChange = { viewModel.chooseDuration(it.toInt()) },
                    valueRange = 0f..12f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF9800),
                        activeTrackColor = Color(0xFFFF9800)
                    )
                )
            }
        }
    }
}

@Composable
private fun JourneyTabContent(sessionsCount: Int, totalMinutes: Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Your quiet progress", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Focus sessions completed")
                    Text("$sessionsCount", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total time protected")
                    Text("$totalMinutes minutes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DenTabContent(
    coinBalance: Int,
    equippedCosmetic: Cosmetic?,
    ownedCosmetics: List<String>,
    onPurchase: (Cosmetic) -> Unit,
    onEquip: (Cosmetic) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Tiny treasures", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Cosmetic.entries.forEach { cosmetic ->
            val owned = ownedCosmetics.contains(cosmetic.id)
            val isEquipped = equippedCosmetic == cosmetic
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = cosmetic.mark, fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = cosmetic.displayName, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (owned) "Yours to wear" else "${cosmetic.price} cozy coins",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    if (owned) {
                        OutlinedButton(onClick = { onEquip(cosmetic) }) {
                            Text(if (isEquipped) "Wearing" else "Wear")
                        }
                    } else {
                        Button(
                            onClick = { onPurchase(cosmetic) },
                            enabled = coinBalance >= cosmetic.price,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Text("Unlock")
                        }
                    }
                }
            }
        }
    }
}
