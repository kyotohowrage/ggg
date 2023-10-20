package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

import java.util.Date;

@Transactional
@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

   @Query(value = "SELECT id FROM search_engine.site WHERE url = ?1",
           nativeQuery = true)
   int findIdByUrl(String url);

   Site findById(int idSite);

   Site findByUrl(String path);

   @Query(value = "SELECT url FROM search_engine.site WHERE id = ?1",
           nativeQuery = true)
   String findUrlById(int id);
   @Query(value = "SELECT status FROM search_engine.site WHERE id = ?1",
           nativeQuery = true)
   String findStatusById(int id);
   @Query(value = "SELECT last_error FROM search_engine.site WHERE id = ?1",
           nativeQuery = true)
   String findLast_errorById(int id);
   @Query(value = "SELECT status_time FROM search_engine.site WHERE id = ?1",
           nativeQuery = true)
   Date findStatus_timeById(int id);
}
