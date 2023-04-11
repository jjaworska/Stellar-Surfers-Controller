package com.tcs.stellarsurfers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.tcs.stellarsurfers.databinding.ActivityMainBinding
import com.tcs.stellarsurfers.motion_sensors.GyroListener
import com.tcs.stellarsurfers.motion_sensors.GyroscopeUtils


class MainActivity : Activity() {
    private lateinit var sensorManager: SensorManager
    private lateinit var sensor: Sensor
    private lateinit var pixelArt: VectorDrawableCompat
    private lateinit var binding: ActivityMainBinding
    private val gyroListener = GyroListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        dealWithGUI()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        binding.connectButton.setOnClickListener {
            startActivity(Intent(this, SetupConnectionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(gyroListener, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(gyroListener)
    }

    private fun dealWithGUI() {
        binding.root.setBackgroundColor(Color.BLACK)
        val wrapper = ContextThemeWrapper(this, R.style.Theme_StellarSurfers)
        pixelArt = VectorDrawableCompat.create(resources, R.drawable.pixel_art, wrapper.theme)!!
        binding.imageView.setImageDrawable(pixelArt)
        gyroListener.setOnSensorDataReceived {
            val newColor = GyroscopeUtils.rotationVecToHSL(it)
            binding.lolcat.setTextColor(newColor)
            pixelArt.colorFilter = PorterDuffColorFilter(newColor, PorterDuff.Mode.SRC_IN)
            binding.imageView.invalidate()
        }
    }
}
