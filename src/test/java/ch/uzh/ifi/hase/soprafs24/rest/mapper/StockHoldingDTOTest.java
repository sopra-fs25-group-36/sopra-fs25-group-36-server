package ch.uzh.ifi.hase.soprafs24.rest.mapper;


import ch.uzh.ifi.hase.soprafs24.rest.dto.StockHoldingDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockHoldingDTOTest {

    @Test
    void testStockHoldingDTOGettersAndSetters() {
        StockHoldingDTO dto = new StockHoldingDTO();

        dto.setSymbol("AAPL");
        dto.setQuantity(10);
        dto.setCategory("Technology");
        dto.setCurrentPrice(150.25);

        assertThat(dto.getSymbol()).isEqualTo("AAPL");
        assertThat(dto.getQuantity()).isEqualTo(10);
        assertThat(dto.getCategory()).isEqualTo("Technology");
        assertThat(dto.getCurrentPrice()).isEqualTo(150.25);
    }

    @Test
    void testStockHoldingDTOConstructor() {
        StockHoldingDTO dto = new StockHoldingDTO("GOOGL", 5, "Technology", 2800.50);

        assertThat(dto.getSymbol()).isEqualTo("GOOGL");
        assertThat(dto.getQuantity()).isEqualTo(5);
        assertThat(dto.getCategory()).isEqualTo("Technology");
        assertThat(dto.getCurrentPrice()).isEqualTo(2800.50);
    }

    @Test
    void testStockHoldingDTODefaultValues() {
        StockHoldingDTO dto = new StockHoldingDTO();

        assertThat(dto.getSymbol()).isNull();
        assertThat(dto.getQuantity()).isEqualTo(0);
        assertThat(dto.getCategory()).isNull();
        assertThat(dto.getCurrentPrice()).isEqualTo(0.0);
    }
}