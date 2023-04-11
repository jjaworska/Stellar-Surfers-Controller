package com.tcs.stellarsurfers.motion_sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GyroListener : SensorEventListener {
    // Create a constant to convert nanoseconds to seconds.
    private val NS2S = 1.0f / 1000000000.0f
    private val deltaRotationVector = FloatArray(4) { 0f }
    private var timestamp: Float = 0f
    private val EPSILON = 0.04
    private var rotationCurrent = floatArrayOf(1f, 0f, 0f)

    private var onSensorDataReceived: ((FloatArray) -> Unit)? = null
    public fun setOnSensorDataReceived(listener: ((FloatArray) -> Unit)) {
        onSensorDataReceived = listener
    }

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
        onSensorDataReceived?.invoke(rotationCurrent)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // no idea lol
    }
}