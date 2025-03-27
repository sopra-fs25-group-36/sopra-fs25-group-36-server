package ch.uzh.ifi.hase.soprafs24.controller;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;

import java.util.ArrayList;
import java.util.List;


//Post mapping to start the game session if all players are ready
@RestController
public class GameController{
    private final GameService gameService;
    private final UserService userService;

    GameController(GameService gameService, UserService userService) {
        this.gameService = gameService;
        this.userService = userService;
    }
}

@PostMapping("/games/create")
@ResponseStatus(HttpStatus.CREATED)
@ResponseBody
public GameGetDTO createGame(@RequestBody GamePostDTO gamePostDTO, @RequestHeader("token") String token) {
    // convert API user to internal representation
    this.userService.checkAuthentication(token);
\
    // convert internal representation of user back to API
    return DTOMapper.INSTANCE.convertGameDataToGameGetDTO(game.status());
}


    @GetMapping("/games/{gameID}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO getGameByID(@PathVariable("gameID") Long gameID, @RequestHeader("token") String token) {
        this.userService.checkAuthentication(token);

        Game game = this.gameService.getGameByGameID(gameID);

        return DTOMapper.INSTANCE.convertGameDataToGameGetDTO(gameData);
    }

    @GetMapping("/games/{gameID}/status")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @CrossOrigin
    public GameGetDTO getStatus(@PathVariable("gameID") Long gameID, @RequestHeader("token") String token) {
        this.userService.checkAuthentication(token);

        Game game = this.gameService.getGameByGameID(gameID);

        GameData gameData = game.status();

        return DTOMapper.INSTANCE.convertGameDataToGameGetDTO(gameData);
    }

    @PostMapping("/games/{gameID}/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @CrossOrigin
    public void start(@PathVariable("gameID") Long gameID, @RequestHeader("token") String token)  {
        this.userService.checkAuthentication(token);

        this.gameService.start(gameID, token);
    }

///////
@RequestMapping("/games")
public class GameController {

    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }
//Can we change the name to gameid?
    @PostMapping("/start/{lobbyId}")
    public ResponseEntity<Game> startGame(@PathVariable Long lobbyId) {
        Game game = gameService.tryStartGame(lobbyId);
        return new ResponseEntity<>(game, HttpStatus.CREATED);
    }
}
