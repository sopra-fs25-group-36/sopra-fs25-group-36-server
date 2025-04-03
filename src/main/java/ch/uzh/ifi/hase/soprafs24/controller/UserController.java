package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
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
    // fetch all users in the internal representation
    List<User> users = userService.getUsers();
    List<UserGetDTO> userGetDTOs = new ArrayList<>();

    // convert each user to the API representation
    for (User user : users) {
      userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    return userGetDTOs;
  }

  @PostMapping("/users")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
    // convert API user to internal representation
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    // create user
    User createdUser = userService.createUser(userInput);
    // convert internal representation of user back to API
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
  }

  @PostMapping("/users/login")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO loginUser(@RequestBody UserPostDTO userPostDTO) {
    // Extract username and password from userPostDTO
    String username = userPostDTO.getUsername();
    String password = userPostDTO.getPassword();

    // Login user using username and password
    User loginUser = userService.loginUser(username, password);

    // Convert internal representation back to API
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(loginUser);
  }

  @GetMapping("/users/{userID}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  @CrossOrigin
  public UserGetDTO getUserById(@PathVariable("userID") Long userID, @RequestHeader("token") String token) {
    // returns a user for a provided userID
    this.userService.checkAuthentication(token);
    User userById = userService.getUserById(userID);

    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(userById);
  }

  @PostMapping("/users/{userID}/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ResponseBody
  @CrossOrigin
  public void logoutUser(@PathVariable("userID") Long userID, @RequestHeader("token") String token) {
    this.userService.checkAuthentication(token);
    this.userService.logoutUser(userID.toString());
  }

  @PostMapping("/{userId}/lobby") // do we need this? we can put it in game controller or lobbby

  @ResponseStatus(HttpStatus.CREATED)
  public LobbyGetDTO createLobby(@PathVariable Long userID, @RequestBody LobbyPostDTO lobbyPostDTO) {
    Lobby lobbyInput = DTOMapper.INSTANCE.convertLobbyPostDTOtoEntity(lobbyPostDTO);

    Lobby createdLobby = lobbyService.createLobby(userID, lobbyInput);

    return DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(createdLobby);
  }
}