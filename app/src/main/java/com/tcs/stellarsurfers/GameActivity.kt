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
import com.tcs.stellarsurfers.databinding.ActivityGameBinding
import com.tcs.stellarsurfers.motion_sensors.GyroListener
import com.tcs.stellarsurfers.utils.StatsCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.Integer.min
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToInt


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

    private var x: Float = 0.0f
    private var y: Float = 0.0f
    private var z: Float = 0.0f
    private var speed: Float = 0.0f
    private var accel: Float = 0.0f
    private var colliding: Int = 0
    private var damage: Int = 0
    private var statistics: String = "speed\nX: $x\nY: $y\nZ: $z"
    private var collisionLog: String = "\n"

    private lateinit var collisionSound: MediaPlayer
    private lateinit var successSound: MediaPlayer
    private lateinit var gameOverSound: MediaPlayer

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

        binding.acceleration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                accelerationValue = (progress.toFloat() - 20.0f) / 10.0f
                accel = accelerationValue
                updateMonitor()
            }

            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        val s = "Engine damage\n" + "*".repeat(damage) + ".".repeat(30 - damage)
        binding.damage.text = s

        binding.readyBtn.setOnClickListener {
            binding.getReady.isVisible = false
            binding.controls.isVisible = true
            statsCollector.init()
            gyroListener.startMeasuring()
            gyroListener.setOnSensorDataReceived {
                val messageSize = 12 + 4
                val buffer = ByteBuffer.allocate(messageSize).order(ByteOrder.LITTLE_ENDIAN)
                buffer.putFloat(it[0]).putFloat(it[1]).putFloat(it[2])
                buffer.putFloat(accelerationValue)
                val bytes = buffer.array()
                sendMessage(bytes)
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
                    if(len == messageLength) {
                        val buffer = ByteBuffer.wrap(message).order(ByteOrder.LITTLE_ENDIAN)
                        val newX = buffer.float
                        val newY = buffer.float
                        val newZ = buffer.float
                        val newSpeed = buffer.float
                        val isColliding = buffer.int
                        val hash = buffer.float

                        Log.d("Bluetooth", "$isColliding, $newX, $newY, $newZ, $newSpeed")
                        if(hash == newX + newY + newZ + newSpeed + isColliding) {
                            x = newX
                            y = newY
                            z = newZ
                            speed = newSpeed
                            if (isColliding != colliding)
                                collisionStateChange(isColliding)
                            statsCollector.updateSpeed(speed)
                            updateMonitor()
                        }
                    }
                }

            }
        }

    }

    private suspend fun gameOver() {
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
            gameOverAnimation.duration = 2000
            binding.gameOverText.startAnimation(gameOverAnimation)
            binding.stats.text = statsCollector.getStats()
        }

        vibrate(1000)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
        }, 15000)
    }

    private suspend fun collisionStateChange(newState: Int) {
        colliding = newState
        if (colliding == 1) {  // COLLIDING
            // play collision sound
            statsCollector.notifyHit()
            withContext(Dispatchers.Default) {
                collisionSound.start()
            }
            vibrate(500)
            val damagePts = (abs(speed) * 30).roundToInt()
            damage = min(damage + damagePts, 30)
            val s = "Engine damage\n" + "*".repeat(damage) + ".".repeat(30 - damage)
            binding.damage.text = s
            if (damage >= 20) {  // start blinking
                binding.controlCollision.colorFilter =
                    PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP)
                val blinking: Animation = AlphaAnimation(0.0f, 1.0f)
                blinking.duration = 500
                blinking.startOffset = 20
                blinking.repeatMode = Animation.REVERSE
                blinking.repeatCount = Animation.INFINITE
                binding.controlCollision.startAnimation(blinking)
            }
            // create log entry
            val sdf = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z")
            val currentDateAndTime = sdf.format(Date())
            var newLog = "\$ $currentDateAndTime: "
            if (damagePts < 3)
                newLog += "minor collision"
            else if (damagePts <= 5)
                newLog += "collision"
            else
                newLog += "major collision"
            if (collisionLog.count{it == '\n'} > 2)
                collisionLog = collisionLog.substring(collisionLog.indexOf('\n') + 1)
            collisionLog += '\n'
            collisionLog += newLog
            if (damage >= 30)
                gameOver()
        }
    }

    private fun updateMonitor() {
        this@GameActivity.runOnUiThread {
            statistics = "Speed: $speed\nX: $x\nY: $y\nZ: $z"
            val l: Int = statistics.length
            val s = SpannableString(statistics + "\n" + collisionLog)
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

    private fun vibrate(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(duration)
        }
    }

    private fun sendMessage(message: ByteArray) {
        try {
            socket.outputStream.write(message)
            // socket.outputStream.flush()
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
