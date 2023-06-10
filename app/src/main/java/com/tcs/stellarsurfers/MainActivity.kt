package com.tcs.stellarsurfers

import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import com.tcs.stellarsurfers.motion_sensors.orientationProvider.ImprovedOrientationSensor1Provider
import com.tcs.stellarsurfers.motion_sensors.orientationProvider.OrientationProvider.OrientationProviderListener


class MainActivity : AppCompatActivity(), OrientationProviderListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometerSensor: Sensor
    private lateinit var geomagneticSensor : Sensor
    private lateinit var pixelArt: VectorDrawableCompat
    private lateinit var binding: ActivityMainBinding
    // private val gyroListener = GyroListener()
    private lateinit var gyroListener: ImprovedOrientationSensor1Provider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gyroListener = ImprovedOrientationSensor1Provider(getSystemService(Context.SENSOR_SERVICE) as SensorManager?)
        gyroListener.addListener(this)
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
        gyroListener.start()
        super.onResume()
        // sensorManager.registerListener(gyroListener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
        // sensorManager.registerListener(gyroListener, geomagneticSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        gyroListener.stop()
        super.onPause()
        // sensorManager.unregisterListener(gyroListener)
    }

    private fun dealWithGUI() {
        binding.root.setBackgroundColor(Color.BLACK)
        val wrapper = ContextThemeWrapper(this, R.style.Theme_StellarSurfers)
        pixelArt = VectorDrawableCompat.create(resources, R.drawable.pixel_art, wrapper.theme)!!
        binding.imageView.setImageDrawable(pixelArt)
        // gyroListener.startMeasuring()
        gyroListener.start()
        /* gyroListener.setOnSensorDataReceived {
            val newColor = GyroscopeUtils.rotationVecToHSL(it)
            binding.connectButton.setBackgroundColor(newColor)
        } */
    }

    override fun notifySensorDataReceived() {
        val angles = FloatArray(4)
        gyroListener.getEulerAngles(angles)
        val newColor = GyroscopeUtils.rotationVecToHSL(angles)
        binding.connectButton.setBackgroundColor(newColor)
    }
}
