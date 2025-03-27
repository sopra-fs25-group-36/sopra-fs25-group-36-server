package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

public class UserGetDTO {

  private Long userID;
  private String name;
  private String token;
  private String username;
  private UserStatus status;

  public Long getId() {
    return userID;
  }

  public void setId(Long id) {
    this.userID = id;
  }


  //but why do we need a name?
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }


  public UserStatus getStatus() {
    return status;
  }
  public void setStatus(UserStatus status) {
    this.status = status;
  }



  public String getToken() {
    return token;



  public void setToken(String token) {
    this.token = token;
}




  }
}
