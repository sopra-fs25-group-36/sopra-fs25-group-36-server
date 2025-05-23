package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class LobbyController {
    private final LobbyService lobbyService;
    private final DTOMapper mapper = DTOMapper.INSTANCE;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @PostMapping("/{userId}/createLobby")
    @ResponseStatus(HttpStatus.CREATED)
    public LobbyGetDTO createLobby(@PathVariable Long userId,
            @RequestBody LobbyPostDTO lobbyPostDTO) {

        Lobby lobbyInput = mapper.convertLobbyPostDTOtoEntity(lobbyPostDTO);
        Lobby created = lobbyService.createLobby(userId, lobbyInput);
        return mapper.convertEntityToLobbyGetDTO(created);
    }

    @PostMapping("lobby/{lobbyId}/joinLobby")
    @ResponseStatus(HttpStatus.OK)
    public LobbyGetDTO joinLobby(@PathVariable Long lobbyId,
            @RequestBody LobbyUserPostDTO dto) {

        Lobby updated = lobbyService.addUserToLobby(lobbyId, dto.getUserId());
        return mapper.convertEntityToLobbyGetDTO(updated);
    }

    @GetMapping("/lobby/{lobbyId}")
    @ResponseStatus(HttpStatus.OK)
    public LobbyGetDTO getLobby(@PathVariable Long lobbyId) {
        return mapper.convertEntityToLobbyGetDTO(lobbyService.getLobbyById(lobbyId));
    }

    @PostMapping("lobby/{lobbyId}/ready")
    @ResponseStatus(HttpStatus.OK)
    public LobbyGetDTO setUserReady(@PathVariable Long lobbyId,
            @RequestBody LobbyUserPostDTO dto) {

        Lobby updated = lobbyService.setUserReady(lobbyId, dto.getUserId());
        return mapper.convertEntityToLobbyGetDTO(updated);
    }
}
