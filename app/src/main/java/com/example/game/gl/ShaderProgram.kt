package com.example.game.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ShaderProgram {
    val programId: Int
    val aPosition: Int
    val aNormal: Int
    val aColor: Int
    val uMvpMatrix: Int
    val uModelMatrix: Int
    val uTintColor: Int
    val uEmissive: Int
    val uLightDir: Int
    val uFogColor: Int
    val uFogStart: Int
    val uFogRange: Int

    init {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        programId = GLES20.glCreateProgram().also { prog ->
            GLES20.glAttachShader(prog, vs)
            GLES20.glAttachShader(prog, fs)
            GLES20.glLinkProgram(prog)
            val status = IntArray(1)
            GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES20.glGetProgramInfoLog(prog)
                GLES20.glDeleteProgram(prog)
                throw IllegalStateException("Shader link failed: $log")
            }
        }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)

        aPosition = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormal = GLES20.glGetAttribLocation(programId, "aNormal")
        aColor = GLES20.glGetAttribLocation(programId, "aColor")
        uMvpMatrix = GLES20.glGetUniformLocation(programId, "uMvpMatrix")
        uModelMatrix = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        uTintColor = GLES20.glGetUniformLocation(programId, "uTintColor")
        uEmissive = GLES20.glGetUniformLocation(programId, "uEmissive")
        uLightDir = GLES20.glGetUniformLocation(programId, "uLightDir")
        uFogColor = GLES20.glGetUniformLocation(programId, "uFogColor")
        uFogStart = GLES20.glGetUniformLocation(programId, "uFogStart")
        uFogRange = GLES20.glGetUniformLocation(programId, "uFogRange")
    }

    fun use() {
        GLES20.glUseProgram(programId)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw IllegalStateException("Shader compile failed: $log")
        }
        return shader
    }

    companion object {
        // Ultra-fast vertex-lit stylized cel shader + vertex fog (zero pow/exp per pixel!)
        private const val VERTEX_SHADER = """
            uniform mat4 uMvpMatrix;
            uniform mat4 uModelMatrix;
            uniform vec3 uTintColor;
            uniform float uEmissive;
            uniform vec3 uLightDir;
            uniform float uFogStart;
            uniform float uFogRange;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec3 aColor;
            varying vec3 vLitColor;
            varying float vFogFactor;
            void main() {
                vec3 n = normalize((uModelMatrix * vec4(aNormal, 0.0)).xyz);
                float ndotl = dot(n, uLightDir);
                float wrap = clamp(ndotl * 0.5 + 0.5, 0.0, 1.0);
                // Crisp stylized 2-band cartoon shading + sky bounce
                float band = mix(0.62, 1.08, smoothstep(0.35, 0.52, wrap));
                float rim = (1.0 - abs(n.z));
                rim = rim * rim * 0.10;
                vec3 baseRgb = aColor * uTintColor;
                vec3 shaded = baseRgb * band + vec3(rim * 0.7, rim * 0.9, rim);
                vLitColor = mix(shaded, baseRgb * 1.15, clamp(uEmissive, 0.0, 1.0));
                vec4 clipPos = uMvpMatrix * vec4(aPosition, 1.0);
                vFogFactor = clamp((clipPos.w - uFogStart) / uFogRange, 0.0, 0.88);
                gl_Position = clipPos;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec3 uFogColor;
            varying vec3 vLitColor;
            varying float vFogFactor;
            void main() {
                vec3 rgb = mix(vLitColor, uFogColor, vFogFactor);
                gl_FragColor = vec4(rgb, 1.0);
            }
        """
    }
}

class Mesh3D(vertices: FloatArray) {
    val vertexCount: Int = vertices.size / 9
    private val vboId: Int

    init {
        val buf: FloatBuffer = ByteBuffer
            .allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        buf.position(0)

        val ids = IntArray(1)
        GLES20.glGenBuffers(1, ids, 0)
        vboId = ids[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertices.size * 4,
            buf,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    fun bind(shader: ShaderProgram) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        val stride = 9 * 4
        GLES20.glEnableVertexAttribArray(shader.aPosition)
        GLES20.glVertexAttribPointer(shader.aPosition, 3, GLES20.GL_FLOAT, false, stride, 0)
        GLES20.glEnableVertexAttribArray(shader.aNormal)
        GLES20.glVertexAttribPointer(shader.aNormal, 3, GLES20.GL_FLOAT, false, stride, 3 * 4)
        GLES20.glEnableVertexAttribArray(shader.aColor)
        GLES20.glVertexAttribPointer(shader.aColor, 3, GLES20.GL_FLOAT, false, stride, 6 * 4)
    }

    fun drawBound() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
    }
}
