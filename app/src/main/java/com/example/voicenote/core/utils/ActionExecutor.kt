package com.example.voicenote.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.voicenote.data.model.CommunicationType
import com.example.voicenote.data.model.Task

class ActionExecutor(private val context: Context) {

    fun executeTaskAction(task: Task) {
        val phone = task.assignedContactPhone ?: return
        val message = "Hi ${task.assignedContactName}, I have assigned you a task: ${task.description}"

        when (task.communicationType) {
            CommunicationType.CALL -> makeCall(phone)
            CommunicationType.SMS -> sendSms(phone, message)
            CommunicationType.WHATSAPP -> openWhatsApp(phone, message)
            CommunicationType.SLACK -> openSlack(task.customUrl)
            CommunicationType.MEET -> openMeet(task.customUrl)
            else -> Toast.makeText(context, "No action type defined", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeCall(phone: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phone")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
        }
    }

    private fun sendSms(phone: String, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openWhatsApp(phone: String, message: String) {
        val url = "https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openSlack(url: String?) {
        val slackUrl = url ?: "slack://open"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(slackUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openMeet(url: String?) {
        val meetUrl = url ?: "https://meet.google.com"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(meetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
