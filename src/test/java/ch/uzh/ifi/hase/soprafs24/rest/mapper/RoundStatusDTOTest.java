package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.rest.dto.RoundStatusDTO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RoundStatusDTOTest {

    @Test
    public void testRoundStatusDTO() {
        boolean allSubmitted = true;
        boolean roundEnded = false;
        long nextRoundStartTime = 1672531200000L;
        RoundStatusDTO roundStatusDTO = new RoundStatusDTO(allSubmitted, roundEnded, nextRoundStartTime);
        assertEquals(allSubmitted, roundStatusDTO.isAllSubmitted());
        assertEquals(roundEnded, roundStatusDTO.isRoundEnded());
        assertEquals(nextRoundStartTime, roundStatusDTO.getNextRoundStartTime());
    }

    @Test
    public void testSetters() {
        RoundStatusDTO roundStatusDTO = new RoundStatusDTO(false, false, 0L);
        roundStatusDTO.setAllSubmitted(true);
        roundStatusDTO.setRoundEnded(true);
        roundStatusDTO.setNextRoundStartTime(1672531200000L);
        assertEquals(true, roundStatusDTO.isAllSubmitted());
        assertEquals(true, roundStatusDTO.isRoundEnded());
        assertEquals(1672531200000L, roundStatusDTO.getNextRoundStartTime());
    }
}