package com.deepseek.balance.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.deepseek.balance.config.ConfigManager

/**
 * 定时刷新小组件的广播接收器
 * 使用 AlarmManager 实现精确定时
 */
class WidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_UPDATE) {
            // 刷新所有小组件
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, BalanceWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                BalanceWidgetProvider.refreshAllWidgets(context, manager, ids)
            }
            // 调度下一次刷新
            scheduleNextUpdate(context)
        }
    }

    companion object {
        const val ACTION_UPDATE = "com.deepseek.balance.ACTION_UPDATE"

        fun scheduleNextUpdate(context: Context) {
            val intervalMinutes = ConfigManager.getRefreshInterval(context).coerceAtLeast(5)
            val intervalMillis = intervalMinutes * 60 * 1000L

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
                action = ACTION_UPDATE
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pending = PendingIntent.getBroadcast(context, 100, intent, flags)

            val triggerAt = System.currentTimeMillis() + intervalMillis
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    intervalMillis,
                    pending
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    intervalMillis,
                    pending
                )
            }
        }

        fun cancelUpdates(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
                action = ACTION_UPDATE
            }
            val pending = PendingIntent.getBroadcast(
                context, 100, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pending)
        }
    }
}
