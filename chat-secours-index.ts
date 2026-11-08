// ABUESA-IA — fonction Supabase « chat-secours » (v5.08)
// Roue de secours du chat : quand Groq refuse (limite gratuite, modèle retiré, demande trop longue),
// ABUESA pose la même question à Cloudflare Workers AI, avec le même compte gratuit que la voix.
// Secrets déjà présents dans Supabase : CF_ACCOUNT_ID et CF_API_TOKEN (même si leur nom a un point ou une virgule en trop).
// Essais dans l'ordre : gpt-oss-120b (même famille que Groq), puis Llama 3.3 70B, puis Llama 3.1 8B.

const ALLOWED = [/^https:\/\/adomhub\.github\.io$/, /^http:\/\/localhost(:\d+)?$/];
function cors(origin: string) {
  const ok = ALLOWED.some((r) => r.test(origin));
  return {
    "Access-Control-Allow-Origin": ok ? origin : "https://adomhub.github.io",
    "Access-Control-Allow-Headers": "content-type, authorization, apikey, x-client-info",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Vary": "Origin",
  };
}
function secret(...names: string[]) {
  for (const n of names) {
    const v = Deno.env.get(n);
    if (v) return v.trim().replace(/[.,;:\s]+$/, "");
  }
  return "";
}
const ACCOUNT = secret("CF_ACCOUNT_ID", "CF_ACCOUNT_ID.", "CF_ACCOUNT_ID,");
const TOKEN = secret("CF_API_TOKEN", "CF_API_TOKEN,", "CF_API_TOKEN.");
const RUN = (model: string) => `https://api.cloudflare.com/client/v4/accounts/${ACCOUNT}/ai/run/${model}`;

// Cherche le texte de la réponse, quel que soit le format renvoyé par Cloudflare.
function textOf(d: any): string {
  const r = d?.result ?? d;
  if (typeof r?.response === "string" && r.response.trim()) return r.response;
  if (typeof r?.output_text === "string" && r.output_text.trim()) return r.output_text;
  const ch = r?.choices?.[0]?.message?.content;
  if (typeof ch === "string" && ch.trim()) return ch;
  if (Array.isArray(r?.output)) {
    const parts: string[] = [];
    for (const o of r.output) {
      if (o?.type === "message" && Array.isArray(o.content)) {
        for (const c of o.content) if (typeof c?.text === "string") parts.push(c.text);
      }
    }
    if (parts.join("").trim()) return parts.join("\n");
  }
  return "";
}

Deno.serve(async (req) => {
  const origin = req.headers.get("origin") || "";
  const H = cors(origin);
  const json = (d: unknown, s = 200) => new Response(JSON.stringify(d), { status: s, headers: { ...H, "Content-Type": "application/json" } });
  if (req.method === "OPTIONS") return new Response("ok", { headers: H });
  if (req.method !== "POST") return json({ error: "POST seulement" }, 405);
  if (!ALLOWED.some((r) => r.test(origin))) return json({ error: "Origine non autorisée" }, 403);
  if (!ACCOUNT || !TOKEN) return json({ error: "Secrets Cloudflare manquants" }, 500);

  let body: any = {};
  try { body = await req.json(); } catch (_) { body = {}; }
  const raw = Array.isArray(body.messages) ? body.messages : [];
  const msgs = raw
    .filter((m: any) => m && (m.role === "system" || m.role === "user" || m.role === "assistant"))
    .map((m: any) => ({ role: m.role, content: String(m.content ?? "").slice(0, m.role === "system" ? 24000 : 2000) }))
    .slice(-8);
  const system = msgs.filter((m: any) => m.role === "system").map((m: any) => m.content).join("\n\n");
  const convo = msgs.filter((m: any) => m.role !== "system");
  if (!convo.length || convo[convo.length - 1].role !== "user") return json({ error: "Question vide" }, 400);

  const headers = { Authorization: `Bearer ${TOKEN}`, "Content-Type": "application/json" };
  const tries: { model: string; body: unknown }[] = [
    { model: "@cf/openai/gpt-oss-120b", body: { instructions: system, input: convo, reasoning: { effort: "low" }, max_output_tokens: 1200 } },
    { model: "@cf/meta/llama-3.3-70b-instruct-fp8-fast", body: { messages: [{ role: "system", content: system }, ...convo], max_tokens: 900 } },
    { model: "@cf/meta/llama-3.1-8b-instruct", body: { messages: [{ role: "system", content: system.slice(0, 12000) }, ...convo.slice(-3)], max_tokens: 700 } },
  ];
  const errors: string[] = [];
  for (const t of tries) {
    try {
      const ctl = new AbortController();
      const tm = setTimeout(() => ctl.abort(), 45000);
      const r = await fetch(RUN(t.model), { method: "POST", headers, body: JSON.stringify(t.body), signal: ctl.signal });
      clearTimeout(tm);
      const tx = await r.text();
      if (!r.ok) { errors.push(`${t.model} → HTTP ${r.status} ${tx.slice(0, 160)}`); continue; }
      let d: any = null; try { d = JSON.parse(tx); } catch (_) { d = null; }
      const reply = textOf(d).trim();
      if (reply) return json({ reply, model: t.model, via: "cloudflare" });
      errors.push(`${t.model} → réponse vide`);
    } catch (e) {
      errors.push(`${t.model} → ${(e as Error)?.message || e}`);
    }
  }
  return json({ error: "secours_indisponible", details: errors }, 502);
});
