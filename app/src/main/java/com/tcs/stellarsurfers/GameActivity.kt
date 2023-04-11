package com.tcs.stellarsurfers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.tcs.stellarsurfers.databinding.ActivityGameBinding
import androidx.databinding.DataBindingUtil
import com.tcs.stellarsurfers.motion_sensors.GyroListener
import java.io.IOException
import java.nio.ByteBuffer


class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var sensor: Sensor
    private val socket = SetupConnectionActivity.socket
    private val gyroListener = GyroListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_game)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        gyroListener.setOnSensorDataReceived {
            val sensorData = "${it[0]}\n${it[1]}\n${it[2]}"
            binding.monitor.text = sensorData
            val msg = "Rotation vector: "
            val buffer = ByteBuffer.allocate(msg.length + 14).put(msg.toByteArray())
            //for (c in msg)
            //    buffer.putChar(c)
            val bytes = buffer.putFloat(it[0]).putFloat(it[1]).putFloat(it[2]).putChar('\n').array()
            sendMessage(bytes)
        }
    }

    private fun sendMessage(message: ByteArray) {
        try {
            socket.outputStream.write(message)
            socket.outputStream.flush()
        } catch (ignored: IOException) {
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(gyroListener, sensor, SensorManager.SENSOR_DELAY_UI)
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
