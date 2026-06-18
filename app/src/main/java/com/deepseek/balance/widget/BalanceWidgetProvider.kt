package com.deepseek.balance.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.deepseek.balance.R
import com.deepseek.balance.config.ConfigActivity
import com.deepseek.balance.config.ConfigManager
import com.deepseek.balance.network.BalanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidgetLoading(context, appWidgetManager, widgetId)
        }
        refreshAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BalanceWidgetProvider::class.java))
            refreshAllWidgets(context, manager, ids)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.deepseek.balance.ACTION_REFRESH"

        fun refreshAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (!ConfigManager.isConfigured(context)) {
                for (widgetId in appWidgetIds) {
                    updateWidgetNotConfigured(context, appWidgetManager, widgetId)
                }
                return
            }

            for (widgetId in appWidgetIds) {
                updateWidgetLoading(context, appWidgetManager, widgetId)
            }

            CoroutineScope(Dispatchers.IO).launch {
                val result = BalanceRepository.fetchBalance(context)
                for (widgetId in appWidgetIds) {
                    when (result) {
                        is BalanceRepository.Result.Success -> {
                            updateWidgetSuccess(
                                context,
                                appWidgetManager,
                                widgetId,
                                result.balance,
                                result.currency
                            )
                        }
                        is BalanceRepository.Result.Error -> {
                            updateWidgetError(context, appWidgetManager, widgetId, result.message)
                        }
                    }
                }
            }
        }

        private fun updateWidgetLoading(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "--")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_loading))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_loading)
            views.setTextViewText(R.id.updateTime, "刷新中...")
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetSuccess(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            balance: Double,
            currency: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            val formatted = String.format(Locale.CHINA, "%,.2f", balance)
            views.setTextViewText(R.id.balanceAmount, formatted)
            views.setTextViewText(R.id.currencySymbol, currency)
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_online))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_online)

            val timeFormat = SimpleDateFormat("HH:mm 更新", Locale.CHINA)
            views.setTextViewText(R.id.updateTime, timeFormat.format(Date()))

            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetError(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            message: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "!")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_offline))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_offline)
            views.setTextViewText(R.id.updateTime, message)
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetNotConfigured(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "...")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_configure_first))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_offline)
            views.setTextViewText(R.id.updateTime, "点击配置")
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun setupClickListeners(context: Context, views: RemoteViews) {
            val configIntent = Intent(context, ConfigActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            val configPending = android.app.PendingIntent.getActivity(context, 0, configIntent, flags)
            views.setOnClickPendingIntent(R.id.balanceAmount, configPending)
            views.setOnClickPendingIntent(R.id.currencySymbol, configPending)
            views.setOnClickPendingIntent(R.id.updateTime, configPending)

            val refreshIntent = Intent(context, BalanceWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPending = android.app.PendingIntent.getBroadcast(context, 1, refreshIntent, flags)
            views.setOnClickPendingIntent(R.id.statusText, refreshPending)
            views.setOnClickPendingIntent(R.id.statusDot, refreshPending)
        }
    }
}
package com.deepseek.balance.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.deepseek.balance.R
import com.deepseek.balance.config.ConfigActivity
import com.deepseek.balance.config.ConfigManager
import com.deepseek.balance.network.BalanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidgetLoading(context, appWidgetManager, widgetId)
        }
        refreshAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BalanceWidgetProvider::class.java))
            refreshAllWidgets(context, manager, ids)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.deepseek.balance.ACTION_REFRESH"

        fun refreshAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (!ConfigManager.isConfigured(context)) {
                for (widgetId in appWidgetIds) {
                    updateWidgetNotConfigured(context, appWidgetManager, widgetId)
                }
                return
            }

            for (widgetId in appWidgetIds) {
                updateWidgetLoading(context, appWidgetManager, widgetId)
            }

            CoroutineScope(Dispatchers.IO).launch {
                val result = BalanceRepository.fetchBalance(context)
                for (widgetId in appWidgetIds) {
                    when (result) {
                        is BalanceRepository.Result.Success -> {
                            updateWidgetSuccess(
                                context,
                                appWidgetManager,
                                widgetId,
                                result.balance,
                                result.currency
                            )
                        }
                        is BalanceRepository.Result.Error -> {
                            updateWidgetError(context, appWidgetManager, widgetId, result.message)
                        }
                    }
                }
            }
        }

        private fun updateWidgetLoading(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "--")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_loading))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_loading)
            views.setTextViewText(R.id.updateTime, "刷新中...")
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetSuccess(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            balance: Double,
            currency: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            val formatted = String.format(Locale.CHINA, "%,.2f", balance)
            views.setTextViewText(R.id.balanceAmount, formatted)
            views.setTextViewText(R.id.currencySymbol, currency)
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_online))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_online)

            val timeFormat = SimpleDateFormat("HH:mm 更新", Locale.CHINA)
            views.setTextViewText(R.id.updateTime, timeFormat.format(Date()))

            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetError(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            message: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "!")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_offline))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_offline)
            views.setTextViewText(R.id.updateTime, message)
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetNotConfigured(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "...")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_configure_first))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_offline)
            views.setTextViewText(R.id.updateTime, "点击配置")
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun setupClickListeners(context: Context, views: RemoteViews) {
            val configIntent = Intent(context, ConfigActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            val configPending = android.app.PendingIntent.getActivity(context, 0, configIntent, flags)
            views.setOnClickPendingIntent(R.id.balanceAmount, configPending)
            views.setOnClickPendingIntent(R.id.currencySymbol, configPending)
            views.setOnClickPendingIntent(R.id.updateTime, configPending)

            val refreshIntent = Intent(context, BalanceWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPending = android.app.PendingIntent.getBroadcast(context, 1, refreshIntent, flags)
            views.setOnClickPendingIntent(R.id.statusText, refreshPending)
            views.setOnClickPendingIntent(R.id.statusDot, refreshPending)
        }
    }
}
package com.deepseek.balance.widget
package com.deepseek.balance.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentNamepackage com.deepseek.balance.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.deepseek.balance.R
import com.deepseek.balance.config.ConfigActivity
import com.deepseek.balance.config.ConfigManager
import com.deepseek.balance.network.BalanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidgetLoading(context, appWidgetManager, widgetId)
        }
        refreshAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BalanceWidgetProvider::class.java))
            refreshAllWidgets(context, manager, ids)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.deepseek.balance.ACTION_REFRESH"

        fun refreshAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (!ConfigManager.isConfigured(context)) {
                for (widgetId in appWidgetIds) {
                    updateWidgetNotConfigured(context, appWidgetManager, widgetId)
                }
                return
            }

            for (widgetId in appWidgetIds) {
                updateWidgetLoading(context, appWidgetManager, widgetId)
            }

            CoroutineScope(Dispatchers.IO).launch {
                val result = BalanceRepository.fetchBalance(context)
                for (widgetId in appWidgetIds) {
                    when (result) {
                        is BalanceRepository.Result.Success -> {
                            updateWidgetSuccess(
                                context,
                                appWidgetManager,
                                widgetId,
                                result.balance,
                                result.currency
                            )
                        }
                        is BalanceRepository.Result.Error -> {
                            updateWidgetError(context, appWidgetManager, widgetId, result.message)
                        }
                    }
                }
            }
        }

        private fun updateWidgetLoading(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "--")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_loading))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_loading)
            views.setTextViewText(R.id.updateTime, "刷新中...")
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetSuccess(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            balance: Double,
            currency: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            val formatted = String.format(Locale.CHINA, "%,.2f", balance)
            views.setTextViewText(R.id.balanceAmount, formatted)
            views.setTextViewText(R.id.currencySymbol, currency)
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_online))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_online)

            val timeFormat = SimpleDateFormat("HH:mm 更新", Locale.CHINA)
            views.setTextViewText(R.id.updateTime, timeFormat.format(Date()))

            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetError(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            message: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "!")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_offline))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_offline)
            views.setTextViewText(R.id.updateTime, message)
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetNotConfigured(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "...")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_configure_first))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_offline)
            views.setTextViewText(R.id.updateTime, "点击配置")
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun setupClickListeners(context: Context, views: RemoteViews) {
            val configIntent = Intent(context, ConfigActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            val configPending = android.app.PendingIntent.getActivity(context, 0, configIntent, flags)
            views.setOnClickPendingIntent(R.id.balanceAmount, configPending)
            views.setOnClickPendingIntent(R.id.currencySymbol, configPending)
            views.setOnClickPendingIntent(R.id.updateTime, configPending)

            val refreshIntent = Intent(context, BalanceWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPending = android.app.PendingIntent.getBroadcast(context, 1, refreshIntent, flags)
            views.setOnClickPendingIntent(R.id.statusText, refreshPending)
            views.setOnClickPendingIntent(R.id.statusDot, refreshPending)
        }
    }
}

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.deepseek.balance.R
import com.deepseek.balance.config.ConfigActivity
import com.deepseek.balance.config.ConfigManager
import com.deepseek.balance.network.BalanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidgetLoading(context, appWidgetManager, widgetId)
        }
        refreshAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BalanceWidgetProvider::class.java))
            refreshAllWidgets(context, manager, ids)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.deepseek.balance.ACTION_REFRESH"

        fun refreshAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (!ConfigManager.isConfigured(context)) {
                for (widgetId in appWidgetIds) {
                    updateWidgetNotConfigured(context, appWidgetManager, widgetId)
                }
                return
            }

            for (widgetId in appWidgetIds) {
                updateWidgetLoading(context, appWidgetManager, widgetId)
            }

            CoroutineScope(Dispatchers.IO).launch {
                val result = BalanceRepository.fetchBalance(context)
                for (widgetId in appWidgetIds) {
                    when (result) {
                        is BalanceRepository.Result.Success -> {
                            updateWidgetSuccess(
                                context,
                                appWidgetManager,
                                widgetId,
                                result.balance,
                                result.currency
                            )
                        }
                        is BalanceRepository.Result.Error -> {
                            updateWidgetError(context, appWidgetManager, widgetId, result.message)
                        }
                    }
                }
            }
        }

        private fun updateWidgetLoading(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "--")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_loading))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_loading)
            views.setTextViewText(R.id.updateTime, "刷新中...")
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetSuccess(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            balance: Double,
            currency: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            val formatted = String.format(Locale.CHINA, "%,.2f", balance)
            views.setTextViewText(R.id.balanceAmount, formatted)
            views.setTextViewText(R.id.currencySymbol, currency)
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_online))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_online)

            val timeFormat = SimpleDateFormat("HH:mm 更新", Locale.CHINA)
            views.setTextViewText(R.id.updateTime, timeFormat.format(Date()))

            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetError(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            message: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "!")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_offline))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_offline)
            views.setTextViewText(R.id.updateTime, message)
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetNotConfigured(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "...")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_configure_first))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_offline)
            views.setTextViewText(R.id.updateTime, "点击配置")
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun setupClickListeners(context: Context, views: RemoteViews) {
            val configIntent = Intent(context, ConfigActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            val configPending = android.app.PendingIntent.getActivity(context, 0, configIntent, flags)
            views.setOnClickPendingIntent(R.id.balanceAmount, configPending)
            views.setOnClickPendingIntent(R.id.currencySymbol, configPending)
            views.setOnClickPendingIntent(R.id.updateTime, configPending)

            val refreshIntent = Intent(context, BalanceWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPending = android.app.PendingIntent.getBroadcast(context, 1, refreshIntent, flags)
            views.setOnClickPendingIntent(R.id.statusText, refreshPending)
            views.setOnClickPendingIntent(R.id.statusDot, refreshPending)
        }
    }
}

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderpackage com.deepseek.balance.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.deepseek.balance.R
import com.deepseek.balance.config.ConfigActivity
import com.deepseek.balance.config.ConfigManager
import com.deepseek.balance.network.BalanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidgetLoading(context, appWidgetManager, widgetId)
        }
        refreshAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BalanceWidgetProvider::class.java))
            refreshAllWidgets(context, manager, ids)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.deepseek.balance.ACTION_REFRESH"

        fun refreshAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (!ConfigManager.isConfigured(context)) {
                for (widgetId in appWidgetIds) {
                    updateWidgetNotConfigured(context, appWidgetManager, widgetId)
                }
                return
            }

            for (widgetId in appWidgetIds) {
                updateWidgetLoading(context, appWidgetManager, widgetId)
            }

            CoroutineScope(Dispatchers.IO).launch {
                val result = BalanceRepository.fetchBalance(context)
                for (widgetId in appWidgetIds) {
                    when (result) {
                        is BalanceRepository.Result.Success -> {
                            updateWidgetSuccess(
                                context,
                                appWidgetManager,
                                widgetId,
                                result.balance,
                                result.currency
                            )
                        }
                        is BalanceRepository.Result.Error -> {
                            updateWidgetError(context, appWidgetManager, widgetId, result.message)
                        }
                    }
                }
            }
        }

        private fun updateWidgetLoading(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "--")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_loading))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_loading)
            views.setTextViewText(R.id.updateTime, "刷新中...")
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetSuccess(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            balance: Double,
            currency: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            val formatted = String.format(Locale.CHINA, "%,.2f", balance)
            views.setTextViewText(R.id.balanceAmount, formatted)
            views.setTextViewText(R.id.currencySymbol, currency)
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_online))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_online)

            val timeFormat = SimpleDateFormat("HH:mm 更新", Locale.CHINA)
            views.setTextViewText(R.id.updateTime, timeFormat.format(Date()))

            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetError(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            message: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "!")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_offline))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_offline)
            views.setTextViewText(R.id.updateTime, message)
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun updateWidgetNotConfigured(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "...")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_configure_first))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_offline)
            views.setTextViewText(R.id.updateTime, "点击配置")
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun setupClickListeners(context: Context, views: RemoteViews) {
            val configIntent = Intent(context, ConfigActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            val configPending = android.app.PendingIntent.getActivity(context, 0, configIntent, flags)
            views.setOnClickPendingIntent(R.id.balanceAmount, configPending)
            views.setOnClickPendingIntent(R.id.currencySymbol, configPending)
            views.setOnClickPendingIntent(R.id.updateTime, configPending)

            val refreshIntent = Intent(context, BalanceWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPending = android.app.PendingIntent.getBroadcast(context, 1, refreshIntent, flags)
            views.setOnClickPendingIntent(R.id.statusText, refreshPending)
            views.setOnClickPendingIntent(R.id.statusDot, refreshPending)
        }
    }
}

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.deepseek.balance.R
import com.deepseek.balance.config.ConfigActivity
import com.deepseek.balance.config.ConfigManager
import com.deepseek.balance.network.BalanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 桌面小组件 Provider
 * 负责显示余额和处理用户交互
 */
class BalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 首次添加时显示初始状态
        for (widgetId in appWidgetIds) {
            updateWidgetLoading(context, appWidgetManager, widgetId)
        }
        // 触发数据获取
        refreshAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BalanceWidgetProvider::class.java))
            refreshAllWidgets(context, manager, ids)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.deepseek.balance.ACTION_REFRESH"

        /**
         * 刷新所有小组件
         */
        fun refreshAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (!ConfigManager.isConfigured(context)) {
                for (widgetId in appWidgetIds) {
                    updateWidgetNotConfigured(context, appWidgetManager, widgetId)
                }
                return
            }

            // 显示加载状态
            for (widgetId in appWidgetIds) {
                updateWidgetLoading(context, appWidgetManager, widgetId)
            }

            // 异步获取余额
            CoroutineScope(Dispatchers.IO).launch {
                val result = BalanceRepository.fetchBalance(context)
                for (widgetId in appWidgetIds) {
                    when (result) {
                        is BalanceRepository.Result.Success -> {
                            updateWidgetSuccess(
                                context,
                                appWidgetManager,
                                widgetId,
                                result.balance,
                                result.currency
                            )
                        }
                        is BalanceRepository.Result.Error -> {
                            updateWidgetError(context, appWidgetManager, widgetId, result.message)
                        }
                    }
                }
            }
        }

        /**
         * 显示加载中状态
         */
        private fun updateWidgetLoading(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "--")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_loading))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_loading)
            views.setTextViewText(R.id.updateTime, "刷新中...")
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        /**
         * 显示余额
         */
        private fun updateWidgetSuccess(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            balance: Double,
            currency: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            val formatted = String.format(Locale.CHINA, "%,.2f", balance)
            views.setTextViewText(R.id.balanceAmount, formatted)
            views.setTextViewText(R.id.currencySymbol, currency)
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_online))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_online)

            val timeFormat = SimpleDateFormat("HH:mm 更新", Locale.CHINA)
            views.setTextViewText(R.id.updateTime, timeFormat.format(Date()))

            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        /**
         * 显示错误状态
         */
        private fun updateWidgetError(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            message: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "!")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_offline))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_offline)
            views.setTextViewText(R.id.updateTime, message)
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        /**
         * 显示未配置状态
         */
        private fun updateWidgetNotConfigured(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            views.setTextViewText(R.id.balanceAmount, "...")
            views.setTextViewText(R.id.statusText, context.getString(R.string.widget_configure_first))
            views.setInt(R.id.statusDot, "setBackgroundResource", R.drawable.status_dot_offline)
            views.setTextViewText(R.id.updateTime, "")
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        /**
         * 设置点击事件监听器
         * - 点击小组件任意位置打开配置页面
         * - 点击刷新按钮手动刷新
         */
        private fun setupClickListeners(context: Context, views: RemoteViews) {
            // 点击整个小组件打开配置页
            val configIntent = Intent(context, ConfigActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val configPending = PendingIntent.getActivity(
                context, 0, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.balanceAmount, configPending)
            views.setOnClickPendingIntent(R.id.currencySymbol, configPending)
            views.setOnClickPendingIntent(R.id.updateTime, configPending)

            // 点击状态行触发刷新
            val refreshIntent = Intent(context, BalanceWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPending = PendingIntent.getBroadcast(
                context, 1, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.statusText, refreshPending)
            views.setOnClickPendingIntent(R.id.statusDot, refreshPending)
        }
    }
}
