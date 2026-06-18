package com.deepseek.balance.config

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.deepseek.balance.R
import com.deepseek.balance.network.BalanceRepository
import com.deepseek.balance.widget.BalanceWidgetProvider
import com.deepseek.balance.widget.WidgetUpdateReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 配置页面：用于设置 API Key、刷新间隔等
 * 通过点击桌面小组件或桌面图标打开
 */
class ConfigActivity : AppCompatActivity() {

    private lateinit var etApiUrl: EditText
    private lateinit var etApiKey: EditText
    private lateinit var etCurrency: EditText
    private lateinit var etRefreshInterval: EditText
    private lateinit var btnSave: Button
    private lateinit var btnTest: Button
    private lateinit var tvTestResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        initViews()
        loadConfig()
    }

    private fun initViews() {
        etApiUrl = findViewById(R.id.etApiUrl)
        etApiKey = findViewById(R.id.etApiKey)
        etCurrency = findViewById(R.id.etCurrency)
        etRefreshInterval = findViewById(R.id.etRefreshInterval)
        btnSave = findViewById(R.id.btnSave)
        btnTest = findViewById(R.id.btnTest)
        tvTestResult = findViewById(R.id.tvTestResult)

        btnSave.setOnClickListener { saveConfig() }
        btnTest.setOnClickListener { testConnection() }
    }

    private fun loadConfig() {
        etApiUrl.setText(ConfigManager.getApiUrl(this))
        etApiKey.setText(ConfigManager.getApiKey(this))
        etCurrency.setText(ConfigManager.getCurrency(this))
        etRefreshInterval.setText(ConfigManager.getRefreshInterval(this).toString())
    }

    private fun testConnection() {
        val url = etApiUrl.text.toString().trim()
        val key = etApiKey.text.toString().trim()

        if (url.isEmpty() || key.isEmpty()) {
            showTestResult("请填写 API 地址和 Key", false)
            return
        }

        // 临时保存以便测试
        val oldUrl = ConfigManager.getApiUrl(this)
        val oldKey = ConfigManager.getApiKey(this)
        val oldPath = ConfigManager.getJsonPath(this)
        val oldCurrency = ConfigManager.getCurrency(this)
        val oldInterval = ConfigManager.getRefreshInterval(this)

        ConfigManager.save(
            this, url, key,
            ConfigManager.DEFAULT_JSON_PATH,
            ConfigManager.DEFAULT_CURRENCY,
            oldInterval
        )

        btnTest.isEnabled = false
        showTestResult("测试中...", true)

        CoroutineScope(Dispatchers.IO).launch {
            val result = BalanceRepository.fetchBalance(this@ConfigActivity)
            withContext(Dispatchers.Main) {
                btnTest.isEnabled = true
                when (result) {
                    is BalanceRepository.Result.Success -> {
                        showTestResult(
                            "✓ 连接成功！余额：${result.currency} ${String.format("%.2f", result.balance)}",
                            true
                        )
                    }
                    is BalanceRepository.Result.Error -> {
                        // 恢复旧配置
                        ConfigManager.save(
                            this@ConfigActivity, oldUrl, oldKey, oldPath, oldCurrency, oldInterval
                        )
                        showTestResult("✗ 连接失败：${result.message}", false)
                    }
                }
            }
        }
    }

    private fun showTestResult(text: String, success: Boolean) {
        tvTestResult.text = text
        tvTestResult.setTextColor(
            if (success) getColor(R.color.widget_status_online)
            else getColor(R.color.widget_status_offline)
        )
        tvTestResult.visibility = View.VISIBLE
    }

    private fun saveConfig() {
        val url = etApiUrl.text.toString().trim()
        val key = etApiKey.text.toString().trim()
        val currency = etCurrency.text.toString().trim().ifEmpty { ConfigManager.DEFAULT_CURRENCY }
        val intervalText = etRefreshInterval.text.toString().trim()
        val interval = intervalText.toIntOrNull()?.coerceAtLeast(5) ?: 30

        if (url.isEmpty() || key.isEmpty()) {
            Toast.makeText(this, R.string.config_empty_fields, Toast.LENGTH_SHORT).show()
            return
        }

        ConfigManager.save(this, url, key, ConfigManager.DEFAULT_JSON_PATH, currency, interval)

        // 取消旧定时器，调度新定时器
        WidgetUpdateReceiver.cancelUpdates(this)
        WidgetUpdateReceiver.scheduleNextUpdate(this)

        // 立即刷新所有小组件
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(
            ComponentName(this, BalanceWidgetProvider::class.java)
        )
        if (ids.isNotEmpty()) {
            BalanceWidgetProvider.refreshAllWidgets(this, manager, ids)
        }

        Toast.makeText(this, R.string.config_saved, Toast.LENGTH_SHORT).show()
        finish()
    }
}
