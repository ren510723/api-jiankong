(function () {
    'use strict';

    const STORAGE_KEY = 'api_balance_config';
    const BALANCE_CACHE_KEY = 'api_balance_cache';

    const els = {
        balanceAmount: document.getElementById('balanceAmount'),
        balanceUnit: document.getElementById('balanceUnit'),
        updateTime: document.getElementById('updateTime'),
        statusValue: document.getElementById('statusValue'),
        nextRefresh: document.getElementById('nextRefresh'),
        refreshBtn: document.getElementById('refreshBtn'),
        toggleSettings: document.getElementById('toggleSettings'),
        settingsCard: document.getElementById('settingsCard'),
        settingsForm: document.getElementById('settingsForm'),
        apiUrl: document.getElementById('apiUrl'),
        apiKeyHeader: document.getElementById('apiKeyHeader'),
        apiKey: document.getElementById('apiKey'),
        jsonPath: document.getElementById('jsonPath'),
        currencyUnit: document.getElementById('currencyUnit'),
        refreshInterval: document.getElementById('refreshInterval'),
        useBearer: document.getElementById('useBearer'),
        testBtn: document.getElementById('testBtn'),
        testResult: document.getElementById('testResult'),
        toast: document.getElementById('toast')
    };

    let config = loadConfig();
    let refreshTimer = null;
    let nextRefreshTime = null;

    populateSettings();

    if (config && config.apiUrl && config.apiKey) {
        fetchBalance();
        startAutoRefresh();
        showCachedBalance();
    }

    els.refreshBtn.addEventListener('click', function () {
        fetchBalance(true);
    });

    els.toggleSettings.addEventListener('click', function () {
        els.settingsCard.classList.toggle('hidden');
    });

    els.settingsForm.addEventListener('submit', function (e) {
        e.preventDefault();
        saveConfig();
    });

    els.testBtn.addEventListener('click', function () {
        const tempConfig = getFormConfig();
        if (!tempConfig.apiUrl || !tempConfig.apiKey) {
            showToast('请先填写 API 地址和 Key', 'error');
            return;
        }
        testConnection(tempConfig);
    });

    if ('serviceWorker' in navigator) {
        window.addEventListener('load', function () {
            navigator.serviceWorker.register('service-worker.js').catch(function (err) {
                console.warn('SW 注册失败:', err);
            });
        });
    }

    function loadConfig() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    function getFormConfig() {
        return {
            apiUrl: els.apiUrl.value.trim(),
            apiKeyHeader: els.apiKeyHeader.value.trim() || 'X-API-Key',
            apiKey: els.apiKey.value.trim(),
            jsonPath: els.jsonPath.value.trim() || 'balance',
            currencyUnit: els.currencyUnit.value.trim() || '¥',
            refreshInterval: parseInt(els.refreshInterval.value, 10) || 60,
            useBearer: els.useBearer.checked
        };
    }

    function populateSettings() {
        if (!config) return;
        els.apiUrl.value = config.apiUrl || '';
        els.apiKeyHeader.value = config.apiKeyHeader || 'X-API-Key';
        els.apiKey.value = config.apiKey || '';
        els.jsonPath.value = config.jsonPath || 'balance';
        els.currencyUnit.value = config.currencyUnit || '¥';
        els.refreshInterval.value = config.refreshInterval || 60;
        els.useBearer.checked = !!config.useBearer;
    }

    function saveConfig() {
        config = getFormConfig();
        if (!config.apiUrl || !config.apiKey) {
            showToast('请填写 API 地址和 Key', 'error');
            return;
        }
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(config));
            showToast('配置已保存', 'success');
            els.settingsCard.classList.add('hidden');
            fetchBalance(true);
            startAutoRefresh();
        } catch (e) {
            showToast('保存失败：' + e.message, 'error');
        }
    }

    function startAutoRefresh() {
        if (refreshTimer) clearInterval(refreshTimer);
        const interval = (config.refreshInterval || 60) * 1000;
        nextRefreshTime = Date.now() + interval;
        refreshTimer = setInterval(function () {
            fetchBalance();
            nextRefreshTime = Date.now() + interval;
        }, interval);
    }

    setInterval(function () {
        if (nextRefreshTime) {
            const remaining = Math.max(0, Math.ceil((nextRefreshTime - Date.now()) / 1000));
            els.nextRefresh.textContent = remaining + ' 秒';
        }
    }, 1000);

    function buildHeaders(cfg) {
        const headers = { 'Accept': 'application/json' };
        if (cfg.useBearer) {
            headers['Authorization'] = 'Bearer ' + cfg.apiKey;
        } else {
            headers[cfg.apiKeyHeader] = cfg.apiKey;
        }
        return headers;
    }

    function fetchBalance(force) {
        if (!config || !config.apiUrl) {
            els.balanceAmount.textContent = '--';
            els.balanceUnit.textContent = '请先配置';
            els.statusValue.textContent = '未配置';
            return;
        }

        els.refreshBtn.classList.add('spin');
        els.statusValue.textContent = '加载中...';

        fetch(config.apiUrl, {
            method: 'GET',
            headers: buildHeaders(config),
            cache: force ? 'no-store' : 'default'
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (data) {
                const balance = extractValue(data, config.jsonPath);
                if (balance === null || balance === undefined || isNaN(parseFloat(balance))) {
                    throw new Error('无法解析余额，路径：' + config.jsonPath);
                }
                updateDisplay(balance);
                cacheBalance(balance);
                els.statusValue.textContent = '✓ 正常';
            })
            .catch(function (err) {
                els.statusValue.textContent = '✗ 错误';
                els.balanceAmount.textContent = '⚠';
                console.error('查询余额失败:', err);
                showCachedBalance(true);
            })
            .finally(function () {
                setTimeout(function () {
                    els.refreshBtn.classList.remove('spin');
                }, 500);
            });
    }

    function testConnection(cfg) {
        els.testResult.innerHTML = '<div class="status-loading">测试中...</div>';
        fetch(cfg.apiUrl, {
            method: 'GET',
            headers: buildHeaders(cfg)
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (data) {
                const balance = extractValue(data, cfg.jsonPath);
                const preview = JSON.stringify(data, null, 2);
                els.testResult.innerHTML =
                    '<div class="status-success">✓ 连接成功！余额：' + (balance !== null ? balance : '未找到') + '</div>' +
                    '<details><summary>查看返回数据</summary><pre class="json-preview">' + escapeHtml(preview) + '</pre></details>';
            })
            .catch(function (err) {
                els.testResult.innerHTML = '<div class="status-error">✗ 连接失败：' + err.message + '</div>';
            });
    }

    function updateDisplay(balance) {
        const num = parseFloat(balance);
        els.balanceAmount.textContent = formatNumber(num);
        els.balanceUnit.textContent = config.currencyUnit || '';
        els.updateTime.textContent = '更新于 ' + formatTime(new Date());
    }

    function cacheBalance(balance) {
        try {
            localStorage.setItem(BALANCE_CACHE_KEY, JSON.stringify({
                balance: balance,
                timestamp: Date.now()
            }));
        } catch (e) { }
    }

    function showCachedBalance(isOffline) {
        try {
            const raw = localStorage.getItem(BALANCE_CACHE_KEY);
            if (!raw) return;
            const cache = JSON.parse(raw);
            if (cache.balance !== undefined) {
                els.balanceAmount.textContent = formatNumber(parseFloat(cache.balance));
                els.balanceUnit.textContent = (config ? config.currencyUnit : '') + (isOffline ? ' (缓存)' : '');
                els.updateTime.textContent = '缓存于 ' + formatTime(new Date(cache.timestamp));
            }
        } catch (e) { }
    }

    function extractValue(obj, path) {
        if (!path) return obj;
        const tokens = path.replace(/\[(\d+)\]/g, '.$1').split('.').filter(Boolean);
        let current = obj;
        for (let i = 0; i < tokens.length; i++) {
            if (current === null || current === undefined) return null;
            const key = /^\d+$/.test(tokens[i]) ? parseInt(tokens[i], 10) : tokens[i];
            current = current[key];
        }
        return current;
    }

    function formatNumber(num) {
        if (isNaN(num)) return '--';
        return num.toLocaleString('zh-CN', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    }

    function formatTime(date) {
        return date.toLocaleString('zh-CN', {
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    }

    function showToast(message, type) {
        els.toast.textContent = message;
        els.toast.className = 'toast toast-' + (type || 'info');
        els.toast.style.opacity = '1';
        setTimeout(function () {
            els.toast.style.opacity = '0';
        }, 2500);
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }
})();
