package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.StockDataPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository("stockDataPointRepository")
public interface StockDataPointRepository extends JpaRepository<StockDataPoint, Long> {

    List<StockDataPoint> findBySymbolOrderByDateAsc(String symbol);

    boolean existsBySymbolAndDate(String symbol, LocalDate date);

}