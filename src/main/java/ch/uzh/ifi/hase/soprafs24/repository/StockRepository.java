package ch.uzh.ifi.hase.soprafs24.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ch.uzh.ifi.hase.soprafs24.entity.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findBySymbol(String symbol);

    List<Stock> findBySymbolAndDate(String symbol, LocalDate date);

    List<Stock> findAllByDate(LocalDate date);

    List<Stock> findBySymbolAndDateBetween(String symbol, LocalDate start, LocalDate end);

    @Query(value = """
                SELECT date
                FROM (
                    SELECT DISTINCT date
                    FROM stock_adjusted
                    WHERE
                        date >= '2023-01-01' AND  -- Only consider dates from 2023-01-01 onwards
                        date <= (SELECT MAX(date) - INTERVAL '9 days' FROM stock_adjusted)
                ) AS valid_dates
                ORDER BY RANDOM()
                LIMIT 1
            """, nativeQuery = true)
    LocalDate findRandomStartDateWith10Days();

    @Query(value = """
                SELECT * FROM stock_adjusted
                WHERE date IN (
                    SELECT DISTINCT date
                    FROM stock_adjusted
                    WHERE date >= :startDate
                    ORDER BY date ASC
                    LIMIT 10
                )
                ORDER BY date ASC
            """, nativeQuery = true)
    List<Stock> findStocksForTenDays(@Param("startDate") LocalDate startDate);

    @Query(value = """
                SELECT * FROM stock_adjusted
                WHERE symbol IN (
                    SELECT symbol FROM stock_adjusted
                    GROUP BY symbol
                    ORDER BY RANDOM()
                    LIMIT 10
                )
                AND date IN (
                    SELECT DISTINCT date
                    FROM stock_adjusted
                    WHERE date >= :startDate
                    ORDER BY date ASC
                    LIMIT 10
                )
                ORDER BY symbol, date ASC
            """, nativeQuery = true)
    List<Stock> findRandom10SymbolsWithFirst10DatesFrom(@Param("startDate") LocalDate startDate);

    @Query(value = "SELECT * FROM stock_adjusted WHERE symbol = :symbol ORDER BY date LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Stock> findBySymbolWithLimitOffset(@Param("symbol") String symbol, @Param("offset") int offset,
            @Param("limit") int limit);
}