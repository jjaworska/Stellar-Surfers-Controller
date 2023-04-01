package com.tcs.stellarsurfers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.tcs.stellarsurfers.databinding.ActivityMainBinding
import kotlin.math.*


class MainActivity : Activity() {
    private lateinit var sensorManager: SensorManager
    private lateinit var sensor: Sensor
    private lateinit var pixelArt: VectorDrawableCompat
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.root.setBackgroundColor(Color.BLACK)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val wrapper = ContextThemeWrapper(this, R.style.Theme_StellarSurfers)
        pixelArt = VectorDrawableCompat.create(resources, R.drawable.pixel_art, wrapper.theme)!!
        // binding.imageView.setImageDrawable(pixelArt)
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

    fun rotationVecToHSL(rotationVec: FloatArray): Int {
        assert((rotationVec[0] in -1.0..1.0)
                && (rotationVec[1] in -1.0..1.0)
                && (rotationVec[2] in -1.0..1.0))
        val x = (rotationVec[0] + 1f) * 180f
        val y = (rotationVec[1] + 1f) * 180f
        val z = (rotationVec[2] + 1f) * 180f
        return Color.HSVToColor(floatArrayOf((x + y + z) / 3, 1f, 1f))
    }

    private val gyroListener = object : SensorEventListener {
        // Create a constant to convert nanoseconds to seconds.
        private val NS2S = 1.0f / 1000000000.0f
        private val deltaRotationVector = FloatArray(4) { 0f }
        private var timestamp: Float = 0f
        private val EPSILON = 0.04
        private var rotationCurrent = floatArrayOf(1f, 0f, 0f)

        override fun onSensorChanged(event: SensorEvent?) {
            if (timestamp != 0f && event != null) {
                val dT = (event.timestamp - timestamp) * NS2S
                // Axis of the rotation sample, not normalized yet.
                var axisX: Float = event.values[0]
                var axisY: Float = event.values[1]
                var axisZ: Float = event.values[2]

                // Calculate the angular speed of the sample
                val omegaMagnitude: Float = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)

                // Normalize the rotation vector if it's big enough to get the axis
                // (that is, EPSILON should represent your maximum allowable margin of error)
                if (omegaMagnitude > EPSILON) {
                    axisX /= omegaMagnitude
                    axisY /= omegaMagnitude
                    axisZ /= omegaMagnitude
                }

                // Integrate around this axis with the angular speed by the timestep
                // in order to get a delta rotation from this sample over the timestep
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                val thetaOverTwo: Float = omegaMagnitude * dT / 2.0f
                val sinThetaOverTwo: Float = sin(thetaOverTwo)
                val cosThetaOverTwo: Float = cos(thetaOverTwo)
                deltaRotationVector[0] = sinThetaOverTwo * axisX
                deltaRotationVector[1] = sinThetaOverTwo * axisY
                deltaRotationVector[2] = sinThetaOverTwo * axisZ
                deltaRotationVector[3] = cosThetaOverTwo
            }
            timestamp = event?.timestamp?.toFloat() ?: 0f
            val deltaRotationMatrix = FloatArray(9) { 0f }
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector)
            // User code should concatenate the delta rotation we computed with the current rotation
            // in order to get the updated rotation.
            val rotationNew = floatArrayOf(0f, 0f, 0f)
            for (i in 0..2)
                for (j in 0..2)
                    rotationNew[i] += rotationCurrent[j] * deltaRotationMatrix[3*j+i]
            rotationCurrent = rotationNew
            val textToAdd = StringBuilder()
            for (i in 0..2)
                textToAdd.append("%.3f".format(rotationCurrent[i])).append("\n")
            val newColor = rotationVecToHSL(rotationCurrent)
            binding.lolcat.setTextColor(newColor)
            // pixelArt.colorFilter = PorterDuffColorFilter(newColor, PorterDuff.Mode.SRC_IN)
            // binding.imageView.invalidate()
        }
        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            // no idea lol
        }
    }

}
