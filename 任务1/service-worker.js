const CACHE_NAME = 'api-balance-monitor-v1';
const PRECACHE_URLS = [
    './',
    'index.html',
    'app.js',
    'styles.css',
    'manifest.json'
];

self.addEventListener('install', function (event) {
    event.waitUntil(
        caches.open(CACHE_NAME).then(function (cache) {
            return cache.addAll(PRECACHE_URLS);
        }).then(function () {
            return self.skipWaiting();
        })
    );
});

self.addEventListener('activate', function (event) {
    event.waitUntil(
        caches.keys().then(function (keys) {
            return Promise.all(keys.map(function (key) {
                if (key !== CACHE_NAME) {
                    return caches.delete(key);
                }
            }));
        }).then(function () {
            return self.clients.claim();
        })
    );
});

self.addEventListener('fetch', function (event) {
    const url = new URL(event.request.url);

    if (event.request.method === 'GET' && url.origin === location.origin) {
        event.respondWith(
            caches.match(event.request).then(function (cached) {
                const networkFetch = fetch(event.request).then(function (response) {
                    const clone = response.clone();
                    caches.open(CACHE_NAME).then(function (cache) {
                        cache.put(event.request, clone);
                    });
                    return response;
                }).catch(function () {
                    return cached;
                });
                return cached || networkFetch;
            })
        );
    }
});
