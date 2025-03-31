package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    // All stock records for a given symbol 
    List<Stock> findBySymbol(String symbol);

    // A specific day's record for a stock symbol
    List<Stock> findBySymbolAndDate(String symbol, LocalDate date);

    // Records for all stocks on a given date
    List<Stock> findAllByDate(LocalDate date);

    // Specific source + symbol
    List<Stock> findBySymbolAndSource(String symbol, String source);

    // Get all prices for a symbol in a time range
    List<Stock> findBySymbolAndDateBetween(String symbol, LocalDate start, LocalDate end);
}
