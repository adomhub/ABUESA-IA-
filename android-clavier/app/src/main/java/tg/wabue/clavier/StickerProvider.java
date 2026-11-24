package tg.wabue.clavier;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.io.IOException;
import java.util.List;

/** Fournit le paquet de stickers WABUƐ à WhatsApp (format officiel des paquets de stickers). */
public class StickerProvider extends ContentProvider {

    public static final String AUTORITE = "tg.wabue.clavier.stickers";
    public static final String PAQUET = "wabue1";
    public static final String NOM = "WABUƐ ikposso";
    public static final String PAQUET2 = "wabue2";
    public static final String NOM2 = "WABUƐ ikposso · groupe";
    static final String EDITEUR = "WABUƐ · Adom K. Alexi";
    static final String SITE = "https://adomhub.github.io/WABOU-WAB-/";
    static final String VIE_PRIVEE = "https://adomhub.github.io/WABOU-WAB-/confidentialite.html";

    static final String[][] STICKERS = {
        {"s01.webp", "👋", "Ivuao ! · Bonjour"},
        {"s02.webp", "🙂", "Ɛza muani ? · Comment vas-tu ?"},
        {"s03.webp", "😊", "Niza ɔbɔɛ · Je vais bien"},
        {"s04.webp", "🙏", "Ewlessɛ ! · Merci"},
        {"s05.webp", "🤲", "Kʉkɔ · S'il te plaît"},
        {"s06.webp", "🙇", "Ɛyadu · Pardon"},
        {"s07.webp", "🤗", "Yanu asɛ ! · Bienvenue"},
        {"s08.webp", "👋", "Ekualɛ · À bientôt"},
        {"s09.webp", "🌇", "Izua ɛzaa · Bonsoir"},
        {"s10.webp", "😴", "Zadɛ · Bonne nuit"},
        {"s11.webp", "👍", "Eɛ · Oui"},
        {"s12.webp", "🙅", "Oo · Non"},
        {"s13.webp", "🤔", "Ninanu ayili · Je ne comprends pas"},
        {"s14.webp", "👉", "Ba-ɛmɛ ! · Viens ici"},
        {"s15.webp", "🍲", "Uvlɛ-atsɩnɔ · J'ai faim"},
        {"s16.webp", "🎉", "Dɔkayɛ ! · Félicitations"},
        {"s17.webp", "🎆", "Ilububuɛ ! · Bonne année"},
        {"s18.webp", "🕊", "Ɛtululua-kayɛ · Paix à toi"},
        {"s19.webp", "✨", "Ɩwolowʋ kotsueyɛ-ɛbua · Que Dieu te bénisse"},
        {"s20.webp", "💚", "Nilɛ akpɔssɔ · Je suis Akposso"},
        {"s21.webp", "🙋", "Azɔyɛ numɛ ? · Comment t'appelles-tu ?"},
        {"s22.webp", "🧭", "Atuna ? · D'où viens-tu ?"},
        {"s23.webp", "🚶", "Ɛyahana ? · Où vas-tu ?"},
        {"s24.webp", "🛒", "Niyaha-udja · Je vais au marché"},
        {"s25.webp", "💧", "Ɔmɔawlɩnɔ · J'ai soif"},
        {"s26.webp", "😩", "Ɩdɔ-ewuinɔ · Je suis fatigué"},
        {"s27.webp", "🪑", "Zatɩ · Assieds-toi"},
        {"s28.webp", "✅", "Ɛsɛsisɩ · C'est vrai"},
        {"s29.webp", "🤷", "Nɩneyi · Je ne sais pas"},
        {"s30.webp", "🙌", "Naduɛlu kalukawa · Respects aux anciens"},
    };

    static final String[][] STICKERS_GROUPE = {
        {"g01.webp", "👥", "Mi za muani ? · Comment allez-vous ?"},
        {"g02.webp", "👋", "Mia vlɛɛɛ ! · Bonjour à tous"},
        {"g03.webp", "🌇", "Mizaaa ! · Bonsoir à tous"},
        {"g04.webp", "😴", "Ki zadɛɛo · Bonne nuit à tous"},
    };

    private static final int METADONNEES = 1, METADONNEES_UN = 2, LISTE = 3, FICHIER = 4;
    private final UriMatcher m = new UriMatcher(UriMatcher.NO_MATCH);

    @Override
    public boolean onCreate() {
        m.addURI(AUTORITE, "metadata", METADONNEES);
        m.addURI(AUTORITE, "metadata/*", METADONNEES_UN);
        m.addURI(AUTORITE, "stickers/*", LISTE);
        m.addURI(AUTORITE, "stickers_asset/*/*", FICHIER);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] p, String s, String[] a, String o) {
        int code = m.match(uri);
        if (code == METADONNEES || code == METADONNEES_UN) {
            MatrixCursor c = new MatrixCursor(new String[]{
                    "sticker_pack_identifier", "sticker_pack_name", "sticker_pack_publisher",
                    "sticker_pack_icon", "android_play_store_link", "ios_app_download_link",
                    "sticker_pack_publisher_email", "sticker_pack_publisher_website",
                    "sticker_pack_privacy_policy_website", "sticker_pack_license_agreement_website",
                    "image_data_version", "whatsapp_will_not_cache_stickers", "animated_sticker_pack"});
            String seul = code == METADONNEES_UN ? uri.getLastPathSegment() : null;
            if (seul == null || PAQUET.equals(seul))
                c.addRow(new Object[]{PAQUET, NOM, EDITEUR, "tray.png", "", "",
                        "alexiadom64@gmail.com", SITE, VIE_PRIVEE, "", "1", 0, 0});
            if (seul == null || PAQUET2.equals(seul))
                c.addRow(new Object[]{PAQUET2, NOM2, EDITEUR, "tray.png", "", "",
                        "alexiadom64@gmail.com", SITE, VIE_PRIVEE, "", "1", 0, 0});
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        }
        if (code == LISTE) {
            MatrixCursor c = new MatrixCursor(new String[]{"sticker_file_name", "sticker_emoji", "sticker_accessibility_text"});
            String[][] liste = PAQUET2.equals(uri.getLastPathSegment()) ? STICKERS_GROUPE : STICKERS;
            for (String[] st : liste) c.addRow(new Object[]{st[0], st[1], st[2]});
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        }
        throw new IllegalArgumentException("URI inconnue : " + uri);
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) {
        if (m.match(uri) != FICHIER) return null;
        List<String> seg = uri.getPathSegments();
        if (seg.size() != 3) return null;
        String paquet = seg.get(1), fichier = seg.get(2);
        if (!(PAQUET.equals(paquet) || PAQUET2.equals(paquet)) || fichier.contains("/") || fichier.contains("..")) return null;
        try {
            return getContext().getAssets().openFd(paquet + "/" + fichier);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (m.match(uri)) {
            case METADONNEES: return "vnd.android.cursor.dir/vnd." + AUTORITE + ".metadata";
            case METADONNEES_UN: return "vnd.android.cursor.item/vnd." + AUTORITE + ".metadata";
            case LISTE: return "vnd.android.cursor.dir/vnd." + AUTORITE + ".stickers";
            case FICHIER: {
                String f = uri.getLastPathSegment();
                return f != null && f.endsWith(".png") ? "image/png" : "image/webp";
            }
        }
        return null;
    }

    @Override public Uri insert(Uri uri, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
