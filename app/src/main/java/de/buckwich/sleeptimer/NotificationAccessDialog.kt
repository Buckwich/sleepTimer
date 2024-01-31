package de.buckwich.sleeptimer

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AlertDialog
import kotlin.system.exitProcess


class NotificationAccessDialog {

    companion object {
        fun show(context: Context) {
            val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AlertDialog.Builder(context, android.R.style.ThemeOverlay_Material_Dialog_Alert)
            } else {
                AlertDialog.Builder(context)
            }

            builder.setTitle("Notification Access")
                    .setMessage("Currently Media Control is only given to first party apps. \nTo still control playback notification access is required\n\nPlease enable notification access in the settings")
                    .setPositiveButton("Open Settings") { _, _ ->
                        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    }
                    .setNegativeButton("Close app") { _, _ ->
                        exitProcess(0)
                    }
                    .show()
        }
    }
}