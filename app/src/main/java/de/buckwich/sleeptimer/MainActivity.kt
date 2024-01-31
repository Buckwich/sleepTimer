package de.buckwich.sleeptimer

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private val timerMinutes = 5F
    private val resetMinutes = 0.5F
    private val threshold = 0.3F
    val TAG = MainActivity::class.simpleName
    private val context = this

    private var countDownTimer: CountDownTimer? = null

    private lateinit var accelerationEventListener: AccelerationEventListener
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerationSensor: Sensor
    private lateinit var mediaPlayerMove: MediaPlayer
    private lateinit var mediaPlayerRestart: MediaPlayer

    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private lateinit var resultTextView: TextView
    private lateinit var remainingTimeTextView: TextView
    private lateinit var remainingTimeProgressBar: ProgressBar

    lateinit var textDefaultColor: ColorStateList
    lateinit var progressDefaultColor: ColorStateList

    private val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private lateinit var sharedPref: SharedPreferences

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "STOPPED")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "DESTROYED")
        countDownTimer?.run { cancel() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultTextView = findViewById(R.id.resultTextView)
        remainingTimeTextView = findViewById(R.id.remainingTimeTextView)
        remainingTimeProgressBar = findViewById(R.id.remainingTimeProgressBar)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        textDefaultColor = remainingTimeTextView.textColors
        progressDefaultColor = remainingTimeProgressBar.progressTintList!!

        accelerationEventListener = AccelerationEventListener(context, threshold)
        mediaPlayerMove = MediaPlayer.create(context, notification)
        mediaPlayerRestart = MediaPlayer.create(context, notification)

        try {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        } catch (e: Exception) {
            Toast.makeText(context, "Your device does not support movement detection ", Toast.LENGTH_LONG).show()
        }

        sharedPref = getSharedPreferences("sleepTimer", Context.MODE_PRIVATE)
    }

    override fun onStart() {

        super.onStart()

        // require notification access
        if (!NotificationListener.isEnabled(context)) {
            NotificationAccessDialog.show(context)
            return
        }
        resultTextView.text = sharedPref.getString("savedSession", "")
    }

    private fun getActiveSessions(context: Context): List<MediaController> {
        if (NotificationListener.isEnabled(context)) {
            var mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

            // NotificationListener required due to permission MEDIA_CONTENT_CONTROL only for first party apps
            var listenerComponent = ComponentName(context, NotificationListener::class.java)
            return mediaSessionManager.getActiveSessions(listenerComponent)
        } else {
            Toast.makeText(context, "Notification Access is required", Toast.LENGTH_LONG).show()
            return emptyList()
        }
    }

    private fun pauseAll() {
        Log.i(TAG, "pauseAll")
        var pausedMedia = ""
        for (session in getActiveSessions(context)) {
            pausedMedia = pausedMedia + session.metadata!!.description + System.getProperty("line.separator")
            session.transportControls.pause()
        }

        resultTextView.text = pausedMedia

        with(sharedPref.edit()) {
            putString("savedSession", pausedMedia)
            apply()
        }
    }

    private fun startTimer() {
        var millisToCount = minutes(timerMinutes)
        countDownTimer = object : CountDownTimer(millisToCount, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeTextView.text = millisToString(millisUntilFinished)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    remainingTimeProgressBar.setProgress((millisUntilFinished * 1000F / millisToCount).toInt(), true)
                } else {
                    remainingTimeProgressBar.progress = (millisUntilFinished * 1000F / millisToCount).toInt()
                }
            }

            override fun onFinish() {
                waitForMovement()
            }
        }.start()
    }

    private fun waitForMovement() {
        Log.i(TAG, "waitForMovement")
        mediaPlayerMove.start()

        sensorManager.registerListener(accelerationEventListener, accelerationSensor, SensorManager.SENSOR_DELAY_NORMAL)

        remainingTimeTextView.setTextColor(0xFFFF4081.toInt())
        remainingTimeProgressBar.progressTintList = ColorStateList.valueOf(0xFFFF4081.toInt())

        var millisToCount = minutes(resetMinutes)
        countDownTimer = object : CountDownTimer(millisToCount, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeTextView.text = millisToString(millisUntilFinished)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    remainingTimeProgressBar.setProgress((millisUntilFinished * 1000F / millisToCount).toInt(), true)
                } else {
                    remainingTimeProgressBar.progress = (millisUntilFinished * 1000F / millisToCount).toInt()
                }
            }

            override fun onFinish() {
                remainingTimeTextView.setTextColor(textDefaultColor)
                remainingTimeProgressBar.progressTintList = progressDefaultColor

                remainingTimeProgressBar.progress = 0
                remainingTimeTextView.text = ""

                sensorManager.unregisterListener(accelerationEventListener)
                pauseAll()
            }
        }.start()
    }

    fun movementDetected() {
        Log.i(TAG, "movementDetected")
        mediaPlayerRestart.start()

        remainingTimeTextView.setTextColor(textDefaultColor)
        remainingTimeProgressBar.progressTintList = progressDefaultColor

        sensorManager.unregisterListener(accelerationEventListener)
        countDownTimer?.cancel()
        startTimer()
    }

    fun startButton(v: View) {
        startButton.visibility = View.INVISIBLE
        stopButton.visibility = View.VISIBLE
        Log.i(TAG, "startButton clicked")
        startTimer()
    }

    fun stopButton(v: View) {
        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.INVISIBLE
        Log.i(TAG, "stopButton clicked")
        countDownTimer?.cancel()
        remainingTimeTextView.setTextColor(textDefaultColor)
        remainingTimeProgressBar.progressTintList = progressDefaultColor

        remainingTimeProgressBar.progress = 0
        remainingTimeTextView.text = ""

        sensorManager.unregisterListener(accelerationEventListener)
    }

    private fun seconds(seconds: Float): Int {
        return (1000 * seconds).toInt()
    }

    private fun minutes(minutes: Float): Long {
        return (60 * seconds(minutes)).toLong()
    }

    private fun millisToString(millis: Long): String {
        var min: Int = (millis / 1000 / 60).toInt()
        var sec: Int = (millis / 1000 % 60).toInt()
        return "$min:${"%02d".format(sec)}"
    }
}

