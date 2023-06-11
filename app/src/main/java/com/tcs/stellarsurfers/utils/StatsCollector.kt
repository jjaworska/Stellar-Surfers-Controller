package com.tcs.stellarsurfers.utils

import java.lang.Float.max
import java.text.SimpleDateFormat
import java.util.Date

class StatsCollector {
    var totalDistance = 0.0f
    var maxSpeed = 0.0f
    var asteroidsShot = 0
    var collisionCount = 0
    private var timestamp: Long = 0
    private var timestamp0: Long = 0
    // to calculate the average
    private var speedSum = 0.0
    public fun init() {
        timestamp0 = System.currentTimeMillis()
    }
    public fun updateSpeed(speed: Float) {
        val newTimestamp = System.currentTimeMillis()
        val deltaTime = (newTimestamp - timestamp)
        totalDistance += deltaTime * speed
        maxSpeed = max(speed, maxSpeed)
        speedSum += speed.toDouble() * deltaTime
    }
    public fun notifyHit() {
        collisionCount += 1
    }
    public fun getStats(): String {
        timestamp = System.currentTimeMillis()
        val totalTimeMillis = timestamp - timestamp0
        val avgSpeed = speedSum / totalTimeMillis
        val format = SimpleDateFormat("mm:ss")
        return "Statistics:\n" +
                "time: ${format.format(Date(totalTimeMillis))}\n" +
                "average speed: ${"%.3f".format(avgSpeed)}\n" +
                "maximum speed: ${"%.3f".format(maxSpeed)}\n" +
                "distance covered: ${"%.3f".format(totalDistance)}\n" +
                "asteroids shot: ${asteroidsShot}\n" +
                "collisions: ${collisionCount}\n"
    }
}
