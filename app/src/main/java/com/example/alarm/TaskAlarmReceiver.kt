package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.RepeatMode
import java.util.Calendar

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, 0)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "یادآوری کار"
        val repeatMode = intent.getStringExtra(EXTRA_REPEAT_MODE) ?: RepeatMode.NONE.name
        val triggerMillis = intent.getLongExtra(EXTRA_TRIGGER_MILLIS, 0L)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "task_reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "یادآوری کارها",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "هشدار و یادآوری لیست کارها"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("یادآوری کار")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(alarmSound)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(taskId, notification)

        // If repeating, schedule next occurrence
        if (repeatMode != RepeatMode.NONE.name && triggerMillis > 0) {
            val nextMillis = calculateNextTriggerMillis(triggerMillis, repeatMode)
            if (nextMillis > System.currentTimeMillis()) {
                TaskAlarmManager.scheduleAlarm(context, taskId, taskTitle, nextMillis, repeatMode)
            }
        }
    }

    private fun calculateNextTriggerMillis(currentMillis: Long, repeatMode: String): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentMillis }
        when (repeatMode) {
            RepeatMode.WEEKLY.name -> cal.add(Calendar.DAY_OF_YEAR, 7)
            RepeatMode.MONTHLY.name -> cal.add(Calendar.MONTH, 1)
            RepeatMode.YEARLY.name -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_REPEAT_MODE = "extra_repeat_mode"
        const val EXTRA_TRIGGER_MILLIS = "extra_trigger_millis"
    }
}
