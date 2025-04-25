package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerStateGetDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerStateGetDTOTest {

    @Test
    void testUserIdGetterAndSetter() {
        PlayerStateGetDTO dto = new PlayerStateGetDTO();
        Long expectedUserId = 42L;

        dto.setUserId(expectedUserId);

        assertThat(dto.getUserId())
                .isNotNull()
                .isEqualTo(expectedUserId);
    }

    @Test
    void testCashBalanceGetterAndSetter() {
        PlayerStateGetDTO dto = new PlayerStateGetDTO();
        double expectedCashBalance = 1234.56;

        dto.setCashBalance(expectedCashBalance);

        assertThat(dto.getCashBalance())
                .isEqualTo(expectedCashBalance);
    }

    @Test
    void testDefaultValues() {
        PlayerStateGetDTO dto = new PlayerStateGetDTO();

        assertThat(dto.getUserId()).isNull();
        assertThat(dto.getCashBalance()).isEqualTo(0.0);
    }

    @Test
    void testUpdateValues() {
        PlayerStateGetDTO dto = new PlayerStateGetDTO();

        dto.setUserId(1L);
        dto.setCashBalance(500.0);

        dto.setUserId(2L);
        dto.setCashBalance(750.0);

        assertThat(dto.getUserId()).isEqualTo(2L);
        assertThat(dto.getCashBalance()).isEqualTo(750.0);
    }
}