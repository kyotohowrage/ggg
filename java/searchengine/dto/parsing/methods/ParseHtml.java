package searchengine.dto.parsing.methods;

import lombok.Data;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.dto.parsing.entity.MarkStop;
import searchengine.dto.parsing.entity.RequestStartTime;
import searchengine.model.Site;
import searchengine.repository.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@Data
public class ParseHtml extends RecursiveTask<ArrayList<String>> {
    private String htmlFile;
    private HashSet<String> allLink = new HashSet<>();
    private MarkStop markStop;
    private Document doc;
    private HashSet<String> absUrl;
    private String pathParent;
    private Site site;
    private RequestStartTime startTime;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexObjectRepository indexObjectRepository;
    private StopObjectRepository stopObjectRepository;
    private InitializationOfEntityFields initializationOfEntityFields;
    private Connection.Response response;
    private Elements element;

    public ParseHtml(HashSet<String> pathArray, Site site, String pathParent, String pathHtml, HashSet<String> allLink, MarkStop markStop, RequestStartTime startTime, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexObjectRepository indexObjectRepository, StopObjectRepository stopObjectRepository) throws IOException {
        this.absUrl = pathArray;
        this.site = site;
        this.htmlFile = pathHtml;
        this.pathParent = pathParent;
        this.allLink = allLink;
        this.markStop = markStop;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexObjectRepository = indexObjectRepository;
        this.stopObjectRepository = stopObjectRepository;
        this.initializationOfEntityFields = new InitializationOfEntityFields(siteRepository, pageRepository, lemmaRepository, indexObjectRepository, stopObjectRepository);
        this.startTime = startTime;
        startTime.setDateStart(System.currentTimeMillis());
        response = Jsoup.connect(pathHtml).followRedirects(true).execute();
        switch (response.statusCode()) {
            case 200 -> {
                this.doc = Jsoup.connect(htmlFile)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                        .referrer("http://www.google.com")
                        .get();
            }
            default -> {
                absUrl = new HashSet<>();
            }
        }
        this.element = doc.select("a");
        absUrl.addAll(element.stream().map(element1 -> element1.absUrl("href")).collect(Collectors.toSet()));
    }


    @Override
    protected ArrayList<String> compute() {
        HashSet<String> name = allLink;
        ArrayList<String> name2;
        ArrayList<ParseHtml> tasks = new ArrayList<>();
        if (name.isEmpty()) {
            initializationOfEntityFields.initialisationSite(site, response.statusCode(), doc);
            initializationOfEntityFields.initialisationPage(site, pathParent, response.statusCode(), doc);
        }
        if (absUrl == null) {
            return new ArrayList<>();
        }
        initPage(name, tasks);
        for (ParseHtml url : tasks) {
            name.addAll(url.join());
        }
        name2 = (ArrayList<String>) name.stream().collect(Collectors.toList());
        return name2;
    }

    private void initPage(HashSet<String> name, ArrayList<ParseHtml> tasks) {
        try {
            ArrayList<String> arrayAbsUrl = (ArrayList<String>) absUrl.stream().collect(Collectors.toList());
            for (int i = 0; i < arrayAbsUrl.size(); i++) {
                if (arrayAbsUrl.get(i).matches(pathParent + "([\\/[a-z0-9-]+]+\\/?[a-z0-9-]*\\/*(.html)?\"?)")) {
                    if (!name.contains(arrayAbsUrl.get(i))) {
                        name.add(arrayAbsUrl.get(i));
                        long startQueryHtml = System.currentTimeMillis();
                        if ((startQueryHtml - startTime.getDateStart()) < 5000) {
                            Thread.sleep(5000 - (startQueryHtml - startTime.getDateStart()));
                        }
                        ParseHtml html;
                        try {
                            if (!markStop.isMarkStop()) {
                                html = new ParseHtml(absUrl, site, pathParent, arrayAbsUrl.get(i), allLink, markStop, startTime, siteRepository, pageRepository, lemmaRepository, indexObjectRepository, stopObjectRepository);
                                initializationOfEntityFields.initialisationPage(site, arrayAbsUrl.get(i), response.statusCode(), doc);
                                html.fork();
                                tasks.add(html);
                            } else {
                                initializationOfEntityFields.initialisationStopObject(site, arrayAbsUrl.get(i));
                            }
                        } catch (IOException e) {throw new RuntimeException(e);}
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}