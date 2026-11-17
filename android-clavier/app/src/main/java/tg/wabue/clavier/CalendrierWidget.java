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
    static final String[] JOURS_IK = {"", "Ɛzawlayɩ", "Imle", "Ɛkpɛ", "Ɛwlɛ", "Ɛɣla", "Ɛva", "Ewleduyɩ"};
    static final String[] JOURS_FR = {"", "dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi"};
    static final String[] MARCHES = {"", "", "Agbo", "Zogbégan", "Doumé", "Badou", "Kougnohou", ""};
    static final String[] MOIS_IK = {"Ɔlatʋ", "Ɔfʋtsɩ", "Zata", "Ɛvlɩlʋɩa", "Ʋlawlɩvɛ", "Inaglo",
            "Nekuneku", "Ipuǝnǝ", "Iwle", "Iwletsɔ", "Ɔtɩɩ", "Ɛyɛbuele"};
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
        v.setTextViewText(R.id.w_jour_fr, JOURS_FR[j] + " " + d + " " + MOIS_FR[m]);
        v.setTextViewText(R.id.w_mois, MOIS_IK[m] + " · " + d);
        String marche = MARCHES[j].isEmpty() ? "" : "🛒 Marché : " + MARCHES[j];
        v.setTextViewText(R.id.w_marche, marche);
        int n = joursAvantOvazu();
        v.setTextViewText(R.id.w_ovazu, n == 0 ? "🎉 C'est OVAZU aujourd'hui !" : "🎉 OVAZU dans " + n + (n > 1 ? " jours" : " jour"));
        Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://adomhub.github.io/ABUESA-IA-/"));
        int f = android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
        v.setOnClickPendingIntent(R.id.w_racine, PendingIntent.getActivity(ctx, 0, i, f));
        return v;
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, vue(ctx));
    }

    /** Met à jour tous les widgets (appelé aussi au changement de date). */
    static void toutMettreAJour(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, CalendrierWidget.class));
        for (int id : ids) mgr.updateAppWidget(id, vue(ctx));
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        String a = intent.getAction();
        if (Intent.ACTION_DATE_CHANGED.equals(a) || Intent.ACTION_TIME_CHANGED.equals(a)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(a)) toutMettreAJour(ctx);
    }
}
