package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLoginDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
public class UserController {

  private final UserService userService;
  private final LobbyService lobbyService;

  UserController(UserService userService, LobbyService lobbyService) {
    this.userService = userService;
    this.lobbyService = lobbyService;
  }

  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserGetDTO> getAllUsers() {
    List<User> users = userService.getUsers();
    List<UserGetDTO> userGetDTOs = new ArrayList<>();

    for (User user : users) {
      userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    return userGetDTOs;
  }

  @PostMapping("/users")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
    User createdUser = userService.createUser(userInput);
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
  }

  @PostMapping("/users/login")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO loginUser(@RequestBody UserLoginDTO userLoginDTO) {
    String username = userLoginDTO.getUsername();
    String password = userLoginDTO.getPassword();
    User loginUser = userService.loginUser(username, password);
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(loginUser);
  }

  @GetMapping("/users/{userID}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  @CrossOrigin
  public UserGetDTO getUserById(@PathVariable("userID") Long userID) {
    User userById = userService.getUserById(userID);
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(userById);
  }

  @PostMapping("/users/{userID}/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ResponseBody
  @CrossOrigin
  public void logoutUser(@PathVariable("userID") Long userID, @RequestHeader("token") String token) {
    this.userService.checkAuthentication(token);
    this.userService.logoutUser(token);
  }

  @PostMapping("/{userId}/lobby")
  @ResponseStatus(HttpStatus.CREATED)
  public LobbyGetDTO createLobby(@PathVariable Long userID, @RequestBody LobbyPostDTO lobbyPostDTO) {
    Lobby lobbyInput = DTOMapper.INSTANCE.convertLobbyPostDTOtoEntity(lobbyPostDTO);
    Lobby createdLobby = lobbyService.createLobby(userID, lobbyInput);
    return DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(createdLobby);
  }
}