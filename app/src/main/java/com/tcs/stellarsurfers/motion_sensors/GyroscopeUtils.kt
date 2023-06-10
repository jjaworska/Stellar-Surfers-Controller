package com.tcs.stellarsurfers.motion_sensors

import android.graphics.Color
import android.util.Log
import kotlin.math.abs

class GyroscopeUtils {
    companion object {
        fun rotationVecToHSL(rotationVec: FloatArray): Int {
            Log.e("rotationVecToHSL", "received ${rotationVec[0]}, ${rotationVec[1]}, ${rotationVec[2]}")
            for (i in 0..2) {
                rotationVec[i] = (rotationVec[i] / Math.PI).toFloat()
                assert(rotationVec[i] in -1.0..1.0)
            }
            val col = Color.rgb(abs(rotationVec[0]), abs(rotationVec[1]), abs(rotationVec[2]))
            val hsv = FloatArray(3)
            Color.colorToHSV(col, hsv)
            hsv[1] = 1.0f
            hsv[2] = 1.0f
            return Color.HSVToColor(hsv)
        }
    }
}