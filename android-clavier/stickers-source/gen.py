import json
S=[("Ɩvʋao !","Bonjour","👋"),("Ɛza mʋanɩ ?","Comment vas-tu ?","🙂"),("Nɩza ɔbɔɛ","Je vais bien","😊"),
("Ewlessɛ !","Merci","🙏"),("Kʋkɔ","S'il te plaît","🤲"),("Ɛyadʋ","Pardon","🙇"),
("Yanʋ asɛ !","Bienvenue","🤗"),("Ekʋalɛ","À bientôt","👋"),("Ɩzʋa ɛzaa","Bonsoir","🌇"),
("Zadɛ","Bonne nuit","😴"),("Eɛ","Oui","👍"),("Oo","Non","🙅"),
("Nɩnanʋ ayɩlɩ","Je ne comprends pas","🤔"),("Ba-ɛmɛ !","Viens ici","👉"),("Ʋvlɛ-atsɩnɔ","J'ai faim","🍲"),
("Dɔkayɛ !","Félicitations","🎉"),("Ɩlʋbʋbʋɛ !","Bonne année","🎆"),("Ɛtʋlʋlʋa-kayɛ","Paix à toi","🕊️"),
("Ɩwolowʋ kotsʋeyɛ-ɛbʋa","Que Dieu te bénisse","✨"),("Nɩlɛ akpɔssɔ","Je suis Akposso","💚"),
("Azɔyɛ nʋmɛ ?","Comment t'appelles-tu ?","🙋"),("Atʋna ?","D'où viens-tu ?","🧭"),("Ɛyahana ?","Où vas-tu ?","🚶"),
("Nɩyaha-ʋdja","Je vais au marché","🛒"),("Ɔmɔawlɩnɔ","J'ai soif","💧"),("Ɩdɔ-ewʋɩnɔ","Je suis fatigué","😩"),
("Zatɩ","Assieds-toi","🪑"),("Ɛsɛsɩsɩ","C'est vrai","✅"),("Nɩneyɩ","Je ne sais pas","🤷"),("Nadʋɛlʋ kalʋkawa","Respects aux anciens","🙌")]
json.dump(S,open('liste.json','w'),ensure_ascii=False)
cards=""
for i,(ik,fr,em) in enumerate(S):
    fond,txt,sous=("#C7D92E","#1E3A22","#2F5233") if i%2==0 else ("#2F5233","#FFFFFF","#C7D92E")
    ik=ik.replace(' !','\u00a0!').replace(' ?','\u00a0?')
    mots=ik.split(' ')
    plus_long=max(len(m) for m in mots)
    size=min(112 if len(mots)<3 else 90, int(700/plus_long))
    if len(mots)>=2 and len(ik)>9: size=min(size,92)
    ik=' '.join('<span style="white-space:nowrap">'+m+'</span>' for m in mots)
    cards+=f'''<div class="s" id="s{i}"><div class="c" style="background:{fond}"><div class="em">{em}</div>
<div class="ik" style="color:{txt};font-size:{size}px">{ik}</div><div class="fr" style="color:{sous};font-size:{38 if len(fr)<=19 else 30}px">{fr}</div><div class="w" style="color:{sous}">WABUƐ</div></div></div>'''
html=f'''<!DOCTYPE html><html><head><meta charset="utf-8"><style>
body{{margin:0;background:transparent;font-family:Inter,"DejaVu Sans",sans-serif}}
.s{{width:512px;height:512px;display:flex;align-items:center;justify-content:center}}
.c{{width:480px;height:480px;border-radius:70px;border:10px solid #fff;box-sizing:border-box;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;padding:20px;box-shadow:0 0 0 2px rgba(0,0,0,.15)}}
.em{{font-size:96px;line-height:1}}
.ik{{font-weight:900;line-height:1.05;margin-top:10px}}
.fr{{font-size:38px;font-weight:700;margin-top:12px}}
.w{{font-size:26px;font-weight:900;letter-spacing:3px;margin-top:10px;opacity:.85}}
</style></head><body>{cards}</body></html>'''
open('stickers.html','w').write(html)
