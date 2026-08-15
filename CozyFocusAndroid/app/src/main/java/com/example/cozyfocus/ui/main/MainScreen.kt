package com.cozyfocus.app.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cozyfocus.app.audio.AmbientSound
import com.cozyfocus.app.data.db.FocusSessionEntity
import com.cozyfocus.app.model.CompanionAnimal
import com.cozyfocus.app.model.Cosmetic
import com.cozyfocus.app.ui.components.CompanionStage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessions by viewModel.repository.sessions.collectAsState(initial = emptyList())
    val inventory by viewModel.inventory.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshNotificationStatus()
        viewModel.toggleTimer()
    }

    DisposableEffect(lifecycleOwner) {
        viewModel.reconcileTimer()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.reconcileTimer()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val toggleTimerWithNotificationPermission = {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED &&
            !uiState.notificationPermissionRequested &&
            !uiState.isRunning
        if (needsPermission) {
            viewModel.markNotificationPermissionRequested()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.toggleTimer()
        }
    }

    val completedToday = remember(sessions) {
        val calendar = Calendar.getInstance()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
        sessions.filter { session ->
            calendar.timeInMillis = session.completedAt
            calendar.get(Calendar.YEAR) == todayYear && calendar.get(Calendar.DAY_OF_YEAR) == todayDay
        }
    }

    val totalMinutes = remember(sessions) {
        (sessions.sumOf { it.durationSeconds } / 60).toInt()
    }

    val todayMinutes = remember(completedToday) {
        (completedToday.sumOf { it.durationSeconds } / 60).toInt()
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White.copy(alpha = 0.95f)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("⏱️", fontSize = 20.sp) },
                    label = { Text("Focus") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("📊", fontSize = 20.sp) },
                    label = { Text("Journey") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Text("🐾", fontSize = 20.sp) },
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
                0 -> FocusTabContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onToggleTimer = toggleTimerWithNotificationPermission
                )
                1 -> JourneyTabContent(
                    sessions = sessions,
                    completedTodayCount = completedToday.size,
                    totalMinutes = totalMinutes,
                    onShare = {
                        viewModel.shareJourneyCard(context, completedToday.size, todayMinutes)
                    }
                )
                2 -> DenTabContent(
                    selectedCompanion = uiState.selectedCompanion,
                    coinBalance = uiState.coinBalance,
                    equippedCosmetic = uiState.equippedCosmetic,
                    ownedCosmetics = inventory.map { it.cosmeticRaw },
                    onSelectCompanion = { viewModel.selectCompanion(it) },
                    onPurchase = { viewModel.purchaseCosmetic(it) },
                    onEquip = { viewModel.equipCosmetic(it) }
                )
            }

            // Completion Toast matching iOS (+5 cozy coins — you did it)
            AnimatedVisibility(
                visible = uiState.completionToastMessage != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨ ", fontSize = 16.sp)
                        Text(
                            text = uiState.completionToastMessage ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusTabContent(
    uiState: MainUiState,
    viewModel: MainScreenViewModel,
    onToggleTimer: () -> Unit
) {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    val minutes = (uiState.remainingSeconds / 60).toInt()
    val seconds = (uiState.remainingSeconds % 60).toInt()
    val timeText = String.format(LocalLocale.current.platformLocale, "%02d:%02d", minutes, seconds)

    var soundMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = greeting, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .background(Color(0xFFFF9800).copy(alpha = 0.1f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "🪙 ${uiState.coinBalance}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFFF9800)
                )
            }
        }

        // Companion Stage
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

        // Timer Display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (uiState.isComplete) "Lovely work" else if (uiState.isRunning) "Focus gently" else "A ${viewModel.durationAdjective} moment",
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeText,
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Action Buttons Row (Play/Pause + Red Destructive Stop)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onToggleTimer,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text(
                    text = viewModel.primaryButtonLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            OutlinedButton(
                onClick = { viewModel.stopTimer() },
                enabled = viewModel.canStop,
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Text(text = "⏹", fontSize = 18.sp, color = if (viewModel.canStop) Color.Red else Color.Gray)
            }
        }

        // Duration Picker Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Choose your time", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        text = viewModel.durationText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "1 min", fontSize = 12.sp, color = Color.Gray)
                    Text(text = "60 min", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        // Focus Controls Row (Ambient Sound Menu, Haptics Toggle, Distraction Shield Button)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ambient Sound Menu Button
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { soundMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${uiState.selectedSound.iconEmoji} ${uiState.selectedSound.label}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                DropdownMenu(
                    expanded = soundMenuExpanded,
                    onDismissRequest = { soundMenuExpanded = false }
                ) {
                    AmbientSound.entries.forEach { sound ->
                        DropdownMenuItem(
                            text = { Text("${sound.iconEmoji}  ${sound.label}") },
                            onClick = {
                                viewModel.selectAmbientSound(sound)
                                soundMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Haptics Toggle Button
            OutlinedButton(
                onClick = { viewModel.toggleHaptics() },
                modifier = Modifier.height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (uiState.hapticsEnabled) Color(0xFFFF9800).copy(alpha = 0.12f) else Color.Transparent
                )
            ) {
                Text(text = if (uiState.hapticsEnabled) "📳 Haptics" else "🔇 Haptics", fontSize = 12.sp)
            }

            // Android exposes its own user-controlled Focus and Do Not Disturb settings.
            OutlinedButton(
                onClick = { viewModel.openSystemFocusSettings() },
                modifier = Modifier.height(42.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "⚙️ Focus", fontSize = 12.sp)
            }
        }

        Text(
            text = when {
                uiState.completionAlertsEnabled -> "Completion alerts are enabled"
                uiState.notificationPermissionRequested ->
                    "Completion alerts are off; sessions still save when the timer ends"
                else -> uiState.focusSettingsStatusText
            },
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun JourneyTabContent(
    sessions: List<FocusSessionEntity>,
    completedTodayCount: Int,
    totalMinutes: Int,
    onShare: () -> Unit
) {
    val currentLocale = LocalLocale.current.platformLocale
    val dateFormat = remember(currentLocale) {
        SimpleDateFormat("EEE, MMM d, HH:mm", currentLocale)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Journey", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onShare, shape = RoundedCornerShape(12.dp)) {
                    Text(text = "📤 Share", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text(text = "Your quiet progress", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Focus sessions completed")
                        Text("${sessions.size}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Time protected")
                        Text("$totalMinutes min", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Today")
                        Text("$completedTodayCount sessions", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(text = "Recent moments", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        if (sessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        text = "Your completed focus moments will rest here.",
                        modifier = Modifier.padding(18.dp),
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(sessions.take(12)) { session ->
                val companion = CompanionAnimal.fromRaw(session.companionRaw)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = companion.symbol, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "A gentle focus session", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = dateFormat.format(Date(session.completedAt)),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Text(
                            text = "+${session.coinsEarned}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DenTabContent(
    selectedCompanion: CompanionAnimal,
    coinBalance: Int,
    equippedCosmetic: Cosmetic?,
    ownedCosmetics: List<String>,
    onSelectCompanion: (CompanionAnimal) -> Unit,
    onPurchase: (Cosmetic) -> Unit,
    onEquip: (Cosmetic) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "The Den", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }

        // Your Companion Section
        item {
            Text(text = "Your companion", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        items(CompanionAnimal.entries) { companion ->
            val isSelected = companion == selectedCompanion
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectCompanion(companion) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = companion.symbol, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(text = companion.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    if (isSelected) {
                        Text(text = "✓", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800), fontSize = 18.sp)
                    }
                }
            }
        }

        // Tiny Treasures Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Tiny treasures", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        items(Cosmetic.entries) { cosmetic ->
            val owned = ownedCosmetics.contains(cosmetic.id)
            val isEquipped = equippedCosmetic == cosmetic
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
