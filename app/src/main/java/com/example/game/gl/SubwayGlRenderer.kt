package com.example.game.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.data.model.KittySkin
import com.example.game.BarrierKind
import com.example.game.GameEngine
import com.example.game.GamePhase
import com.example.game.PowerUpKind
import com.example.game.RenderFrameState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class SubwayGlRenderer(
    private val engine: GameEngine
) : GLSurfaceView.Renderer {

    private lateinit var shader: ShaderProgram
    private lateinit var trackSegmentMesh: Mesh3D
    private lateinit var trainMeshes13: Array<Mesh3D>
    private lateinit var trainMeshes17: Array<Mesh3D>
    private lateinit var rampMesh: Mesh3D
    private lateinit var lowBarricadeMesh: Mesh3D
    private lateinit var highGateMesh: Mesh3D
    private lateinit var coinMesh: Mesh3D
    private lateinit var magnetMesh: Mesh3D
    private lateinit var jetpackMesh: Mesh3D
    private lateinit var unitCubeMesh: Mesh3D
    private lateinit var unitSphereMesh: Mesh3D
    private lateinit var unitConeMesh: Mesh3D
    private lateinit var kittyHeadMeshes: Map<KittySkin, Mesh3D>
    private lateinit var kittyTorsoMeshes: Map<KittySkin, Mesh3D>

    private val frameState = RenderFrameState()
    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var lastFrameNs: Long = 0L
    private var globalTime: Float = 0f
    private var smoothCamY: Float = 3.9f
    private var smoothCamX: Float = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Bright sunny Subway Surfers sky-blue backdrop!
        GLES20.glClearColor(0.32f, 0.72f, 0.98f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        shader = ShaderProgram()
        trackSegmentMesh = MeshBuilder.createSubwayTrackSegmentMesh()
        trainMeshes13 = Array(4) { idx -> MeshBuilder.createTrainMesh(idx, 13f) }
        trainMeshes17 = Array(4) { idx -> MeshBuilder.createTrainMesh(idx, 17f) }
        rampMesh = MeshBuilder.createRampMesh()
        lowBarricadeMesh = MeshBuilder.createLowBarricadeMesh()
        highGateMesh = MeshBuilder.createHighGateMesh()
        coinMesh = MeshBuilder.createCoinMesh()
        magnetMesh = MeshBuilder.createMagnetMesh()
        jetpackMesh = MeshBuilder.createJetpackMesh()
        unitCubeMesh = MeshBuilder.createUnitCube(1f, 1f, 1f)
        unitSphereMesh = MeshBuilder.createUnitSphere(1f, 1f, 1f)
        unitConeMesh = MeshBuilder.createUnitCone(1f, 1f, 1f)

        kittyHeadMeshes = KittySkin.entries.associateWith { skin ->
            MeshBuilder.createKittyHeadMesh(
                skin.furRgb.first, skin.furRgb.second, skin.furRgb.third,
                skin.bellyRgb.first, skin.bellyRgb.second, skin.bellyRgb.third,
                skin.accentRgb.first, skin.accentRgb.second, skin.accentRgb.third
            )
        }
        kittyTorsoMeshes = KittySkin.entries.associateWith { skin ->
            MeshBuilder.createKittyTorsoMesh(
                skin.furRgb.first, skin.furRgb.second, skin.furRgb.third,
                skin.bellyRgb.first, skin.bellyRgb.second, skin.bellyRgb.third,
                skin.accentRgb.first, skin.accentRgb.second, skin.accentRgb.third
            )
        }

        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
        Matrix.perspectiveM(projMatrix, 0, 56f, aspect, 0.5f, 135f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNs = now
        globalTime += dt

        engine.stepAndCopyRenderState(dt, frameState)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        shader.use()
        GLES20.glUniform3f(shader.uLightDir, 0.42f, 0.78f, 0.46f)
        GLES20.glUniform3f(shader.uFogColor, 0.50f, 0.80f, 0.99f)
        GLES20.glUniform1f(shader.uFogStart, 36f)
        GLES20.glUniform1f(shader.uFogRange, 78f)
        GLES20.glUniform3f(shader.uTintColor, 1f, 1f, 1f)
        GLES20.glUniform1f(shader.uEmissive, 0f)

        val targetCamY = frameState.playerY + (if (frameState.jetpackTimer > 0f) 3.1f else 3.85f)
        val targetCamX = frameState.playerX * 0.56f
        smoothCamY += (targetCamY - smoothCamY) * (dt * 7.5f).coerceAtMost(1f)
        smoothCamX += (targetCamX - smoothCamX) * (dt * 8.5f).coerceAtMost(1f)

        val shakeX = if (frameState.cameraShake > 0f) sin(globalTime * 55f) * frameState.cameraShake * 0.25f else 0f
        val shakeY = if (frameState.cameraShake > 0f) cos(globalTime * 48f) * frameState.cameraShake * 0.20f else 0f

        val eyeZ = if (frameState.phase == GamePhase.MENU) 5.8f else 7.1f
        val lookY = frameState.playerY + 1.35f
        Matrix.setLookAtM(
            viewMatrix, 0,
            smoothCamX + shakeX, smoothCamY + shakeY, eyeZ,
            frameState.playerX * 0.42f, lookY, -14f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        drawSubwayWorldBatched()
        drawSkylineSilhouettes()
        drawTrainsAndBarriers()
        drawCoinsAndPowerUps()
        drawKittyCharacter()
        drawParticles()
    }

    private fun drawSubwayWorldBatched() {
        trackSegmentMesh.bind(shader)
        GLES20.glUniform3f(shader.uTintColor, 1f, 1f, 1f)
        GLES20.glUniform1f(shader.uEmissive, 0f)

        // Entire 140-unit metro corridor rendered in just 7 draw calls!
        val baseZ = frameState.trackScroll
        for (i in 0 until 7) {
            val segZ = baseZ - i * 20f
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 0f, 0f, segZ)
            drawBoundMesh(trackSegmentMesh)
        }
    }

    private fun drawSkylineSilhouettes() {
        unitCubeMesh.bind(shader)
        GLES20.glUniform1f(shader.uEmissive, 0.25f)
        // Distant sunlit city buildings rising behind the metro yard
        val xs = floatArrayOf(-26f, -15f, -5f, 7f, 17f, 28f)
        val hs = floatArrayOf(22f, 32f, 26f, 35f, 24f, 28f)
        val ws = floatArrayOf(8f, 9f, 7.5f, 10f, 8f, 9f)
        for (i in xs.indices) {
            GLES20.glUniform3f(shader.uTintColor, 0.40f, 0.62f, 0.88f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, xs[i], hs[i] * 0.4f, -108f)
            Matrix.scaleM(modelMatrix, 0, ws[i], hs[i], 8f)
            drawBoundMesh(unitCubeMesh)
        }
        GLES20.glUniform3f(shader.uTintColor, 1f, 1f, 1f)
        GLES20.glUniform1f(shader.uEmissive, 0f)
    }

    private fun drawTrainsAndBarriers() {
        // Trains: 1 draw call per train!
        for (i in 0 until frameState.trainCount) {
            val tx = GameEngine.laneToX(frameState.trainLane[i])
            val tz = frameState.trainZ[i]
            val len = frameState.trainLength[i]
            val colorIdx = frameState.trainColor[i].coerceIn(0, 3)
            val mesh = if (len > 15f) trainMeshes17[colorIdx] else trainMeshes13[colorIdx]

            mesh.bind(shader)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, tx, 0f, tz)
            drawBoundMesh(mesh)

            if (frameState.trainHasRamp[i]) {
                rampMesh.bind(shader)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, tx, GameEngine.TRAIN_ROOF_Y * 0.5f, tz + len * 0.5f + 2.2f)
                Matrix.scaleM(modelMatrix, 0, 1.72f, GameEngine.TRAIN_ROOF_Y, 4.4f)
                drawBoundMesh(rampMesh)
            }
        }

        // Barriers: 1 draw call per barrier!
        for (i in 0 until frameState.barrierCount) {
            val bx = GameEngine.laneToX(frameState.barrierLane[i])
            val bz = frameState.barrierZ[i]
            val mesh = if (frameState.barrierKind[i] == BarrierKind.LOW_HURDLE) {
                lowBarricadeMesh
            } else {
                highGateMesh
            }
            mesh.bind(shader)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, bx, 0f, bz)
            drawBoundMesh(mesh)
        }
    }

    private fun drawCoinsAndPowerUps() {
        if (frameState.coinCount > 0) {
            coinMesh.bind(shader)
            GLES20.glUniform1f(shader.uEmissive, 0.35f)
            for (i in 0 until frameState.coinCount) {
                val spin = (globalTime * 4.2f + frameState.coinSpin[i]) * 57.2958f
                val bob = sin(globalTime * 4.5f + frameState.coinSpin[i]) * 0.08f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, frameState.coinX[i], frameState.coinY[i] + bob, frameState.coinZ[i])
                Matrix.rotateM(modelMatrix, 0, spin, 0f, 1f, 0f)
                drawBoundMesh(coinMesh)
            }
        }

        for (i in 0 until frameState.powerUpCount) {
            val px = GameEngine.laneToX(frameState.powerUpLane[i])
            val py = frameState.powerUpY[i] + sin(globalTime * 4f) * 0.16f
            val pz = frameState.powerUpZ[i]
            val mesh = if (frameState.powerUpKind[i] == PowerUpKind.MAGNET) magnetMesh else jetpackMesh
            mesh.bind(shader)
            GLES20.glUniform1f(shader.uEmissive, 0.45f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, px, py, pz)
            Matrix.rotateM(modelMatrix, 0, globalTime * 115f, 0f, 1f, 0f)
            drawBoundMesh(mesh)
        }
        GLES20.glUniform1f(shader.uEmissive, 0f)
    }

    private fun drawKittyCharacter() {
        val skin = frameState.activeSkin
        val furR = skin.furRgb.first; val furG = skin.furRgb.second; val furB = skin.furRgb.third
        val bellyR = skin.bellyRgb.first; val bellyG = skin.bellyRgb.second; val bellyB = skin.bellyRgb.third

        val isSliding = frameState.slideTimer > 0f
        val isFlying = frameState.jetpackTimer > 0f
        val baseX = frameState.playerX
        val baseY = frameState.playerY
        val hop = when {
            isFlying -> sin(globalTime * 6f) * 0.12f
            isSliding -> -0.24f
            else -> abs(sin(frameState.runCycle)) * 0.14f
        }
        val pitchDeg = when {
            isFlying -> -22f
            isSliding -> -55f
            else -> -8f
        }
        val rollDeg = frameState.playerRoll * 57.2958f
        val swing = if (isFlying) 10f else sin(frameState.runCycle) * 38f
        val rootY = baseY + (if (isSliding) 0.46f else 0.78f) + hop

        // Ground contact shadow
        if (!isFlying) {
            unitSphereMesh.bind(shader)
            val shadowY = if (frameState.onTrainRoof) GameEngine.TRAIN_ROOF_Y + 0.03f else 0.02f
            GLES20.glUniform3f(shader.uTintColor, 0.12f, 0.14f, 0.22f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, baseX, shadowY, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.95f, 0.05f, 0.95f)
            drawBoundMesh(unitSphereMesh)
        }

        // 1. Baked Kitty Torso (1 draw call)
        val torsoMesh = kittyTorsoMeshes.getValue(skin)
        torsoMesh.bind(shader)
        GLES20.glUniform3f(shader.uTintColor, 1f, 1f, 1f)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, baseX, rootY, 0f)
        if (rollDeg != 0f) Matrix.rotateM(modelMatrix, 0, rollDeg, 0f, 0f, 1f)
        Matrix.rotateM(modelMatrix, 0, pitchDeg, 1f, 0f, 0f)
        drawBoundMesh(torsoMesh)

        // 2. Baked Kitty Head + Ears + Headphones + Eyes + Nose (1 draw call!)
        val headMesh = kittyHeadMeshes.getValue(skin)
        headMesh.bind(shader)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, baseX, rootY + 0.62f, 0f)
        if (rollDeg != 0f) Matrix.rotateM(modelMatrix, 0, rollDeg * 0.7f, 0f, 0f, 1f)
        drawBoundMesh(headMesh)

        // 3. Articulated Arms, Legs, and Swishing Tail (unitCubeMesh bound ONCE)
        unitCubeMesh.bind(shader)
        GLES20.glUniform3f(shader.uTintColor, furR, furG, furB)
        // Left & Right Arms
        drawKittyLimb(baseX, rootY, pitchDeg + swing, rollDeg, -0.35f, 0.08f, 0f, 0.17f, 0.44f, 0.17f)
        drawKittyLimb(baseX, rootY, pitchDeg - swing, rollDeg, 0.35f, 0.08f, 0f, 0.17f, 0.44f, 0.17f)
        // Left & Right Legs
        drawKittyLimb(baseX, rootY, pitchDeg - swing, rollDeg, -0.15f, -0.44f, 0f, 0.20f, 0.48f, 0.20f)
        drawKittyLimb(baseX, rootY, pitchDeg + swing, rollDeg, 0.15f, -0.44f, 0f, 0.20f, 0.48f, 0.20f)

        // Swishing tail + bright tip
        val tailWag = sin(globalTime * 9f) * 28f
        drawKittyLimb(baseX, rootY, pitchDeg + 45f, rollDeg + tailWag, 0f, -0.04f, 0.40f, 0.13f, 0.60f, 0.13f)
        GLES20.glUniform3f(shader.uTintColor, bellyR, bellyG, bellyB)
        drawKittyLimb(baseX, rootY, pitchDeg + 45f, rollDeg + tailWag * 1.2f, 0f, 0.18f, 0.60f, 0.14f, 0.18f, 0.14f)
        GLES20.glUniform3f(shader.uTintColor, 1f, 1f, 1f)

        // 4. Active Jetpack + Flame Cones when flying!
        if (isFlying) {
            jetpackMesh.bind(shader)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, baseX, rootY + 0.05f, 0.30f)
            if (rollDeg != 0f) Matrix.rotateM(modelMatrix, 0, rollDeg, 0f, 0f, 1f)
            Matrix.rotateM(modelMatrix, 0, pitchDeg, 1f, 0f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.85f, 0.85f, 0.85f)
            drawBoundMesh(jetpackMesh)

            unitConeMesh.bind(shader)
            val flicker = 0.42f + abs(sin(globalTime * 34f)) * 0.24f
            GLES20.glUniform3f(shader.uTintColor, 1.0f, 0.82f, 0.18f)
            GLES20.glUniform1f(shader.uEmissive, 0.95f)
            drawKittyLimb(baseX, rootY, pitchDeg + 180f, rollDeg, -0.18f, -0.35f, 0.32f, 0.22f, flicker, 0.22f)
            drawKittyLimb(baseX, rootY, pitchDeg + 180f, rollDeg, 0.18f, -0.35f, 0.32f, 0.22f, flicker, 0.22f)
            GLES20.glUniform3f(shader.uTintColor, 1f, 1f, 1f)
            GLES20.glUniform1f(shader.uEmissive, 0f)
        }

        // 5. Orbiting Magnet above head when Magnet is active!
        if (frameState.magnetTimer > 0f) {
            magnetMesh.bind(shader)
            GLES20.glUniform1f(shader.uEmissive, 0.55f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, baseX, rootY + 1.38f + sin(globalTime * 5f) * 0.08f, 0f)
            Matrix.rotateM(modelMatrix, 0, globalTime * 200f, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.56f, 0.56f, 0.56f)
            drawBoundMesh(magnetMesh)
            GLES20.glUniform1f(shader.uEmissive, 0f)
        }
    }

    private fun drawParticles() {
        if (frameState.particleCount <= 0) return
        unitSphereMesh.bind(shader)
        GLES20.glUniform1f(shader.uEmissive, 0.9f)
        for (i in 0 until frameState.particleCount) {
            val s = frameState.partSize[i]
            if (s <= 0.01f) continue
            GLES20.glUniform3f(shader.uTintColor, frameState.partR[i], frameState.partG[i], frameState.partB[i])
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, frameState.partX[i], frameState.partY[i], frameState.partZ[i])
            Matrix.scaleM(modelMatrix, 0, s, s, s)
            drawBoundMesh(unitSphereMesh)
        }
        GLES20.glUniform3f(shader.uTintColor, 1f, 1f, 1f)
        GLES20.glUniform1f(shader.uEmissive, 0f)
    }

    private fun drawKittyLimb(
        px: Float, py: Float,
        pitchDeg: Float, rollDeg: Float,
        ox: Float, oy: Float, oz: Float,
        sx: Float, sy: Float, sz: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, px, py, 0f)
        if (rollDeg != 0f) Matrix.rotateM(modelMatrix, 0, rollDeg, 0f, 0f, 1f)
        if (pitchDeg != 0f) Matrix.rotateM(modelMatrix, 0, pitchDeg, 1f, 0f, 0f)
        Matrix.translateM(modelMatrix, 0, ox, oy, oz)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        drawBoundMesh(unitCubeMesh)
    }

    private fun drawBoundMesh(mesh: Mesh3D) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrix, 1, false, modelMatrix, 0)
        mesh.drawBound()
    }
}
