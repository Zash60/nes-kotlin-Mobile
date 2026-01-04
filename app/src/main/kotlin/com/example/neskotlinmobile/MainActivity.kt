package com.example.neskotlinmobile

import android.app.Activity
import android.content.Intent
import android.hardware.input.InputManager
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity() {

    companion object {
        const val RETRO_DEVICE_ID_JOYPAD_B = 0
        const val RETRO_DEVICE_ID_JOYPAD_Y = 1
        const val RETRO_DEVICE_ID_JOYPAD_SELECT = 2
        const val RETRO_DEVICE_ID_JOYPAD_START = 3
        const val RETRO_DEVICE_ID_JOYPAD_UP = 4
        const val RETRO_DEVICE_ID_JOYPAD_DOWN = 5
        const val RETRO_DEVICE_ID_JOYPAD_LEFT = 6
        const val RETRO_DEVICE_ID_JOYPAD_RIGHT = 7
        const val RETRO_DEVICE_ID_JOYPAD_A = 8
        const val RETRO_DEVICE_ID_JOYPAD_X = 9
        init {
            System.loadLibrary("quicknes")
        }

        private var frameData: ShortArray? = null
        private var frameWidth = 0
        private var frameHeight = 0

        @JvmStatic
        external fun init()

        @JvmStatic
        external fun loadGame(romData: ByteArray): Boolean

        @JvmStatic
        external fun runFrame()

        @JvmStatic
        external fun setInputState(port: Int, id: Int, pressed: Boolean)

        @JvmStatic
        external fun reset()

        @JvmStatic
        external fun unloadGame()

        @JvmStatic
        external fun deinit()

        @JvmStatic
        fun onVideoFrame(frameData: ShortArray, width: Int, height: Int) {
            this.frameData = frameData
            frameWidth = width
            frameHeight = height
        }

        @JvmStatic
        fun onAudioSample(left: Short, right: Short) {
            // Handle audio sample
        }
    }

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: GameRenderer
    private lateinit var inputManager: InputManager
    private var romLoaded = false

    private val pickRomLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                loadRomFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputManager = getSystemService(INPUT_SERVICE) as InputManager

        // Initialize QuickNES
        init()

        glSurfaceView = findViewById(R.id.gameView)
        renderer = GameRenderer()
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        // Set up buttons
        findViewById<Button>(R.id.buttonA).setOnTouchListener { _, event -> handleInput(event, RETRO_DEVICE_ID_JOYPAD_A) }
        findViewById<Button>(R.id.buttonB).setOnTouchListener { _, event -> handleInput(event, RETRO_DEVICE_ID_JOYPAD_B) }
        findViewById<Button>(R.id.buttonSelect).setOnTouchListener { _, event -> handleInput(event, RETRO_DEVICE_ID_JOYPAD_SELECT) }
        findViewById<Button>(R.id.buttonStart).setOnTouchListener { _, event -> handleInput(event, RETRO_DEVICE_ID_JOYPAD_START) }
        findViewById<Button>(R.id.buttonUp).setOnTouchListener { _, event -> handleInput(event, RETRO_DEVICE_ID_JOYPAD_UP) }
        findViewById<Button>(R.id.buttonDown).setOnTouchListener { _, event -> handleInput(event, RETRO_DEVICE_ID_JOYPAD_DOWN) }
        findViewById<Button>(R.id.buttonLeft).setOnTouchListener { _, event -> handleInput(event, RETRO_DEVICE_ID_JOYPAD_LEFT) }
        findViewById<Button>(R.id.buttonRight).setOnTouchListener { _, event -> handleInput(event, RETRO_DEVICE_ID_JOYPAD_RIGHT) }
        findViewById<Button>(R.id.loadRomButton).setOnClickListener { pickRom() }
    }

    private fun pickRom() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        pickRomLauncher.launch(intent)
    }

    private fun loadRomFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val romData = inputStream?.readBytes()
            if (romData != null && loadGame(romData)) {
                romLoaded = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleInput(event: MotionEvent, buttonId: Int): Boolean {
        val pressed = event.action != MotionEvent.ACTION_UP
        setInputState(0, buttonId, pressed)
        return true
    }

    private fun isGamepad(deviceId: Int): Boolean {
        val device = inputManager.getInputDevice(deviceId)
        return device != null && (device.sources and InputDevice.SOURCE_GAMEPAD) != 0
    }

    private fun mapKeyToId(keyCode: Int): Int? {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> RETRO_DEVICE_ID_JOYPAD_A
            KeyEvent.KEYCODE_BUTTON_B -> RETRO_DEVICE_ID_JOYPAD_B
            KeyEvent.KEYCODE_BUTTON_X -> RETRO_DEVICE_ID_JOYPAD_X
            KeyEvent.KEYCODE_BUTTON_Y -> RETRO_DEVICE_ID_JOYPAD_Y
            KeyEvent.KEYCODE_BUTTON_START -> RETRO_DEVICE_ID_JOYPAD_START
            KeyEvent.KEYCODE_BUTTON_SELECT -> RETRO_DEVICE_ID_JOYPAD_SELECT
            else -> null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unloadGame()
        deinit()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && isGamepad(event.deviceId)) {
            val id = mapKeyToId(keyCode)
            if (id != null) {
                setInputState(0, id, true)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && isGamepad(event.deviceId)) {
            val id = mapKeyToId(keyCode)
            if (id != null) {
                setInputState(0, id, false)
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event != null && isGamepad(event.deviceId)) {
            val x = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val y = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
            setInputState(0, RETRO_DEVICE_ID_JOYPAD_UP, y < -0.5f)
            setInputState(0, RETRO_DEVICE_ID_JOYPAD_DOWN, y > 0.5f)
            setInputState(0, RETRO_DEVICE_ID_JOYPAD_LEFT, x < -0.5f)
            setInputState(0, RETRO_DEVICE_ID_JOYPAD_RIGHT, x > 0.5f)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    inner class GameRenderer : GLSurfaceView.Renderer {
        private var textureId = 0
        private val vertexBuffer: FloatBuffer
        private val texCoordBuffer: FloatBuffer
        private val frameBuffer: java.nio.ShortBuffer = java.nio.ByteBuffer.allocateDirect(256 * 240 * 2).order(java.nio.ByteOrder.nativeOrder()).asShortBuffer()
        private val vertexShaderCode = """
            attribute vec4 vPosition;
            attribute vec2 vTexCoord;
            varying vec2 texCoord;
            void main() {
                gl_Position = vPosition;
                texCoord = vTexCoord;
            }
        """
        private val fragmentShaderCode = """
            precision mediump float;
            uniform sampler2D uTexture;
            varying vec2 texCoord;
            void main() {
                gl_FragColor = texture2D(uTexture, texCoord);
            }
        """
        private var program = 0

        init {
            val vertices = floatArrayOf(
                -1f, 1f, 0f,   // top left
                -1f, -1f, 0f,  // bottom left
                1f, 1f, 0f,    // top right
                1f, -1f, 0f    // bottom right
            )
            vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            vertexBuffer.put(vertices).position(0)
            val texCoords = floatArrayOf(
                0f, 0f,
                0f, 1f,
                1f, 0f,
                1f, 1f
            )
            texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            texCoordBuffer.put(texCoords).position(0)
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
            program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            if (romLoaded && frameData != null) {
                runFrame()
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
                frameBuffer.clear()
                frameBuffer.put(frameData)
                frameBuffer.position(0)
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, frameWidth, frameHeight, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, frameBuffer)
                GLES20.glUseProgram(program)
                val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
                val texCoordHandle = GLES20.glGetAttribLocation(program, "vTexCoord")
                GLES20.glEnableVertexAttribArray(positionHandle)
                GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
                GLES20.glEnableVertexAttribArray(texCoordHandle)
                GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
                GLES20.glDisableVertexAttribArray(positionHandle)
                GLES20.glDisableVertexAttribArray(texCoordHandle)
            }
        }

        private fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            return shader
        }
    }
}