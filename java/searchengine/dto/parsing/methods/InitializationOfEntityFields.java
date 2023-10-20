package searchengine.dto.parsing.methods;

import lombok.Data;
import org.jsoup.nodes.Document;
import searchengine.dto.parsing.entity.SiteName;
import searchengine.model.*;
import searchengine.repository.*;
import searchengine.services.lemma.LemmaServiceImpl;

import java.util.*;

@Data
public class InitializationOfEntityFields {

    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexObjectRepository indexObjectRepository;
    private StopObjectRepository stopObjectRepository;

    public InitializationOfEntityFields(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexObjectRepository indexObjectRepository, StopObjectRepository stopObjectRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexObjectRepository = indexObjectRepository;
        this.stopObjectRepository = stopObjectRepository;
    }

    protected Site initialisationSite(Site site, int httpStatusCode, Document doc) {
        switch (httpStatusCode) {
            case 200 -> {
                site.setLastError(HttpStatusCodeError.httpStatusCodeError(httpStatusCode));
                site.setName(SiteName.siteName(doc));
            }
            default -> {
                site.setStatus(Status.FAILED);
                site.setStatusTime(new Date());
                site.setLastError(HttpStatusCodeError.httpStatusCodeError(httpStatusCode));
                site.setName("");
            }
        }
        System.out.println("save " + site.getUrl());
        siteRepository.save(site);
        return site;
    }

    protected void initialisationPage(Site site, String pathHtml, int httpCode, Document doc) {
        Page page = new Page();
        page.setCode(httpCode);
        page.setContent(String.valueOf(doc));
        String path = pathHtml.replaceAll(site.getUrl(), "");
        page.setPath(path);
        site.setStatusTime(new Date());
        page.setSite(site);
        siteRepository.save(site);
        pageRepository.save(page);
        System.out.println("save page " + path + " for site " + site.getUrl());
        initialisationLemmas(doc, site, page);
    }

    private void initialisationLemmas(Document doc, Site site, Page page) {
        LemmaServiceImpl lemmaServiceImpl = new LemmaServiceImpl(doc);
        HashMap<String, Integer> lemmas = lemmaServiceImpl.lemmas();
        Set<String> lemmasArray = lemmas.keySet();
        for (String s : lemmasArray) {
            Lemma lemma = new Lemma();
            lemma.setSite(site);
            lemma.setLemma(s);
            lemma.setFrequency(1);
            Lemma lemmaDB = lemmaRepository.findByLemmaAndSite_id(s, site.getId());
            if (lemmaDB == null) {
                lemmaRepository.save(lemma);
                initialisationIndex(lemma, page, lemmas.get(s));
            } else {
                lemmaRepository.updateContentLemma(lemmaDB.getId());
                initialisationIndex(lemmaDB, page, lemmas.get(s));
            }
        }
    }

    private void initialisationIndex(Lemma lemma, Page page, int rank) {
        IndexObject indexObject = new IndexObject();
        indexObject.setPage(page);
        indexObject.setLemma(lemma);
        indexObject.setRunk((float) rank);
        indexObjectRepository.save(indexObject);
    }

    protected void initialisationStopObject(Site site, String path) {
        StopObject stopObject = new StopObject();
        stopObject.setSite(site);
        stopObject.setPathHtml(path);
        stopObjectRepository.save(stopObject);

    }
}
