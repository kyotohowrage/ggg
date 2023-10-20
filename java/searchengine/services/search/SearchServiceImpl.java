package searchengine.services.search;

import com.github.demidko.aot.WordformMeaning;
import lombok.Getter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.ArrayUtils;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.response.SnippetEntity;
import searchengine.model.Status;
import searchengine.services.indexing.IndexingServiceImpl;
import searchengine.services.lemma.LemmaServiceImpl;
import searchengine.dto.response.ResponseSearch;
import searchengine.dto.response.ResponseSearchData;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexObjectRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;
import static searchengine.services.indexing.IndexingServiceImpl.normalisePathParent;

@Service
@Getter
public class SearchServiceImpl implements SearchService {
    private final SitesList sites;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexObjectRepository indexObjectRepository;
    private final String colorGreen = "\u001b[32m";
    private final String colorReset = "\u001b[0m";
    private final String colorBlue = "\u001b[34m";

    @Autowired
    public SearchServiceImpl(SitesList sites, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexObjectRepository indexObjectRepository) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexObjectRepository = indexObjectRepository;
    }

    int sr6 = 0;

    @Override
    public ResponseSearch searchResponce(String query, int offset, int limit, String site) {
        ResponseSearch responseSearch = new ResponseSearch();
        String normalizedSite = site;
        if (queryIsEmpty(query)) {
            responseSearch.setError("Задан пустой поисковый запрос");
            return responseSearch;
        }
        LemmaServiceImpl lemmaServiceImpl = new LemmaServiceImpl();
        HashSet<String> arrayLemmasQuery = new HashSet<>();
        arrayLemmasQuery.addAll(lemmaServiceImpl.stringsForLemmas(query));
        int site_id = 0;
        if (site != null) {
            normalizedSite = IndexingServiceImpl.normalisePathParent(site);
            site_id = siteRepository.findIdByUrl(normalizedSite);
            System.out.println(colorGreen + "siteRepository5 " + ++sr6);
        }
        ArrayList<String> sortedFrequencyLemmas = frequencyLemmaQuery(arrayLemmasQuery, site_id);
        ArrayList<ResponseSearchData> responseSearchData = responseObjectArrayList(sortedFrequencyLemmas, normalizedSite);
        ArrayList<ResponseSearchData> responseSearchDataLimit = (ArrayList<ResponseSearchData>) responseSearchData.stream().skip(offset).limit(limit).collect(Collectors.toList());
        if (!isTheSiteIndexed(normalizedSite)) {
            responseSearch.setError("Индексация не завершена! Ответ на запрос будет не полон!");
        }
        responseSearch.setError("");
        responseSearch.setResult(true);
        responseSearch.setCount(responseSearchData.size());
        responseSearch.setData(responseSearchDataLimit);
        return responseSearch;
    }


    public boolean queryIsEmpty(String query) {
        String[] strings = query.split("[^а-яёЁА-Я]");
        if (ArrayUtils.isEmpty(strings)) {
            return true;
        }
        return false;
    }

    int pr5 = 0;
    int lr5 = 0;

    public ArrayList<String> frequencyLemmaQuery(Set<String> array, int site_id) {
        HashMap<String, Integer> countLemma = new HashMap<>();
        float allPages;
        if (site_id == 0) {
            collectionAtZero(array, countLemma);
        } else {
            for (String s : array) {
                Lemma lemma = lemmaRepository.findByLemmaAndSite_id(s, site_id);
                System.out.println(colorGreen + "lemmaRepository5 " + ++lr5);
                allPages = pageRepository.findCountPageBySite_id(site_id);
                System.out.println(colorGreen + "pageRepository5 " + ++pr5);
                if (lemma == null) {
                    continue;
                }
                if (lemma.getFrequency() / allPages > 0.7) {
                    continue;
                }
                countLemma.put(s, lemma.getFrequency());
            }
        }
        return (ArrayList<String>) countLemma.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    int pr4 = 0;
    int lr4 = 0;

    public void collectionAtZero(Set<String> array, HashMap<String, Integer> countLemma) {
        float allPages = pageRepository.findCountPageAll();
        System.out.println(colorGreen + "pageRepository4 " + ++pr4);
        System.out.println(colorReset + "float allPages " + allPages);
        for (String s : array) {
            ArrayList<Lemma> lemmas = lemmaRepository.findAllByLemma(s);
            System.out.println(colorGreen + "lemmaRepository4 " + ++lr4);
            lemmas.forEach(lemma -> System.out.println(colorReset + lemma.toString()));
            if (lemmas == null) {
                continue;
            }
            int frequencyAll = 0;
            for (Lemma l : lemmas) {
                frequencyAll += l.getFrequency();
            }
            if ((frequencyAll / allPages) > 0.7) {
                System.out.println(allPages / frequencyAll);
                continue;
            }
            countLemma.put(s, frequencyAll);
        }
    }


    public ArrayList<ResponseSearchData> responseObjectArrayList(ArrayList<String> sortedFrequencyLemmas, String site) {
        ArrayList<ResponseSearchData> array;
        LinkedList<Page> pagesOne;
        LinkedList<Page> pagesNext;
        if (site == null) {
            pagesOne = pagesWithLemma(sortedFrequencyLemmas.get(0));
            for (int i = 1; i < sortedFrequencyLemmas.size(); i++) {
                pagesNext = pagesWithLemma(sortedFrequencyLemmas.get(i));
                pagesOne.forEach(pagesNext::removeFirstOccurrence);
            }
        } else {
            pagesOne = lemmaPagesOnTheWebsite(sortedFrequencyLemmas.get(0), site);
            for (int i = 1; i < sortedFrequencyLemmas.size(); i++) {
                pagesNext = lemmaPagesOnTheWebsite(sortedFrequencyLemmas.get(i), site);
                pagesOne.forEach(pagesNext::removeFirstOccurrence);
            }
        }
        System.out.println(colorBlue + "before start snippetGenerator");
        array = snippetGenerator(pagesOne, sortedFrequencyLemmas);
        System.out.println(colorBlue + "after start snippetGenerator");
        return array;
    }

    int lr3 = 0;
    int inr3 = 0;
    int pr3 = 0;

    public LinkedList<Page> pagesWithLemma(String lemma) {
        ArrayList<Integer> lemma_id = lemmaRepository.findAllIdByLemma(lemma);
        System.out.println(colorGreen + "lemmaRepository3 " + ++lr3);
        lemma_id.forEach(integer -> System.out.println(colorReset + integer));
        ArrayList<Integer> pagesId = indexObjectRepository.findAllByLemma_idIn(lemma_id);
        System.out.println(colorGreen + "indexObjectRepository3 " + ++inr3);
        pagesId.forEach(integer -> System.out.println(colorReset + integer));
        LinkedList<Page> pages = new LinkedList();
        pages.addAll(pageRepository.findAllById(pagesId));
        System.out.println(colorGreen + "pageRepository3 " + ++pr3);
        pages.forEach(page -> System.out.println(colorReset + page.getPath()));
        return pages;
    }

    int sr4 = 0;
    int lr2 = 0;
    int inr2 = 0;
    int pr2 = 0;

    public LinkedList<Page> lemmaPagesOnTheWebsite(String lemma, String site) {
        int site_id = siteRepository.findIdByUrl(site);
        System.out.println(colorGreen + "siteRepository4 " + ++sr4);
        ArrayList<Integer> lemma_id = lemmaRepository.findAllIdByLemmaAndSite_id(lemma, site_id);
        System.out.println(colorGreen + "lemmaRepository2 " + ++lr2);
        ArrayList<Integer> pagesId = indexObjectRepository.findAllByLemma_idIn(lemma_id);
        System.out.println(colorGreen + "indexObjectRepository2 " + ++inr2);
        LinkedList<Page> pages = new LinkedList();
        pages.addAll(pageRepository.findAllById(pagesId));
        System.out.println(colorGreen + "pageRepository2 " + ++pr2);
        return pages;
    }

    int sr5 = 0;

    public ArrayList<ResponseSearchData> snippetGenerator(LinkedList<Page> pagesList, ArrayList<String> lemmasQuery) {
        ArrayList<ResponseSearchData> snippets = new ArrayList<>();
        for (Page page : pagesList) {
            ResponseSearchData searchData = new ResponseSearchData();
            String snippet = snippetExtractor(page, lemmasQuery);
            searchData.setSnippet(snippet);
            String siteUrl = siteRepository.findUrlById(page.getSite().getId());
            System.out.println(colorGreen + "siteRepository4 " + ++sr5);
            String siteUrlCropped = siteUrl.substring(0, siteUrl.length() - 1);
            searchData.setSite(siteUrlCropped);
            searchData.setUri("/" + page.getPath());
            Document doc = Jsoup.parse(page.getContent());
            searchData.setTitle(doc.select("title").text());
            for (Site site : sites.getSites()) {
                String url = normalisePathParent(site.getUrl());
                if (url.equals(siteUrl)) {
                    String name = site.getName();
                    searchData.setSiteName(name);
                }
            }
            if (searchData.getSiteName() == null) {
                searchData.setSiteName(Jsoup.parse(page.getContent()).select("title").text());
            }
            searchData.setRelevance(revalenceCalculator(lemmasQuery, page));
            snippets.add(searchData);
        }
        return snippets;
    }

    public String snippetExtractor(Page page, ArrayList<String> lemmasQuery) {
        LemmaServiceImpl lemmaService = new LemmaServiceImpl();
        ArrayList<Element> elements = lemmaService.elementsInDoc(Jsoup.parse(page.getContent()));
        ArrayList<SnippetEntity> snippetArray = new ArrayList<>();
        for (Element el : elements) {
            extractedSnippet(lemmasQuery, snippetArray, el);
        }
        Collections.sort(snippetArray, Comparator.comparing(SnippetEntity::getNumberOfMatches));
        StringBuilder stringBuilder = new StringBuilder();
        int countWordInSnippet = 0;

        for (SnippetEntity snippetEntity : snippetArray) {
            if (countWordInSnippet < 30) {
                stringBuilder.append(snippetEntity.getSnippet());
                countWordInSnippet += snippetEntity.getNumberOfWords();
            }
        }
        return stringBuilder.toString();
    }


    private void extractedSnippet(ArrayList<String> lemmasQuery, ArrayList<SnippetEntity> snippetArray, Element el) {
        StringBuilder stringBuilder = new StringBuilder();
        HashSet<String> snippetHashSet = new HashSet<>();
        int count = 0;
        String text = el.text();
        if (text.matches("^((8|\\+7)[\\- ]?)?(\\(?\\d{3}\\)?[\\- ]?)?[\\d\\- ]{7,10}$")) {
            return;
        }
        int wordLength = 0;
        String[] words = text.split(" ");
        for (int i = 0; i < words.length; i++) {
            for (String l : lemmasQuery) {
                String w = words[i].replaceAll("[^а-яёЁА-Я]", "");
                if (wordComparator(w, l)) {
                    System.out.println(colorReset + w);
                    words[i] = "<b>" + words[i] + "</b>";
                    count++;
                    wordLength += words.length;
                }
            }
            if (count > 0) {
                for (String s : words) {
                    stringBuilder.append(s + " ");
                }
            }
            if (wordLength > 30) {
                continue;
            }
            String snippet = stringBuilder.toString();
            stringBuilder.setLength(0);
            if (!snippetHashSet.contains(snippet)|!snippet.equals("")) {
                snippetHashSet.add(snippet);
                SnippetEntity snippetEntity = new SnippetEntity(snippet, count, wordLength);
                snippetArray.add(snippetEntity);
            }
        }
    }

    public boolean wordComparator(String world, String lemma) {
        List<WordformMeaning> lemmas = lookupForMeanings(world);
//   TODO:    КОСТЫЛЬ!!! Надо менять или дополнять библиотеку.
        if (lemmas.isEmpty()) {
            return false;
        }
        String lemmaFromTheWord = String.valueOf(lemmas.get(0).getLemma());
        if (lemmaFromTheWord.equals(lemma)) {
            return true;
        }
        return false;
    }


    int pr = 0;
    int lr = 0;
    int inr = 0;

    public int revalenceCalculator(ArrayList<String> lemmasQuery, Page page) {
        int absoluteRevalence = 0;
        for (String s : lemmasQuery) {
            int site_id = pageRepository.findSite_idById(page.getId());
            System.out.println(colorGreen + "pageRepository " + colorBlue + site_id + " " + colorGreen + ++pr);
            int lemma_id = 0;
            try {
                lemma_id = lemmaRepository.findIdByLemmaAndSite_id(s, site_id);
                System.out.println(colorGreen + "lemmaRepository " + colorBlue + lemma_id + " " + colorGreen + ++lr);
            } catch (Exception e) {
                continue;
            }
            if (lemma_id != 0) {
                absoluteRevalence += indexObjectRepository.findById(lemma_id).get().getRunk();
                System.out.println(colorGreen + "indexObjectRepository " + colorBlue + absoluteRevalence + " " + colorGreen + ++inr);
            }
        }
        return absoluteRevalence;
    }

    public ArrayList<ResponseSearchData> normalizerRelevance(ArrayList<ResponseSearchData> array) {
        ArrayList<ResponseSearchData> arraySort = array;
        float maxRelevance = 0;
        for (ResponseSearchData r : arraySort) {
            if (r.getRelevance() > maxRelevance) {
                maxRelevance = r.getRelevance();
            }
        }
        for (ResponseSearchData r : arraySort) {
            float relativeRelevance = r.getRelevance() / maxRelevance;
            r.setRelevance(relativeRelevance);
        }
        arraySort.sort(Comparator.comparing(ResponseSearchData::getRelevance));
        return arraySort;
    }

    int sr2 = 0;
    int sr3 = 0;

    public boolean isTheSiteIndexed(String site) {
        if (site == null) {
            List<searchengine.model.Site> siteList = siteRepository.findAll();
            System.out.println(colorGreen + "siteRepository2 " + ++sr2);
            for (searchengine.model.Site s : siteList) {
                if (!s.getStatus().equals(Status.INDEXED)) {
                    return false;
                }
            }
        }
        searchengine.model.Site siteSolo = siteRepository.findByUrl(site);
        System.out.println(colorGreen + "siteRepository3 " + ++sr3);
        if (!siteSolo.getStatus().equals(Status.INDEXED)) {
            return false;
        }
        return true;
    }
}
