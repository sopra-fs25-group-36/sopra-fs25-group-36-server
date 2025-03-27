package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyUserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class LobbyController {

    private final LobbyService lobbyService;

    LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @PostMapping("/{userId}/createLobby")
    @ResponseStatus(HttpStatus.CREATED)
    public LobbyGetDTO createLobby(@PathVariable Long userId, @RequestBody LobbyPostDTO lobbyPostDTO) {
        Lobby lobbyInput = DTOMapper.INSTANCE.convertLobbyPostDTOtoEntity(lobbyPostDTO);

        Lobby createdLobby = lobbyService.createLobby(userId, lobbyInput);

        return DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(createdLobby);
    }

    /**
     * POST endpoint to add a user to an existing lobby.
     *
     * @param lobbyId the ID of the lobby to join
     * @param lobbyUserPostDTO contains the userId to be added to the lobby
     * @return the updated lobby as LobbyGetDTO
     */
    @PostMapping("/{lobbyId}/joinLobby")
    @ResponseStatus(HttpStatus.OK)
    public LobbyGetDTO joinLobby(@PathVariable Long lobbyId,
                                 @RequestBody LobbyUserPostDTO lobbyUserPostDTO) {
        Lobby updatedLobby = lobbyService.addUserToLobby(lobbyId, lobbyUserPostDTO.getUserId());
        return DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(updatedLobby);
    }

    @GetMapping("/lobby/{lobbyId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyGetDTO getLobby(@PathVariable Long lobbyId) {
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        return DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(lobby);
    }
    
    
}

