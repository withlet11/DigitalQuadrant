package io.github.withlet11.digitalquadrant

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.util.Collections
import java.util.LinkedList
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sign
import kotlin.math.sqrt

// @RequiresApi(Build.VERSION_CODES.S)
@Composable
fun QuadrantScreen(
    index: Int,
    isAutoHoldEnabled: Boolean,
    vibrator: Vibrator
) {
    val pagerState =
        rememberPagerState(pageCount = { 2 }, initialPage = index)
    val ctx = LocalContext.current

    val sensorManager: SensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerateSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val pastData = Collections.synchronizedList(LinkedList<SensorXYZ>())
    var pitchY by remember { mutableFloatStateOf(0f) }
    var rollY by remember { mutableFloatStateOf(0f) }
    var pitchZ by remember { mutableFloatStateOf(0f) }
    var rollZ by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var sensorX = 0.0
    var sensorY = 0.0
    var sensorZ = 0.0
    val accelerateSensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (!isPaused && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                synchronized(pastData) {
                    val xyz = pastData.removeAt(0)
                    xyz.x = event.values[0].toDouble()
                    xyz.y = event.values[1].toDouble()
                    xyz.z = event.values[2].toDouble()
                    pastData.add(xyz)
                    sensorX = pastData.map { it.x }.average()
                    sensorY = pastData.map { it.y }.average()
                    sensorZ = pastData.map { it.z }.average()

                    if (isStable()) {
                        if (isAutoHoldEnabled && !isPaused) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val vibrationEffect =
                                    VibrationEffect.createOneShot(
                                        301,
                                        VibrationEffect.DEFAULT_AMPLITUDE
                                    )
                                vibrator.vibrate(vibrationEffect)
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(301)
                            }

                            isPaused = true
                        }
                    } else {
                        pitchY = Math.toDegrees(sqrt(sensorX * sensorX + sensorZ * sensorZ).let {
                            if (it == 0.0) PI / 2.0 * sign(sensorY) else atan(sensorY / it)
                        }).toFloat()

                        rollY = Math.toDegrees(
                            when (sign(sensorZ)) {
                                1.0 -> atan(sensorX / sensorZ)
                                -1.0 -> atan(sensorX / sensorZ) + sign(sensorX) * PI
                                else -> PI / 2.0 * sign(sensorX)
                            }
                        ).toFloat()

                        pitchZ = Math.toDegrees(sqrt(sensorX * sensorX + sensorY * sensorY).let {
                            if (it == 0.0) PI / 2.0 * sign(sensorZ) else atan(sensorZ / it)
                        }).toFloat()

                        rollZ = Math.toDegrees(
                            when (sign(sensorY)) {
                                1.0 -> atan(sensorX / sensorY)
                                -1.0 -> atan(sensorX / sensorY) + sign(sensorX) * PI
                                else -> PI / 2.0 * sign(sensorX)
                            }
                        ).toFloat()
                    }
                }
            }
        }

        fun isStable(): Boolean = run {
            synchronized(pastData) {
                val deviation = pastData.map {
                    (it.x - sensorX) * (it.x - sensorX) +
                            (it.y - sensorY) * (it.y - sensorY) +
                            (it.z - sensorZ) * (it.z - sensorZ)
                }.average()
                deviation < 0.003
            }
        }
    }

    sensorManager.registerListener(
        accelerateSensorEventListener,
        accelerateSensor,
        SensorManager.SENSOR_DELAY_NORMAL
    )

    for (i in 1..10) {
        synchronized(pastData) {
            pastData.add(SensorXYZ(0.0, 0.0, 0.0))
        }
    }

    val PERIOD = 100L

    /*
    fun isAutoHoldEnabledChanged(enabled: Boolean) {
        isAutoHoldEnabled = enabled
    }
     */

    Surface {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page: Int ->
            when (page) {
                0 -> GridView(
                    pitch = pitchY, roll = rollY, isPaused = isPaused,
                    modifier = Modifier.clickable(onClick = { isPaused = !isPaused })
                )

                else -> Reticle(
                    altitude = -pitchZ, roll = -rollZ, isPaused = isPaused,
                    modifier = Modifier.clickable(onClick = { isPaused = !isPaused })
                )
            }
        }
    }
}