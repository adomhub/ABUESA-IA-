"""Fabrique la liste de mots ikposso du dictionnaire de suggestions (Keyman).
Sources : le dictionnaire du site (index.html) + les mots ajoutés depuis l'admin (Supabase).
Règle d'Alexi : le son écrit ē s'écrit toujours ɛ."""
import re, json, sys, unicodedata, urllib.request, collections, pathlib

RACINE = pathlib.Path(__file__).resolve().parent.parent
SORTIE = pathlib.Path(__file__).resolve().parent / 'source' / 'wordlist.tsv'
URL = 'https://wyssiljzdvsxlnylqcia.supabase.co/rest/v1/dictionary_words?select=ak'
CLE = 'sb_publishable_-W5Kvecx25W0QmjMG3wcTg_YsSWb4Gx'  # clé publique du site

def textes():
    html = (RACINE / 'index.html').read_text(encoding='utf-8')
    debut = html.index('const DICTIONARY = [')
    fin = html.index('];', debut)
    for m in re.finditer(r"ak:\s*'((?:[^'\\]|\\.)*)'", html[debut:fin]):
        yield m.group(1)
    # mots des stickers validés par Alexi (comptent 3 fois)
    for nom in ('stickers-liste.json', 'stickers-liste-groupe.json'):
        f = RACINE / 'android-clavier' / nom
        if f.exists():
            for ik, fr, em in json.loads(f.read_text(encoding='utf-8')):
                for _ in range(3):
                    yield ik
    try:
        req = urllib.request.Request(URL, headers={'apikey': CLE, 'Authorization': 'Bearer ' + CLE})
        for ligne in json.load(urllib.request.urlopen(req, timeout=30)):
            if ligne.get('ak'):
                yield ligne['ak']
    except Exception as e:
        print('Supabase injoignable, mots du site seulement :', e, file=sys.stderr)

def nettoyer(t):
    t = re.sub(r'\([^)]*\)?', ' ', t)  # retire les notes en français entre parenthèses
    t = unicodedata.normalize('NFC', t).replace('ē', 'ɛ').replace('Ē', 'Ɛ')
    t = t.replace('ė', 'ɛ').replace('ä', 'a')
    # Guide d'orthographe (Alexi, 24 nov. 2026) : tous les i → ɩ et tous les u → ʋ
    t = unicodedata.normalize('NFD', t.lower()).replace('ʊ', 'ʋ')
    t = re.sub(r"\b(ou|abuesa)\b", lambda m: m.group(0).upper(), t)  # mots français protégés
    t = t.translate(str.maketrans('iu', 'ɩʋ')).replace('OU', 'ou').replace('ABUESA', 'abuesa')
    return unicodedata.normalize('NFC', t)

compte = collections.Counter()
for t in textes():
    for mot in re.split(r"[\s/,;:.!?…()\"«»'’\-]+", nettoyer(t)):
        mot = mot.strip()
        if len(mot) >= 2 and not mot.isdigit() and mot not in ('ou', 'abuesa'):
            compte[mot] += 1

# Les mots validés par Alexi passent toujours devant
for m in list(compte): compte[m] *= 50
# Mots réels des locuteurs, tirés des transcriptions WAXAL (Google Research / Université du Ghana, CC-BY-4.0)
WAXAL = SORTIE.parent / 'waxal_kpo_frequences.tsv'
if WAXAL.exists():
    for ligne in WAXAL.read_text(encoding='utf-8').splitlines():
        m, _, n = ligne.partition('\t')
        if m and n.isdigit():
            m = nettoyer(m)
            if len(m) >= 2: compte[m] += int(n)

SORTIE.write_text(''.join(f'{m}\t{n}\n' for m, n in sorted(compte.items(), key=lambda x: (-x[1], x[0]))), encoding='utf-8')
print(len(compte), 'mots ikposso écrits dans', SORTIE.name)
