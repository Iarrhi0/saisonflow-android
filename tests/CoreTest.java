import com.saisonflow.app.Episode;
import com.saisonflow.app.QualitySelector;
import java.util.HashMap;
import java.util.Map;

public final class CoreTest {
    static int assertions=0;
    static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Map<String,String> qualities = new HashMap<>();
        qualities.put("360p", "https://media.example.org/s1e1-360.mp4");
        qualities.put("480p", "https://media.example.org/s1e1-480.mp4");
        Episode e = new Episode("Ma série",1,1,"Introduction",qualities);
        check(e.displayName().contains("S1E1"),"Saison/épisode");
        check(QualitySelector.select(qualities,"360p").endsWith("360.mp4"),"360p selection");
        check(QualitySelector.select(qualities,"480p").endsWith("480.mp4"),"480p selection");
        check(QualitySelector.select(qualities,"Auto").endsWith("480.mp4"),"auto 480 preferred");
        qualities.remove("480p");
        check(QualitySelector.select(qualities,"Auto").endsWith("360.mp4"),"auto 360 fallback");
        check(QualitySelector.select(qualities,"480p")==null,"No fake 480p");
        check(QualitySelector.isSupportedDirectVideo("https://example.org/video.mp4?token=xx"),"signed mp4");
        check(QualitySelector.isSupportedDirectVideo("https://example.org/video.WEBM"),"webm");
        check(!QualitySelector.isSupportedDirectVideo("http://example.org/video.mp4"),"https required");
        check(!QualitySelector.isSupportedDirectVideo("https://youtube.com/watch?v=1"),"not youtube pages");
        check(!QualitySelector.isSupportedDirectVideo("https://example.org/playlist.m3u8"),"HLS future only");
        check(!QualitySelector.isSupportedDirectVideo("file:///sdcard/video.mp4"),"invalid scheme");
        check(QualitySelector.select(new HashMap<>(),"Auto")==null,"missing sources");
        qualities.put("480p", "https://example.org/watch/123");
        check(QualitySelector.select(qualities,"Auto").endsWith("360.mp4"),"invalid preferred URL falls back");
        check(QualitySelector.select(qualities,"480p")==null,"invalid explicit quality rejected");
        qualities.put("360p", "  https://example.org/video.mp4  ");
        check("https://example.org/video.mp4".equals(QualitySelector.select(qualities,"Auto")),"trim before enqueue");
        check(QualitySelector.select(null,"Auto")==null,"null sources");
        check(!QualitySelector.isSupportedDirectVideo(null),"null URL");
        check(!QualitySelector.isSupportedDirectVideo("https://example.org/video.mp4 extra"),"malformed URL");
        System.out.println("OK: " + assertions + " core assertions");
    }
}
