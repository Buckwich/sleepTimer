package de.buckwich.sleeptimer

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import kotlin.math.sqrt


class AccelerationEventListener(private val context: MainActivity,private var threshold:Float) : SensorEventListener {

    private val TAG = AccelerationEventListener::class.simpleName
    private val previousValue = arrayOf(0F, 0F, 0F)

    fun setThreshold(newThreshold: Float){
        threshold=newThreshold
    }
    fun getThreshold(): Float {
        return threshold
    }
    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0] - previousValue[0]
        val y = event.values[1] - previousValue[1]
        val z = event.values[2] - previousValue[2]
        val movement = sqrt(x * x + y * y + z * z)
        Log.i(TAG,"Movement: ${"%.2f".format(movement)}")

        if (movement > threshold) {
            context.movementDetected()
        }
        previousValue[0]=event.values[0]
        previousValue[1]=event.values[1]
        previousValue[2]=event.values[2]
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.i(TAG,"Accuracy changed to $accuracy")
    }

}

