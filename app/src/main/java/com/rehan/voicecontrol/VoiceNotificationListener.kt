package com.rehan.voicecontrol

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class VoiceNotificationListener : NotificationListenerService() {

    companion object {
        var lastSenderName: String? = null
        var lastMessageText: String? = null
        var lastAppName: String? = null

        private val trackedPackages = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.facebook.orca",
            "com.google.android.apps.messaging",
            "com.instagram.android",
            "org.telegram.messenger"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        if (sbn.packageName !in trackedPackages) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        if (!title.isNullOrBlank()) {
            lastSenderName = title
            lastMessageText = text ?: ""
            lastAppName = appNameFromPackage(sbn.packageName)
        }
    }

    private fun appNameFromPackage(packageName: String): String {
        return when (packageName) {
            "com.whatsapp", "com.whatsapp.w4b" -> "WhatsApp"
            "com.facebook.orca" -> "Messenger"
            "com.google.android.apps.messaging" -> "SMS"
            "com.instagram.android" -> "Instagram"
            "org.telegram.messenger" -> "Telegram"
            else -> "Message"
        }
    }
}
