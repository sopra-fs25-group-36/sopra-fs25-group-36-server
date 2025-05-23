package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;
  private User testUser;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    testUser = new User();
    testUser.setId(1L);
    testUser.setName("testName");
    testUser.setUsername("testUsername");
    testUser.setPassword("password");
    Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
  }

  @Test
  public void createUser_validInputs_success() {
    User createdUser = userService.createUser(testUser);
    Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
    assertEquals(testUser.getId(), createdUser.getId());
    assertEquals(testUser.getName(), createdUser.getName());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
  }

  @Test
  public void createUser_duplicateName_throwsException() {
    userService.createUser(testUser);
    Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_duplicateInputs_throwsException() {
    userService.createUser(testUser);
    Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void loginUser_validInputs_success() {
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);
    User loginUser = userService.loginUser(testUser.getUsername(), testUser.getPassword());
    assertEquals(testUser.getId(), loginUser.getId());
  }

  @Test
  public void loginUser_invalidUsername_throwsException() {
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);
    assertThrows(ResponseStatusException.class,
        () -> userService.loginUser(testUser.getUsername(), testUser.getPassword()));
  }

  @Test
  public void loginUser_invalidPassword_throwsException() {
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);
    assertThrows(ResponseStatusException.class,
        () -> userService.loginUser(testUser.getUsername(), "wrongPassword"));
  }

  @Test
  public void logoutUser_validToken_success() {
    Mockito.when(userRepository.findByToken(Mockito.any())).thenReturn(testUser);
    userService.logoutUser(testUser.getToken());
    Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
  }

  @Test
  public void logoutUser_invalidToken_throwsException() {
    Mockito.when(userRepository.findByToken(Mockito.any())).thenReturn(null);
    assertThrows(ResponseStatusException.class, () -> userService.logoutUser("invalidToken"));
  }

  @Test
  public void getUsers_success() {
    Mockito.when(userRepository.findAll()).thenReturn(List.of(testUser));
    List<User> users = userService.getUsers();
    assertEquals(1, users.size());
    assertEquals(testUser.getId(), users.get(0).getId());
    assertEquals(testUser.getName(), users.get(0).getName());
  }

}
