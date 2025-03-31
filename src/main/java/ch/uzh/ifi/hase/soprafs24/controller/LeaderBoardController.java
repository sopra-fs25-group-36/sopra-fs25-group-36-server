package ch.uzh.ifi.hase.soprafs24.controller;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.LeaderBoardEntry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderBoardEntryGetDTO;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.HttpStatus; 



@RestController
@RequestMapping("/game")
public class LeaderBoardController {

    private final GameService gameService;

    @Autowired
    public LeaderBoardController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/{gameId}/leader")
    public List<LeaderBoardEntryGetDTO> getLeaderBoard(@PathVariable Long gameId) {
        GameManager game = gameService.getGame(gameId);
        List<LeaderBoardEntry> rawBoard = game.getLeaderBoard();
    
        List<LeaderBoardEntryGetDTO> dtos = new ArrayList<>();
    
        for (LeaderBoardEntry entry : rawBoard) {
            LeaderBoardEntryGetDTO dto = new LeaderBoardEntryGetDTO();
            dto.setUserId(entry.getUserId());
            dto.setTotalAssets(entry.getTotalAssets());
            dtos.add(dto);
        }
    
        return dtos;
    }
}    
