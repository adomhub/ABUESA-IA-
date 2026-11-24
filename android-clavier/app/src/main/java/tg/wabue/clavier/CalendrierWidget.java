package tg.wabue.clavier;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.Calendar;

/** Widget « Calendrier akposso » : le jour en ikposso, le mois, le marché du jour et OVAZU. */
public class CalendrierWidget extends AppWidgetProvider {

    // Calendar.DAY_OF_WEEK : 1 = dimanche … 7 = samedi
    static final String[] JOURS_IK = {"", "Ɛzawlayɩ", "Ɩmle", "Ɛkpɛ", "Ɛwlɛ", "Ɛɣla", "Ɛva", "Ewledʋyɩ"};
    static final String[] JOURS_FR = {"", "dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi"};
    /** Marchés des Akposso du Ghana (donnés par Alexi le 23 nov. 2026), affichés sur les téléphones en anglais. */
    static final String[] MARCHES_GH = {"", "", "", "Akpɔssɔ Cabo", "Akpɔssɔ Kubi", "", "Ɔclabɛ · Akpɔssɔ Cabo", ""};
    static final String[] MARCHES = {"", "", "Agbo", "Zogbégan", "Doumé", "Badou", "Kougnohou", "Agbo N'Kougna"};
    static final String[] MOIS_IK = {"Ɔlatʋ", "Ɔfʋtsɩ", "Zata", "Ɛvlɩlʋɩa", "Ʋlawlɩvɛ", "Ɩnaglo",
            "Nekʋnekʋ", "Ɩpʋǝnǝ", "Ɩwle", "Ɩwletsɔ", "Ɔtɩɩ", "Ɛyɛbʋele"};
    static final String[] JOURS_EN = {"", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    static final String[] MOIS_EN = {"January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"};
    static final String[] MOIS_FR = {"janvier", "février", "mars", "avril", "mai", "juin",
            "juillet", "août", "septembre", "octobre", "novembre", "décembre"};

    /** OVAZU : chaque 2e samedi de décembre. */
    static Calendar ovazu(int annee) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(annee, Calendar.DECEMBER, 1);
        while (c.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) c.add(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.DAY_OF_MONTH, 7);
        return c;
    }

    static int joursAvantOvazu() {
        Calendar auj = Calendar.getInstance();
        Calendar a = Calendar.getInstance();
        a.clear();
        a.set(auj.get(Calendar.YEAR), auj.get(Calendar.MONTH), auj.get(Calendar.DAY_OF_MONTH));
        Calendar o = ovazu(auj.get(Calendar.YEAR));
        if (o.before(a)) o = ovazu(auj.get(Calendar.YEAR) + 1);
        return (int) Math.round((o.getTimeInMillis() - a.getTimeInMillis()) / 86400000.0);
    }

    static RemoteViews vue(Context ctx) {
        Calendar c = Calendar.getInstance();
        int j = c.get(Calendar.DAY_OF_WEEK), m = c.get(Calendar.MONTH), d = c.get(Calendar.DAY_OF_MONTH);
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_calendrier);
        v.setTextViewText(R.id.w_jour, JOURS_IK[j]);
        int an = c.get(Calendar.YEAR);
        boolean fr = "fr".equals(java.util.Locale.getDefault().getLanguage());
        v.setTextViewText(R.id.w_titre, fr ? "ÉMƖÉ · AUJOURD'HUI · WABUƐ" : "ÉMƖÉ · TODAY · WABUƐ");
        v.setTextViewText(R.id.w_jour_fr, fr ? JOURS_FR[j] + " " + d + " " + MOIS_FR[m] + " " + an
                                             : JOURS_EN[j] + ", " + d + " " + MOIS_EN[m] + " " + an);
        v.setTextViewText(R.id.w_mois, "Ʋ̀tsɩ " + MOIS_IK[m] + " · " + d + "   Ɩlʋɛ " + an);
        String[] marches = fr ? MARCHES : MARCHES_GH;
        String marche = marches[j].isEmpty() ? (fr ? "🛒 Pas de marché aujourd'hui" : "🛒 No market today")
                                             : "🛒 Ʋdzayɩ : " + marches[j];
        v.setTextViewText(R.id.w_marche, marche);
        int n = joursAvantOvazu();
        v.setTextViewText(R.id.w_ovazu, fr
            ? (n == 0 ? "🎉 Jour J : c'est OVAZU aujourd'hui !" : "🎉 OVAZU dans " + n + (n > 1 ? " jours" : " jour") + " (J-" + n + ")")
            : (n == 0 ? "🎉 Today is OVAZU!" : "🎉 OVAZU in " + n + (n > 1 ? " days" : " day") + " (D-" + n + ")"));
        Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://adomhub.github.io/WABOU-WAB-/"));
        int f = android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
        v.setOnClickPendingIntent(R.id.w_racine, PendingIntent.getActivity(ctx, 0, i, f));
        return v;
    }

    static final String MAJ = "tg.wabue.clavier.MAJ_CALENDRIER";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, vue(ctx));
        programmerMinuit(ctx);
    }

    @Override
    public void onEnabled(Context ctx) { programmerMinuit(ctx); }

    /** Réveil juste après minuit pour changer de jour, même si le téléphone économise la batterie. */
    static void programmerMinuit(Context ctx) {
        android.app.AlarmManager am = (android.app.AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, CalendrierWidget.class).setAction(MAJ);
        int f = PendingIntent.FLAG_UPDATE_CURRENT | (android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 1, i, f);
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 30); c.set(Calendar.MILLISECOND, 0);
        if (android.os.Build.VERSION.SDK_INT >= 23) am.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
        else am.set(android.app.AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
    }

    /** Met à jour tous les widgets (appelé aussi au changement de date). */
    static void toutMettreAJour(Context ctx) {
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, CalendrierWidget.class));
            if (ids == null || ids.length == 0) return;
            for (int id : ids) mgr.updateAppWidget(id, vue(ctx));
            programmerMinuit(ctx);
        } catch (Exception ignore) { }
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        String a = intent.getAction();
        if (MAJ.equals(a) || Intent.ACTION_DATE_CHANGED.equals(a) || Intent.ACTION_TIME_CHANGED.equals(a)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(a) || Intent.ACTION_BOOT_COMPLETED.equals(a)
                || "android.intent.action.MY_PACKAGE_REPLACED".equals(a)) toutMettreAJour(ctx);
    }
}
