package com.tcs.stellarsurfers

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils.HSLToColor
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.tcs.stellarsurfers.databinding.ActivityGameBinding
import com.tcs.stellarsurfers.motion_sensors.GyroListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.DecimalFormat
import kotlin.math.abs


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

    private var x: Float = 0.0f
    private var y: Float = 0.0f
    private var z: Float = 0.0f
    private var speed: Float = 0.0f
    private var accel: Float = 0.0f

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
                accel = accelerationValue
                updateMonitor()
                //binding.monitor.text = accelerationValue.toString()
                if (seekBar != null) {
                    seekBar.progressDrawable.colorFilter =
                        PorterDuffColorFilter(accelerationColor, PorterDuff.Mode.SRC_ATOP)
                }
            }

            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        binding.readyBtn.setOnClickListener {
            binding.getReady.isVisible = false
            binding.controls.isVisible = true
            gyroListener.startMeasuring()
            gyroListener.setOnSensorDataReceived {
                val sensorData = "${it[0]}\n${it[1]}\n${it[2]}"

                /*
                 * data format:
                 * 3 floats - rotation angles
                 * 1 float - acceleration
                 *
                 * messages must be of constant size specified in messageSize
                 */

                val messageSize = 12 + 4
                val buffer = ByteBuffer.allocate(messageSize).order(ByteOrder.LITTLE_ENDIAN)
                buffer.putFloat(it[0]).putFloat(it[1]).putFloat(it[2])
                buffer.putFloat(accelerationValue)
                val bytes = buffer.array()
                sendMessage(bytes)
            }
        }

        MainScope().launch {
            val messageLength = 16
            val message = ByteArray(messageLength)
            while(true) {
                withContext(Dispatchers.IO) {
                    SetupConnectionActivity.socket.inputStream.read(message, 0, messageLength)

                    val buffer = ByteBuffer.wrap(message).order(ByteOrder.LITTLE_ENDIAN)
                    x = buffer.float
                    y = buffer.float
                    z = buffer.float
                    speed = buffer.float
                    updateMonitor()
                    //Log.i("Receiver", "got message $message $x, $y, $z, $speed")
                }

            }
        }

    }

    private fun updateMonitor() {
        val s = "X: $x, Y: $y, Z: $z, speed: $speed"
//        val s = "X: " + "%06f".format(x) + "Y: " + "%06f".format(y) + "Z: " + "%06f".format(z) +
//                "speed: " + "%06.2f".format(speed)
        //Log.i("abc", s)
        binding.monitor.text = s
    }

    private fun sendMessage(message: ByteArray) {
        try {
            socket.outputStream.write(message)
            socket.outputStream.flush()
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
