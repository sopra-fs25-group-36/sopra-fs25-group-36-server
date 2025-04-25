package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/**
 * Converts between entities and DTOs.
 * MapStruct generates the implementation at build-time.
 */
@Mapper
public interface DTOMapper {

  DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

  /* ----------------------------------------------------------------
   *  User mappings
   * ---------------------------------------------------------------- */

  @Mapping(target = "id",     ignore = true)
  @Mapping(target = "token",  ignore = true)
  @Mapping(target = "status", ignore = true)
  User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

  @Mapping(source = "id",     target = "id")
  @Mapping(source = "name",   target = "name")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "status", target = "status")
  UserGetDTO convertEntityToUserGetDTO(User user);

  @Mapping(source = "token", target = "token")
  @Mapping(source = "id",    target = "id")
  UserTokenDTO convertEntityToUserTokenDTO(User user);

  /* ----------------------------------------------------------------
   *  Lobby mappings
   * ---------------------------------------------------------------- */

  /** Convert LobbyPostDTO ➜ Lobby entity.
      All lobby attributes are controlled in the back-end. */
  @Mapping(target = "id",                 ignore = true)
  @Mapping(target = "playerReadyStatuses", ignore = true)
  @Mapping(target = "createdAt",          ignore = true)
  @Mapping(target = "active",             ignore = true)
  @Mapping(target = "timeLimitSeconds",   ignore = true)
  Lobby convertLobbyPostDTOtoEntity(LobbyPostDTO lobbyPostDTO);

  /** Convert Lobby entity ➜ LobbyGetDTO.
      createdAt (Instant) is sent as epoch-milliseconds (long). */
  @Mapping(source = "id",                 target = "id")
  @Mapping(source = "playerReadyStatuses", target = "playerReadyStatuses")
  @Mapping(target = "createdAt",
           expression = "java(lobby.getCreatedAt().toEpochMilli())")
  @Mapping(source = "active",             target = "active")
  @Mapping(source = "timeLimitSeconds",   target = "timeLimitSeconds")
  LobbyGetDTO convertEntityToLobbyGetDTO(Lobby lobby);
}
