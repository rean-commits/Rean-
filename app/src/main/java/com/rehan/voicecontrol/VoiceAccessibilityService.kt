package com.rehan.voicecontrol

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat

class VoiceAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_GESTURE = "com.rehan.voicecontrol.ACTION_GESTURE"
        const val ACTION_TAP = "com.rehan.voicecontrol.ACTION_TAP"
        const val ACTION_TYPE = "com.rehan.voicecontrol.ACTION_TYPE"
        const val ACTION_TAP_RESULT = "com.rehan.voicecontrol.ACTION_TAP_RESULT"
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_GESTURE -> {
                    when (intent.getStringExtra("gesture")) {
                        "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
                        "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
                        "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                    }
                }
                ACTION_TAP -> {
                    val target = intent.getStringExtra("target") ?: return
                    val found = findAndClick(target)
                    sendTapResult(target, found)
                }
                ACTION_TYPE -> {
                    val text = intent.getStringExtra("text") ?: return
                    val found = findFocusedAndType(text)
                    sendTapResult(text, found)
                }
            }
        }
    }

    private fun sendTapResult(target: String, success: Boolean) {
        val resultIntent = Intent(ACTION_TAP_RESULT)
        resultIntent.putExtra("target", target)
        resultIntent.putExtra("success", success)
        resultIntent.setPackage(packageName)
        sendBroadcast(resultIntent)
    }

    private fun findAndClick(targetText: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeByText(root, targetText.lowercase())
        if (node != null) {
            val clickable = findClickableAncestor(node)
            return clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
        }
        return false
    }

    private fun findFocusedAndType(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val arguments = Bundle()
        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text
        )
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun findNodeByText(node: AccessibilityNodeInfo?, query: String): AccessibilityNodeInfo? {
        if (node == null) return null

        val nodeText = node.text?.toString()?.lowercase() ?: ""
        val nodeDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (nodeText.contains(query) || nodeDesc.contains(query) ||
            (query.contains(nodeText) && nodeText.isNotEmpty()) ) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findNodeByText(child, query)
            if (result != null) return result
        }
        return null
    }

    private fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter().apply {
            addAction(ACTION_GESTURE)
            addAction(ACTION_TAP)
            addAction(ACTION_TYPE)
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}
