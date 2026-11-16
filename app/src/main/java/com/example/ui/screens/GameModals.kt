package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.KittySkin
import com.example.data.model.MissionIconType
import com.example.data.model.MissionProgressItem
import com.example.data.model.PlayerProfile
import com.example.data.model.PowerUpType
import com.example.data.model.RunRecord
import com.example.data.repository.GameRepository
import com.example.ui.UiState
import com.example.ui.theme.CoinGold
import com.example.ui.theme.JetpackOrange
import com.example.ui.theme.MagnetRuby
import com.example.ui.theme.MetroCyan
import com.example.ui.theme.MetroDeepBg
import com.example.ui.theme.MetroSurface
import com.example.ui.theme.MetroSurfaceBright
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MissionsModalSheet(
    profile: PlayerProfile,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    ModalContainer(
        title = stringResource(id = R.string.missions_title),
        coinBalance = profile.totalCoins,
        onClose = onClose
    ) {
        val missions = profile.activeMissions()
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MetroSurfaceBright),
            border = BorderStroke(1.dp, MetroCyan.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Conjunto de Objetivos #${profile.missionTier}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MetroCyan
                    )
                    Text(
                        text = "Complete os 3 objetivos para subir seu multiplicador!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Surface(
                    color = CoinGold,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${profile.scoreMultiplier}x",
                        style = MaterialTheme.typography.titleLarge,
                        color = MetroDeepBg,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(missions, key = { it.id }) { mission ->
                MissionCard(mission = mission)
            }
            item {
                Surface(
                    color = MintEmerald.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MintEmerald.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = CoinGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Recompensa ao completar: +${50L * profile.missionTier} moedas e +1x no multiplicador de pontos!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MissionCard(mission: MissionProgressItem) {
    val (icon, accent) = when (mission.iconType) {
        MissionIconType.MAGNET -> Icons.Filled.Bolt to MagnetRuby
        MissionIconType.JETPACK -> Icons.Filled.FlightTakeoff to JetpackOrange
        MissionIconType.DISTANCE -> Icons.AutoMirrored.Filled.DirectionsRun to MetroCyan
        MissionIconType.TRAINS -> Icons.Filled.DirectionsTransit to CoinGold
    }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MetroSurface),
        border = BorderStroke(
            1.dp,
            if (mission.isCompleted) MintEmerald else accent.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mission.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${minOf(mission.current, mission.target)} / ${mission.target}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (mission.isCompleted) MintEmerald else TextSecondary
                    )
                }
                if (mission.isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Objetivo completo",
                        tint = MintEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { mission.fraction },
                color = if (mission.isCompleted) MintEmerald else accent,
                trackColor = MetroSurfaceBright,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
fun ShopModalSheet(
    profile: PlayerProfile,
    onSelectSkin: (KittySkin) -> Unit,
    onUpgradePowerUp: (PowerUpType) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    ModalContainer(
        title = stringResource(id = R.string.shop_title),
        coinBalance = profile.totalCoins,
        onClose = onClose
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "Personagens Gatinhos 3D",
                    style = MaterialTheme.typography.titleLarge,
                    color = MetroCyan
                )
            }
            items(KittySkin.entries, key = { it.id }) { skin ->
                val isUnlocked = profile.unlockedSkins.contains(skin.id)
                val isEquipped = profile.selectedSkin == skin.id
                val canAfford = profile.totalCoins >= skin.priceCoins
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MetroSurface),
                    border = BorderStroke(
                        1.5.dp,
                        if (isEquipped) MetroCyan else Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(skin.previewHex))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Pets,
                                contentDescription = skin.displayName,
                                tint = MetroDeepBg,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = skin.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = skin.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = { onSelectSkin(skin) },
                            enabled = !isEquipped && (isUnlocked || canAfford),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isEquipped -> MintEmerald
                                    isUnlocked -> MetroCyan
                                    else -> CoinGold
                                },
                                contentColor = MetroDeepBg
                            ),
                            modifier = Modifier.testTag("skin_btn_${skin.id}")
                        ) {
                            Text(
                                text = when {
                                    isEquipped -> "Equipado"
                                    isUnlocked -> "Equipar"
                                    else -> "${skin.priceCoins} moedas"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Upgrades de Poderes",
                    style = MaterialTheme.typography.titleLarge,
                    color = CoinGold
                )
            }

            item {
                PowerUpUpgradeCard(
                    type = PowerUpType.MAGNET,
                    level = profile.magnetLevel,
                    durationLabel = "${profile.magnetDurationSeconds()}s de duração",
                    totalCoins = profile.totalCoins,
                    accent = MagnetRuby,
                    icon = Icons.Filled.Bolt,
                    onUpgrade = { onUpgradePowerUp(PowerUpType.MAGNET) }
                )
            }

            item {
                PowerUpUpgradeCard(
                    type = PowerUpType.JETPACK,
                    level = profile.jetpackLevel,
                    durationLabel = "${profile.jetpackDurationSeconds()}s de voo",
                    totalCoins = profile.totalCoins,
                    accent = JetpackOrange,
                    icon = Icons.Filled.FlightTakeoff,
                    onUpgrade = { onUpgradePowerUp(PowerUpType.JETPACK) }
                )
            }
        }
    }
}

@Composable
private fun PowerUpUpgradeCard(
    type: PowerUpType,
    level: Long,
    durationLabel: String,
    totalCoins: Long,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onUpgrade: () -> Unit
) {
    val isMax = level >= 5L
    val cost = GameRepository.upgradeCostForLevel(level)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MetroSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = type.title,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${type.title} (Nív. $level/5)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = durationLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Button(
                onClick = onUpgrade,
                enabled = !isMax && totalCoins >= cost,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = TextPrimary
                ),
                modifier = Modifier.testTag("upgrade_${type.name.lowercase()}")
            ) {
                Text(
                    text = if (isMax) "MÁX" else "$cost m",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HistoryModalSheet(
    runsState: UiState<List<RunRecord>>,
    coinBalance: Long,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    ModalContainer(
        title = stringResource(id = R.string.history_title),
        coinBalance = coinBalance,
        onClose = onClose
    ) {
        when (runsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MetroCyan)
                }
            }
            is UiState.Error -> {
                Text(
                    text = runsState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
            is UiState.Success -> {
                if (runsState.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Nenhuma corrida registrada ainda. Corra nos trilhos!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(runsState.data, key = { it.id }) { run ->
                            val skin = KittySkin.fromId(run.skinId)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MetroSurface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "${run.score} pts • ${skin.displayName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${run.distanceMeters}m • Ímã: ${run.magnetCoins} moedas • Voo: ${run.jetpackMeters}m",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                    Surface(
                                        color = CoinGold.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "+${run.coinsCollected}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = CoinGold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModalContainer(
    title: String,
    coinBalance: Long,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroDeepBg.copy(alpha = 0.92f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("modal_close_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MetroSurfaceBright,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.55f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MonetizationOn,
                            contentDescription = "Moedas",
                            tint = CoinGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = coinBalance.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = CoinGold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
