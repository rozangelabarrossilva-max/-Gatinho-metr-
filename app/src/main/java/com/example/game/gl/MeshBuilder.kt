package com.example.game.gl

import com.example.game.GameEngine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MeshBatch {
    private val data = ArrayList<Float>(2048)

    fun addBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float
    ) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz
        // +Z
        addQuad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, 1f, r, g, b)
        // -Z
        addQuad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f, r, g, b)
        // +X
        addQuad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f, r, g, b)
        // -X
        addQuad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f, r, g, b)
        // +Y
        addQuad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f, r, g, b)
        // -Y
        addQuad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f, r, g, b)
    }

    fun addCylinder(
        cx: Float, cy: Float, cz: Float,
        radius: Float, height: Float,
        segments: Int,
        r: Float, g: Float, b: Float,
        axisX: Boolean = false,
        axisZ: Boolean = false
    ) {
        val halfH = height * 0.5f
        for (i in 0 until segments) {
            val a0 = (2.0 * PI * i / segments).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)

            if (axisZ) {
                // Disc/cylinder oriented along Z (great for upright coin!)
                val x0 = cx + radius * c0; val y0 = cy + radius * s0
                val x1 = cx + radius * c1; val y1 = cy + radius * s1
                addVertex(x0, y0, cz - halfH, c0, s0, 0f, r, g, b)
                addVertex(x1, y1, cz - halfH, c1, s1, 0f, r, g, b)
                addVertex(x1, y1, cz + halfH, c1, s1, 0f, r, g, b)
                addVertex(x0, y0, cz - halfH, c0, s0, 0f, r, g, b)
                addVertex(x1, y1, cz + halfH, c1, s1, 0f, r, g, b)
                addVertex(x0, y0, cz + halfH, c0, s0, 0f, r, g, b)

                addVertex(cx, cy, cz + halfH, 0f, 0f, 1f, r, g, b)
                addVertex(x0, y0, cz + halfH, 0f, 0f, 1f, r, g, b)
                addVertex(x1, y1, cz + halfH, 0f, 0f, 1f, r, g, b)

                addVertex(cx, cy, cz - halfH, 0f, 0f, -1f, r, g, b)
                addVertex(x1, y1, cz - halfH, 0f, 0f, -1f, r, g, b)
                addVertex(x0, y0, cz - halfH, 0f, 0f, -1f, r, g, b)
            } else if (axisX) {
                val y0 = cy + radius * c0; val z0 = cz + radius * s0
                val y1 = cy + radius * c1; val z1 = cz + radius * s1
                addVertex(cx - halfH, y0, z0, 0f, c0, s0, r, g, b)
                addVertex(cx - halfH, y1, z1, 0f, c1, s1, r, g, b)
                addVertex(cx + halfH, y1, z1, 0f, c1, s1, r, g, b)
                addVertex(cx - halfH, y0, z0, 0f, c0, s0, r, g, b)
                addVertex(cx + halfH, y1, z1, 0f, c1, s1, r, g, b)
                addVertex(cx + halfH, y0, z0, 0f, c0, s0, r, g, b)

                addVertex(cx + halfH, cy, cz, 1f, 0f, 0f, r, g, b)
                addVertex(cx + halfH, y0, z0, 1f, 0f, 0f, r, g, b)
                addVertex(cx + halfH, y1, z1, 1f, 0f, 0f, r, g, b)

                addVertex(cx - halfH, cy, cz, -1f, 0f, 0f, r, g, b)
                addVertex(cx - halfH, y1, z1, -1f, 0f, 0f, r, g, b)
                addVertex(cx - halfH, y0, z0, -1f, 0f, 0f, r, g, b)
            } else {
                val x0 = cx + radius * c0; val z0 = cz + radius * s0
                val x1 = cx + radius * c1; val z1 = cz + radius * s1
                addVertex(x0, cy - halfH, z0, c0, 0f, s0, r, g, b)
                addVertex(x1, cy - halfH, z1, c1, 0f, s1, r, g, b)
                addVertex(x1, cy + halfH, z1, c1, 0f, s1, r, g, b)
                addVertex(x0, cy - halfH, z0, c0, 0f, s0, r, g, b)
                addVertex(x1, cy + halfH, z1, c1, 0f, s1, r, g, b)
                addVertex(x0, cy + halfH, z0, c0, 0f, s0, r, g, b)

                addVertex(cx, cy + halfH, cz, 0f, 1f, 0f, r, g, b)
                addVertex(x0, cy + halfH, z0, 0f, 1f, 0f, r, g, b)
                addVertex(x1, cy + halfH, z1, 0f, 1f, 0f, r, g, b)

                addVertex(cx, cy - halfH, cz, 0f, -1f, 0f, r, g, b)
                addVertex(x1, cy - halfH, z1, 0f, -1f, 0f, r, g, b)
                addVertex(x0, cy - halfH, z0, 0f, -1f, 0f, r, g, b)
            }
        }
    }

    fun addCone(
        cx: Float, cy: Float, cz: Float,
        rx: Float, height: Float, rz: Float,
        segments: Int,
        tiltZRad: Float,
        r: Float, g: Float, b: Float
    ) {
        val halfH = height * 0.5f
        val ct = cos(tiltZRad); val st = sin(tiltZRad)
        for (i in 0 until segments) {
            val a0 = (2.0 * PI * i / segments).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val lx0 = rx * cos(a0); val lz0 = rz * sin(a0)
            val lx1 = rx * cos(a1); val lz1 = rz * sin(a1)
            val ly = -halfH

            val p0x = cx + (lx0 * ct - ly * st); val p0y = cy + (lx0 * st + ly * ct)
            val p1x = cx + (lx1 * ct - ly * st); val p1y = cy + (lx1 * st + ly * ct)
            val apx = cx + (-halfH * -st); val apy = cy + (halfH * ct)

            val nx = cos((a0 + a1) * 0.5f)
            val nz = sin((a0 + a1) * 0.5f)
            addVertex(p0x, p0y, cz + lz0, nx, 0.5f, nz, r, g, b)
            addVertex(p1x, p1y, cz + lz1, nx, 0.5f, nz, r, g, b)
            addVertex(apx, apy, cz, nx, 0.6f, nz, r, g, b)
        }
    }

    fun addEllipsoid(
        cx: Float, cy: Float, cz: Float,
        rx: Float, ry: Float, rz: Float,
        stacks: Int, slices: Int,
        r: Float, g: Float, b: Float
    ) {
        for (i in 0 until stacks) {
            val phi0 = (PI * i / stacks).toFloat()
            val phi1 = (PI * (i + 1) / stacks).toFloat()
            for (j in 0 until slices) {
                val th0 = (2.0 * PI * j / slices).toFloat()
                val th1 = (2.0 * PI * (j + 1) / slices).toFloat()

                val p00 = ellipPt(cx, cy, cz, rx, ry, rz, phi0, th0)
                val p10 = ellipPt(cx, cy, cz, rx, ry, rz, phi1, th0)
                val p11 = ellipPt(cx, cy, cz, rx, ry, rz, phi1, th1)
                val p01 = ellipPt(cx, cy, cz, rx, ry, rz, phi0, th1)

                addVertPt(p00, r, g, b); addVertPt(p10, r, g, b); addVertPt(p11, r, g, b)
                addVertPt(p00, r, g, b); addVertPt(p11, r, g, b); addVertPt(p01, r, g, b)
            }
        }
    }

    private fun ellipPt(
        cx: Float, cy: Float, cz: Float,
        rx: Float, ry: Float, rz: Float,
        phi: Float, theta: Float
    ): FloatArray {
        val nx = sin(phi) * cos(theta)
        val ny = cos(phi)
        val nz = sin(phi) * sin(theta)
        return floatArrayOf(cx + rx * nx, cy + ry * ny, cz + rz * nz, nx, ny, nz)
    }

    private fun addVertPt(p: FloatArray, r: Float, g: Float, b: Float) {
        addVertex(p[0], p[1], p[2], p[3], p[4], p[5], r, g, b)
    }

    private fun addQuad(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float
    ) {
        addVertex(x0, y0, z0, nx, ny, nz, r, g, b)
        addVertex(x1, y1, z1, nx, ny, nz, r, g, b)
        addVertex(x2, y2, z2, nx, ny, nz, r, g, b)

        addVertex(x0, y0, z0, nx, ny, nz, r, g, b)
        addVertex(x2, y2, z2, nx, ny, nz, r, g, b)
        addVertex(x3, y3, z3, nx, ny, nz, r, g, b)
    }

    fun addVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float
    ) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(r); data.add(g); data.add(b)
    }

    fun build(): Mesh3D = Mesh3D(data.toFloatArray())
}

object MeshBuilder {

    fun createUnitCube(r: Float = 1f, g: Float = 1f, b: Float = 1f): Mesh3D {
        return MeshBatch().apply { addBox(0f, 0f, 0f, 1f, 1f, 1f, r, g, b) }.build()
    }

    fun createUnitSphere(r: Float = 1f, g: Float = 1f, b: Float = 1f): Mesh3D {
        return MeshBatch().apply { addEllipsoid(0f, 0f, 0f, 0.5f, 0.5f, 0.5f, 7, 10, r, g, b) }.build()
    }

    fun createUnitCone(r: Float = 1f, g: Float = 1f, b: Float = 1f): Mesh3D {
        return MeshBatch().apply { addCone(0f, 0f, 0f, 0.5f, 1f, 0.5f, 10, 0f, r, g, b) }.build()
    }

    /**
     * Entire 20-unit long Subway Surfers open-air metro yard segment baked into ONE mesh:
     * warm sunlit track ballast, 3 pairs of shiny steel rails, sleepers, concrete platforms,
     * yellow tactile safety stripes, colorful graffiti/brick side walls, and steel overhead gantries!
     */
    fun createSubwayTrackSegmentMesh(): Mesh3D {
        val m = MeshBatch()
        val segLen = 20f
        // Warm sandy gravel ballast bed
        m.addBox(0f, -0.26f, 0f, 10.6f, 0.30f, segLen, 0.42f, 0.39f, 0.37f)
        // Subtle lighter lane strips between tracks
        m.addBox(-1.2f, -0.10f, 0f, 0.25f, 0.04f, segLen, 0.34f, 0.32f, 0.31f)
        m.addBox(1.2f, -0.10f, 0f, 0.25f, 0.04f, segLen, 0.34f, 0.32f, 0.31f)

        // 3 pairs of shiny steel rails
        for (lane in 0..2) {
            val lx = GameEngine.laneToX(lane)
            m.addBox(lx - 0.54f, -0.02f, 0f, 0.11f, 0.12f, segLen, 0.78f, 0.84f, 0.92f)
            m.addBox(lx + 0.54f, -0.02f, 0f, 0.11f, 0.12f, segLen, 0.78f, 0.84f, 0.92f)
        }

        // Sleepers (dormentes) baked into this single mesh!
        var sz = -9f
        while (sz <= 9f) {
            for (lane in 0..2) {
                val lx = GameEngine.laneToX(lane)
                m.addBox(lx, -0.08f, sz, 1.56f, 0.08f, 0.34f, 0.36f, 0.23f, 0.16f)
            }
            sz += 2.0f
        }

        // Left & right bright station platforms + yellow safety edges + graffiti metro walls
        for (side in listOf(-1f, 1f)) {
            m.addBox(side * 6.9f, 0.32f, 0f, 3.8f, 1.24f, segLen, 0.76f, 0.78f, 0.82f)
            // Iconic yellow platform edge line
            m.addBox(side * 5.08f, 0.95f, 0f, 0.24f, 0.08f, segLen, 1.0f, 0.82f, 0.12f)
            // Terracotta brick metro boundary walls
            m.addBox(side * 8.9f, 2.35f, 0f, 0.65f, 4.4f, segLen, 0.76f, 0.34f, 0.26f)
            // White wall cap trim
            m.addBox(side * 8.9f, 4.62f, 0f, 0.78f, 0.22f, segLen, 0.94f, 0.95f, 0.98f)
            // Stylized colorful graffiti/mural panels along walls
            val muralR = if (side < 0f) 0.12f else 0.98f
            val muralG = if (side < 0f) 0.82f else 0.42f
            val muralB = if (side < 0f) 0.96f else 0.22f
            m.addBox(side * 8.54f, 2.5f, -3.5f, 0.08f, 1.6f, 6.2f, muralR, muralG, muralB)
            m.addBox(side * 8.54f, 2.5f, 4.5f, 0.08f, 1.6f, 5.4f, 0.32f, 0.90f, 0.54f)

            // Station light poles / support columns every 20m
            m.addBox(side * 5.75f, 3.8f, 0f, 0.34f, 7.2f, 0.34f, 0.24f, 0.32f, 0.52f)
            // Colorful station roof canopy overhang
            m.addBox(side * 6.8f, 6.9f, 0f, 3.2f, 0.28f, segLen, 0.16f, 0.58f, 0.86f)
        }

        // Overhead steel subway signal gantry every 20m
        m.addBox(0f, 7.6f, 0f, 11.6f, 0.38f, 0.42f, 0.22f, 0.28f, 0.44f)
        // Green/Cyan lane signal lights on gantry
        m.addBox(-2.4f, 7.25f, 0.25f, 0.7f, 0.36f, 0.14f, 0.18f, 0.95f, 0.55f)
        m.addBox(0f, 7.25f, 0.25f, 0.7f, 0.36f, 0.14f, 0.18f, 0.95f, 0.55f)
        m.addBox(2.4f, 7.25f, 0.25f, 0.7f, 0.36f, 0.14f, 0.18f, 0.95f, 0.55f)

        return m.build()
    }

    /**
     * Full Subway Surfers style colorful 3D subway car baked into ONE mesh per color scheme!
     */
    fun createTrainMesh(colorIndex: Int, length: Float): Mesh3D {
        val m = MeshBatch()
        val (bodyR, bodyG, bodyB) = when (colorIndex) {
            0 -> Triple(0.08f, 0.68f, 0.92f) // Iconic Subway Cyan-Blue
            1 -> Triple(0.95f, 0.28f, 0.24f) // Bright Express Red
            2 -> Triple(0.20f, 0.80f, 0.46f) // Vibrant Mint-Green
            else -> Triple(0.98f, 0.72f, 0.12f) // Golden Metro Yellow
        }
        val (stripeR, stripeG, stripeB) = when (colorIndex) {
            1 -> Triple(1.0f, 0.88f, 0.22f)
            else -> Triple(0.96f, 0.98f, 1.0f)
        }

        // Dark wheel bogies
        m.addBox(0f, 0.32f, 0f, 1.72f, 0.52f, length - 0.35f, 0.16f, 0.18f, 0.24f)
        // Main vibrant train car shell
        m.addBox(0f, 1.30f, 0f, 1.92f, 1.56f, length, bodyR, bodyG, bodyB)
        // Crisp horizontal racing stripe
        m.addBox(0f, 1.08f, 0f, 1.96f, 0.22f, length + 0.02f, stripeR, stripeG, stripeB)
        // Rounded white/silver roof deck + twin AC vents (great visual cue for roof running!)
        m.addBox(0f, 2.16f, 0f, 1.72f, 0.20f, length - 0.3f, 0.90f, 0.93f, 0.97f)
        m.addBox(0f, 2.33f, -length * 0.22f, 1.05f, 0.18f, 2.0f, 0.68f, 0.73f, 0.82f)
        m.addBox(0f, 2.33f, length * 0.22f, 1.05f, 0.18f, 2.0f, 0.68f, 0.73f, 0.82f)

        // Front & back cab windshields + glowing headlights + LED destination sign
        val frontZ = length * 0.5f
        m.addBox(0f, 1.56f, frontZ + 0.02f, 1.56f, 0.62f, 0.06f, 0.10f, 0.15f, 0.26f)
        m.addBox(0f, 1.98f, frontZ + 0.03f, 0.96f, 0.18f, 0.06f, 0.15f, 0.95f, 0.62f)
        m.addBox(-0.60f, 0.82f, frontZ + 0.03f, 0.32f, 0.22f, 0.06f, 1.0f, 0.95f, 0.45f)
        m.addBox(0.60f, 0.82f, frontZ + 0.03f, 0.32f, 0.22f, 0.06f, 1.0f, 0.95f, 0.45f)

        // Side passenger windows baked in!
        var wz = -length * 0.35f
        while (wz <= length * 0.35f) {
            m.addBox(0f, 1.55f, wz, 1.96f, 0.46f, 1.30f, 0.12f, 0.19f, 0.32f)
            wz += 2.3f
        }

        return m.build()
    }

    fun createRampMesh(): Mesh3D {
        val m = MeshBatch()
        val ny = 1f / sqrt(2f)
        val nz = 1f / sqrt(2f)
        // Bright yellow/orange striped ramp sloping from +Z (y=0) to -Z (y=1)
        m.addVertex(-0.5f, -0.5f, 0.5f, 0f, ny, nz, 1.0f, 0.78f, 0.12f)
        m.addVertex(0.5f, -0.5f, 0.5f, 0f, ny, nz, 1.0f, 0.78f, 0.12f)
        m.addVertex(0.5f, 0.5f, -0.5f, 0f, ny, nz, 1.0f, 0.55f, 0.08f)

        m.addVertex(-0.5f, -0.5f, 0.5f, 0f, ny, nz, 1.0f, 0.78f, 0.12f)
        m.addVertex(0.5f, 0.5f, -0.5f, 0f, ny, nz, 1.0f, 0.55f, 0.08f)
        m.addVertex(-0.5f, 0.5f, -0.5f, 0f, ny, nz, 1.0f, 0.55f, 0.08f)

        // Side walls
        m.addVertex(-0.5f, -0.5f, 0.5f, -1f, 0f, 0f, 0.25f, 0.25f, 0.30f)
        m.addVertex(-0.5f, 0.5f, -0.5f, -1f, 0f, 0f, 0.25f, 0.25f, 0.30f)
        m.addVertex(-0.5f, -0.5f, -0.5f, -1f, 0f, 0f, 0.25f, 0.25f, 0.30f)

        m.addVertex(0.5f, -0.5f, -0.5f, 1f, 0f, 0f, 0.25f, 0.25f, 0.30f)
        m.addVertex(0.5f, 0.5f, -0.5f, 1f, 0f, 0f, 0.25f, 0.25f, 0.30f)
        m.addVertex(0.5f, -0.5f, 0.5f, 1f, 0f, 0f, 0.25f, 0.25f, 0.30f)
        return m.build()
    }

    /**
     * Iconic Subway Surfers red & white striped jump barricade
     */
    fun createLowBarricadeMesh(): Mesh3D {
        val m = MeshBatch()
        // White support legs
        m.addBox(-0.74f, 0.48f, 0f, 0.16f, 0.96f, 0.22f, 0.95f, 0.96f, 0.98f)
        m.addBox(0.74f, 0.48f, 0f, 0.16f, 0.96f, 0.22f, 0.95f, 0.96f, 0.98f)
        // Red & white striped crossbar
        m.addBox(-0.60f, 0.80f, 0f, 0.44f, 0.36f, 0.26f, 0.98f, 0.22f, 0.24f)
        m.addBox(-0.20f, 0.80f, 0f, 0.36f, 0.36f, 0.27f, 0.98f, 0.98f, 0.98f)
        m.addBox(0.20f, 0.80f, 0f, 0.36f, 0.36f, 0.26f, 0.98f, 0.22f, 0.24f)
        m.addBox(0.60f, 0.80f, 0f, 0.44f, 0.36f, 0.27f, 0.98f, 0.98f, 0.98f)
        // Top orange warning beacons
        m.addBox(-0.74f, 1.03f, 0f, 0.18f, 0.14f, 0.18f, 1.0f, 0.68f, 0.08f)
        m.addBox(0.74f, 1.03f, 0f, 0.18f, 0.14f, 0.18f, 1.0f, 0.68f, 0.08f)
        return m.build()
    }

    /**
     * Iconic Subway Surfers tall overhead slide sign
     */
    fun createHighGateMesh(): Mesh3D {
        val m = MeshBatch()
        m.addBox(-0.88f, 1.35f, 0f, 0.16f, 2.7f, 0.16f, 0.86f, 0.90f, 0.96f)
        m.addBox(0.88f, 1.35f, 0f, 0.16f, 2.7f, 0.16f, 0.86f, 0.90f, 0.96f)
        // Big colorful overhead metro board
        m.addBox(0f, 1.96f, 0f, 1.94f, 1.15f, 0.26f, 0.10f, 0.65f, 0.96f)
        // High-contrast yellow bottom warning stripe (clear slide cue!)
        m.addBox(0f, 1.30f, 0f, 1.96f, 0.22f, 0.28f, 1.0f, 0.84f, 0.12f)
        // Downward arrow cue
        m.addBox(0f, 2.02f, 0.15f, 0.24f, 0.48f, 0.06f, 1.0f, 0.96f, 0.4f)
        m.addBox(0f, 1.72f, 0.15f, 0.56f, 0.18f, 0.06f, 1.0f, 0.96f, 0.4f)
        return m.build()
    }

    /**
     * Bright golden faceted Subway Surfers coin with inner raised rim
     */
    fun createCoinMesh(): Mesh3D {
        val m = MeshBatch()
        m.addCylinder(0f, 0f, 0f, 0.38f, 0.12f, 12, 1.0f, 0.82f, 0.12f, axisZ = true)
        m.addCylinder(0f, 0f, 0f, 0.24f, 0.15f, 10, 1.0f, 0.93f, 0.42f, axisZ = true)
        return m.build()
    }

    /**
     * Single-draw-call U-shaped Horseshoe Magnet pickup
     */
    fun createMagnetMesh(): Mesh3D {
        val m = MeshBatch()
        m.addBox(-0.34f, 0.12f, 0f, 0.24f, 0.68f, 0.24f, 0.98f, 0.18f, 0.34f)
        m.addBox(0.34f, 0.12f, 0f, 0.24f, 0.68f, 0.24f, 0.98f, 0.18f, 0.34f)
        m.addBox(0f, -0.26f, 0f, 0.92f, 0.24f, 0.24f, 0.98f, 0.18f, 0.34f)
        // Bright silver poles
        m.addBox(-0.34f, 0.50f, 0f, 0.25f, 0.22f, 0.25f, 0.92f, 0.96f, 1.0f)
        m.addBox(0.34f, 0.50f, 0f, 0.25f, 0.22f, 0.25f, 0.92f, 0.96f, 1.0f)
        return m.build()
    }

    /**
     * Single-draw-call twin-thruster Jetpack pickup
     */
    fun createJetpackMesh(): Mesh3D {
        val m = MeshBatch()
        m.addCylinder(-0.22f, 0.08f, 0f, 0.17f, 0.82f, 10, 1.0f, 0.44f, 0.08f)
        m.addCylinder(0.22f, 0.08f, 0f, 0.17f, 0.82f, 10, 1.0f, 0.44f, 0.08f)
        m.addBox(0f, 0.12f, 0.05f, 0.58f, 0.46f, 0.22f, 0.12f, 0.86f, 1.0f)
        // Thruster nozzles
        m.addCylinder(-0.22f, -0.38f, 0f, 0.13f, 0.18f, 8, 0.25f, 0.28f, 0.38f)
        m.addCylinder(0.22f, -0.38f, 0f, 0.13f, 0.18f, 8, 0.25f, 0.28f, 0.38f)
        return m.build()
    }

    /**
     * Adorable stylized 3D Kitty Head baked per skin color into ONE mesh!
     */
    fun createKittyHeadMesh(
        furR: Float, furG: Float, furB: Float,
        bellyR: Float, bellyG: Float, bellyB: Float,
        accR: Float, accG: Float, accB: Float
    ): Mesh3D {
        val m = MeshBatch()
        // Soft rounded head
        m.addEllipsoid(0f, 0f, 0f, 0.35f, 0.31f, 0.32f, 8, 12, furR, furG, furB)
        // Fluffy cheek tufts
        m.addEllipsoid(-0.26f, -0.06f, -0.06f, 0.14f, 0.11f, 0.12f, 5, 8, furR, furG, furB)
        m.addEllipsoid(0.26f, -0.06f, -0.06f, 0.14f, 0.11f, 0.12f, 5, 8, furR, furG, furB)
        // Muzzle & cute pink nose (both subtle back/front visibility)
        m.addEllipsoid(0f, -0.08f, -0.25f, 0.16f, 0.10f, 0.11f, 5, 8, bellyR, bellyG, bellyB)
        m.addEllipsoid(0f, -0.03f, -0.34f, 0.06f, 0.045f, 0.045f, 4, 6, 1.0f, 0.42f, 0.58f)
        // Expressive eyes
        m.addEllipsoid(-0.14f, 0.05f, -0.27f, 0.07f, 0.085f, 0.04f, 5, 8, 0.07f, 0.10f, 0.18f)
        m.addEllipsoid(0.14f, 0.05f, -0.27f, 0.07f, 0.085f, 0.04f, 5, 8, 0.07f, 0.10f, 0.18f)
        // Pointed cat ears + pink inner ears
        m.addCone(-0.21f, 0.33f, 0f, 0.13f, 0.30f, 0.11f, 8, 0.28f, furR, furG, furB)
        m.addCone(0.21f, 0.33f, 0f, 0.13f, 0.30f, 0.11f, 8, -0.28f, furR, furG, furB)
        m.addCone(-0.21f, 0.31f, -0.04f, 0.07f, 0.18f, 0.06f, 6, 0.28f, 1.0f, 0.52f, 0.70f)
        m.addCone(0.21f, 0.31f, -0.04f, 0.07f, 0.18f, 0.06f, 6, -0.28f, 1.0f, 0.52f, 0.70f)
        // Cool Subway Headphones!
        m.addCylinder(-0.35f, 0.02f, 0f, 0.14f, 0.10f, 10, accR, accG, accB, axisX = true)
        m.addCylinder(0.35f, 0.02f, 0f, 0.14f, 0.10f, 10, accR, accG, accB, axisX = true)
        m.addBox(0f, 0.32f, 0f, 0.66f, 0.08f, 0.10f, accR, accG, accB)
        return m.build()
    }

    /**
     * Baked stylized Kitty Torso + Hoodie Collar/Belly Patch
     */
    fun createKittyTorsoMesh(
        furR: Float, furG: Float, furB: Float,
        bellyR: Float, bellyG: Float, bellyB: Float,
        accR: Float, accG: Float, accB: Float
    ): Mesh3D {
        val m = MeshBatch()
        m.addBox(0f, 0.04f, 0f, 0.54f, 0.62f, 0.38f, furR, furG, furB)
        m.addBox(0f, 0.02f, -0.18f, 0.34f, 0.44f, 0.06f, bellyR, bellyG, bellyB)
        // Sporty neck bandana/collar matching skin accent!
        m.addBox(0f, 0.31f, 0f, 0.58f, 0.11f, 0.42f, accR, accG, accB)
        return m.build()
    }
}
