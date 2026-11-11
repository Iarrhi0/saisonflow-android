package com.saisonflow.app;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Pure Java media metadata. No code from third-party streaming applications. */
public final class Episode {
    public final String seriesTitle;
    public final int season;
    public final int number;
    public final String title;
    public final Map<String, String> qualities;

    public Episode(String seriesTitle, int season, int number, String title, Map<String, String> qualities) {
        this.seriesTitle = seriesTitle;
        this.season = season;
        this.number = number;
        this.title = title;
        this.qualities = Collections.unmodifiableMap(new HashMap<>(qualities));
    }

    public String displayName() {
        return seriesTitle + " · S" + season + "E" + number + " · " + title;
    }
}
