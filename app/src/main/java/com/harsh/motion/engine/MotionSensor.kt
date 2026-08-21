package com.harsh.motion.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Thin wrapper around the rotation-vector sensor (a single hardware-fused
 * sensor, cheaper than combining accelerometer + magnetometer manually).
 *
 * Battery discipline: the listener is registered only between [start] and
 * [stop] — callers must stop it whenever the wallpaper/preview is not visible —
 * and [start] takes each sensor separately so a config that uses neither tilt
 * nor shake powers no sensor at all.
 */
class MotionSensor(context: Context) {

    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    /** Smoothed pitch/roll in roughly [-1, 1], updated on the sensor's own thread cadence. */
    @Volatile var tiltX: Float = 0f; private set
    @Volatile var tiltY: Float = 0f; private set

    private var lastShake = 0L
    var onShake: (() -> Unit)? = null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val roll = orientation[2].coerceIn(-1.2f, 1.2f) / 1.2f
                    val pitch = orientation[1].coerceIn(-1.2f, 1.2f) / 1.2f
                    // Light smoothing to avoid jittery motion.
                    tiltX += (roll - tiltX) * 0.15f
                    tiltY += (pitch - tiltY) * 0.15f
                }
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    val magnitude = event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] + event.values[2] * event.values[2]
                    val now = System.currentTimeMillis()
                    if (magnitude > 190f && now - lastShake > 1200) {
                        lastShake = now
                        onShake?.invoke()
                    }
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private var shakeSensor: Sensor? = null
    private var listening = false

    /**
     * Registers only what the caller asks for. Both sensors below are fused
     * (rotation vector = gyro + accelerometer + magnetometer; linear
     * acceleration = gyro + accelerometer), so each one costs real battery for
     * as long as it is registered, regardless of whether anything reads it.
     * Asking for a sensor no active effect uses is pure drain.
     */
    fun start(withTilt: Boolean, withShake: Boolean) {
        if (listening) return
        if (!withTilt && !withShake) return
        listening = true
        if (withTilt) {
            rotationSensor?.let { manager.registerListener(listener, it, TILT_SAMPLING_US) }
        }
        if (withShake) {
            shakeSensor = manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            shakeSensor?.let { manager.registerListener(listener, it, SHAKE_SAMPLING_US) }
        }
    }

    fun stop() {
        if (!listening) return
        listening = false
        manager.unregisterListener(listener)
    }

    private companion object {
        // 30Hz. The renderer eases toward the reading with a ~167ms time
        // constant (see TILT_FOLLOW_RATE), so sampling faster than this cannot
        // change what is drawn — it only adds CPU wake-ups. The previous
        // SENSOR_DELAY_GAME was 50Hz.
        const val TILT_SAMPLING_US = 33_000

        // Shake only needs to notice a sharp jolt, and it is rate-limited to
        // one burst per 1.2s anyway.
        const val SHAKE_SAMPLING_US = 60_000
    }
}
