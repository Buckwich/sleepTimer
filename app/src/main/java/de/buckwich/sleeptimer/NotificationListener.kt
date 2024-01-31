package de.buckwich.sleeptimer

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat


/**
 * A notification listener service to allows us to grab active media sessions from their
 * notifications.
 * This class is only used on API 21+ because the Android media framework added getActiveSessions
 * in API 21.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class NotificationListener : NotificationListenerService() {
    companion object {
        // Helper method to check if our notification listener is enabled. In order to get active media
        // sessions, we need an enabled notification listener component.

        fun isEnabled(context: Context): Boolean {
            return NotificationManagerCompat
                    .getEnabledListenerPackages(context)
                    .contains(context.packageName)
        }
    }
}
