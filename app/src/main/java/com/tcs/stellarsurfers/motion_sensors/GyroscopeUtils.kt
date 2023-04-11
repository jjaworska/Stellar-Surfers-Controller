package com.tcs.stellarsurfers.motion_sensors

import android.graphics.Color

class GyroscopeUtils {
    companion object {
        fun rotationVecToHSL(rotationVec: FloatArray): Int {
            assert(
                (rotationVec[0] in -1.0..1.0)
                        && (rotationVec[1] in -1.0..1.0)
                        && (rotationVec[2] in -1.0..1.0)
            )
            val x = (rotationVec[0] + 1f) * 180f
            val y = (rotationVec[1] + 1f) * 180f
            val z = (rotationVec[2] + 1f) * 180f
            return Color.HSVToColor(floatArrayOf((x + y + z) / 3, 1f, 1f))
        }
    }
}