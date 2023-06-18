package com.tcs.stellarsurfers

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.*
import android.speech.tts.TextToSpeech
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.tcs.stellarsurfers.SetupConnectionActivity.Companion.socket
import com.tcs.stellarsurfers.databinding.ActivityGameBinding
import com.tcs.stellarsurfers.motion_sensors.GyroListener
import com.tcs.stellarsurfers.utils.StatsCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometerSensor: Sensor
    private lateinit var geomagneticSensor : Sensor
    private lateinit var vibratorManager: VibratorManager
    private lateinit var vibrator: Vibrator
    private var accelerationValue: Float = 0.0f
    private val socket = SetupConnectionActivity.socket
    private val gyroListener = GyroListener()
    private val statsCollector = StatsCollector()

    /* for outcoming messages */
    val messageSize = 20
    private val MSG_INFO = 10
    private val MSG_SHOOT = 11
    private val MSG_DEAD = 12

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var collisionSound: MediaPlayer
    private lateinit var successSound: MediaPlayer
    private lateinit var gameOverSound: MediaPlayer
    private lateinit var blinking: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_game)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (Build.VERSION.SDK_INT >= 31) {
            vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.defaultVibrator
        } else {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        geomagneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        binding.shoot.setOnClickListener {
            shoot(0.0f, 0.0f)
        }

        binding.acceleration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                accelerationValue = (progress.toFloat() - 20.0f) / 10.0f
                statsCollector.accel = accelerationValue
                updateMonitor()
            }

            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        // val s = "Engine damage\n" + "*".repeat(damage) + ".".repeat(30 - damage)
        binding.damage.text = statsCollector.getDamageStr()

        binding.readyBtn.setOnClickListener {
            binding.getReady.isVisible = false
            binding.controls.isVisible = true
            statsCollector.init()
            gyroListener.startMeasuring()
            gyroListener.setOnSensorDataReceived {
                sendMessage(MSG_INFO, it[0], it[1], it[2], accelerationValue)
            }
            Log.d("Bluetooth", Locale.getAvailableLocales().toString())
            textToSpeech = TextToSpeech(this) {
                if (it == TextToSpeech.SUCCESS) {
                    val result = textToSpeech.setLanguage(Locale.UK)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "The Language not supported!")
                    }
                }
            }
        }

        // initialize sound effects
        collisionSound = MediaPlayer.create(applicationContext, R.raw.collision1)
        successSound = MediaPlayer.create(applicationContext, R.raw.success2)
        gameOverSound = MediaPlayer.create(applicationContext, R.raw.tinnitus2)

        MainScope().launch {
            val messageLength = 24
            val message = ByteArray(messageLength)
            while(true) {
                withContext(Dispatchers.IO) {
                    val len = SetupConnectionActivity.socket.inputStream.read(message, 0, messageLength)
                    // Log.i("bluetooth", "$len")
                    if (len == messageLength) {
                        val buffer = ByteBuffer.wrap(message).order(ByteOrder.LITTLE_ENDIAN)
                        val newX = buffer.float
                        val newY = buffer.float
                        val newZ = buffer.float
                        val newSpeed = buffer.float
                        val collisionInfo= buffer.int
                        val hash = buffer.float

                        if (collisionInfo > 0)
                            Log.d("Bluetooth", "$collisionInfo, $newX, $newY, $newZ, $newSpeed")
                        if(hash == newX + newY + newZ + newSpeed + collisionInfo) {
                            // Log.d("Bluetooth", "Correct hash")
                            statsCollector.x = newX
                            statsCollector.y = newY
                            statsCollector.z = newZ
                            statsCollector.update(newSpeed, collisionInfo)
                            if (statsCollector.shouldNotifyCollision())
                                notifyCollision()
                            if (statsCollector.shouldNotifyCollisionAheadPlanet())
                                notifyCollisionAheadPlanet()
                            if (statsCollector.shouldNotifyCollisionAheadAsteroid())
                                notifyCollisionAheadAsteroid()
                            if (statsCollector.shouldNotifyAsteroidShot())
                                notifyAsteroidShot()
                            updateMonitor()
                        }
                    }
                }
            }
        }
    }

    private suspend fun gameOver() {
        sendMessage(MSG_DEAD, 0.0f, 0.0f, 0.0f, 0.0f)
        withContext(Dispatchers.Main) {
            gameOverSound.start()
        }
        this@GameActivity.runOnUiThread {
            binding.controls.isVisible = false
            binding.gameOver.isVisible = true
            val colors = arrayOf(ColorDrawable(Color.BLACK), ColorDrawable(Color.RED))
            val mTransition = TransitionDrawable(colors)
            binding.gameOver.background = mTransition
            mTransition.startTransition(5000)
            val anim = ObjectAnimator.ofInt(binding.gameOver, "scrollY", 0, binding.gameOver.getBottom()).setDuration(2000)
            Handler(Looper.getMainLooper()).postDelayed({
                anim.start()
            }, 5000)
            binding.gameOverText.isVisible = true
            val gameOverAnimation = AlphaAnimation(0.0f, 1.0f)
            gameOverAnimation.duration = 4000
            binding.gameOverText.startAnimation(gameOverAnimation)
            binding.stats.text = statsCollector.getFinalStatistics()
        }

        vibrate(1000)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
        }, 15000)
    }

    private suspend fun notifyCollision() {
        // play collision sound
        withContext(Dispatchers.Default) {
            collisionSound.start()
        }
        vibrate(500)
        binding.damage.text = statsCollector.getDamageStr()
        if (statsCollector.damage >= 20) {  // start blinking
            textToSpeech.speak("Engine condition: critical", TextToSpeech.QUEUE_FLUSH, null, "none at all")
            binding.controlCollision.colorFilter =
                PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP)

            val blinking = AlphaAnimation(0.0f, 1.0f)
            blinking.duration = 500
            blinking.startOffset = 20
            blinking.repeatMode = Animation.REVERSE
            blinking.repeatCount = Animation.INFINITE
            binding.controlCollision.startAnimation(blinking)
        }
        if (statsCollector.damage >= 30)
            gameOver()
    }

    private fun notifyCollisionAheadPlanet() {
        Log.e("GameActivity", "collision ahead!")
        textToSpeech.speak("Warning, planet ahead", TextToSpeech.QUEUE_FLUSH, null, "none at all")
        val blinking = AlphaAnimation(0.0f, 1.0f)
        blinking.duration = 500
        blinking.startOffset = 20
        blinking.repeatMode = Animation.REVERSE
        blinking.repeatCount = 1
        binding.controlTba.startAnimation(blinking)
    }

    private fun notifyCollisionAheadAsteroid() {
        Log.e("GameActivity", "collision ahead (asteroid)!")
        textToSpeech.speak("Warning, asteroid ahead", TextToSpeech.QUEUE_FLUSH, null, "none at all")
        val blink = blinking
        blink.repeatCount = 1
        binding.controlTba.startAnimation(blink)
    }

    private suspend fun notifyAsteroidShot() {
        Log.e("GameActivity", "Shot asteroid")
        withContext(Dispatchers.Default) {
            successSound.start()
        }
        textToSpeech.speak("Target shot", TextToSpeech.QUEUE_FLUSH, null, "none at all")
    }

    private fun updateMonitor() {
        this@GameActivity.runOnUiThread {
            // statistics = "Speed: $speed\nX: $x\nY: $y\nZ: $z"
            val statistics = statsCollector.getStatsString()
            val l: Int = statistics.length
            val s = SpannableString(statistics + "\n" + statsCollector.getCollisionLog())
            s.setSpan(RelativeSizeSpan(0.6f), l + 1, s.length, 0)
            binding.monitor.text = s
            // speed control light color
            val floatArr = FloatArray(3)
            if (accelerationValue < 0.0f) {
                floatArr[0] = 240.0f
                floatArr[1] = -accelerationValue / 1.5f
            } else {
                floatArr[0] = 0.0f
                floatArr[1] = accelerationValue / 8.5f
            }
            floatArr[2] = 1.0f
            val accelerationColor = Color.HSVToColor(floatArr)
            binding.controlSpeed.colorFilter =
                PorterDuffColorFilter(accelerationColor, PorterDuff.Mode.SRC_ATOP)
        }
    }

    private fun shoot(x: Float, y: Float) {
        sendMessage(MSG_SHOOT, x, y, 0.0f, 0.0f)
    }

    private fun vibrate(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(duration)
        }
    }

    private fun sendMessage(info_type: Int, x: Float, y: Float, z: Float, accel: Float) {
        val buffer = ByteBuffer.allocate(messageSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(info_type)
        buffer.putFloat(x).putFloat(y).putFloat(z).putFloat(accel)
        val bytes = buffer.array()
        try {
            socket.outputStream.write(bytes)
        } catch (ignored: IOException) {
            ignored.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(gyroListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(gyroListener, geomagneticSensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(gyroListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (SetupConnectionActivity.socket.isConnected)
            SetupConnectionActivity.socket.close()
    }
}
