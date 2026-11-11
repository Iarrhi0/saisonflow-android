package com.saisonflow.app;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/** V0.1 uses Android's persistent DownloadManager; no DRM, HLS, DASH or scraping. */
public final class DownloadEngine {
    private static final AtomicInteger sequence = new AtomicInteger();
    private final Context context;
    private final DownloadManager manager;
    private final DownloadStore store;

    public DownloadEngine(Context context, DownloadStore store) {
        this.context = context;
        this.store = store;
        this.manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }
    public DownloadManager system() { return manager; }

    public long enqueue(String url, String displayTitle, String quality, boolean wifiOnly) {
        url = QualitySelector.supported(url);
        if (!QualitySelector.isSupportedDirectVideo(url))
            throw new IllegalArgumentException("Lien HTTPS direct vidéo nécessaire (MP4, WEBM, MKV...). Les pages Web et flux HLS ne sont pas pris en charge dans cette version.");
        String path = Uri.parse(url).getPath();
        String ext = path.substring(path.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        String safeTitle = displayTitle.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (safeTitle.length() > 64) safeTitle = safeTitle.substring(0, 64);
        if (safeTitle.isEmpty()) safeTitle = "video";
        String filename = safeTitle + "_" + System.currentTimeMillis() + "_" + sequence.incrementAndGet() + ext;

        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
        req.setTitle(displayTitle);
        req.setDescription("SaisonFlow · " + quality);
        req.setAllowedOverMetered(!wifiOnly);
        req.setAllowedOverRoaming(false);
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        req.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, filename);
        long id = manager.enqueue(req);
        store.save(id, displayTitle, quality);
        return id;
    }
}
