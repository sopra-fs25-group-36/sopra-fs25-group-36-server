package ch.uzh.ifi.hase.soprafs24.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ch.uzh.ifi.hase.soprafs24.entity.News;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    Optional<News> findByUrl(String url);

    List<News> findByPublishedTimeBetweenOrderByPublishedTimeDesc(LocalDateTime startTime, LocalDateTime endTime);
}