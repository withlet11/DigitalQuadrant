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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.lang.System.currentTimeMillis
import java.util.Collections
import java.util.LinkedList
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sign
import kotlin.math.sqrt

data class SensorXYZ(var x: Double, var y: Double, var z: Double)

@Composable
fun QuadrantScreen(
    index: Int,
    isAutoHoldEnabled: Boolean,
    vibrator: Vibrator,
    modifier: Modifier = Modifier
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
    var lastExecutedTime = 0L

    val accelerateSensorEventListener = remember(isAutoHoldEnabled) {
        object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            }

            override fun onSensorChanged(event: SensorEvent) {
                if (!isPaused && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val currentTime = currentTimeMillis()
                    if (currentTime - lastExecutedTime < 150) return

                    lastExecutedTime = currentTime

                    synchronized(pastData) {
                        if (pastData.isEmpty()) {
                            return
                        }
                        val xyz = pastData.removeAt(0)
                        xyz.x = event.values[0].toDouble()
                        xyz.y = event.values[1].toDouble()
                        xyz.z = event.values[2].toDouble()
                        pastData.add(xyz)
                        val sensorX = pastData.map { it.x }.average()
                        val sensorY = pastData.map { it.y }.average()
                        val sensorZ = pastData.map { it.z }.average()

                        if (isStable(sensorX, sensorY, sensorZ, pastData)) {
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
                            pitchY =
                                Math.toDegrees(sqrt(sensorX * sensorX + sensorZ * sensorZ).let {
                                    if (it == 0.0) PI / 2.0 * sign(sensorY) else atan(sensorY / it)
                                }).toFloat()

                            rollY = Math.toDegrees(
                                when (sign(sensorZ)) {
                                    1.0 -> atan(sensorX / sensorZ)
                                    -1.0 -> atan(sensorX / sensorZ) + sign(sensorX) * PI
                                    else -> PI / 2.0 * sign(sensorX)
                                }
                            ).toFloat()

                            pitchZ =
                                Math.toDegrees(sqrt(sensorX * sensorX + sensorY * sensorY).let {
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

            fun isStable(
                currentSensorX: Double,
                currentSensorY: Double,
                currentSensorZ: Double,
                currentPastData: List<SensorXYZ>
            ): Boolean = run {
                synchronized(pastData) {
                    if (currentPastData.isEmpty()) return false
                    val deviation = pastData.map {
                        (it.x - currentSensorX) * (it.x - currentSensorX) +
                                (it.y - currentSensorY) * (it.y - currentSensorY) +
                                (it.z - currentSensorZ) * (it.z - currentSensorZ)
                    }.average()
                    deviation < 0.003
                }
            }
        }
    }

    DisposableEffect(sensorManager, accelerateSensor, accelerateSensorEventListener) {
        synchronized(pastData) {
            repeat(10) {
                pastData.add(SensorXYZ(0.0, 0.0, 0.0))
            }
        }

        sensorManager.registerListener(
            accelerateSensorEventListener,
            accelerateSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        onDispose {
            sensorManager.unregisterListener(accelerateSensorEventListener)
        }
    }

    Surface(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page: Int ->
            when (page) {
                0 -> GridView(
                    pitch = pitchY, roll = rollY, isPaused = isPaused,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, onClick = { isPaused = !isPaused })
                )

                else -> ReticleView(
                    altitude = -pitchZ, roll = -rollZ, isPaused = isPaused,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isPaused = !isPaused })
                )
            }
        }
    }
}