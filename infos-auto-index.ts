// ABUESA-IA — fonction Supabase « infos-auto » / bright-action (v5.00 : + validation des mots proposés ; v4.91 : réessaie si Groq dit 429)
// Chaque matin, l'IA cherche sur internet les nouvelles annonces publiques du Togo
// (concours, examens, bourses, emplois publics), vérifie que chaque lien existe vraiment,
// puis les ajoute dans la table infos_utiles avec auto = true.
//
// Appels acceptés :
//   - par la tâche programmée (pg_cron) :  .../functions/v1/infos-auto?key=CODE_ADMIN
//   - par l'espace admin du site :        POST { action: "run" | "delete" | "mots_list" | "mot_valider" | "mot_refuser", id?, code }
// Secrets utilisés (déjà présents dans Supabase) : GROQ_API_KEY, ADMIN_CODE.
// SUPABASE_URL et SUPABASE_SERVICE_ROLE_KEY sont fournis automatiquement par Supabase.

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "content-type, authorization, apikey, x-client-info",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
};
const json = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), { status, headers: { ...CORS, "Content-Type": "application/json" } });

const SB_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SB_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const GROQ = Deno.env.get("GROQ_API_KEY") ?? "";
const clean = (s: string) => String(s || "").trim().replace(/[.,;:\s]+$/, "");
const ADMIN = clean(Deno.env.get("ADMIN_CODE") ?? "");
const CATS = ["Concours", "Examen", "Bourse", "Emploi", "Info"];
const MAX_NEW = 6;

function db(path: string, init: RequestInit = {}) {
  return fetch(`${SB_URL}/rest/v1/${path}`, {
    ...init,
    headers: { apikey: SB_KEY, Authorization: `Bearer ${SB_KEY}`, "Content-Type": "application/json", ...(init.headers || {}) },
  });
}
const norm = (s: string) =>
  String(s || "").toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/[^a-z0-9]+/g, " ").trim();

async function linkWorks(url: string): Promise<boolean> {
  try {
    const ctl = new AbortController();
    const t = setTimeout(() => ctl.abort(), 10000);
    const r = await fetch(url, { method: "GET", redirect: "follow", signal: ctl.signal, headers: { "User-Agent": "Mozilla/5.0 (ABUESA-IA)" } });
    clearTimeout(t);
    try { await r.body?.cancel(); } catch (_) { /* rien */ }
    return r.status >= 200 && r.status < 400;
  } catch (_) {
    return false;
  }
}

let lastRaw = "";
async function askGroq(model: string, today: string) {
  const prompt = `Nous sommes le ${today}. Tu es l'assistant d'ABUESA-IA, une application pour les habitants du Togo (région des Plateaux : Wawa, Badou, Atakpamé, Kpalimé).
Fais plusieurs recherches sur internet, par exemple :
- « Togo concours fonction publique ${today.slice(0, 4)} »
- « Togo inscriptions BAC II BEPC CEPD ${today.slice(0, 4)}-${Number(today.slice(0, 4)) + 1} »
- « Togo recrutement avis de concours »
- « Togo bourses jeunes formation gratuite »
- « site:gouv.tg concours » et « site:gouv.tg avis »
Garde les annonces ENCORE VALABLES aujourd'hui : concours ou recrutements dont les dossiers sont encore ouverts, inscriptions aux examens en cours ou à venir, résultats publiés récemment, bourses ouvertes.
Ne garde que des informations vraies, lues sur une page que tu as réellement consultée. Préfère les sites officiels (.gouv.tg), puis la presse togolaise sérieuse (republicoftogo.com, togofirst.com, icilome.com, etc.).
Ta réponse finale doit être UNIQUEMENT un tableau JSON, sans texte avant ni après, avec au maximum 8 éléments :
[{"categorie":"Concours|Examen|Bourse|Emploi|Info","titre":"titre court en français","description":"1 ou 2 phrases : ce qu'il faut savoir (conditions, lieu, comment s'inscrire)","lien":"https://adresse exacte de la page source","date_limite":"AAAA-MM-JJ ou null"}]
Si vraiment tu ne trouves rien de sûr, réponds [].`;
  const r = await fetch("https://api.groq.com/openai/v1/chat/completions", {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${GROQ}` },
    body: JSON.stringify({
      model,
      messages: [{ role: "user", content: prompt }],
      tools: [{ type: "browser_search" }],
      tool_choice: "required",
      reasoning_effort: "medium",
      temperature: 0.3,
      max_completion_tokens: 2000,
    }),
  });
  if (!r.ok) {
    const wait = Number(r.headers.get("retry-after") || "0");
    const err: any = new Error(`Groq ${r.status}`);
    err.status = r.status; err.wait = Number.isFinite(wait) ? wait : 0;
    throw err;
  }
  const d = await r.json();
  const text: string = d?.choices?.[0]?.message?.content ?? "";
  lastRaw = text.slice(0, 400);
  const m = text.replace(/```(?:json)?/g, "").match(/\[[\s\S]*\]/);
  if (!m) return [];
  try {
    const arr = JSON.parse(m[0]);
    return Array.isArray(arr) ? arr : [];
  } catch (_) {
    return [];
  }
}

async function run() {
  const today = new Date().toISOString().slice(0, 10);
  let found: any[] = [];
  let modelUsed = "";
  let lastErr: any = null;
  // On essaie le grand modèle, puis le petit ; si Groq dit « trop de demandes » (429), on attend un peu et on réessaie.
  const tries = ["openai/gpt-oss-120b", "openai/gpt-oss-20b", "openai/gpt-oss-20b"];
  for (let i = 0; i < tries.length; i++) {
    try {
      found = await askGroq(tries[i], today);
      modelUsed = tries[i];
      lastErr = null;
      break;
    } catch (e: any) {
      lastErr = e;
      if (e?.status === 429 && i < tries.length - 1) {
        const ms = Math.min(Math.max((e.wait || 0) * 1000, 15000), 25000);
        await new Promise((res) => setTimeout(res, ms));
      }
    }
  }
  if (lastErr) throw lastErr;

  // annonces déjà connues (pour ne pas publier deux fois la même)
  const ex = await db("infos_utiles?select=titre,lien&order=created_at.desc&limit=300");
  const existing: any[] = ex.ok ? await ex.json() : [];
  const seenLinks = new Set(existing.map((x) => String(x.lien || "").replace(/\/$/, "")));
  const seenTitles = new Set(existing.map((x) => norm(x.titre)));

  const added: any[] = [];
  const skipped: any[] = [];
  for (const it of found) {
    if (added.length >= MAX_NEW) break;
    const titre = String(it?.titre || "").trim().slice(0, 200);
    const lien = String(it?.lien || "").trim();
    const description = String(it?.description || "").trim().slice(0, 600);
    const categorie = CATS.includes(it?.categorie) ? it.categorie : "Info";
    let date_limite: string | null = /^\d{4}-\d{2}-\d{2}$/.test(String(it?.date_limite)) ? String(it.date_limite) : null;
    if (!titre || !/^https:\/\//i.test(lien)) { skipped.push({ titre, raison: "titre ou lien manquant" }); continue; }
    if (date_limite && date_limite < today) { skipped.push({ titre, raison: "date limite passée" }); continue; }
    if (seenLinks.has(lien.replace(/\/$/, "")) || seenTitles.has(norm(titre))) { skipped.push({ titre, raison: "déjà publiée" }); continue; }
    if (!(await linkWorks(lien))) { skipped.push({ titre, raison: "lien qui ne marche pas" }); continue; }
    let source = "";
    try { source = new URL(lien).hostname.replace(/^www\./, ""); } catch (_) { /* rien */ }
    const row = { categorie, titre, description, lien, date_limite, auto: true, source };
    const ins = await db("infos_utiles", { method: "POST", headers: { Prefer: "return=minimal" }, body: JSON.stringify(row) });
    if (ins.ok) {
      added.push(row);
      seenLinks.add(lien.replace(/\/$/, ""));
      seenTitles.add(norm(titre));
    } else {
      skipped.push({ titre, raison: "erreur base " + ins.status });
    }
  }
  return { ok: true, date: today, modele: modelUsed, trouvees: found.length, ajoutees: added.length, annonces: added, ignorees: skipped, ...(found.length ? {} : { reponse_ia: lastRaw }) };
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS });
  if (!SB_URL || !SB_KEY || !GROQ || !ADMIN) return json({ error: "Secrets manquants (GROQ_API_KEY, ADMIN_CODE)." }, 500);

  const url = new URL(req.url);
  let body: any = {};
  if (req.method === "POST") { try { body = await req.json(); } catch (_) { body = {}; } }
  const code = clean(body.code ?? url.searchParams.get("key") ?? "");
  if (code !== ADMIN) return json({ error: "Code incorrect" }, 403);

  try {
    // --- Mots proposés par les visiteurs (table mots_proposes) ---
    if (body.action === "mots_list") {
      const r = await db("mots_proposes?select=id,ikposso,francais,anglais,exemple,origine,created_at&order=created_at.desc&limit=100");
      if (!r.ok) return json({ error: r.status === 404 ? "La table mots_proposes n'existe pas encore : passe le fichier supabase_mots_proposes.sql." : "Lecture impossible (" + r.status + ")" }, 500);
      return json({ ok: true, mots: await r.json() });
    }
    if (body.action === "mot_valider") {
      const id = Number(body.id);
      const cut = (x: unknown, n: number) => String(x ?? "").trim().slice(0, n);
      const ak = cut(body.ak, 60), en = cut(body.en, 120);
      const fr = cut(body.fr, 120) || en;
      if (!Number.isFinite(id) || !ak || !fr) return json({ error: "Il faut le mot ikposso et son sens" }, 400);
      let sans_anglais = false;
      let ins = await db("dictionary_words", { method: "POST", headers: { Prefer: "return=minimal" }, body: JSON.stringify(en ? { fr, ak, en } : { fr, ak }) });
      if (!ins.ok && en) {   // colonne « en » absente : on garde au moins le mot
        ins = await db("dictionary_words", { method: "POST", headers: { Prefer: "return=minimal" }, body: JSON.stringify({ fr, ak }) });
        sans_anglais = true;
      }
      if (!ins.ok) return json({ error: "Ajout au dictionnaire impossible (" + ins.status + ")" }, 500);
      await db(`mots_proposes?id=eq.${id}`, { method: "DELETE" });
      return json({ ok: true, sans_anglais });
    }
    if (body.action === "mot_refuser") {
      const id = Number(body.id);
      if (!Number.isFinite(id)) return json({ error: "id manquant" }, 400);
      const r = await db(`mots_proposes?id=eq.${id}`, { method: "DELETE" });
      return r.ok ? json({ ok: true }) : json({ error: "Suppression impossible" }, 500);
    }
    if (body.action === "delete") {
      const id = Number(body.id);
      if (!Number.isFinite(id)) return json({ error: "id manquant" }, 400);
      const r = await db(`infos_utiles?id=eq.${id}`, { method: "DELETE" });
      return r.ok ? json({ ok: true }) : json({ error: "Suppression impossible" }, 500);
    }
    return json(await run());
  } catch (e) {
    return json({ error: "La recherche a échoué. Réessaie plus tard." , detail: String((e as Error)?.message || e).slice(0, 120) }, 500);
  }
});
