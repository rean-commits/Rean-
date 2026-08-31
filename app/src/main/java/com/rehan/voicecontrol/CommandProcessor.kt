package com.rehan.voicecontrol

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CommandProcessor(private val context: Context) {

    fun process(rawText: String) {
        val text = rawText.lowercase().trim()

        when {
            (text.contains("call") && (text.contains("answer") || text.contains("receive") || text.contains("utha"))) -> {
                answerCall()
            }

            (text.contains("call") && (text.contains("kaato") || text.contains("cut") || text.contains("reject"))) -> {
                rejectCall()
            }

            (text.contains("message") && (text.contains("kiska") || text.contains("kis ka") ||
                text.contains("padho") || text.contains("naya") || text.contains("read"))) -> {
                readLastMessage()
            }

            text.contains("reply") -> {
                val replyBody = text.replace("reply", "").replace("karo", "").trim()
                replyToLastMessage(replyBody)
            }

            text.contains("message") -> {
                val (name, msgBody) = extractNameAndMessage(text)
                val viaWhatsapp = text.contains("whatsapp")
                sendMessage(name, msgBody, viaWhatsapp)
            }

            text.contains("call") -> {
                val name = extractNameForCall(text)
                makeCall(name)
            }

            text.contains("khol") || text.contains("open") -> {
                val appName = extractAppName(text)
                openApp(appName)
            }

            text.contains("wifi on") || text.contains("wifi chalu") -> {
                say("WiFi settings khol raha hoon")
                openSettingsPanel(Settings.Panel.ACTION_WIFI)
            }
            text.contains("wifi off") || text.contains("wifi band") -> {
                say("WiFi settings khol raha hoon")
                openSettingsPanel(Settings.Panel.ACTION_WIFI)
            }

            text.contains("bluetooth") -> {
                say("Bluetooth settings khol raha hoon")
                context.startActivity(
                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }

            text.contains("brightness") || text.contains("roshni") -> {
                say("Display settings khol raha hoon")
                context.startActivity(
                    Intent(Settings.ACTION_DISPLAY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }

            text.contains("back jao") || text.contains("go back") -> {
                say("Theek hai")
                sendGestureCommand("back")
            }
            text.contains("recent") -> {
                say("Theek hai")
                sendGestureCommand("recents")
            }
            text.contains("home") -> {
                say("Theek hai")
                sendGestureCommand("home")
            }

            text.contains("dabao") || text.contains("tap") || text.contains("press") -> {
                val target = extractTapTarget(text)
                sendTapCommand(target)
            }

            text.contains("likho") || text.contains("type") -> {
                val toType = extractTypeText(text)
                sendTypeCommand(toType)
            }

            else -> {
                val msg = "Samajh nahi aaya: $rawText"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                say("Maazrat, samajh nahi aaya")
            }
        }
    }

    private fun say(text: String) {
        VoiceSpeaker.speak(text)
    }

    private fun extractAppName(text: String): String {
        return text
            .replace("open", "")
            .replace("khol", "")
            .replace("karo", "")
            .replace("do", "")
            .trim()
    }

    private fun extractNameForCall(text: String): String {
        return text
            .replace("call", "")
            .replace("karo", "")
            .replace("ko", "")
            .trim()
    }

    private fun extractNameAndMessage(text: String): Pair<String, String> {
        var cleaned = text
            .replace("whatsapp", "")
            .replace("pe", "")
            .replace("par", "")

        val splitWord = when {
            cleaned.contains("message karo") -> "message karo"
            cleaned.contains("likho") -> "likho"
            cleaned.contains("message") -> "message"
            else -> "message"
        }

        val parts = cleaned.split(splitWord, limit = 2)
        val namePart = parts.getOrElse(0) { "" }.replace("ko", "").trim()
        val msgPart = parts.getOrElse(1) { "" }.trim()
        return Pair(namePart, msgPart)
    }

    private fun openApp(appNameQuery: String) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val match = apps.firstOrNull { appInfo ->
            val label = pm.getApplicationLabel(appInfo).toString().lowercase()
            label.contains(appNameQuery) || appNameQuery.contains(label)
        }

        if (match != null) {
            val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
            if (launchIntent != null) {
                val appLabel = pm.getApplicationLabel(match).toString()
                context.startActivity(launchIntent)
                Toast.makeText(context, "$appLabel khol raha hoon", Toast.LENGTH_SHORT).show()
                say("$appLabel khol raha hoon")
                return
            }
        }
        Toast.makeText(context, "App nahi mili: $appNameQuery", Toast.LENGTH_SHORT).show()
        say("App nahi mili")
    }

    private fun findPhoneNumber(name: String): String? {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$name%")

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                return cursor.getString(numberIndex)
            }
        }
        return null
    }

    private fun makeCall(contactName: String) {
        val number = findPhoneNumber(contactName)
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (number != null && hasCallPermission) {
            say("$contactName ko call kar raha hoon")
            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(callIntent)
        } else if (number != null) {
            say("$contactName ka number mil gaya, dialer khol raha hoon")
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            context.startActivity(dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            say("$contactName nahi mila contacts mein, dialer khol raha hoon")
            val intent = Intent(Intent.ACTION_DIAL)
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun answerCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                telecomManager.acceptRingingCall()
                say("Call utha raha hoon")
            } else {
                say("Call answer karne ki permission nahi hai")
            }
        } else {
            say("Ye feature is Android version par nahi chalta")
        }
    }

    private fun rejectCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                telecomManager.endCall()
                say("Call kaat di")
            } else {
                say("Call kaatne ki permission nahi hai")
            }
        } else {
            say("Ye feature is Android version par nahi chalta")
        }
    }

    private fun sendMessage(name: String, message: String, viaWhatsapp: Boolean) {
        val number = findPhoneNumber(name)
        if (number == null) {
            say("$name nahi mila contacts mein")
            return
        }

        if (viaWhatsapp) {
            say("$name ko WhatsApp pe message bhej raha hoon")
            try {
                val cleanNumber = number.replace(" ", "").replace("-", "")
                val uri = Uri.parse("https://wa.me/$cleanNumber?text=${Uri.encode(message)}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                android.os.Handler(context.mainLooper).postDelayed({
                    val tapIntent = Intent(VoiceAccessibilityService.ACTION_TAP)
                    tapIntent.putExtra("target", "send")
                    context.sendBroadcast(tapIntent)
                }, 3000)
            } catch (e: Exception) {
                say("WhatsApp message bhejne mein masla aaya")
            }
        } else {
            val hasSmsPermission = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasSmsPermission) {
                say("$name ko message bhej raha hoon")
                try {
                    val smsManager = context.getSystemService(SmsManager::class.java)
                        ?: SmsManager.getDefault()
                    smsManager.sendTextMessage(number, null, message, null, null)
                } catch (e: Exception) {
                    say("Message bhejne mein masla aaya")
                }
            } else {
                say("SMS permission nahi hai")
            }
        }
    }

    private fun readLastMessage() {
        val sender = VoiceNotificationListener.lastSenderName
        val message = VoiceNotificationListener.lastMessageText
        val app = VoiceNotificationListener.lastAppName

        if (sender == null) {
            say("Koi naya message nahi hai")
            return
        }
        val spoken = "$app par $sender ka message aaya: $message"
        Toast.makeText(context, spoken, Toast.LENGTH_LONG).show()
        say(spoken)
    }

    private fun replyToLastMessage(replyBody: String) {
        val sender = VoiceNotificationListener.lastSenderName
        val app = VoiceNotificationListener.lastAppName

        if (sender == null) {
            say("Koi message hi nahi hai jiska reply karun")
            return
        }
        if (replyBody.isBlank()) {
            say("Kya likhna hai, samajh nahi aaya")
            return
        }
        val viaWhatsapp = app == "WhatsApp"
        sendMessage(sender, replyBody, viaWhatsapp)
    }

    private fun openSettingsPanel(panelAction: String) {
        try {
            val intent = Intent(panelAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Settings panel open nahi ho saka", Toast.LENGTH_SHORT).show()
            say("Settings nahi khul saki")
        }
    }

    private fun sendGestureCommand(action: String) {
        val intent = Intent(VoiceAccessibilityService.ACTION_GESTURE)
        intent.putExtra("gesture", action)
        context.sendBroadcast(intent)
    }

    private fun extractTapTarget(text: String): String {
        return text
            .replace("dabao", "")
            .replace("tap", "")
            .replace("press", "")
            .replace("button", "")
            .replace("par", "")
            .replace("ko", "")
            .trim()
    }

    private fun extractTypeText(text: String): String {
        return when {
            text.contains("likho") -> text.substringAfter("likho").trim()
            text.contains("type") -> text.substringAfter("type").trim()
            else -> text
        }
    }

    private fun sendTapCommand(target: String) {
        if (target.isBlank()) {
            Toast.makeText(context, "Kya dabana hai, naam nahi mila", Toast.LENGTH_SHORT).show()
            say("Kya dabana hai, samajh nahi aaya")
            return
        }
        val intent = Intent(VoiceAccessibilityService.ACTION_TAP)
        intent.putExtra("target", target)
        context.sendBroadcast(intent)
        Toast.makeText(context, "\"$target\" dhoond raha hoon...", Toast.LENGTH_SHORT).show()
        say("$target dhoond raha hoon")
    }

    private fun sendTypeCommand(toType: String) {
        if (toType.isBlank()) return
        val intent = Intent(VoiceAccessibilityService.ACTION_TYPE)
        intent.putExtra("text", toType)
        context.sendBroadcast(intent)
        say("Likh raha hoon")
    }
}
