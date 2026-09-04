package com.hireflow.app.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hireflow.app.MainActivity

const val INTERVIEW_CHANNEL = "interview_reminders"

fun createInterviewChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            INTERVIEW_CHANNEL,
            "Nhắc lịch phỏng vấn",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Thông báo trước các cuộc phỏng vấn đã lên lịch" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

fun scheduleInterviewReminder(
    context: Context,
    interviewId: Long,
    candidateName: String,
    position: String,
    interviewAt: Long
) {
    val triggerAt = (interviewAt - 15 * 60_000).coerceAtLeast(System.currentTimeMillis() + 5_000)
    val intent = Intent(context, InterviewReminderReceiver::class.java).apply {
        putExtra("candidate", candidateName)
        putExtra("position", position)
        putExtra("notification_id", interviewId.toInt())
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        interviewId.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
}

class InterviewReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        createInterviewChannel(context)
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val candidate = intent.getStringExtra("candidate") ?: "Ứng viên"
        val position = intent.getStringExtra("position") ?: "Vị trí tuyển dụng"
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, INTERVIEW_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Sắp đến lịch phỏng vấn")
            .setContentText("$candidate · $position · còn 15 phút")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        NotificationManagerCompat.from(context).notify(
            intent.getIntExtra("notification_id", candidate.hashCode()),
            notification
        )
    }
}
