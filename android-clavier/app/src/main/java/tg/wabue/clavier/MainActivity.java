package tg.wabue.clavier;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Écran d'accueil : mise en route du clavier en 2 étapes + zone d'essai. */
public class MainActivity extends Activity {

    private TextView etat1, etat2;

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFFF7F3E8);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        sv.addView(col);

        // En-tête vert citron
        LinearLayout tete = new LinearLayout(this);
        tete.setOrientation(LinearLayout.HORIZONTAL);
        tete.setGravity(Gravity.CENTER_VERTICAL);
        tete.setBackgroundColor(WabueIME.CITRON);
        tete.setPadding(dp(18), dp(28), dp(18), dp(18));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        tete.addView(logo, new LinearLayout.LayoutParams(dp(72), dp(72)));
        LinearLayout titres = new LinearLayout(this);
        titres.setOrientation(LinearLayout.VERTICAL);
        titres.setPadding(dp(14), 0, 0, 0);
        TextView t1 = texte("WABOUÉ", 34, Color.BLACK, true);
        t1.setTypeface(Typeface.SERIF, Typeface.BOLD);
        TextView t2 = texte("WABUƐ CLAVIER IKPOSSO", 17, WabueIME.TOUCHE, true);
        titres.addView(t1);
        titres.addView(t2);
        tete.addView(titres);
        col.addView(tete);

        LinearLayout corps = new LinearLayout(this);
        corps.setOrientation(LinearLayout.VERTICAL);
        corps.setPadding(dp(18), dp(18), dp(18), dp(30));
        col.addView(corps);

        corps.addView(texte("Écris l'ikposso partout : WhatsApp, Facebook, SMS… avec ɛ ɔ ɩ ʋ ɣ ɖ ŋ.", 19, Color.BLACK, false));

        etat1 = texte("", 18, Color.BLACK, true);
        corps.addView(espace(16));
        corps.addView(texte("Étape 1", 15, WabueIME.OR, true));
        corps.addView(bouton("Activer le clavier", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        }));
        corps.addView(texte("Allume « WABUƐ CLAVIER IKPOSSO », puis reviens ici.", 16, 0xFF444444, false));
        corps.addView(etat1);

        etat2 = texte("", 18, Color.BLACK, true);
        corps.addView(espace(16));
        corps.addView(texte("Étape 2", 15, WabueIME.OR, true));
        corps.addView(bouton("Choisir le clavier", new View.OnClickListener() {
            @Override public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            }
        }));
        corps.addView(texte("Choisis « WABUƐ CLAVIER IKPOSSO » dans la liste.", 16, 0xFF444444, false));
        corps.addView(etat2);

        corps.addView(espace(20));
        corps.addView(texte("Calendrier akposso", 15, WabueIME.OR, true));
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int jj = cal.get(java.util.Calendar.DAY_OF_WEEK), mm = cal.get(java.util.Calendar.MONTH);
        corps.addView(texte("Emie (aujourd'hui) : " + CalendrierWidget.JOURS_IK[jj] + " · "
                + CalendrierWidget.MOIS_IK[mm] + " " + cal.get(java.util.Calendar.DAY_OF_MONTH)
                + "\nOVAZU dans " + CalendrierWidget.joursAvantOvazu() + " jours", 18, Color.BLACK, true));
        corps.addView(bouton("📅 Mettre le calendrier sur l'écran d'accueil", new View.OnClickListener() {
            @Override public void onClick(View v) { epinglerWidget(); }
        }));
        corps.addView(texte("Sinon : appui long sur l'écran d'accueil → Widgets → WABUƐ.", 16, 0xFF444444, false));

        corps.addView(espace(20));
        corps.addView(texte("Correction automatique", 15, WabueIME.OR, true));
        final android.content.SharedPreferences prefs = getSharedPreferences("reglages", MODE_PRIVATE);
        final android.widget.CheckBox cc = new android.widget.CheckBox(this);
        cc.setText("Mettre les bonnes lettres ikposso (ewlesse → ewlessɛ). ⌫ juste après pour annuler.");
        cc.setTextSize(17);
        cc.setChecked(prefs.getBoolean("autocorrect", true));
        cc.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean on) {
                prefs.edit().putBoolean("autocorrect", on).apply();
            }
        });
        corps.addView(cc);

        corps.addView(espace(20));
        corps.addView(texte("Stickers WhatsApp", 15, WabueIME.OR, true));
        corps.addView(bouton("💬 Ajouter les 30 stickers ikposso à WhatsApp", new View.OnClickListener() {
            @Override public void onClick(View v) { ajouterStickers(StickerProvider.PAQUET, StickerProvider.NOM); }
        }));
        corps.addView(espace(8));
        corps.addView(bouton("👥 Ajouter les stickers « pour le groupe »", new View.OnClickListener() {
            @Override public void onClick(View v) { ajouterStickers(StickerProvider.PAQUET2, StickerProvider.NOM2); }
        }));
        corps.addView(texte("Salutations, merci, bonne nuit… en ikposso, aux couleurs de WABUƐ.", 16, 0xFF444444, false));

        corps.addView(espace(20));
        corps.addView(texte("Essaie ici :", 17, Color.BLACK, true));
        EditText essai = new EditText(this);
        essai.setHint("Écris un mot en ikposso…");
        essai.setTextSize(22);
        essai.setMinLines(2);
        corps.addView(essai);

        corps.addView(espace(20));
        corps.addView(texte("Astuces : appui long sur e, o, u, d, b… pour é, ɔ, ʋ, ɗ, ɓ. Deux appuis sur ⇧ pour TOUT EN MAJUSCULES. Appui long sur ´ pour les autres tons. 🌐 pour changer de clavier.", 16, 0xFF333333, false));

        corps.addView(espace(20));
        corps.addView(bouton("Visiter WABUƐ", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://adomhub.github.io/ABUESA-IA-/")));
            }
        }));
        corps.addView(espace(12));
        corps.addView(texte("🎁 Un cadeau de WABUƐ au peuple akposso. Soutiens-nous : 1000 FCFA par Flooz au +228 99 61 99 69. Ewlessɛ !", 16, 0xFF333333, false));

        setContentView(sv);
    }

    private void epinglerWidget() {
        android.appwidget.AppWidgetManager mgr = android.appwidget.AppWidgetManager.getInstance(this);
        boolean ok = false;
        if (android.os.Build.VERSION.SDK_INT >= 26 && mgr.isRequestPinAppWidgetSupported()) {
            ok = mgr.requestPinAppWidget(new android.content.ComponentName(this, CalendrierWidget.class), null, null);
        }
        if (!ok) android.widget.Toast.makeText(this, "Fais un appui long sur l'écran d'accueil → Widgets → WABUƐ.", android.widget.Toast.LENGTH_LONG).show();
    }

    private void ajouterStickers(String id, String nom) {
        Intent i = new Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
        i.putExtra("sticker_pack_id", id);
        i.putExtra("sticker_pack_authority", StickerProvider.AUTORITE);
        i.putExtra("sticker_pack_name", nom);
        try {
            startActivityForResult(i, 200);
        } catch (android.content.ActivityNotFoundException e) {
            android.widget.Toast.makeText(this, "WhatsApp n'est pas installé sur ce téléphone.", android.widget.Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int code, int resultat, Intent data) {
        super.onActivityResult(code, resultat, data);
        if (code == 200 && resultat == RESULT_CANCELED && data != null) {
            String err = data.getStringExtra("validation_error");
            if (err != null) android.widget.Toast.makeText(this, "WhatsApp : " + err, android.widget.Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() { super.onResume(); majEtat(); CalendrierWidget.toutMettreAJour(this); }

    @Override
    public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) majEtat(); }

    private void majEtat() {
        boolean active = false;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            for (InputMethodInfo i : imm.getEnabledInputMethodList()) {
                if (i.getPackageName().equals(getPackageName())) active = true;
            }
        }
        String courant = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        boolean choisi = courant != null && courant.startsWith(getPackageName());
        etat1.setText(active ? "✅ Clavier activé" : "⬜ Pas encore activé");
        etat2.setText(choisi ? "✅ Clavier choisi : tout est prêt !" : "⬜ Pas encore choisi");
    }

    private TextView texte(String s, float taille, int couleur, boolean gras) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(taille);
        t.setTextColor(couleur);
        t.setPadding(0, dp(4), 0, dp(4));
        if (gras) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private View espace(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return v;
    }

    private TextView bouton(String s, View.OnClickListener clic) {
        TextView b = texte(s, 20, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable g = new GradientDrawable();
        g.setColor(WabueIME.TOUCHE);
        g.setCornerRadius(dp(14));
        b.setBackground(g);
        b.setOnClickListener(clic);
        b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return b;
    }
}
