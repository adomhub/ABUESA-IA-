"""Extrait les mots ikposso des ~18 000 transcriptions WAXAL (Google Research et Université du Ghana, CC-BY-4.0).
Sert uniquement aux suggestions du clavier WABUƐ. ʊ/Ʊ sont remplacés par ʋ/Ʋ (accord d'Alexi, 24 nov. 2026)."""
import collections, re, unicodedata, pathlib
from huggingface_hub import HfApi, hf_hub_download
import pyarrow.parquet as pq

REPO = "google/WaxalNLP"
SORTIE = pathlib.Path("dictionnaire-ikposso/source/waxal_kpo_frequences.tsv")
fichiers = [f.path for f in HfApi().list_repo_tree(REPO, repo_type="dataset", path_in_repo="data/ASR/kpo")
            if f.path.endswith(".parquet") and not f.path.split("/")[-1].startswith("unlabeled")]
compte = collections.Counter()
phrases = 0
MOT = re.compile(r"^[a-zɛɔɩʋɣɖŋǝƒɓɗʒƶ̀-ͯ']{2,25}$")
for p in fichiers:
    t = pq.read_table(hf_hub_download(REPO, p, repo_type="dataset"), columns=["transcription"])
    for s in t.column("transcription").to_pylist():
        if not s: continue
        phrases += 1
        s = unicodedata.normalize("NFC", s).replace("ʊ", "ʋ").replace("Ʊ", "Ʋ").lower()
        for m in re.split(r"[\s/,;:.!?…()\"«»’\-–—\[\]0-9]+", s):
            m = m.strip("'")
            if MOT.match(unicodedata.normalize("NFD", m)):
                compte[m] += 1
gardes = {m: n for m, n in compte.items() if n >= 3}
SORTIE.write_text("".join(f"{m}\t{n}\n" for m, n in sorted(gardes.items(), key=lambda x: (-x[1], x[0]))), encoding="utf-8")
print(phrases, "phrases ;", len(compte), "mots différents ;", len(gardes), "gardés (au moins 3 fois)")
