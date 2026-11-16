package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.data.model.KittySkin
import com.example.data.model.PlayerProfile
import com.example.game.GamePhase
import com.example.game.HudSnapshot
import com.example.game.gl.SubwayGameSurface
import com.example.ui.GameViewModel
import com.example.ui.MenuSheet
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
fun GameScreen(
    viewModel: GameViewModel,
    onSignedOut: () -> Unit
) {
    val hud by viewModel.hudState.collectAsStateWithLifecycle()
    val profileUiState by viewModel.profileState.collectAsStateWithLifecycle()
    val recentRunsState by viewModel.recentRunsState.collectAsStateWithLifecycle()
    val activeSheet by viewModel.activeSheet.collectAsStateWithLifecycle()
    val lastRunOutcome by viewModel.lastRunOutcome.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val profile = (profileUiState as? UiState.Success)?.data ?: PlayerProfile()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    // Back press pauses during active gameplay
    BackHandler(enabled = hud.phase == GamePhase.PLAYING) {
        viewModel.engine.pauseGame()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Live 60fps Stylized 3D Subway OpenGL World
        SubwayGameSurface(
            engine = viewModel.engine,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Phase-specific Compose HUD & Overlays
        when (hud.phase) {
            GamePhase.MENU -> {
                MainMenuOverlay(
                    profile = profile,
                    isMuted = isMuted,
                    onPlay = { viewModel.startRun() },
                    onOpenMissions = { viewModel.openSheet(MenuSheet.MISSIONS) },
                    onOpenShop = { viewModel.openSheet(MenuSheet.SHOP) },
                    onOpenHistory = { viewModel.openSheet(MenuSheet.HISTORY) },
                    onToggleMute = { viewModel.toggleMute() },
                    onSignOut = {
                        GoogleAuthHelper.signOut(credentialManager, scope, onSignedOut)
                    }
                )
            }
            GamePhase.PLAYING -> {
                GameplayHudOverlay(
                    hud = hud,
                    isMuted = isMuted,
                    onToggleMute = { viewModel.toggleMute() },
                    onPause = { viewModel.engine.pauseGame() },
                    onLeft = { viewModel.engine.swipeLeft() },
                    onRight = { viewModel.engine.swipeRight() },
                    onJump = { viewModel.engine.swipeUp() },
                    onSlide = { viewModel.engine.swipeDown() }
                )
            }
            GamePhase.PAUSED -> {
                GameplayHudOverlay(
                    hud = hud,
                    isMuted = isMuted,
                    onToggleMute = { viewModel.toggleMute() },
                    onPause = { viewModel.engine.resumeGame() },
                    onLeft = {},
                    onRight = {},
                    onJump = {},
                    onSlide = {}
                )
                PauseOverlay(
                    onResume = { viewModel.engine.resumeGame() },
                    onRestart = { viewModel.startRun() },
                    onBackToMenu = { viewModel.engine.returnToMenu() }
                )
            }
            GamePhase.CRASHED -> {
                GameOverOverlay(
                    hud = hud,
                    isNewRecord = lastRunOutcome?.isNewHighScore == true ||
                        (hud.score > profile.highScore && hud.score > 0L),
                    missionTierAdvanced = lastRunOutcome?.missionTierAdvanced == true,
                    newMultiplier = lastRunOutcome?.newMultiplier ?: profile.scoreMultiplier,
                    bonusCoins = lastRunOutcome?.bonusCoinsAwarded ?: 0L,
                    onRestart = { viewModel.startRun() },
                    onMenu = { viewModel.engine.returnToMenu() }
                )
            }
        }

        // 3. Secondary Sheets (Missions / Shop / History)
        when (activeSheet) {
            MenuSheet.MISSIONS -> MissionsModalSheet(
                profile = profile,
                onClose = { viewModel.closeSheet() }
            )
            MenuSheet.SHOP -> ShopModalSheet(
                profile = profile,
                onSelectSkin = { viewModel.equipOrUnlockSkin(it) },
                onUpgradePowerUp = { viewModel.upgradePowerUp(it) },
                onClose = { viewModel.closeSheet() }
            )
            MenuSheet.HISTORY -> HistoryModalSheet(
                runsState = recentRunsState,
                coinBalance = profile.totalCoins,
                onClose = { viewModel.closeSheet() }
            )
            MenuSheet.NONE -> Unit
        }

        // 4. Action feedback banner
        AnimatedVisibility(
            visible = actionMessage != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 68.dp, start = 20.dp, end = 20.dp)
        ) {
            Surface(
                onClick = { viewModel.clearActionMessage() },
                color = MetroSurfaceBright,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MetroCyan)
            ) {
                Text(
                    text = actionMessage.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun MainMenuOverlay(
    profile: PlayerProfile,
    isMuted: Boolean,
    onPlay: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleMute: () -> Unit,
    onSignOut: () -> Unit
) {
    val activeSkin = KittySkin.fromId(profile.selectedSkin)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MetroDeepBg.copy(alpha = 0.78f),
                        Color.Transparent,
                        MetroDeepBg.copy(alpha = 0.88f)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxSize()
                .align(Alignment.Center),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = MetroSurface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, MetroCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Pets,
                                contentDescription = null,
                                tint = Color(activeSkin.previewHex),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeSkin.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("menu_mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Som",
                            tint = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(id = R.string.sign_out),
                            tint = TextSecondary
                        )
                    }
                }

                // Title & Stats Pills
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPill(
                        icon = Icons.Filled.EmojiEvents,
                        value = "${profile.highScore} pts",
                        accent = MetroCyan
                    )
                    StatPill(
                        icon = Icons.Filled.MonetizationOn,
                        value = "${profile.totalCoins}",
                        accent = CoinGold
                    )
                    StatPill(
                        icon = Icons.Filled.Bolt,
                        value = "${profile.scoreMultiplier}x",
                        accent = MintEmerald
                    )
                }
            }

            // Bottom Missions Preview + Navigation + Play Button
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Active Objectives Quick Card
                Card(
                    onClick = onOpenMissions,
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MetroSurface.copy(alpha = 0.90f)),
                    border = BorderStroke(1.dp, MetroCyan.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("menu_missions_card")
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Objetivos Nível ${profile.missionTier}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MetroCyan
                            )
                            Text(
                                text = "Ver todos",
                                style = MaterialTheme.typography.labelLarge,
                                color = CoinGold
                            )
                        }
                        profile.activeMissions().take(2).forEach { m ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = m.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${minOf(m.current, m.target)}/${m.target}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (m.isCompleted) MintEmerald else CoinGold
                                )
                            }
                        }
                    }
                }

                // Secondary Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MenuActionTile(
                        icon = Icons.Filled.Flag,
                        label = "Objetivos",
                        accent = MagnetRuby,
                        onClick = onOpenMissions,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("missions_button")
                    )
                    MenuActionTile(
                        icon = Icons.Filled.Storefront,
                        label = "Gatinhos",
                        accent = CoinGold,
                        onClick = onOpenShop,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shop_button")
                    )
                    MenuActionTile(
                        icon = Icons.Filled.History,
                        label = "Corridas",
                        accent = MetroCyan,
                        onClick = onOpenHistory,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("history_button")
                    )
                }

                // Giant Play CTA
                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintEmerald,
                        contentColor = MetroDeepBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .testTag("play_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.play_now),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun GameplayHudOverlay(
    hud: HudSnapshot,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onPause: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onJump: () -> Unit,
    onSlide: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(14.dp)
    ) {
        // Top HUD row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Score & Multiplier
                Surface(
                    color = MetroDeepBg.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MetroCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = hud.score.toString().padStart(5, '0'),
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MetroCyan,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${hud.scoreMultiplier}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MetroDeepBg,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${hud.distanceMeters} m",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                    }
                }

                // Coins + Mute + Pause
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MetroDeepBg.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.65f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MonetizationOn,
                                contentDescription = "Moedas",
                                tint = CoinGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = hud.coins.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = CoinGold
                            )
                        }
                    }

                    FilledIconButton(
                        onClick = onToggleMute,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MetroDeepBg.copy(alpha = 0.78f),
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("hud_mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Som"
                        )
                    }

                    FilledIconButton(
                        onClick = onPause,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MetroDeepBg.copy(alpha = 0.78f),
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pause_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = stringResource(id = R.string.pause)
                        )
                    }
                }
            }

            // Active Power-up Bars (Ímã & Mochila a Jato)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (hud.magnetRemaining > 0f) {
                    PowerUpTimerBar(
                        icon = Icons.Filled.Bolt,
                        label = stringResource(id = R.string.magnet_label),
                        fraction = (hud.magnetRemaining / hud.magnetDuration).coerceIn(0f, 1f),
                        accent = MagnetRuby,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (hud.jetpackRemaining > 0f) {
                    PowerUpTimerBar(
                        icon = Icons.Filled.FlightTakeoff,
                        label = stringResource(id = R.string.jetpack_label),
                        fraction = (hud.jetpackRemaining / hud.jetpackDuration).coerceIn(0f, 1f),
                        accent = JetpackOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Dynamic Toast Callout
            AnimatedVisibility(
                visible = hud.toastMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Surface(
                    color = CoinGold,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = hud.toastMessage.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MetroDeepBg,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Bottom Responsive Touch Pad + Swipe Hint
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArcadeControlButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        description = "Esquerda",
                        onClick = onLeft,
                        tag = "ctrl_left"
                    )
                    ArcadeControlButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        description = "Direita",
                        onClick = onRight,
                        tag = "ctrl_right"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArcadeControlButton(
                        icon = Icons.Filled.KeyboardArrowDown,
                        description = "Rolar",
                        onClick = onSlide,
                        tag = "ctrl_slide"
                    )
                    ArcadeControlButton(
                        icon = Icons.Filled.KeyboardArrowUp,
                        description = "Pular",
                        onClick = onJump,
                        tag = "ctrl_jump"
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.swipe_hint),
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ArcadeControlButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tag: String
) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MetroDeepBg.copy(alpha = 0.55f),
            contentColor = TextPrimary
        ),
        modifier = Modifier
            .size(54.dp)
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun PowerUpTimerBar(
    icon: ImageVector,
    label: String,
    fraction: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MetroDeepBg.copy(alpha = 0.82f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = { fraction },
                color = accent,
                trackColor = MetroSurfaceBright,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onBackToMenu: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MetroDeepBg.copy(alpha = 0.82f))
            .padding(24.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MetroSurface),
            border = BorderStroke(1.5.dp, MetroCyan.copy(alpha = 0.6f)),
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = "Jogo Pausado",
                    style = MaterialTheme.typography.displaySmall,
                    color = TextPrimary
                )
                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MintEmerald, contentColor = MetroDeepBg),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("resume_button")
                ) {
                    Text(stringResource(id = R.string.resume), style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(
                    onClick = onRestart,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("pause_restart_button")
                ) {
                    Text(stringResource(id = R.string.restart), color = TextPrimary)
                }
                OutlinedButton(
                    onClick = onBackToMenu,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("pause_menu_button")
                ) {
                    Text(stringResource(id = R.string.back_to_menu), color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    hud: HudSnapshot,
    isNewRecord: Boolean,
    missionTierAdvanced: Boolean,
    newMultiplier: Long,
    bonusCoins: Long,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MetroDeepBg.copy(alpha = 0.84f))
            .padding(20.dp)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MetroSurface),
            border = BorderStroke(1.5.dp, if (isNewRecord) CoinGold else MetroCyan.copy(alpha = 0.55f)),
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(22.dp)
            ) {
                if (isNewRecord) {
                    Surface(
                        color = CoinGold,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.new_record),
                            style = MaterialTheme.typography.labelLarge,
                            color = MetroDeepBg,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(id = R.string.game_over_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = TextPrimary
                )

                Text(
                    text = "${hud.score} pts",
                    style = MaterialTheme.typography.displayLarge,
                    color = MetroCyan
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SummaryStatBox(label = "Moedas", value = "+${hud.coins}", color = CoinGold, modifier = Modifier.weight(1f))
                    SummaryStatBox(label = "Distância", value = "${hud.distanceMeters} m", color = MetroCyan, modifier = Modifier.weight(1f))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SummaryStatBox(label = "Moedas c/ Ímã", value = "${hud.magnetCoins}", color = MagnetRuby, modifier = Modifier.weight(1f))
                    SummaryStatBox(label = "Voo a Jato", value = "${hud.jetpackMeters} m", color = JetpackOrange, modifier = Modifier.weight(1f))
                }

                if (missionTierAdvanced) {
                    Surface(
                        color = MintEmerald.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MintEmerald),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Objetivos completos! +$bonusCoins moedas e multiplicador agora é ${newMultiplier}x!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MintEmerald,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = onRestart,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MintEmerald, contentColor = MetroDeepBg),
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("restart_button")
                ) {
                    Text(stringResource(id = R.string.restart), style = MaterialTheme.typography.titleLarge)
                }

                OutlinedButton(
                    onClick = onMenu,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("gameover_menu_button")
                ) {
                    Text(stringResource(id = R.string.back_to_menu), color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun SummaryStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MetroSurfaceBright,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = color)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun StatPill(
    icon: ImageVector,
    value: String,
    accent: Color
) {
    Surface(
        color = MetroSurface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = value, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
    }
}

@Composable
private fun MenuActionTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MetroSurface.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
    }
}
