package com.saisonflow.app;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

/**
 * Only selects *actual supplied sources*. A requested 360p / 480p quality is not
 * a transcoding instruction. Unsupported/unauthorized web pages and HLS manifests
 * cannot be downloaded by the v0.1 DownloadManager engine.
 */
public final class QualitySelector {
    private QualitySelector() {}

    public static String select(Map<String, String> options, String requested) {
        if (options == null) return null;
        if (!"Auto".equals(requested)) return supported(options.get(requested));
        for (String resolution : new String[]{"480p", "360p", "original"}) {
            String url = supported(options.get(resolution));
            if (url != null) return url;
        }
        return null;
    }

    private static String clean(String text) {
        return text == null || text.trim().isEmpty() ? null : text.trim();
    }

    public static String supported(String text) {
        String url = clean(text);
        return isSupportedDirectVideo(url) ? url : null;
    }

    public static boolean isSupportedDirectVideo(String rawUrl) {
        if (rawUrl == null) return false;
        try {
            URI uri = new URI(rawUrl.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) return false;
            String path = uri.getPath();
            if (path == null) return false;
            String s = path.toLowerCase(Locale.ROOT);
            return s.endsWith(".mp4") || s.endsWith(".webm") || s.endsWith(".mkv")
                || s.endsWith(".mov") || s.endsWith(".m4v") || s.endsWith(".3gp");
        } catch (Exception ignored) { return false; }
    }
}
