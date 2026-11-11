package com.saisonflow.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Standalone v0.1: series/season batch queue, individual episodes, 360p/480p,
 * HTTPS direct video links, persistent download history, external share intent.
 * This does NOT modify or bundle AniWave, Kibo, LOOKR, 1DM or Snaptube.
 */
public final class MainActivity extends Activity {
    private static final int IMPORT_JSON = 108;
    private static final int PAGE_SIZE = 40;
    private static final int BG = Color.rgb(11,16,29);
    private static final int PANEL = Color.rgb(28,36,55);
    private static final int FG = Color.rgb(237,244,246);
    private static final int SECONDARY = Color.rgb(169,185,190);
    private static final int ACCENT = Color.rgb(255,133,31);

    private Catalogue catalogue;
    private DownloadStore store;
    private DownloadEngine engine;
    private LinearLayout body;
    private String tab = "Accueil";
    private String linkUrl = "";
    private String linkTitle = "";
    private boolean wifiOnly = false;
    private int historyPage = 0;
    private String seriesName = null;
    private int seasonNumber = 1;
    private String wantedQuality = "Auto";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        store = new DownloadStore(this);
        engine = new DownloadEngine(this, store);
        try(InputStream in = getAssets().open("catalogue-exemple.json")) {
            catalogue = Catalogue.parse(readText(in));
        } catch(Exception e) { catalogue = new Catalogue(); }
        acceptIntent(getIntent());
        render();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        acceptIntent(intent);
        render();
    }

    private void acceptIntent(Intent intent) {
        if (intent == null) return;
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            String received = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (received != null) { linkUrl = received.trim(); tab = "Lien"; }
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            linkUrl = intent.getDataString(); tab = "Lien";
        }
    }

    private void render() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(12), dp(15), dp(12), dp(8));
        outer.setBackgroundColor(BG);
        TextView brand = text("  SaisonFlow", 31, FG, true);
        android.text.SpannableString name = new android.text.SpannableString(brand.getText());
        name.setSpan(new android.text.style.ForegroundColorSpan(ACCENT), 8, name.length(), 0);
        brand.setText(name);
        brand.setGravity(Gravity.CENTER);
        android.graphics.drawable.Drawable mark = getDrawable(R.drawable.flow_mark);
        mark.setBounds(0, 0, dp(36), dp(36));
        LinearLayout logo = new LinearLayout(this);
        logo.setGravity(Gravity.CENTER);
        brand.setCompoundDrawables(mark, null, null, null);
        logo.addView(brand); outer.addView(logo);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, dp(12), 0, dp(8));
        for (String name : new String[]{"Accueil", "Catalogue", "Lien", "Téléchargés", "Réglages"}) {
            Button b = button(name, () -> { tab = name; render(); });
            b.setTextSize(10);
            b.setAllCaps(false);
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(tab.equals(name) ? ACCENT : PANEL));
            b.setTextColor(tab.equals(name) ? BG : FG);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(47), 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            tabs.addView(b, lp);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(3), dp(2), dp(3), dp(20));
        scroll.addView(body);
        outer.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        outer.addView(tabs);
        outer.setFitsSystemWindows(true);
        setContentView(outer);
        switch(tab) {
            case "Accueil": renderSources(); break;
            case "Catalogue": renderCatalogue(); break;
            case "Lien": renderLink(); break;
            case "Téléchargés": renderHistory(); break;
            default: renderSettings();
        }
    }

    private void renderSources() {

        EditText search = edit("", "Rechercher un animé", false);
        search.setSingleLine(true);
        search.setPadding(dp(16),dp(10),dp(16),dp(10));
        search.setBackground(surface(PANEL, ACCENT));
        body.addView(search);
        addHeading("À découvrir");
        LinearLayout matches = new LinearLayout(this);
        matches.setOrientation(LinearLayout.VERTICAL);
        body.addView(matches);
        try (InputStream in = getAssets().open("animes.json")) {
            org.json.JSONArray titles = new org.json.JSONArray(readText(in));
            Runnable filter = () -> searchCatalogues(search, matches, titles);
            search.addTextChangedListener(new android.text.TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) { filter.run(); }
                public void afterTextChanged(android.text.Editable s) {}
            });
            filter.run();
            addButton("Tous les animés Miruro", () -> openSource("https://www.miruro.tv/trending"));
        } catch (Exception e) { addNote("Index local indisponible"); }
        addHeading("Catalogues");
        addButton("FRAnime", () -> openSource("https://franime.fr/"));
        addButton("Anime-Sama via Flixnet", () -> openSource("https://flixnet.site/animesama/"));
        addButton("Kibo", () -> openSource("https://www.kiboanime.app/"));
        addButton("Aniwave · Miruro", () -> openSource("https://www.miruro.tv/"));
        addButton("French Stream", () -> openSource("https://fss.lol/"));
        addButton("MovieBox", () -> openSource("https://officialmoviebox.com/"));
        addButton("Hanime · 18+", () -> openSource("https://hanime.tv/"));
    }

    private final java.util.concurrent.ExecutorService searches = java.util.concurrent.Executors.newFixedThreadPool(3);
    private final android.os.Handler debounce = new android.os.Handler(android.os.Looper.getMainLooper());
    private int searchVersion;

    private void searchCatalogues(EditText field, LinearLayout list, org.json.JSONArray titles) {
        int version = ++searchVersion;
        debounce.removeCallbacksAndMessages(null);
        list.removeAllViews();
        String query = field.getText().toString().trim().toLowerCase(Locale.ROOT);
        java.util.Map<String, LinearLayout> groups = new java.util.LinkedHashMap<>();
        for (int i = 0; i < titles.length(); i++) {
            org.json.JSONObject hit = titles.optJSONObject(i);
            String title = hit.optString("title");
            if (title.toLowerCase(Locale.ROOT).contains(query) && (!query.isEmpty() || cover(title) != 0))
                addHit(list, groups, title, "Miruro", hit.optString("url"));
            if (query.isEmpty() && groups.size() == 7) break;
        }
        if (query.length() < 2) return;
        debounce.postDelayed(() -> {
            for (String provider : new String[]{"French Stream", "Internet Archive"}) {
                searches.execute(() -> {
                    java.util.Map<String,String> hits = new java.util.LinkedHashMap<>();
                    String failure = null;
                    try {
                        if (provider.equals("French Stream")) {
                            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                                    new java.util.zip.GZIPInputStream(getAssets().open("french-stream.tsv.gz")), StandardCharsets.UTF_8))) {
                                String path;
                                while ((path = reader.readLine()) != null && hits.size() < 40) {
                                    String title = path.substring(path.indexOf((char)45)+1, path.length()-5).replace((char)45,(char)32);
                                    if (title.contains(query)) hits.put(title, "https://fss.lol/" + path);
                                }
                            }
                        } else {
                            String safe = query.replaceAll("[^\\p{L}\\p{N} ]", " ").trim();
                            if (safe.isEmpty()) return;
                            String term = "mediatype:movies AND title:\"" + safe + "\"";
                            java.net.URL url = new java.net.URL("https://archive.org/advancedsearch.php?q=" +
                                    Uri.encode(term) + "&fl[]=identifier&fl[]=title&rows=20&output=json");
                            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                            conn.setConnectTimeout(6000); conn.setReadTimeout(8000);
                            try (InputStream in = conn.getInputStream()) {
                                org.json.JSONArray docs = new org.json.JSONObject(readText(in)).getJSONObject("response").getJSONArray("docs");
                                for (int i=0; i<docs.length(); i++) {
                                    org.json.JSONObject doc = docs.getJSONObject(i);
                                    hits.put(doc.getString("title"), "https://archive.org/details/" + Uri.encode(doc.getString("identifier")));
                                }
                            } finally { conn.disconnect(); }
                        }
                    } catch (Exception e) { failure = provider + " indisponible"; }
                    String error = failure;
                    runOnUiThread(() -> {
                        if (version != searchVersion || !list.isAttachedToWindow()) return;
                        for (Map.Entry<String,String> hit : hits.entrySet())
                            addHit(list, groups, hit.getKey(), provider, hit.getValue());
                        if (error != null || hits.isEmpty()) list.addView(text(error != null ? error : provider + " · aucun résultat", 13, SECONDARY, false));
                    });
                });
            }
        }, 350);
    }

    private void addHit(LinearLayout list, Map<String,LinearLayout> groups, String title, String provider, String url) {
        String key = title.trim().toLowerCase(Locale.ROOT);
        LinearLayout group = groups.get(key);
        if (group == null) {
            LinearLayout row = card();
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int poster = cover(title);
            if (poster != 0) {
                android.widget.ImageView image = new android.widget.ImageView(this);
                image.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                image.setImageResource(poster);
                image.setBackground(surface(PANEL,PANEL));
                image.setClipToOutline(true);
                row.addView(image,new LinearLayout.LayoutParams(dp(92),dp(128)));
            }
            group = new LinearLayout(this);
            group.setOrientation(LinearLayout.VERTICAL);
            group.setPadding(dp(12),0,0,0);
            row.addView(group,new LinearLayout.LayoutParams(0,-2,1));
            group.addView(text(title, 21, FG, true));
            list.addView(row);
            groups.put(key, group);

        }
        group.addView(button(provider, () -> openSource(url)));
    }

    @Override protected void onDestroy() {
        debounce.removeCallbacksAndMessages(null);
        searches.shutdownNow();
        super.onDestroy();
    }

    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    private void openSource(String url) {
        android.webkit.WebView web = new android.webkit.WebView(this);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.setWebChromeClient(new android.webkit.WebChromeClient());
        web.setWebViewClient(new android.webkit.WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(android.webkit.WebView view,
                    android.webkit.WebResourceRequest request) {
                return !"https".equals(request.getUrl().getScheme());
            }
        });
        web.setDownloadListener((source, agent, disposition, type, size) -> {
            if (!QualitySelector.isSupportedDirectVideo(source)) {
                toast("Ce lien ne fournit pas de fichier vidéo direct."); return;
            }
            new AlertDialog.Builder(this).setTitle("Télécharger cette vidéo ?")
                .setNegativeButton("Annuler", null).setPositiveButton("Télécharger", (dialog, button) -> {
                    try { engine.enqueue(source, web.getTitle() == null ? "Vidéo" : web.getTitle(), "original", wifiOnly); toast("Ajouté aux téléchargements"); }
                    catch (RuntimeException ex) { alert("Téléchargement impossible", ex.getMessage()); }
                }).show();
        });
        android.app.Dialog window = new android.app.Dialog(this, android.R.style.Theme_Material_NoActionBar);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(button("Retour aux sources", window::dismiss));
        layout.addView(web, new LinearLayout.LayoutParams(-1, 0, 1f));
        window.setContentView(layout);
        window.setOnKeyListener((dialog, key, event) -> {
            if (key != android.view.KeyEvent.KEYCODE_BACK || !web.canGoBack()) return false;
            if (event.getAction() == android.view.KeyEvent.ACTION_UP) web.goBack();
            return true;
        });
        window.setOnDismissListener(dialog -> { web.stopLoading(); layout.removeView(web); web.destroy(); });
        window.show();
        web.loadUrl(url);
    }

    private void renderCatalogue() {
        addHeading("Séries et saisons");
        addNote("Bandes-annonces incluses : Big Buck Bunny © Blender Foundation (peach.blender.org) et Sintel © Blender Foundation (sintel.org), sous licence Creative Commons Attribution 3.0.");
        addButton("Importer mon catalogue JSON", this::importCatalogue);
        if (catalogue.bySeries.isEmpty()) return;

        List<String> names = new ArrayList<>(catalogue.bySeries.keySet());
        if (!names.contains(seriesName)) seriesName = names.get(0);
        addNote("Série");
        Spinner seriesSpinner = spinner(names, names.indexOf(seriesName));
        body.addView(seriesSpinner);

        List<Integer> seasonNumbers = new ArrayList<>(catalogue.bySeries.get(seriesName).keySet());
        if (seasonNumbers.isEmpty()) return;
        if (!seasonNumbers.contains(seasonNumber)) seasonNumber = seasonNumbers.get(0);
        addNote("Saison");
        List<String> seasons = new ArrayList<>();
        for (int n : seasonNumbers) seasons.add("Saison " + n);
        Spinner seasonSpinner = spinner(seasons, seasonNumbers.indexOf(seasonNumber));
        body.addView(seasonSpinner);
        addNote("Qualité demandée (disponible selon les liens fournis)");
        Spinner qualitySpinner = qualitySpinner();
        body.addView(qualitySpinner);

        seriesSpinner.setOnItemSelectedListener(new SimpleSelected() {
            @Override public void selected(int pos) {
                String value = names.get(pos);
                if (!value.equals(seriesName)) { seriesName = value; seasonNumber = 1; render(); }
            }
        });
        seasonSpinner.setOnItemSelectedListener(new SimpleSelected() {
            @Override public void selected(int pos) {
                int value = seasonNumbers.get(pos);
                if (value != seasonNumber) { seasonNumber = value; render(); }
            }
        });

        List<Episode> episodes = catalogue.bySeries.get(seriesName).get(seasonNumber);
        addHeading("Épisodes : " + episodes.size());
        addNote("Choisis les épisodes ou lance toute la saison. Chaque épisode nécessite sa propre URL de fichier vidéo pour la qualité demandée.");
        List<CheckBox> choices = new ArrayList<>();
        for (Episode episode : episodes) {
            LinearLayout card = card();
            CheckBox checkbox = new CheckBox(this);
            checkbox.setText("Épisode " + episode.number + " · " + episode.title);
            checkbox.setTextColor(FG);
            checkbox.setChecked(true);
            card.addView(checkbox);
            choices.add(checkbox);
            String available = "Qualités fournies : ";
            boolean any = false;
            for (String q : new String[]{"360p", "480p", "original"}) {
                if (QualitySelector.isSupportedDirectVideo(episode.qualities.get(q))) {
                    available += (any ? ", " : "") + q;
                    any = true;
                }
            }
            addText(card, any ? available : "Aucun lien vidéo direct configuré", 12, SECONDARY, false);
            card.addView(button("Regarder", () -> {
                String source = QualitySelector.select(episode.qualities, wantedQuality);
                if (source == null) { toast("Qualité indisponible."); return; }
                playVideo(Uri.parse(source), episode.title);
            }));
            Button one = button("Télécharger cet épisode", () -> queueEpisodes(java.util.Collections.singletonList(episode), wantedQuality));
            one.setTextSize(12);
            card.addView(one);
            body.addView(card);
        }
        addButton("Télécharger les épisodes cochés", () -> {
            List<Episode> selected = new ArrayList<>();
            for (int i = 0; i < choices.size(); i++) if (choices.get(i).isChecked()) selected.add(episodes.get(i));
            queueEpisodes(selected, wantedQuality);
        });
        addButton("Télécharger toute la saison", () -> queueEpisodes(episodes, wantedQuality));
    }

    private void queueEpisodes(List<Episode> episodes, String quality) {
        if (episodes.isEmpty()) { toast("Aucun épisode sélectionné."); return; }
        int queued = 0, skipped = 0;
        for (Episode episode : episodes) {
            String url = QualitySelector.select(episode.qualities, quality);
            if (!QualitySelector.isSupportedDirectVideo(url)) { skipped++; continue; }
            try {
                engine.enqueue(url, episode.displayName(), quality, wifiOnly);
                queued++;
            } catch(Exception ex) { skipped++; }
        }
        alert("Mise en téléchargement", queued + " épisode(s) mis en file. " + skipped +
                " ignoré(s) faute de lien compatible ou en raison d'une erreur. Consulte l'onglet Téléchargés.");
    }

    private void renderLink() {
        addHeading("Télécharger par lien");
        addNote("Colle un lien HTTPS direct vers un fichier vidéo .mp4, .webm, .mkv, .mov ou .m4v. Les pages de streaming et les playlists .m3u8 ne sont pas encore gérées par le moteur v0.1.");
        EditText url = edit(linkUrl, "https://exemple.org/ma-video.mp4", true);
        body.addView(url);
        EditText title = edit(linkTitle, "Nom de la vidéo (facultatif)", false);
        body.addView(title);
        addNote("Qualité réelle du lien (étiquette de classement, pas conversion)");
        Spinner quality = qualitySpinner();
        body.addView(quality);
        addButton("Télécharger cette vidéo", () -> {
            linkUrl = url.getText().toString().trim();
            linkTitle = title.getText().toString().trim();
            if (linkTitle.isEmpty()) linkTitle = "Ma vidéo";
            try {
                engine.enqueue(linkUrl, linkTitle, wantedQuality, wifiOnly);
                toast("Ajouté aux téléchargements");
                tab = "Téléchargés"; render();
            } catch(Exception ex) { alert("Lien non pris en charge", ex.getMessage()); }
        });
        addButton("Partager le lien vers 1DM ou un autre gestionnaire", () -> {
            linkUrl = url.getText().toString().trim();
            if (linkUrl.isEmpty()) { toast("Colle d'abord un lien."); return; }
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain"); share.putExtra(Intent.EXTRA_TEXT, linkUrl);
            startActivity(Intent.createChooser(share, "Choisir une application compatible"));
        });
        addButton("Copier le lien", () -> {
            linkUrl = url.getText().toString().trim();
            ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Lien vidéo", linkUrl));
            toast("Lien copié");
        });
    }

    private void renderHistory() {
        int count = store.count();
        addHeading("Téléchargements (" + count + ")");
        addNote("Téléchargements Android système : plusieurs fichiers peuvent être mis en file et transférés en arrière-plan. L'ordonnancement et la concurrence sont gérés par Android dans cette version.");
        addButton("Actualiser", this::render);
        if (count == 0) { addNote("Aucun téléchargement pour le moment."); return; }
        int maxPage = (count-1) / PAGE_SIZE;
        if (historyPage > maxPage) historyPage = maxPage;
        addNote("Page " + (historyPage + 1) + " sur " + (maxPage + 1) + " (40 par page, historique non plafonné)");
        for (DownloadStore.Record record : store.page(historyPage, PAGE_SIZE)) {
            LinearLayout card = card();
            addText(card, record.title, 15, FG, true);
            addText(card, "Qualité : " + record.quality + " · ID " + record.id, 12, SECONDARY, false);
            int status = -1;
            long downloaded = 0, total = -1;
            try(Cursor c = engine.system().query(new DownloadManager.Query().setFilterById(record.id))) {
                if (c != null && c.moveToFirst()) {
                    status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    downloaded = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                }
            } catch(Exception ignored) { }
            addText(card, statusLabel(status, downloaded, total), 13, ACCENT, false);
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                Button play = button("Lire la vidéo", () -> openCompleted(record.id));
                card.addView(play);
            }
            Button delete = button("Annuler / supprimer le téléchargement", () -> {
                new AlertDialog.Builder(this).setTitle("Supprimer ?")
                    .setMessage("Supprimer ce transfert et son fichier local, s'il existe ?")
                    .setNegativeButton("Retour", null)
                    .setPositiveButton("Supprimer", (d, w) -> {
                        engine.system().remove(record.id);
                        store.remove(record.id);
                        render();
                    }).show();
            });
            delete.setTextSize(12);
            card.addView(delete);
            body.addView(card);
        }
        LinearLayout pages = new LinearLayout(this);
        if (historyPage > 0) {
            Button previous = button("Précédent", () -> { historyPage--; render(); });
            pages.addView(previous, new LinearLayout.LayoutParams(0, -2, 1));
        }
        if (historyPage < maxPage) {
            Button next = button("Suivant", () -> { historyPage++; render(); });
            pages.addView(next, new LinearLayout.LayoutParams(0, -2, 1));
        }
        body.addView(pages);
    }

    private String statusLabel(int status, long done, long total) {
        String label;
        switch(status) {
            case DownloadManager.STATUS_PENDING: label="En attente"; break;
            case DownloadManager.STATUS_RUNNING: label="En cours"; break;
            case DownloadManager.STATUS_PAUSED: label="Mis en attente par Android"; break;
            case DownloadManager.STATUS_SUCCESSFUL: label="Terminé"; break;
            case DownloadManager.STATUS_FAILED: label="Échec — réessaie avec une URL valide"; break;
            default: label="Transfert introuvable";
        }
        if (total > 0) label += " · " + Math.min(100, done*100/total) + "%";
        else if (done > 0) label += " · " + (done/1048576) + " MiB";
        return label;
    }

    private void openCompleted(long id) {
        Uri uri = engine.system().getUriForDownloadedFile(id);
        if (uri == null) { toast("Fichier non trouvé."); return; }
        playVideo(uri, "Lecture hors connexion");
    }

    private void playVideo(Uri uri, String title) {
        android.widget.VideoView video = new android.widget.VideoView(this);
        android.widget.MediaController controls = new android.widget.MediaController(this);
        video.setMediaController(controls);
        controls.setAnchorView(video);
        AlertDialog player = new AlertDialog.Builder(this).setTitle(title).setView(video)
                .setNegativeButton("Fermer", null).create();
        player.setOnDismissListener(dialog -> video.stopPlayback());
        video.setOnErrorListener((media, what, extra) -> {
            player.dismiss();
            alert("Lecture impossible", "Vérifie la connexion ou la disponibilité du fichier.");
            return true;
        });
        player.show();
        video.setVideoURI(uri);
        video.start();
    }

    private void renderSettings() {
        addHeading("Réglages");
        CheckBox wifi = new CheckBox(this);
        wifi.setText("Télécharger uniquement avec réseau non facturé (généralement Wi-Fi)");
        wifi.setTextColor(FG);
        wifi.setChecked(wifiOnly);
        wifi.setOnCheckedChangeListener((v, checked) -> wifiOnly = checked);
        body.addView(wifi);
        addNote("Les fichiers sont enregistrés dans le dossier Movies propre à SaisonFlow. Android gère les transferts, les notifications et leur poursuite en arrière-plan.");
        addButton("Ouvrir les téléchargements système", () -> {
            try { startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)); }
            catch(Exception ex) { toast("Impossible d'ouvrir les téléchargements système."); }
        });
        addHeading("À venir");
        addNote("Accélération multi-segments HTTP Range; moteur de pause/reprise personnalisée; HLS/DASH autorisés sans DRM; sous-titres externes; lecteur intégré; recherche; paramètres de concurrence. Les fonctions affichées ici ne sont pas encore implémentées.");
        addNote("SaisonFlow est un projet indépendant. Aucun code, logo, nom de marque ou catalogue propriétaire de Kibo, AniWave, LOOKR, 1DM ou Snaptube n'est inclus.");
    }

    private void importCatalogue() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, IMPORT_JSON);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != IMPORT_JSON || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try(InputStream in = getContentResolver().openInputStream(data.getData())) {
            if (in == null) throw new IllegalArgumentException("Document illisible");
            Catalogue loaded = Catalogue.parse(readText(in));
            catalogue = loaded;
            seriesName = null; seasonNumber = 1;
            toast("Catalogue importé : " + catalogue.bySeries.size() + " série(s)");
            render();
        } catch(Exception ex) { alert("Importation impossible", ex.toString()); }
    }
    private static String readText(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192]; int n;
        while ((n = in.read(buf)) >= 0) out.write(buf,0,n);
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private void addHeading(String value) { TextView t = text(value,19,FG,true); t.setPadding(0,dp(15),0,dp(10)); body.addView(t); }
    private void addNote(String value) { TextView t = text(value,13,SECONDARY,false); t.setPadding(0,dp(6),0,dp(9)); body.addView(t); }
    private void addButton(String label, Runnable task) { Button b = button(label, task); b.setAllCaps(false); body.addView(b); }
    private TextView text(String value, int sp, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(sp); v.setTextColor(color);
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD); return v;
    }
    private void addText(LinearLayout parent, String value, int sp, int color, boolean bold) {
        TextView t = text(value,sp,color,bold); t.setPadding(0,dp(4),0,dp(5)); parent.addView(t);
    }
    private int cover(String title) {
        switch (title.toLowerCase(Locale.ROOT)) {
            case "naruto": return R.drawable.cover_naruto;
            case "naruto: shippuden": return R.drawable.cover_shippuden;
            case "one piece": return R.drawable.cover_one_piece;
            case "black clover": return R.drawable.cover_black_clover;
            case "bleach": return R.drawable.cover_bleach;
            case "hunter×hunter (2011)": return R.drawable.cover_hunter;
            case "vinland saga": return R.drawable.cover_vinland;
            default: return 0;
        }
    }
    private android.graphics.drawable.GradientDrawable surface(int color, int border) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(color); shape.setCornerRadius(dp(14)); shape.setStroke(dp(1),border); return shape;
    }
    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(12), dp(10), dp(12), dp(10)); l.setBackground(surface(PANEL, Color.rgb(54,64,83)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,dp(5),0,dp(8)); l.setLayoutParams(lp); return l;
    }
    private Button button(String label, Runnable runnable) {
        Button b = new Button(this); b.setText(label); b.setTextSize(13); b.setAllCaps(false);
        b.setTextColor(FG); b.setOnClickListener(v -> runnable.run()); return b;
    }
    private EditText edit(String value, String hint, boolean url) {
        EditText e = new EditText(this); e.setText(value); e.setHint(hint); e.setTextColor(FG);
        e.setHintTextColor(SECONDARY);
        e.setInputType(url ? InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI : InputType.TYPE_CLASS_TEXT);
        e.setSingleLine(!url); e.setMinLines(url?2:1); return e;
    }
    private Spinner spinner(List<String> options, int selected) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter); s.setSelection(Math.max(0,selected)); return s;
    }
    private Spinner qualitySpinner() {
        List<String> options = java.util.Arrays.asList("Auto", "360p", "480p");
        Spinner sp = spinner(options, Math.max(0,options.indexOf(wantedQuality)));
        sp.setOnItemSelectedListener(new SimpleSelected() {
            @Override public void selected(int pos) { wantedQuality = options.get(pos); }
        });
        return sp;
    }
    private abstract static class SimpleSelected implements android.widget.AdapterView.OnItemSelectedListener {
        public abstract void selected(int pos);
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { selected(position); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
    }
    private void alert(String title, String detail) { new AlertDialog.Builder(this).setTitle(title).setMessage(detail).setPositiveButton("OK", null).show(); }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
    private int dp(int d) { return Math.round(d*getResources().getDisplayMetrics().density); }
}
