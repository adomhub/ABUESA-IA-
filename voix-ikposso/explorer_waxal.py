"""Explore la partie ikposso (kpo) du corpus WAXAL de Google (CC-BY-4.0) et écrit un rapport."""
import json, io, sys
from huggingface_hub import HfApi, hf_hub_download
import pyarrow.parquet as pq

REPO = "google/WaxalNLP"
api = HfApi()
lignes = ["# Rapport WAXAL — partie ikposso (kpo)", "", "Source : https://huggingface.co/datasets/google/WaxalNLP (licence CC-BY-4.0, Google Research et partenaires dont l'Université du Ghana).", ""]
try:
    fichiers = api.list_repo_tree(REPO, repo_type="dataset", recursive=True)
    tous = [(f.path, getattr(f, "size", 0) or 0) for f in fichiers]
except Exception as e:
    lignes.append(f"Erreur d'accès : {e}")
    open("voix-ikposso/waxal-kpo-rapport.md", "w").write("\n".join(lignes)); sys.exit(0)

kpo = [(p, s) for p, s in tous if "kpo" in p.lower()]
lignes.append(f"Fichiers du corpus : {len(tous)} ; fichiers ikposso : {len(kpo)}")
lignes.append("")
for p, s in kpo:
    lignes.append(f"- `{p}` — {s/1e6:.1f} Mo")
lignes.append("")
parquets = [p for p, s in kpo if p.endswith(".parquet")]
total, duree, exemples = 0, 0.0, []
for p in parquets:
    try:
        chemin = hf_hub_download(REPO, p, repo_type="dataset")
        pf = pq.ParquetFile(chemin)
        cols = [c for c in pf.schema_arrow.names if c != "audio"]
        t = pf.read(columns=cols).to_pandas()
        total += len(t)
        for c in ("duration", "audio_duration", "duration_s", "length"):
            if c in t.columns:
                duree += float(t[c].fillna(0).sum()); break
        txt = next((c for c in ("transcription", "text", "sentence", "transcript") if c in t.columns), None)
        lignes.append(f"## `{p}` : {len(t)} lignes ; colonnes : {', '.join(cols)}")
        if txt:
            for v in t[txt].dropna().head(15):
                exemples.append(str(v))
    except Exception as e:
        lignes.append(f"## `{p}` : erreur {e}")
lignes += ["", f"**Total : {total} enregistrements**" + (f", environ {duree/3600:.1f} heures" if duree else ""), "", "## Exemples de transcriptions ikposso", ""]
lignes += [f"- {x}" for x in exemples[:40]]
open("voix-ikposso/waxal-kpo-rapport.md", "w").write("\n".join(lignes) + "\n")
print("\n".join(lignes[:60]))
