package tg.wabue.clavier;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CLAVIER IKPOSSO WABUƐ — clavier Android pour écrire l'ikposso.
 * Disposition : lettres ikposso en haut, AZERTY, appui long pour les accents,
 * touche des tons, majuscules (1 appui) et verrouillage (2 appuis), suggestions.
 */
public class WabueIME extends InputMethodService {

    // Couleurs WABUƐ
    static final int FOND = 0xFF1E3A22;      // vert forêt foncé
    static final int TOUCHE = 0xFF2F5233;    // vert forêt
    static final int FONCTION = 0xFF14281A;  // touches de fonction
    static final int CITRON = 0xFFC7D92E;    // vert citron
    static final int OR = 0xFFB8860B;        // or

    static final String TON = "TON", MAJ = "MAJ", EFF = "EFF", NUM = "123", ABC = "ABC",
            GLOBE = "GLOBE", ESP = "ESP", ENT = "ENT";
    static final String AIGU = "́";

    static final String[][] LETTRES = {
            {"ɛ", "ɔ", "ɩ", "ʋ", "ɣ", "ɖ", "ŋ", "ǝ", "ʉ", TON},
            {"a", "z", "e", "r", "t", "y", "u", "i", "o", "p"},
            {"q", "s", "d", "f", "g", "h", "j", "k", "l", "m"},
            {MAJ, "w", "x", "c", "v", "b", "n", "'", EFF},
            {NUM, GLOBE, ",", ESP, ".", ENT}
    };
    static final String[][] CHIFFRES = {
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
            {"@", "#", "&", "*", "-", "+", "=", "(", ")", "/"},
            {"€", "%", "_", ":", ";", "!", "?", "\"", "₵", EFF},
            {ABC, GLOBE, ",", ESP, ".", ENT}
    };

    static final Map<String, String[]> APPUI_LONG = new HashMap<>();
    static {
        APPUI_LONG.put("a", new String[]{"à", "â", "ä"});
        APPUI_LONG.put("e", new String[]{"ɛ", "é", "è", "ê", "ë"});
        APPUI_LONG.put("i", new String[]{"ɩ", "ɨ", "î", "ï"});
        APPUI_LONG.put("o", new String[]{"ɔ", "ô", "ö"});
        APPUI_LONG.put("u", new String[]{"ʋ", "ʉ", "ù", "û", "ü"});
        APPUI_LONG.put("c", new String[]{"ç"});
        APPUI_LONG.put("g", new String[]{"ɣ"});
        APPUI_LONG.put("d", new String[]{"ɖ", "ɗ"});
        APPUI_LONG.put("n", new String[]{"ŋ", "ɲ"});
        APPUI_LONG.put("y", new String[]{"ÿ"});
        APPUI_LONG.put("f", new String[]{"ƒ"});
        APPUI_LONG.put("b", new String[]{"ɓ"});
        APPUI_LONG.put("'", new String[]{"’", "«", "»"});
        APPUI_LONG.put(",", new String[]{"?", "!", ";", ":"});
        APPUI_LONG.put(".", new String[]{"…", "-", "\"", "(", ")"});
        APPUI_LONG.put(TON, new String[]{"̀", "̂", "̌", "̄", "̃"});
    }

    // 0 = minuscules, 1 = une majuscule, 2 = majuscules verrouillées, 3 = chiffres
    private int couche = 0;
    private long dernierMaj = 0;

    private LinearLayout racine, clavier, barre;
    private HorizontalScrollView defile;
    private final Handler h = new Handler(Looper.getMainLooper());
    private Runnable repete, appuiLong;
    private boolean longFait;

    private final List<String> mots = new ArrayList<>();
    private final List<String> motsPlis = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        chargerMots();
    }

    private void chargerMots() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("mots.tsv"), "UTF-8"))) {
            String l;
            while ((l = br.readLine()) != null) {
                String m = l.split("\t")[0].trim();
                if (m.isEmpty()) continue;
                mots.add(m);              // déjà triés du plus fréquent au moins fréquent
                motsPlis.add(plier(m));
            }
        } catch (Exception ignore) { }
    }

    static String plier(String s) {
        String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "");
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public View onCreateInputView() {
        racine = new LinearLayout(this);
        racine.setOrientation(LinearLayout.VERTICAL);
        racine.setBackgroundColor(FOND);

        defile = new HorizontalScrollView(this);
        defile.setHorizontalScrollBarEnabled(false);
        defile.setBackgroundColor(CITRON);
        barre = new LinearLayout(this);
        barre.setOrientation(LinearLayout.HORIZONTAL);
        barre.setGravity(Gravity.CENTER_VERTICAL);
        defile.addView(barre, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)));
        racine.addView(defile, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        clavier = new LinearLayout(this);
        clavier.setOrientation(LinearLayout.VERTICAL);
        clavier.setPadding(dp(3), dp(4), dp(3), dp(6));
        racine.addView(clavier);

        dessiner();
        barreAccueil();
        return racine;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        couche = 0;
        if (clavier != null) { dessiner(); majSuggestions(); }
    }

    // ---------- Dessin du clavier ----------

    private void dessiner() {
        clavier.removeAllViews();
        String[][] rangs = couche == 3 ? CHIFFRES : LETTRES;
        for (String[] rang : rangs) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            for (String k : rang) r.addView(touche(k));
            clavier.addView(r, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        }
    }

    private boolean majuscule() { return couche == 1 || couche == 2; }

    private String texteDe(String k) {
        switch (k) {
            case TON: return "´";
            case MAJ: return couche == 2 ? "⇪" : "⇧";
            case EFF: return "⌫";
            case NUM: return "123";
            case ABC: return "abc";
            case GLOBE: return "🌐";
            case ESP: return "WABUƐ";
            case ENT: return "↵";
        }
        return majuscule() ? k.toUpperCase(Locale.ROOT) : k;
    }

    private float poids(String k) {
        switch (k) {
            case MAJ: case EFF: return 1.5f;
            case NUM: case ABC: return 1.4f;
            case ENT: return 1.6f;
            case GLOBE: return 1.1f;
            case ESP: return 4.4f;
        }
        return 1f;
    }

    private boolean estIkposso(String k) {
        return "ɛɔɩʋɣɖŋǝʉ".contains(k) && k.length() == 1;
    }

    private TextView touche(final String k) {
        final TextView t = new TextView(this);
        t.setText(texteDe(k));
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        boolean fonction = k.length() > 1 || k.equals(TON);
        int couleur = TOUCHE, texte = Color.WHITE;
        if (estIkposso(k)) { couleur = CITRON; texte = Color.BLACK; }
        else if (k.equals(ESP)) { couleur = TOUCHE; texte = 0x99FFFFFF; }
        else if (fonction) { couleur = FONCTION; }
        if (k.equals(MAJ) && couche == 2) { couleur = OR; }
        if (k.equals(MAJ) && couche == 1) { couleur = TOUCHE; }
        t.setTextColor(texte);
        float taille = 24;
        if (k.equals(ESP)) taille = 14;
        else if (k.equals(NUM) || k.equals(ABC)) taille = 17;
        t.setTextSize(taille);
        if (estIkposso(k)) t.setTypeface(Typeface.DEFAULT_BOLD);
        final GradientDrawable fond = new GradientDrawable();
        fond.setColor(couleur);
        fond.setCornerRadius(dp(8));
        t.setBackground(fond);
        final int couleurBase = couleur;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, poids(k));
        lp.setMargins(dp(2.5f), dp(3), dp(2.5f), dp(3));
        t.setLayoutParams(lp);

        t.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        fond.setColor(OR);
                        longFait = false;
                        if (k.equals(EFF)) {
                            effacer();
                            repete = new Runnable() {
                                @Override public void run() { effacer(); h.postDelayed(this, 55); }
                            };
                            h.postDelayed(repete, 420);
                        } else if (APPUI_LONG.containsKey(k) || k.equals(GLOBE)) {
                            appuiLong = new Runnable() {
                                @Override public void run() {
                                    longFait = true;
                                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                    if (k.equals(GLOBE)) choisirClavier(); else montrerVariantes(k);
                                }
                            };
                            h.postDelayed(appuiLong, 380);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        fond.setColor(couleurBase);
                        if (repete != null) { h.removeCallbacks(repete); repete = null; }
                        if (appuiLong != null) { h.removeCallbacks(appuiLong); appuiLong = null; }
                        if (e.getAction() == MotionEvent.ACTION_UP && !longFait && !k.equals(EFF)) appui(k);
                        return true;
                }
                return false;
            }
        });
        return t;
    }

    // ---------- Actions ----------

    private void appui(String k) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        switch (k) {
            case MAJ:
                long now = System.currentTimeMillis();
                if (couche == 2) couche = 0;
                else if (couche == 1 && now - dernierMaj < 400) couche = 2;
                else if (couche == 1) couche = 0;
                else couche = 1;
                dernierMaj = now;
                dessiner();
                return;
            case NUM: couche = 3; dessiner(); return;
            case ABC: couche = 0; dessiner(); return;
            case GLOBE: changerClavier(); return;
            case ESP: ic.commitText(" ", 1); majSuggestions(); return;
            case ENT: entree(ic); return;
            case TON: ic.commitText(AIGU, 1); majSuggestions(); return;
        }
        ecrire(majuscule() ? k.toUpperCase(Locale.ROOT) : k);
    }

    private void ecrire(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.commitText(s, 1);
        if (couche == 1 && Character.isLetter(s.codePointAt(0))) { couche = 0; dessiner(); }
        majSuggestions();
    }

    private void effacer() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence sel = ic.getSelectedText(0);
        if (sel != null && sel.length() > 0) ic.commitText("", 1);
        else sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        majSuggestions();
    }

    private void entree(InputConnection ic) {
        EditorInfo ei = getCurrentInputEditorInfo();
        int action = ei == null ? EditorInfo.IME_ACTION_NONE : (ei.imeOptions & EditorInfo.IME_MASK_ACTION);
        boolean pasAction = ei == null || (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
        if (!pasAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action);
        } else {
            ic.commitText("\n", 1);
        }
        majSuggestions();
    }

    private void changerClavier() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        boolean ok = false;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 28) ok = switchToNextInputMethod(false);
        } catch (Exception ignore) { }
        if (!ok && imm != null) imm.showInputMethodPicker();
    }

    private void choisirClavier() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showInputMethodPicker();
    }

    // ---------- Barre du haut : suggestions et variantes ----------

    private TextView boutonBarre(String texte, int couleurTexte, View.OnClickListener clic) {
        TextView b = new TextView(this);
        b.setText(texte);
        b.setTextSize(21);
        b.setTextColor(couleurTexte);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), 0, dp(16), 0);
        if (clic != null) b.setOnClickListener(clic);
        b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return b;
    }

    private void barreAccueil() {
        if (barre == null) return;
        defile.setBackgroundColor(CITRON);
        barre.removeAllViews();
        TextView t = boutonBarre("WABUƐ · ikposso", 0xFF1E3A22, null);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        barre.addView(t);
    }

    private void montrerVariantes(String k) {
        String[] v = APPUI_LONG.get(k);
        if (v == null) return;
        defile.setBackgroundColor(OR);
        barre.removeAllViews();
        for (final String c : v) {
            final String sortie = k.equals(TON) ? c : (majuscule() ? c.toUpperCase(Locale.ROOT) : c);
            String vu = k.equals(TON) ? "◌" + c : sortie;
            TextView b = boutonBarre(vu, Color.WHITE, new View.OnClickListener() {
                @Override public void onClick(View view) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    InputConnection ic = getCurrentInputConnection();
                    if (ic == null) return;
                    if (k.equals(TON)) { ic.commitText(sortie, 1); majSuggestions(); }
                    else ecrire(sortie);
                }
            });
            b.setTextSize(26);
            b.setTypeface(Typeface.DEFAULT_BOLD);
            barre.addView(b);
        }
        defile.scrollTo(0, 0);
    }

    private String motEnCours() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return "";
        CharSequence avant = ic.getTextBeforeCursor(40, 0);
        if (avant == null) return "";
        int i = avant.length();
        while (i > 0) {
            char c = avant.charAt(i - 1);
            int type = Character.getType(c);
            if (Character.isLetter(c) || type == Character.NON_SPACING_MARK) i--;
            else break;
        }
        return avant.subSequence(i, avant.length()).toString();
    }

    private void majSuggestions() {
        if (barre == null) return;
        final String debut = motEnCours();
        if (debut.isEmpty() || mots.isEmpty()) { barreAccueil(); return; }
        String p = plier(debut);
        List<String> trouves = new ArrayList<>();
        for (int i = 0; i < mots.size() && trouves.size() < 6; i++) {
            if (motsPlis.get(i).startsWith(p) && !mots.get(i).equals(debut.toLowerCase(Locale.ROOT))) trouves.add(mots.get(i));
        }
        if (trouves.isEmpty()) { barreAccueil(); return; }
        defile.setBackgroundColor(CITRON);
        barre.removeAllViews();
        final boolean cap = Character.isUpperCase(debut.codePointAt(0));
        for (String m : trouves) {
            final String mot = cap ? m.substring(0, 1).toUpperCase(Locale.ROOT) + m.substring(1) : m;
            TextView b = boutonBarre(mot, Color.BLACK, new View.OnClickListener() {
                @Override public void onClick(View view) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    InputConnection ic = getCurrentInputConnection();
                    if (ic == null) return;
                    String actuel = motEnCours();
                    ic.deleteSurroundingText(actuel.length(), 0);
                    ic.commitText(mot + " ", 1);
                    barreAccueil();
                }
            });
            barre.addView(b);
            View sep = new View(this);
            sep.setBackgroundColor(0x551E3A22);
            barre.addView(sep, new LinearLayout.LayoutParams(dp(1), dp(26)));
        }
        defile.scrollTo(0, 0);
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int cs, int ce) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, cs, ce);
        majSuggestions();
    }
}
