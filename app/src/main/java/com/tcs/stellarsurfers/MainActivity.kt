package com.tcs.stellarsurfers

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.tcs.stellarsurfers.databinding.ActivityMainBinding
import com.tcs.stellarsurfers.motion_sensors.GyroListener
import com.tcs.stellarsurfers.motion_sensors.GyroscopeUtils


class MainActivity : AppCompatActivity() {
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometerSensor: Sensor
    private lateinit var geomagneticSensor : Sensor
    private lateinit var pixelArt: VectorDrawableCompat
    private lateinit var binding: ActivityMainBinding
    private val gyroListener = GyroListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        dealWithGUI()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        geomagneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        binding.connectButton.setOnClickListener {
            startActivity(Intent(this, SetupConnectionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(gyroListener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(gyroListener, geomagneticSensor, SensorManager.SENSOR_DELAY_GAME)
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
        gyroListener.startMeasuring()
        gyroListener.setOnSensorDataReceived {
            val newColor = GyroscopeUtils.rotationVecToHSL(it)
            binding.lolcat.setTextColor(newColor)
            pixelArt.colorFilter = PorterDuffColorFilter(newColor, PorterDuff.Mode.SRC_IN)
            binding.imageView.invalidate()
        }
    }
}
