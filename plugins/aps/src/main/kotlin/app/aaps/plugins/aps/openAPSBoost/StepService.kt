package app.aaps.plugins.aps.openAPSBoost

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import kotlin.math.roundToLong

/**
 * Step counting service for Boost activity detection.
 *
 * Tracks steps in 5-minute buckets and provides aggregated counts
 * for 5, 10, 15, 30, and 60 minute windows. Used by OpenAPSBoostPlugin
 * to detect activity/inactivity and adjust profile percentage and targets.
 *
 * Must be registered as a SensorEventListener for Sensor.TYPE_STEP_COUNTER
 * in the application's main activity or service.
 */
object StepService : SensorEventListener {

    private const val TAG = "StepService"
    private var previousStepCount = -1
    private val stepsMap = LinkedHashMap<Long, Int>()
    private const val FIVE_MINUTES_IN_MS = 300000
    private const val NUM_OF_5MIN_BLOCKS_TO_KEEP = 20

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.i(TAG, "onAccuracyChanged: Sensor: $sensor; accuracy: $accuracy")
    }

    private fun currentTimeIn5Min(): Long {
        return (System.currentTimeMillis() / FIVE_MINUTES_IN_MS.toDouble()).roundToLong()
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        sensorEvent ?: return

        val now = currentTimeIn5Min()
        val stepCount = sensorEvent.values[0].toInt()
        if (previousStepCount >= 0) {
            var recentStepCount = stepCount - previousStepCount
            if (stepsMap.contains(now)) {
                recentStepCount += stepsMap.getValue(now)
            }
            stepsMap[now] = recentStepCount
        }
        previousStepCount = stepCount

        if (stepsMap.size > NUM_OF_5MIN_BLOCKS_TO_KEEP) {
            val removeBefore = now - NUM_OF_5MIN_BLOCKS_TO_KEEP
            stepsMap.entries.removeIf { it.key < removeBefore }
        }
    }

    fun getRecentStepCount5Min(): Int {
        val now = currentTimeIn5Min() - 1
        return if (stepsMap.contains(now)) stepsMap.getValue(now) else 0
    }

    fun getRecentStepCount10Min(): Int {
        val tenMinAgo = currentTimeIn5Min() - 2
        return if (stepsMap.contains(tenMinAgo)) stepsMap.getValue(tenMinAgo) else 0
    }

    fun getRecentStepCount15Min(): Int {
        val fifteenMinAgo = currentTimeIn5Min() - 3
        return if (stepsMap.contains(fifteenMinAgo)) stepsMap.getValue(fifteenMinAgo) else 0
    }

    fun getRecentStepCount30Min(): Int {
        return getStepsInLastXMin(6)
    }

    fun getRecentStepCount60Min(): Int {
        return getStepsInLastXMin(12)
    }

    private fun getStepsInLastXMin(numberOf5MinIncrements: Int): Int {
        var stepCount = 0
        val cutoff = currentTimeIn5Min() - numberOf5MinIncrements
        for (entry in stepsMap.entries) {
            if (entry.key > cutoff) {
                stepCount += entry.value
            }
        }
        return stepCount
    }
}
