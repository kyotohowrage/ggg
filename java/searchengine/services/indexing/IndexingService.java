package searchengine.services.indexing;

import searchengine.dto.parsing.entity.MarkStop;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public interface IndexingService {
    List<String> initialisationArrayPath();

    void initialisationIndexing(String pathHtml, MarkStop markStop) throws IOException;

    static String normalisePathParent(String pathParent) {
        return null;
    }

    HashSet<String> pathToThePageFromBD(String pathHtml);

    boolean pageRefresh(String url) throws IOException;
}
