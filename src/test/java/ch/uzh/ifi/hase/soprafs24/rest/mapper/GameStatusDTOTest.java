package ch.uzh.ifi.hase.soprafs24.rest.mapper;


import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStatusDTO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GameStatusDTOTest {

    @Test
    public void testGameStatusDTO() {
        // Given
        int currentRound = 5;
        boolean active = true;

        // When
        GameStatusDTO gameStatusDTO = new GameStatusDTO(currentRound, active);

        // Then
        assertEquals(currentRound, gameStatusDTO.currentRound());
        assertEquals(active, gameStatusDTO.active());
    }
}