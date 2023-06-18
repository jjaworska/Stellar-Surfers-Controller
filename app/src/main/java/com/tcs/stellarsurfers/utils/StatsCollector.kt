package com.tcs.stellarsurfers.utils

import android.util.Log
import java.lang.Float.max
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt

/*
 * the game logic delegated to a separate class
 */
class StatsCollector {
    private val INFO_SAFE = 0
    private val INFO_COLLISION = 1
    private val INFO_COLLISION_AHEAD_PLANET = 2
    private val INFO_COLLISION_AHEAD_ASTEROID = 3
    private val INFO_ASTEROID_SHOT = 4
    // private var statistics: String = "speed\nX: $x\nY: $y\nZ: $z"
    private var collisionLog: String = "\n"
    private var totalDistance = 0.0f
    private var maxSpeed = 0.0f
    private var asteroidsShot = 0
    private var collisionCount = 0
    var x = 0.0f
    var y = 0.0f
    var z = 0.0f
    var speed = 0.0f
    var accel: Float = 0.0f
    var damage: Int = 0
    private var askedCollisionStatus = true
    private var collisionStatus = 0
    private var timestamp: Long = 0
    private var timestamp0: Long = 0
    private var frameCnt: Long = 0
    // to calculate the average
    private var speedSum = 0.0
    fun init() {
        timestamp0 = System.currentTimeMillis()
        timestamp = timestamp0
    }
    fun update(newSpeed: Float, collisionInfo: Int) {
        speed = newSpeed
        val newTimestamp = System.currentTimeMillis()
        // val deltaTime = (newTimestamp - timestamp)
        // totalDistance += deltaTime * speed
        maxSpeed = max(speed, maxSpeed)
        // speedSum += speed.toDouble() * deltaTime
        speedSum += speed.toDouble()
        timestamp = newTimestamp
        frameCnt += 1
        if (collisionInfo != collisionStatus) {
            if (collisionInfo == INFO_COLLISION)
                newCollision()
            if (collisionInfo == INFO_ASTEROID_SHOT)
                asteroidsShot += 1
            if (collisionStatus == INFO_COLLISION && collisionInfo != INFO_SAFE)
                return
            collisionStatus = collisionInfo
            Log.e("statsCollector", "Changing status to $collisionStatus")
            askedCollisionStatus = false
        }
    }

    private fun newCollision() {
        collisionCount += 1
        // create log entry
        val sdf = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z")
        val currentDateAndTime = sdf.format(Date())
        var newLog = "\$ $currentDateAndTime: "
        val damagePts: Int = (kotlin.math.abs(speed) * 30).roundToInt()
        damage = (damage + damagePts).coerceAtMost(30)
        if (damagePts < 3)
            newLog += "minor collision"
        else if (damagePts <= 5)
            newLog += "collision"
        else
            newLog += "major collision"
        if (collisionLog.count{it == '\n'} > 2)
            collisionLog = collisionLog.substring(collisionLog.indexOf('\n') + 1)
        collisionLog += '\n'
        collisionLog += newLog
    }

    fun getFinalStatistics(): String {
        timestamp = System.currentTimeMillis()
        val totalTimeMillis = timestamp - timestamp0
        val avgSpeed = speedSum / totalTimeMillis
        val format = SimpleDateFormat("mm:ss")
        totalDistance = (totalTimeMillis.toFloat() * avgSpeed.toFloat() / 1000.0f)
        return "Statistics:\n" +
                "time: ${format.format(Date(totalTimeMillis))}\n" +
                "average speed: ${"%.3f".format(avgSpeed)}\n" +
                "maximum speed: ${"%.3f".format(maxSpeed)}\n" +
                "distance covered: ${"%.3f".format(totalDistance)}\n" +
                "asteroids shot: ${asteroidsShot}\n" +
                "collisions: ${collisionCount}\n"
    }

    fun getDamageStr(): String {
        return "Engine damage\n" + "*".repeat(damage) + ".".repeat(30 - damage)
    }

    fun shouldNotifyCollision(): Boolean {
        val ans = (collisionStatus == INFO_COLLISION && !askedCollisionStatus)
        askedCollisionStatus = (askedCollisionStatus || ans)
        return ans
    }

    fun shouldNotifyCollisionAheadPlanet(): Boolean {
        val ans = (collisionStatus == INFO_COLLISION_AHEAD_PLANET && !askedCollisionStatus)
        askedCollisionStatus = (askedCollisionStatus || ans)
        return ans
    }

    fun shouldNotifyCollisionAheadAsteroid(): Boolean {
        val ans = (collisionStatus == INFO_COLLISION_AHEAD_ASTEROID && !askedCollisionStatus)
        askedCollisionStatus = (askedCollisionStatus || ans)
        return ans
    }

    fun shouldNotifyAsteroidShot(): Boolean {
        val ans = (collisionStatus == INFO_ASTEROID_SHOT && !askedCollisionStatus)
        askedCollisionStatus = (askedCollisionStatus || ans)
        return ans
    }

    fun getStatsString(): String {
        return "speed: $speed\nX: $x\nY: $y\nZ: $z"
    }

    fun getCollisionLog(): String {
        return collisionLog
    }
}
