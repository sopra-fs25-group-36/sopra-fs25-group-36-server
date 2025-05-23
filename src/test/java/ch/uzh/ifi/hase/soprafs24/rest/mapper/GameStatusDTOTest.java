package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStatusDTO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GameStatusDTOTest {

    @Test
    public void testGameStatusDTO() {
        int currentRound = 5;
        boolean active = true;
        long remainingTime = 120L;
        GameStatusDTO gameStatusDTO = new GameStatusDTO(currentRound, active, remainingTime);
        assertEquals(currentRound, gameStatusDTO.currentRound());
        assertEquals(active, gameStatusDTO.active());
        assertEquals(remainingTime, gameStatusDTO.remainingTime());
    }
}