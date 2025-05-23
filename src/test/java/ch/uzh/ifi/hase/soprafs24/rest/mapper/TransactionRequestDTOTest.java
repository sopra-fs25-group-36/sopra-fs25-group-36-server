package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.rest.dto.TransactionRequestDTO;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionRequestDTOTest {

    @Test
    void testGettersAndSetters() {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setStockId("AAPL");
        dto.setQuantity(10);
        dto.setType("BUY");
        assertThat(dto.getStockId()).isEqualTo("AAPL");
        assertThat(dto.getQuantity()).isEqualTo(10);
        assertThat(dto.getType()).isEqualTo("BUY");
    }

    @Test
    void testDefaultValues() {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        assertThat(dto.getStockId()).isNull();
        assertThat(dto.getQuantity()).isEqualTo(0);
        assertThat(dto.getType()).isNull();
    }

    @Test
    void testUpdateValues() {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setStockId("MSFT");
        dto.setQuantity(5);
        dto.setType("SELL");
        dto.setStockId("GOOGL");
        dto.setQuantity(20);
        dto.setType("BUY");
        assertThat(dto.getStockId()).isEqualTo("GOOGL");
        assertThat(dto.getQuantity()).isEqualTo(20);
        assertThat(dto.getType()).isEqualTo("BUY");
    }

    @Test
    void testQuantityZero() {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setStockId("NFLX");
        dto.setQuantity(0);
        dto.setType("BUY");
        assertThat(dto.getStockId()).isEqualTo("NFLX");
        assertThat(dto.getQuantity()).isEqualTo(0);
        assertThat(dto.getType()).isEqualTo("BUY");
    }
}