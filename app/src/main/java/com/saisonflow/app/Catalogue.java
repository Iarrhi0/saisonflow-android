package com.saisonflow.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Catalogue {
    public final Map<String, Map<Integer, List<Episode>>> bySeries = new LinkedHashMap<>();

    public static Catalogue parse(String json) throws Exception {
        Catalogue c = new Catalogue();
        JSONObject root = new JSONObject(json);
        JSONArray series = root.getJSONArray("series");
        for (int i = 0; i < series.length(); i++) {
            JSONObject s = series.getJSONObject(i);
            String title = s.getString("title").trim();
            if (title.isEmpty() || c.bySeries.containsKey(title)) throw new IllegalArgumentException("Nom de série vide ou dupliqué");
            Map<Integer, List<Episode>> seasons = new LinkedHashMap<>();
            JSONArray seasonArr = s.getJSONArray("seasons");
            for (int j = 0; j < seasonArr.length(); j++) {
                JSONObject se = seasonArr.getJSONObject(j);
                int seasonNum = se.getInt("number");
                if (seasonNum < 1 || seasons.containsKey(seasonNum)) throw new IllegalArgumentException("Saison invalide / dupliquée");
                List<Episode> episodes = new ArrayList<>();
                JSONArray epArr = se.getJSONArray("episodes");
                for (int k = 0; k < epArr.length(); k++) {
                    JSONObject ep = epArr.getJSONObject(k);
                    int episodeNum = ep.getInt("number");
                    if (episodeNum < 1) throw new IllegalArgumentException("Numéro d'épisode invalide");
                    JSONObject q = ep.getJSONObject("qualities");
                    Map<String, String> urls = new LinkedHashMap<>();
                    for (String quality : new String[]{"360p", "480p", "original"}) {
                        if (q.has(quality)) urls.put(quality, q.optString(quality, ""));
                    }
                    episodes.add(new Episode(title, seasonNum, episodeNum, ep.optString("title", "Episode " + episodeNum), urls));
                }
                seasons.put(seasonNum, episodes);
            }
            c.bySeries.put(title, seasons);
        }
        return c;
    }
}
