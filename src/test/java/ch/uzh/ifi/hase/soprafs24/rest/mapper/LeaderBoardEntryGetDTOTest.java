package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderBoardEntryGetDTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LeaderBoardEntryGetDTO Test")
class LeaderBoardEntryGetDTOTest {

    @Test
    @DisplayName("Create DTO and verify getters and setters")
    void createAndAccessDTOFields() {
        Long userId = 42L;
        Double totalAssets = 12345.67;
        LeaderBoardEntryGetDTO dto = new LeaderBoardEntryGetDTO();
        dto.setUserId(userId);
        dto.setTotalAssets(totalAssets);
        assertNotNull(dto);
        assertEquals(userId, dto.getUserId(), "userId should match the value set");
        assertEquals(totalAssets, dto.getTotalAssets(), "totalAssets should match the value set");
    }
}
