/*
 * QuadrantFragment.kt
 *
 * Copyright 2020 Yasuhiro Yamakawa <withlet11@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.withlet11.digitalquadrant

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import java.util.*
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sign
import kotlin.math.sqrt

data class SensorXYZ(var x: Double, var y: Double, var z: Double)

open class QuadrantFragment : Fragment(), SensorEventListener {
    private var sensorX = 0.0
    private var sensorY = 0.0
    private var sensorZ = 0.0
    private var isAutoHoldEnabled = false

    val isStable
        get() = isAutoHoldEnabled && run {
            synchronized(pastData) {
                val deviation = pastData.map { ( it.x - sensorX) * (it.x - sensorX) +
                        ( it.y - sensorY) * (it.y - sensorY) +
                        ( it.z - sensorZ) * (it.z - sensorZ) }.average()
                deviation < 0.003
            }
        }

    val pitchY
        get() = Math.toDegrees(sqrt(sensorX * sensorX + sensorZ * sensorZ).let {
            if (it == 0.0) PI / 2.0 * sign(sensorY) else atan(sensorY / it)
        }).toFloat()

    val rollY
        get() = Math.toDegrees(when (sign(sensorZ)) {
            1.0 -> atan(sensorX / sensorZ)
            -1.0 -> atan(sensorX / sensorZ) + sign(sensorX) * PI
            else -> PI / 2.0 * sign(sensorX)
        }).toFloat()

    val pitchZ
        get() = Math.toDegrees(sqrt(sensorX * sensorX + sensorY * sensorY).let {
            if (it == 0.0) PI / 2.0 * sign(sensorZ) else atan(sensorZ / it)
        }).toFloat()

    val rollZ
        get() = Math.toDegrees(when (sign(sensorY)) {
            1.0 -> atan(sensorX / sensorY)
            -1.0 -> atan(sensorX / sensorY) + sign(sensorX) * PI
            else -> PI / 2.0 * sign(sensorX)
        }).toFloat()

    private val pastData = Collections.synchronizedList(LinkedList<SensorXYZ>())

    private lateinit var sensorManager: SensorManager

    init {
        for (i in 1..10) {
            synchronized(pastData) {
                pastData.add(SensorXYZ(0.0, 0.0, 0.0))
            }
        }
    }

    companion object {
        const val PERIOD = 100L
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            synchronized(pastData) {
                val xyz =  pastData.removeFirst()
                xyz.x = event.values[0].toDouble()
                xyz.y = event.values[1].toDouble()
                xyz.z = event.values[2].toDouble()
                pastData.add(xyz)
                sensorX = pastData.map { it.x }.average()
                sensorY = pastData.map { it.y }.average()
                sensorZ = pastData.map { it.z }.average()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // get argument
        val args = arguments
        isAutoHoldEnabled = args?.getBoolean("IS_AUTOHOLD_ENABLED") ?: false
        sensorManager = activity!!.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onResume() {
        super.onResume()
        val accelerate = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerate, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    fun isAutoHoldEnabledChanged(enabled: Boolean) {
        isAutoHoldEnabled = enabled
    }
}
