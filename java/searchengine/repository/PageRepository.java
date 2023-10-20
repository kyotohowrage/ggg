package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    @Query(value = "SELECT id FROM search_engine.page WHERE path = ?1",
            nativeQuery = true)
    int findIdByPath(String pathHtml);

    @Query(value = "SELECT path FROM search_engine.page WHERE site_id = ?1",
            nativeQuery = true)
    ArrayList<String> findPathBySite_id(int site_id);

    @Query(value = "SELECT count(id) FROM search_engine.page WHERE site_id = ?1",
            nativeQuery = true)
    int findCountPageBySite_id(int site_id);
    @Query(value = "SELECT count(id) FROM search_engine.page",
            nativeQuery = true)
    int findCountPageAll();

    @Query(value = "SELECT * FROM search_engine.page WHERE site_id = ?1 and path = ?2",
            nativeQuery = true)
    Page findBySite_idAndPath(int site_id, String path);

    @Modifying
    @Query(value = "UPDATE search_engine.page p SET p.content = ?2 WHERE p.id = ?1",
            nativeQuery = true)
    void updateContentPage(Integer id, String s);
    @Query(value = "SELECT site_id FROM search_engine.page WHERE id = ?1",
            nativeQuery = true)
    int findSite_idById(Integer id);

//    default ArrayList<Integer> findAllById(ArrayList<Integer> pagesId){
//        Set<Page> pages = new HashSet<>();
//        for (int id : pagesId) {
//            ArrayList<Page> page_id = findAllById(id);
//            pages.addAll(page_id);
//        }
//        return (ArrayList<Integer>) pages.stream().collect(Collectors.toList());
//    }
}
