# 🎤 Dictée ikposso — guide simple

Carnet : `voix-ikposso/dictee_ikposso_colab.ipynb`

Ouvrir directement dans Google Colab (gratuit, avec un compte Gmail) :
https://colab.research.google.com/github/adomhub/WABOU-WAB-/blob/main/voix-ikposso/dictee_ikposso_colab.ipynb

## Étapes
1. Ouvrir le lien ci-dessus (de préférence sur un ordinateur).
2. Menu **Exécution → Modifier le type d'exécution → GPU T4** → Enregistrer.
3. Menu **Exécution → Tout exécuter**. Accepter l'accès à Google Drive quand il est demandé (étape 13).
4. Laisser tourner 2 à 4 heures, onglet ouvert.
5. Résultat : modèle dans Google Drive (`WABUE-dictee-ikposso`) + démo micro en bas de page.

## Ce que fait le carnet
- Télécharge 8 des 24 fichiers d'entraînement ikposso de WAXAL (≈ 4 800 phrases audio) — licence CC-BY-4.0.
- Met les textes dans l'orthographe WABƲƐ (ʊ → ʋ, ɪ → ɩ).
- Entraîne un adaptateur ikposso sur le modèle MMS de Meta (facebook/mms-1b-all).
- Mesure la qualité (WER = erreurs sur les mots, CER = erreurs sur les lettres).

## Ensuite
- Ajouter les voix de Mozilla Common Voice (ikposso) dès qu'elles seront publiées.
- Ajouter UGSpeechData si l'Université du Ghana donne son accord.
- Brancher la dictée dans le clavier et l'appli WABƲƐ.
