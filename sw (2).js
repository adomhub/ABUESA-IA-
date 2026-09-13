// ABUESA — Service Worker
// Met en cache l'app shell pour un fonctionnement hors-ligne de base.
// Alexi : incrémente CACHE_NAME à chaque mise à jour majeure du contenu
// pour forcer les téléphones à retélécharger la nouvelle version.
const CACHE_NAME = 'abuesa-v1';
const APP_SHELL = [
  './',
  './index.html',
  './manifest.json',
  './icône-192.png',
  './icône-512.png',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL))
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    )
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  // Ne jamais mettre en cache les appels API (Groq) ni les tuiles de carte —
  // seulement l'app shell statique.
  if (url.origin !== self.location.origin) return;

  event.respondWith(
    caches.match(event.request).then((cached) => {
      const fetchPromise = fetch(event.request)
        .then((networkResponse) => {
          if (networkResponse && networkResponse.status === 200) {
            const clone = networkResponse.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
          }
          return networkResponse;
        })
        .catch(() => cached);
      return cached || fetchPromise;
    })
  );
});
