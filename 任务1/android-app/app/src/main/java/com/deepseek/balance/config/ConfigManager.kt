package com.deepseek.balance.config

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * 配置管理类：负责保存和读取用户配置
 * 使用 SharedPreferences 实现持久化
 */
object ConfigManager {
    private const val PREF_NAME = "deepseek_widget_config"
    private const val KEY_API_URL = "api_url"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_JSON_PATH = "json_path"
    private const val KEY_CURRENCY = "currency"
    private const val KEY_REFRESH_INTERVAL = "refresh_interval"

    // DeepSeek 默认配置
    const val DEFAULT_API_URL = "https://api.deepseek.com/user/balance"
    const val DEFAULT_JSON_PATH = "balance_infos[0].total_balance"
    const val DEFAULT_CURRENCY = "¥"
    const val DEFAULT_REFRESH_INTERVAL = 30  // 分钟

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isConfigured(context: Context): Boolean {
        val prefs = getPrefs(context)
        val url = prefs.getString(KEY_API_URL, null)
        val key = prefs.getString(KEY_API_KEY, null)
        return !url.isNullOrBlank() && !key.isNullOrBlank()
    }

    fun getApiUrl(context: Context): String =
        getPrefs(context).getString(KEY_API_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL

    fun getApiKey(context: Context): String =
        getPrefs(context).getString(KEY_API_KEY, "") ?: ""

    fun getJsonPath(context: Context): String =
        getPrefs(context).getString(KEY_JSON_PATH, DEFAULT_JSON_PATH) ?: DEFAULT_JSON_PATH

    fun getCurrency(context: Context): String =
        getPrefs(context).getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY

    fun getRefreshInterval(context: Context): Int =
        getPrefs(context).getInt(KEY_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL)

    fun save(
        context: Context,
        apiUrl: String,
        apiKey: String,
        jsonPath: String,
        currency: String,
        refreshInterval: Int
    ) {
        getPrefs(context).edit().apply {
            putString(KEY_API_URL, apiUrl)
            putString(KEY_API_KEY, apiKey)
            putString(KEY_JSON_PATH, jsonPath)
            putString(KEY_CURRENCY, currency)
            putInt(KEY_REFRESH_INTERVAL, refreshInterval)
            apply()
        }
    }

    /**
     * 从 JSON 响应中按路径提取余额
     * 支持格式：balance、data.balance、balance_infos[0].total_balance
     */
    fun extractBalance(json: String, path: String): Double? {
        return try {
            val jsonObj = JSONObject(json)
            val parts = path.replace(Regex("\\[(\\d+)\\]"), ".$1").split(".")
            var current: Any? = jsonObj
            for (part in parts) {
                if (current == null) return null
                current = if (current is JSONObject) {
                    val key = if (part.toIntOrNull() != null) part.toInt() else part
                    current.opt(key.toString())
                } else if (current is org.json.JSONArray) {
                    val idx = part.toIntOrNull() ?: return null
                    current.opt(idx)
                } else {
                    return null
                }
                if (current == null || current == JSONObject.NULL) return null
            }
            current.toString().toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
