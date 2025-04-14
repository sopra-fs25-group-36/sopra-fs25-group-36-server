package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FinvizNewsArticleDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * DTOMapperTest
 * Tests if the mapping between the internal and the external/API representation
 * works.
 */
public class DTOMapperTest {
  @Test
  public void testCreateUser_fromUserPostDTO_toUser_success() {
    // create UserPostDTO
    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setName("name");
    userPostDTO.setUsername("username");

    // MAP -> Create user
    User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    // check content
    assertEquals(userPostDTO.getName(), user.getName());
    assertEquals(userPostDTO.getUsername(), user.getUsername());
  }

  @Test
  public void testGetUser_fromUser_toUserGetDTO_success() {
    // create User
    User user = new User();
    user.setName("Firstname Lastname");
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.OFFLINE);
    user.setToken("1");

    // MAP -> Create UserGetDTO
    UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

    // check content
    assertEquals(user.getId(), userGetDTO.getId());
    assertEquals(user.getName(), userGetDTO.getName());
    assertEquals(user.getUsername(), userGetDTO.getUsername());
    assertEquals(user.getStatus(), userGetDTO.getStatus());
  }

  // --- Test for FinvizNewsArticleDTO ---
  @Test
  public void testCreateAndGet_FinvizNewsArticleDTO_success() {
    // Define sample data
    String title = "Test Article Title";
    String source = "Test Source";
    String date = "2024-01-01 10:00:00";
    String url = "http://example.com/news";
    String category = "Test Category";

    // Create DTO using constructor
    FinvizNewsArticleDTO dto = new FinvizNewsArticleDTO(title, source, date, url, category);

    // Assert values using getters
    assertNotNull(dto);
    assertEquals(title, dto.getTitle());
    assertEquals(source, dto.getSource());
    assertEquals(date, dto.getDate());
    assertEquals(url, dto.getUrl());
    assertEquals(category, dto.getCategory());

    // Test default constructor and setters (optional but good for coverage)
    FinvizNewsArticleDTO dto2 = new FinvizNewsArticleDTO();
    dto2.setTitle(title);
    dto2.setSource(source);
    dto2.setDate(date);
    dto2.setUrl(url);
    dto2.setCategory(category);

    assertEquals(title, dto2.getTitle());
    assertEquals(source, dto2.getSource());
    assertEquals(date, dto2.getDate());
    assertEquals(url, dto2.getUrl());
    assertEquals(category, dto2.getCategory());
  }
}
