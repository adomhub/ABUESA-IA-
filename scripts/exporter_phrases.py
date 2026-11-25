"""Exporte les phrases ikposso validées (dictionnaire partagé Supabase) vers lexique/1000-phrases.md.
Lancé automatiquement par .github/workflows/phrases-1000.yml (chaque jour + à la demande)."""
import json, re, urllib.request, datetime, pathlib
src = open('index.html', encoding='utf-8').read()
url = re.search(r"DEFAULT_SUPABASE_URL = '([^']+)'", src).group(1)
key = re.search(r"sb_publishable_[A-Za-z0-9_-]+", src).group(0)
req = urllib.request.Request(url + '/rest/v1/dictionary_words?select=ak,fr,en,created_at&order=created_at.asc',
                             headers={'apikey': key, 'Authorization': 'Bearer ' + key})
rows = json.load(urllib.request.urlopen(req))
phrases = [r for r in rows if re.search(r'\s', (r.get('ak') or '').strip())]
today = datetime.date.today().isoformat()
out = [f"# Objectif 1000 phrases — bibliothèque WABUƐ\n",
       f"Phrases ikposso validées par Alexi (dictionnaire partagé). Mise à jour automatique : {today}.\n",
       f"**{len(phrases)} / 1000 phrases**\n",
       "| # | Ikposso | Français |", "|---|---|---|"]
for i, r in enumerate(phrases, 1):
    ak = (r['ak'] or '').replace('|', '/'); fr = (r.get('fr') or '').replace('|', '/')
    out.append(f"| {i} | {ak} | {fr} |")
pathlib.Path('lexique/1000-phrases.md').write_text('\n'.join(out) + '\n', encoding='utf-8')
print(len(phrases), 'phrases')
