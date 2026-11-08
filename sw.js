// ABUESA-IA — Service Worker
// 1) Garde l'app en cache pour un fonctionnement hors-ligne (réseau d'abord, cache en secours)
// 2) Reçoit les notifications push et ouvre l'app quand on touche la notification
// Alexi : change le numéro de CACHE_NAME à chaque nouvelle version pour forcer la mise à jour.
const CACHE_NAME = 'abuesa-v34';
// v4.86 : les photos venant de Supabase sont gardées sur le téléphone (pour ne pas les retélécharger à chaque visite)
const MEDIA_CACHE = 'abuesa-media-v1';
const MEDIA_MAX = 150;
const APP_SHELL = [
  './',
  './index.html',
  './manifest.json',
  './icon-192.png',
  './icon-512.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL).catch(() => {}))
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME && k !== MEDIA_CACHE).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

// Réseau d'abord : l'utilisateur voit toujours la dernière version ; le cache sert seulement hors connexion.
self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  // Photos publiques de Supabase (galerie, fil, partenaires) : cache d'abord. Une photo ne change jamais une fois envoyée.
  if (url.hostname.endsWith('.supabase.co') && url.pathname.indexOf('/storage/v1/object/public/') !== -1
      && req.destination === 'image' && !req.headers.has('range')) {
    event.respondWith(mediaFirst(url.href, req));
    return;
  }
  if (url.origin !== self.location.origin) return; // Supabase (données), Jitsi, etc. : on ne touche pas
  // v4.98 : pour les pages et le code (html, js, json), on redemande toujours à GitHub s'il y a du nouveau
  // (sinon le navigateur garde l'ancienne version jusqu'à 10 minutes après un dépôt).
  const fresh = req.mode === 'navigate' || /\.(html|js|json)$/.test(url.pathname) || url.pathname.endsWith('/');
  event.respondWith(
    fetch(fresh ? new Request(req, { cache: 'no-cache' }) : req)
      .then((res) => {
        if (res && res.ok) {
          const copy = res.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(req, copy)).catch(() => {});
        }
        return res;
      })
      .catch(() => caches.match(req).then((r) => r || caches.match('./index.html')))
  );
});

// ===== Notifications push =====
self.addEventListener('push', (event) => {
  let data = {};
  try { data = event.data ? event.data.json() : {}; } catch (e) {
    data = { title: 'ABUESA-IA', body: event.data ? event.data.text() : '' };
  }
  const title = data.title || 'ABUESA-IA';
  const options = {
    body: data.body || '',
    icon: 'icon-192.png',
    badge: 'icon-192.png',
    data: { url: data.url || './' },
    tag: data.tag || 'abuesa',
    renotify: true
  };
  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const target = new URL((event.notification.data && event.notification.data.url) || './', self.registration.scope).href;
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((list) => {
      for (const c of list) {
        if (c.url.startsWith(self.registration.scope) && 'focus' in c) { c.navigate(target).catch(() => {}); return c.focus(); }
      }
      return self.clients.openWindow(target);
    })
  );
});

async function mediaFirst(href, req) {
  try {
    const cache = await caches.open(MEDIA_CACHE);
    const hit = await cache.match(href);
    if (hit) return hit;
    const res = await fetch(href, { mode: 'cors', credentials: 'omit' });
    if (res && res.status === 200) {
      await cache.put(href, res.clone());
      const keys = await cache.keys();
      if (keys.length > MEDIA_MAX) await Promise.all(keys.slice(0, keys.length - MEDIA_MAX).map((k) => cache.delete(k)));
    }
    return res;
  } catch (e) {
    return fetch(req);
  }
}
