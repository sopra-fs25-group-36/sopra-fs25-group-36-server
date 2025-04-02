package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserTokenDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyPostDTO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User or Lobby) to the external/API representation (e.g.,
 * UserGetDTO or LobbyGetDTO for getting, and UserPostDTO or LobbyPostDTO for creating)
 * and vice versa.
 */
@Mapper
public interface DTOMapper {

  DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

  @Mapping(source = "name", target = "name")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "password", target = "password")
  User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "name", target = "name")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "status", target = "status")
  UserGetDTO convertEntityToUserGetDTO(User user);

  @Mapping(source = "token", target = "token")
  @Mapping(source = "id", target = "id")
  UserTokenDTO convertEntityToUserTokenDTO(User user);

  // Lobby mappings

  /**
   * Convert LobbyPostDTO to Lobby entity.
   * All attributes are defined in the back end, no need to post anything from the front end.
   */
  Lobby convertLobbyPostDTOtoEntity(LobbyPostDTO lobbyPostDTO);

  /**
   * Convert Lobby entity to LobbyGetDTO.
   */
  @Mapping(source = "id", target = "id")
  @Mapping(source = "playerReadyStatuses", target = "playerReadyStatuses")
  @Mapping(source = "createdAt", target = "createdAt")
  @Mapping(source = "active", target = "active")
  @Mapping(source = "timeLimitSeconds", target = "timeLimitSeconds")
  LobbyGetDTO convertEntityToLobbyGetDTO(Lobby lobby);
}
