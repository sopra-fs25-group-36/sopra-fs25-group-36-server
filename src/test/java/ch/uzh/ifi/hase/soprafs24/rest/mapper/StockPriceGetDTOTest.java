package ch.uzh.ifi.hase.soprafs24.rest.mapper;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockPriceGetDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StockPriceGetDTOTest {

    @Test
    void testStockPriceGetDTOGettersAndSetters() {
        StockPriceGetDTO dto = new StockPriceGetDTO();

        LocalDate today = LocalDate.now();

        dto.setSymbol("TSLA");
        dto.setRound(3);
        dto.setPrice(700.75);
        dto.setCategory("Automotive");
        dto.setDate(today);

        assertThat(dto.getSymbol()).isEqualTo("TSLA");
        assertThat(dto.getRound()).isEqualTo(3);
        assertThat(dto.getPrice()).isEqualTo(700.75);
        assertThat(dto.getCategory()).isEqualTo("Automotive");
        assertThat(dto.getDate()).isEqualTo(today);
    }

    @Test
    void testStockPriceGetDTODefaultValues() {
        StockPriceGetDTO dto = new StockPriceGetDTO();

        assertThat(dto.getSymbol()).isNull();
        assertThat(dto.getRound()).isEqualTo(0);
        assertThat(dto.getPrice()).isEqualTo(0.0);
        assertThat(dto.getCategory()).isNull();
        assertThat(dto.getDate()).isNull();
    }
}
