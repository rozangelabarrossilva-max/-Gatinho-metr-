package com.example.game

import com.example.data.model.KittySkin
import com.example.data.model.PlayerProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class GamePhase {
    MENU,
    PLAYING,
    PAUSED,
    CRASHED
}

enum class PowerUpKind {
    MAGNET,
    JETPACK
}

data class TrainEntity(
    val id: Int,
    val lane: Int,
    var z: Float,
    val length: Float,
    val colorIndex: Int,
    val isMoving: Boolean,
    val hasRamp: Boolean,
    var countedPassed: Boolean = false
)

enum class BarrierKind { LOW_HURDLE, HIGH_OVERHEAD }

data class BarrierEntity(
    val id: Int,
    val lane: Int,
    var z: Float,
    val kind: BarrierKind
)

data class CoinEntity(
    val id: Int,
    var x: Float,
    var y: Float,
    var z: Float,
    var collected: Boolean = false,
    var magnetized: Boolean = false,
    val spinOffset: Float = 0f
)

data class PowerUpEntity(
    val id: Int,
    val lane: Int,
    var y: Float,
    var z: Float,
    val kind: PowerUpKind,
    var collected: Boolean = false
)

data class ParticleEntity(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var life: Float,
    var maxLife: Float,
    var r: Float,
    var g: Float,
    var b: Float,
    var size: Float
)

data class HudSnapshot(
    val phase: GamePhase = GamePhase.MENU,
    val score: Long = 0L,
    val coins: Long = 0L,
    val distanceMeters: Long = 0L,
    val magnetCoins: Long = 0L,
    val jetpackFlights: Long = 0L,
    val jetpackMeters: Long = 0L,
    val trainsPassed: Long = 0L,
    val magnetRemaining: Float = 0f,
    val magnetDuration: Float = 7.5f,
    val jetpackRemaining: Float = 0f,
    val jetpackDuration: Float = 6.5f,
    val scoreMultiplier: Long = 1L,
    val isFlying: Boolean = false,
    val activeSkin: KittySkin = KittySkin.GINGER,
    val toastMessage: String? = null
)

/**
 * Lock-free render snapshot copied in <0.1ms so OpenGL drawing never stalls touch input.
 */
class RenderFrameState {
    var phase: GamePhase = GamePhase.MENU
    var activeSkin: KittySkin = KittySkin.GINGER
    var playerX: Float = 0f
    var playerY: Float = 0f
    var playerRoll: Float = 0f
    var slideTimer: Float = 0f
    var runCycle: Float = 0f
    var onTrainRoof: Boolean = false
    var cameraShake: Float = 0f
    var trackScroll: Float = 0f
    var magnetTimer: Float = 0f
    var jetpackTimer: Float = 0f

    var trainCount: Int = 0
    val trainLane = IntArray(24)
    val trainZ = FloatArray(24)
    val trainLength = FloatArray(24)
    val trainColor = IntArray(24)
    val trainHasRamp = BooleanArray(24)

    var barrierCount: Int = 0
    val barrierLane = IntArray(24)
    val barrierZ = FloatArray(24)
    val barrierKind = Array(24) { BarrierKind.LOW_HURDLE }

    var coinCount: Int = 0
    val coinX = FloatArray(140)
    val coinY = FloatArray(140)
    val coinZ = FloatArray(140)
    val coinSpin = FloatArray(140)

    var powerUpCount: Int = 0
    val powerUpLane = IntArray(12)
    val powerUpY = FloatArray(12)
    val powerUpZ = FloatArray(12)
    val powerUpKind = Array(12) { PowerUpKind.MAGNET }

    var particleCount: Int = 0
    val partX = FloatArray(120)
    val partY = FloatArray(120)
    val partZ = FloatArray(120)
    val partR = FloatArray(120)
    val partG = FloatArray(120)
    val partB = FloatArray(120)
    val partSize = FloatArray(120)
}

class GameEngine(
    private val soundFx: SoundFxEngine,
    private val onRunFinished: (HudSnapshot) -> Unit
) {
    companion object {
        const val LANE_WIDTH = 2.4f
        const val TRAIN_ROOF_Y = 2.26f
        const val JETPACK_FLY_Y = 6.2f
        const val SPAWN_Z = -112f
        const val DESPAWN_Z = 14f
        fun laneToX(lane: Int): Float = (lane - 1) * LANE_WIDTH
    }

    private val stateLock = Any()

    var phase: GamePhase = GamePhase.MENU
        private set
    var activeSkin: KittySkin = KittySkin.GINGER
    var scoreMultiplier: Long = 1L
    var magnetDurationSec: Float = 7.5f
    var jetpackDurationSec: Float = 6.5f

    var targetLane: Int = 1
        private set
    var playerX: Float = 0f
        private set
    var playerY: Float = 0f
        private set
    var playerVy: Float = 0f
        private set
    var playerRoll: Float = 0f
        private set
    var slideTimer: Float = 0f
        private set
    var runCycle: Float = 0f
        private set
    var onTrainRoof: Boolean = false
        private set
    var cameraShake: Float = 0f
        private set
    var trackScroll: Float = 0f
        private set

    var magnetTimer: Float = 0f
        private set
    var jetpackTimer: Float = 0f
        private set
    var worldSpeed: Float = 21.5f
        private set
    var distanceFloat: Float = 0f
        private set
    var coinsCollected: Long = 0L
        private set
    var magnetCoinsCollected: Long = 0L
        private set
    var jetpackFlightsCount: Long = 0L
        private set
    var jetpackDistanceFloat: Float = 0f
        private set
    var trainsPassedCount: Long = 0L
        private set
    private var coinStreak: Int = 0
    private var toastMessage: String? = null
    private var toastTimer: Float = 0f

    private val trains = ArrayList<TrainEntity>(16)
    private val barriers = ArrayList<BarrierEntity>(16)
    private val coins = ArrayList<CoinEntity>(120)
    private val powerUps = ArrayList<PowerUpEntity>(8)
    private val particles = ArrayList<ParticleEntity>(120)

    private var nextId = 1
    private var spawnCooldown = 0f
    private var jetpackCoinCooldown = 0f
    private var jetpackLanePattern = 1
    private var hudEmitAccum = 0f
    private var lastEmittedScore = -1L
    private var lastEmittedCoins = -1L
    private var missionBaselineMagnet = 0L
    private var missionTargetMagnet = 20L
    private var missionBaselineJetpack = 0L
    private var missionTargetJetpack = 1L
    private var magnetMissionNotified = false
    private var jetpackMissionNotified = false

    private val _hudState = MutableStateFlow(HudSnapshot())
    val hudState: StateFlow<HudSnapshot> = _hudState.asStateFlow()

    fun syncProfileConfig(profile: PlayerProfile) {
        synchronized(stateLock) {
            activeSkin = KittySkin.fromId(profile.selectedSkin)
            scoreMultiplier = profile.scoreMultiplier.coerceAtLeast(1L)
            magnetDurationSec = profile.magnetDurationSeconds()
            jetpackDurationSec = profile.jetpackDurationSeconds()
            missionBaselineMagnet = profile.missionCoinsWithMagnet
            missionBaselineJetpack = profile.missionJetpackFlights
            val missions = profile.activeMissions()
            missionTargetMagnet = missions.firstOrNull { it.id == "magnet" }?.target ?: 20L
            missionTargetJetpack = missions.firstOrNull { it.id == "jetpack" }?.target ?: 1L
            publishHudLocked(force = true)
        }
    }

    fun startNewRun() {
        synchronized(stateLock) {
            trains.clear()
            barriers.clear()
            coins.clear()
            powerUps.clear()
            particles.clear()

            targetLane = 1
            playerX = 0f
            playerY = 0f
            playerVy = 0f
            playerRoll = 0f
            slideTimer = 0f
            onTrainRoof = false
            cameraShake = 0f
            magnetTimer = 0f
            jetpackTimer = 0f
            worldSpeed = 22f
            distanceFloat = 0f
            coinsCollected = 0L
            magnetCoinsCollected = 0L
            jetpackFlightsCount = 0L
            jetpackDistanceFloat = 0f
            trainsPassedCount = 0L
            coinStreak = 0
            toastMessage = null
            toastTimer = 0f
            magnetMissionNotified = missionBaselineMagnet >= missionTargetMagnet
            jetpackMissionNotified = missionBaselineJetpack >= missionTargetJetpack

            spawnCooldown = 0.5f
            spawnCoinLine(lane = 1, startZ = -20f, count = 6, baseY = 0.65f, spacing = 3.0f)
            spawnChunk(-44f)
            spawnChunk(-74f)
            spawnChunk(-104f)

            phase = GamePhase.PLAYING
            publishHudLocked(force = true)
        }
    }

    fun pauseGame() {
        synchronized(stateLock) {
            if (phase == GamePhase.PLAYING) {
                phase = GamePhase.PAUSED
                publishHudLocked(force = true)
            }
        }
    }

    fun resumeGame() {
        synchronized(stateLock) {
            if (phase == GamePhase.PAUSED) {
                phase = GamePhase.PLAYING
                publishHudLocked(force = true)
            }
        }
    }

    fun returnToMenu() {
        synchronized(stateLock) {
            trains.clear()
            barriers.clear()
            coins.clear()
            powerUps.clear()
            particles.clear()
            targetLane = 1
            playerX = 0f
            playerY = 0f
            playerVy = 0f
            magnetTimer = 0f
            jetpackTimer = 0f
            slideTimer = 0f
            phase = GamePhase.MENU
            publishHudLocked(force = true)
        }
    }

    fun swipeLeft() {
        synchronized(stateLock) {
            if (phase != GamePhase.PLAYING) return
            if (targetLane > 0) {
                targetLane--
                soundFx.playSwipe()
            }
        }
    }

    fun swipeRight() {
        synchronized(stateLock) {
            if (phase != GamePhase.PLAYING) return
            if (targetLane < 2) {
                targetLane++
                soundFx.playSwipe()
            }
        }
    }

    fun swipeUp() {
        synchronized(stateLock) {
            if (phase != GamePhase.PLAYING) return
            if (jetpackTimer > 0f) return
            val groundY = currentFloorUnderPlayerLocked()
            if (playerY <= groundY + 0.14f) {
                playerVy = 15.6f
                slideTimer = 0f
                onTrainRoof = false
                soundFx.playJump()
            }
        }
    }

    fun swipeDown() {
        synchronized(stateLock) {
            if (phase != GamePhase.PLAYING) return
            if (jetpackTimer > 0f) return
            val groundY = currentFloorUnderPlayerLocked()
            if (playerY > groundY + 0.18f) {
                playerVy = -30f
                slideTimer = 0.45f
            } else {
                slideTimer = 0.65f
                soundFx.playSwipe()
            }
        }
    }

    fun stepAndCopyRenderState(rawDt: Float, out: RenderFrameState) {
        val dt = rawDt.coerceIn(0.001f, 0.05f)
        var finishedSnapshot: HudSnapshot? = null

        synchronized(stateLock) {
            if (phase == GamePhase.MENU) {
                runCycle += dt * 7.5f
                trackScroll = (trackScroll + dt * 9f) % 20f
                copyToRenderStateLocked(out)
                return
            }

            if (phase == GamePhase.PLAYING) {
                if (toastTimer > 0f) {
                    toastTimer -= dt
                    if (toastTimer <= 0f) toastMessage = null
                }
                cameraShake = (cameraShake - dt * 2.6f).coerceAtLeast(0f)

                val targetBaseSpeed = (22f + (distanceFloat / 120f) * 1.4f).coerceAtMost(38f)
                val speedFactor = if (jetpackTimer > 0f) 1.52f else 1.0f
                worldSpeed = targetBaseSpeed * speedFactor

                val dz = worldSpeed * dt
                trackScroll = (trackScroll + dz) % 20f
                distanceFloat += dz * 0.65f
                runCycle += dt * (if (jetpackTimer > 0f) 6f else 15f)

                if (slideTimer > 0f) slideTimer = (slideTimer - dt).coerceAtLeast(0f)
                if (magnetTimer > 0f) magnetTimer = (magnetTimer - dt).coerceAtLeast(0f)
                if (jetpackTimer > 0f) {
                    jetpackTimer = (jetpackTimer - dt).coerceAtLeast(0f)
                    jetpackDistanceFloat += dz * 0.65f
                    spawnJetpackExhaustLocked()
                    jetpackCoinCooldown -= dt
                    if (jetpackCoinCooldown <= 0f) {
                        jetpackCoinCooldown = 0.20f
                        if (Random.nextFloat() < 0.24f) {
                            jetpackLanePattern = Random.nextInt(0, 3)
                        }
                        coins.add(
                            CoinEntity(
                                id = nextId++,
                                x = laneToX(jetpackLanePattern),
                                y = JETPACK_FLY_Y + 0.35f,
                                z = -72f,
                                spinOffset = Random.nextFloat() * 6.28f
                            )
                        )
                    }
                }

                val desiredX = laneToX(targetLane)
                val dx = desiredX - playerX
                playerX += dx * (dt * 16f).coerceAtMost(1f)
                playerRoll = (-dx * 0.16f).coerceIn(-0.42f, 0.42f)

                val floorY = currentFloorUnderPlayerLocked()
                if (jetpackTimer > 0f) {
                    playerY += (JETPACK_FLY_Y - playerY) * (dt * 7.5f).coerceAtMost(1f)
                    playerVy = 0f
                    onTrainRoof = false
                } else {
                    playerVy -= 41f * dt
                    playerY += playerVy * dt
                    if (playerY <= floorY) {
                        playerY = floorY
                        if (playerVy < 0f) playerVy = 0f
                        onTrainRoof = floorY > 0.5f
                    } else {
                        onTrainRoof = false
                    }
                }

                var i = trains.size - 1
                while (i >= 0) {
                    val train = trains[i]
                    val trainAdvance = if (train.isMoving) dz * 1.48f else dz
                    train.z += trainAdvance
                    if (!train.countedPassed && train.z - train.length * 0.5f > 2.0f) {
                        train.countedPassed = true
                        trainsPassedCount++
                    }
                    if (train.z - train.length * 0.5f > DESPAWN_Z) {
                        trains.removeAt(i)
                    }
                    i--
                }

                i = barriers.size - 1
                while (i >= 0) {
                    val b = barriers[i]
                    b.z += dz
                    if (b.z > DESPAWN_Z) barriers.removeAt(i)
                    i--
                }

                i = powerUps.size - 1
                while (i >= 0) {
                    val pu = powerUps[i]
                    pu.z += dz
                    if (!pu.collected &&
                        abs(laneToX(pu.lane) - playerX) < 1.15f &&
                        abs(pu.z) < 1.45f &&
                        abs(pu.y - (playerY + 0.8f)) < 1.65f
                    ) {
                        pu.collected = true
                        when (pu.kind) {
                            PowerUpKind.MAGNET -> {
                                magnetTimer = magnetDurationSec
                                soundFx.playPowerUp(isJetpack = false)
                                showToastLocked("ÍMÃ ATIVADO!")
                                spawnBurstLocked(playerX, playerY + 1f, 0f, 1.0f, 0.25f, 0.42f, 12)
                            }
                            PowerUpKind.JETPACK -> {
                                jetpackTimer = jetpackDurationSec
                                jetpackFlightsCount++
                                slideTimer = 0f
                                soundFx.playPowerUp(isJetpack = true)
                                showToastLocked("VOANDO DE MOCHILA A JATO!")
                                spawnBurstLocked(playerX, playerY + 1f, 0f, 1.0f, 0.55f, 0.1f, 14)
                                if (!jetpackMissionNotified &&
                                    missionBaselineJetpack + jetpackFlightsCount >= missionTargetJetpack
                                ) {
                                    jetpackMissionNotified = true
                                    soundFx.playMissionComplete()
                                    showToastLocked("Objetivo de Voo Completo!")
                                }
                            }
                        }
                    }
                    if (pu.collected || pu.z > DESPAWN_Z) powerUps.removeAt(i)
                    i--
                }

                i = coins.size - 1
                while (i >= 0) {
                    val coin = coins[i]
                    coin.z += dz
                    if (!coin.collected) {
                        if (magnetTimer > 0f && coin.z < 3.5f && coin.z > -19f) {
                            coin.magnetized = true
                        }
                        if (coin.magnetized) {
                            val pull = (dt * 15f).coerceAtMost(1f)
                            coin.x += (playerX - coin.x) * pull
                            coin.y += ((playerY + 0.85f) - coin.y) * pull
                            coin.z += (0f - coin.z) * pull
                        }
                        val collectRadiusX = if (coin.magnetized) 1.3f else 1.05f
                        if (abs(coin.x - playerX) < collectRadiusX &&
                            abs(coin.z) < 1.35f &&
                            abs(coin.y - (playerY + 0.8f)) < 1.55f
                        ) {
                            coin.collected = true
                            coinsCollected++
                            if (magnetTimer > 0f || coin.magnetized) {
                                magnetCoinsCollected++
                                if (!magnetMissionNotified &&
                                    missionBaselineMagnet + magnetCoinsCollected >= missionTargetMagnet
                                ) {
                                    magnetMissionNotified = true
                                    soundFx.playMissionComplete()
                                    showToastLocked("Objetivo do Ímã Completo!")
                                }
                            }
                            coinStreak++
                            soundFx.playCoin(coinStreak)
                            spawnBurstLocked(coin.x, coin.y, coin.z, 1.0f, 0.86f, 0.25f, 5)
                        }
                    }
                    if (coin.collected || coin.z > DESPAWN_Z) coins.removeAt(i)
                    i--
                }

                spawnCooldown -= dz
                if (spawnCooldown <= 0f) {
                    spawnChunk(SPAWN_Z)
                    spawnCooldown = 31f
                }

                if (jetpackTimer <= 0f && checkCollisionLocked()) {
                    phase = GamePhase.CRASHED
                    cameraShake = 0.75f
                    soundFx.playCrash()
                    spawnBurstLocked(playerX, playerY + 0.9f, 0f, 1.0f, 0.35f, 0.35f, 18)
                    publishHudLocked(force = true)
                    finishedSnapshot = _hudState.value
                } else {
                    updateParticlesLocked(dt)
                    hudEmitAccum += dt
                    if (hudEmitAccum >= 0.08f) {
                        hudEmitAccum = 0f
                        publishHudLocked(force = false)
                    }
                }
            } else {
                updateParticlesLocked(dt)
                cameraShake = (cameraShake - dt * 2.6f).coerceAtLeast(0f)
            }

            copyToRenderStateLocked(out)
        }

        if (finishedSnapshot != null) {
            onRunFinished(finishedSnapshot!!)
        }
    }

    // Also keep step(dt) for unit tests
    fun step(rawDt: Float) {
        stepAndCopyRenderState(rawDt, scratchRenderState)
    }

    private val scratchRenderState = RenderFrameState()

    private fun copyToRenderStateLocked(out: RenderFrameState) {
        out.phase = phase
        out.activeSkin = activeSkin
        out.playerX = playerX
        out.playerY = playerY
        out.playerRoll = playerRoll
        out.slideTimer = slideTimer
        out.runCycle = runCycle
        out.onTrainRoof = onTrainRoof
        out.cameraShake = cameraShake
        out.trackScroll = trackScroll
        out.magnetTimer = magnetTimer
        out.jetpackTimer = jetpackTimer

        val tc = minOf(trains.size, out.trainLane.size)
        out.trainCount = tc
        for (i in 0 until tc) {
            val t = trains[i]
            out.trainLane[i] = t.lane
            out.trainZ[i] = t.z
            out.trainLength[i] = t.length
            out.trainColor[i] = t.colorIndex
            out.trainHasRamp[i] = t.hasRamp
        }

        val bc = minOf(barriers.size, out.barrierLane.size)
        out.barrierCount = bc
        for (i in 0 until bc) {
            val b = barriers[i]
            out.barrierLane[i] = b.lane
            out.barrierZ[i] = b.z
            out.barrierKind[i] = b.kind
        }

        val cc = minOf(coins.size, out.coinX.size)
        out.coinCount = cc
        for (i in 0 until cc) {
            val c = coins[i]
            out.coinX[i] = c.x
            out.coinY[i] = c.y
            out.coinZ[i] = c.z
            out.coinSpin[i] = c.spinOffset
        }

        val pc = minOf(powerUps.size, out.powerUpLane.size)
        out.powerUpCount = pc
        for (i in 0 until pc) {
            val p = powerUps[i]
            out.powerUpLane[i] = p.lane
            out.powerUpY[i] = p.y
            out.powerUpZ[i] = p.z
            out.powerUpKind[i] = p.kind
        }

        val ptc = minOf(particles.size, out.partX.size)
        out.particleCount = ptc
        for (i in 0 until ptc) {
            val pt = particles[i]
            val fade = (pt.life / pt.maxLife).coerceIn(0f, 1f)
            out.partX[i] = pt.x
            out.partY[i] = pt.y
            out.partZ[i] = pt.z
            out.partR[i] = pt.r
            out.partG[i] = pt.g
            out.partB[i] = pt.b
            out.partSize[i] = pt.size * fade
        }
    }

    private fun currentFloorUnderPlayerLocked(): Float {
        var floor = 0f
        for (i in 0 until trains.size) {
            val t = trains[i]
            val tx = laneToX(t.lane)
            if (abs(playerX - tx) > 0.95f) continue
            val halfLen = t.length * 0.5f
            val frontZ = t.z + halfLen
            val backZ = t.z - halfLen

            if (t.hasRamp) {
                val rampStartZ = frontZ + 4.4f
                if (0f in frontZ..rampStartZ) {
                    val progress = ((rampStartZ - 0f) / 4.4f).coerceIn(0f, 1f)
                    val rampY = progress * TRAIN_ROOF_Y
                    if (playerY >= rampY - 0.7f) {
                        floor = maxOf(floor, rampY)
                    }
                }
            }
            if (0f in backZ..frontZ && playerY >= TRAIN_ROOF_Y - 0.55f) {
                floor = maxOf(floor, TRAIN_ROOF_Y)
            }
        }
        return floor
    }

    private fun checkCollisionLocked(): Boolean {
        val playerHalfW = 0.40f
        val playerBottom = playerY
        val playerTop = playerY + (if (slideTimer > 0f) 0.72f else 1.55f)

        for (i in 0 until trains.size) {
            val t = trains[i]
            val tx = laneToX(t.lane)
            if (abs(playerX - tx) < 0.76f + playerHalfW) {
                val halfLen = t.length * 0.5f
                val frontZ = t.z + halfLen
                val backZ = t.z - halfLen
                if (0f in (backZ - 0.35f)..(frontZ + 0.35f)) {
                    val onRamp = t.hasRamp && 0f >= frontZ - 0.25f
                    if (!onRamp && playerBottom < TRAIN_ROOF_Y - 0.42f) {
                        return true
                    }
                }
            }
        }

        for (i in 0 until barriers.size) {
            val b = barriers[i]
            val bx = laneToX(b.lane)
            if (abs(playerX - bx) < 0.75f + playerHalfW && abs(b.z) < 0.70f) {
                when (b.kind) {
                    BarrierKind.LOW_HURDLE -> if (playerBottom < 0.92f) return true
                    BarrierKind.HIGH_OVERHEAD -> if (playerTop > 1.02f && playerBottom < 2.5f) return true
                }
            }
        }
        return false
    }

    private fun spawnChunk(baseZ: Float) {
        val blockedCount = Random.nextInt(1, 3)
        // Pick lanes without allocating list copies in tight loops
        val p0 = Random.nextInt(0, 3)
        val p1 = (p0 + Random.nextInt(1, 3)) % 3
        val openLane = 3 - p0 - p1
        val selectedLanes = if (blockedCount == 1) intArrayOf(p0) else intArrayOf(p0, p1)

        for (lane in selectedLanes) {
            val roll = Random.nextFloat()
            when {
                roll < 0.54f -> {
                    val isMoving = Random.nextFloat() < 0.34f
                    val hasRamp = !isMoving && Random.nextFloat() < 0.48f
                    val len = if (Random.nextBoolean()) 13f else 17f
                    trains.add(
                        TrainEntity(
                            id = nextId++,
                            lane = lane,
                            z = baseZ,
                            length = len,
                            colorIndex = Random.nextInt(0, 4),
                            isMoving = isMoving,
                            hasRamp = hasRamp
                        )
                    )
                    if (hasRamp || Random.nextFloat() < 0.55f) {
                        spawnCoinLine(lane, baseZ - len * 0.3f, 4, TRAIN_ROOF_Y + 0.7f, 2.7f)
                    }
                }
                roll < 0.80f -> {
                    barriers.add(BarrierEntity(nextId++, lane, baseZ, BarrierKind.LOW_HURDLE))
                    spawnCoinArc(lane, baseZ)
                }
                else -> {
                    barriers.add(BarrierEntity(nextId++, lane, baseZ, BarrierKind.HIGH_OVERHEAD))
                    spawnCoinLine(lane, baseZ - 3f, 3, 0.55f, 3.0f)
                }
            }
        }

        val powerUpRoll = Random.nextFloat()
        if (powerUpRoll < 0.25f && powerUps.size < 3) {
            val kind = if (Random.nextFloat() < 0.52f) PowerUpKind.MAGNET else PowerUpKind.JETPACK
            powerUps.add(PowerUpEntity(nextId++, openLane, 1.0f, baseZ + 3f, kind))
        } else {
            spawnCoinLine(openLane, baseZ - 5f, 5, 0.7f, 2.9f)
        }
    }

    private fun spawnCoinLine(lane: Int, startZ: Float, count: Int, baseY: Float, spacing: Float) {
        val x = laneToX(lane)
        for (i in 0 until count) {
            coins.add(
                CoinEntity(
                    id = nextId++,
                    x = x,
                    y = baseY,
                    z = startZ - i * spacing,
                    spinOffset = i * 0.45f
                )
            )
        }
    }

    private fun spawnCoinArc(lane: Int, centerZ: Float) {
        val x = laneToX(lane)
        val offsets = floatArrayOf(-3.2f, -1.6f, 0f, 1.6f, 3.2f)
        val heights = floatArrayOf(1.1f, 1.95f, 2.35f, 1.95f, 1.1f)
        for (i in offsets.indices) {
            coins.add(
                CoinEntity(
                    id = nextId++,
                    x = x,
                    y = heights[i],
                    z = centerZ + offsets[i],
                    spinOffset = i * 0.35f
                )
            )
        }
    }

    private fun spawnJetpackExhaustLocked() {
        if (particles.size > 70) return
        for (s in -1..1 step 2) {
            val side = s * 0.24f
            particles.add(
                ParticleEntity(
                    x = playerX + side + (Random.nextFloat() - 0.5f) * 0.08f,
                    y = playerY + 0.55f,
                    z = 0.38f,
                    vx = (Random.nextFloat() - 0.5f) * 0.8f,
                    vy = -4.2f - Random.nextFloat() * 2.5f,
                    vz = 5.5f + Random.nextFloat() * 3f,
                    life = 0.24f,
                    maxLife = 0.24f,
                    r = 1.0f,
                    g = 0.48f + Random.nextFloat() * 0.42f,
                    b = 0.1f,
                    size = 0.24f
                )
            )
        }
    }

    private fun spawnBurstLocked(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, count: Int) {
        if (particles.size > 80) return
        repeat(count) {
            val angle = Random.nextFloat() * 6.283f
            val speed = 2.2f + Random.nextFloat() * 4.5f
            particles.add(
                ParticleEntity(
                    x = x,
                    y = y,
                    z = z,
                    vx = cos(angle) * speed,
                    vy = 1.8f + Random.nextFloat() * 4.5f,
                    vz = sin(angle) * speed,
                    life = 0.36f,
                    maxLife = 0.36f,
                    r = r,
                    g = g,
                    b = b,
                    size = 0.17f + Random.nextFloat() * 0.09f
                )
            )
        }
    }

    private fun updateParticlesLocked(dt: Float) {
        var i = particles.size - 1
        while (i >= 0) {
            val p = particles[i]
            p.life -= dt
            if (p.life <= 0f) {
                particles.removeAt(i)
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.z += p.vz * dt
                p.vy -= 8f * dt
            }
            i--
        }
    }

    private fun showToastLocked(msg: String) {
        toastMessage = msg
        toastTimer = 2.4f
    }

    private fun publishHudLocked(force: Boolean) {
        val meters = distanceFloat.toLong()
        val totalScore = (meters * 2L + coinsCollected * 10L) * scoreMultiplier
        if (!force && totalScore == lastEmittedScore && coinsCollected == lastEmittedCoins &&
            magnetTimer <= 0f && jetpackTimer <= 0f && toastMessage == null
        ) {
            return
        }
        lastEmittedScore = totalScore
        lastEmittedCoins = coinsCollected
        _hudState.value = HudSnapshot(
            phase = phase,
            score = totalScore,
            coins = coinsCollected,
            distanceMeters = meters,
            magnetCoins = magnetCoinsCollected,
            jetpackFlights = jetpackFlightsCount,
            jetpackMeters = jetpackDistanceFloat.toLong(),
            trainsPassed = trainsPassedCount,
            magnetRemaining = magnetTimer,
            magnetDuration = magnetDurationSec,
            jetpackRemaining = jetpackTimer,
            jetpackDuration = jetpackDurationSec,
            scoreMultiplier = scoreMultiplier,
            isFlying = jetpackTimer > 0f,
            activeSkin = activeSkin,
            toastMessage = toastMessage
        )
    }
}
