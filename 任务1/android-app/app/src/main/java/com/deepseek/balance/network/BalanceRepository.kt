package com.deepseek.balance.network

import android.content.Context
import com.deepseek.balance.config.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 余额数据仓库：处理网络请求并解析余额
 */
object BalanceRepository {

    sealed class Result {
        data class Success(val balance: Double, val currency: String) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * 异步获取余额
     * @param context 上下文
     * @return Result.Success 包含余额和货币，Result.Error 包含错误信息
     */
    suspend fun fetchBalance(context: Context): Result = withContext(Dispatchers.IO) {
        if (!ConfigManager.isConfigured(context)) {
            return@withContext Result.Error("未配置")
        }

        val apiUrl = ConfigManager.getApiUrl(context)
        val apiKey = ConfigManager.getApiKey(context)
        val jsonPath = ConfigManager.getJsonPath(context)
        val currency = ConfigManager.getCurrency(context)

        try {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("User-Agent", "DeepSeekBalanceWidget/1.0")

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.Error("HTTP $responseCode")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val balance = ConfigManager.extractBalance(response, jsonPath)
            if (balance == null) {
                Result.Error("解析失败")
            } else {
                Result.Success(balance, currency)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "网络错误")
        }
    }
}
