package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class UserLoginDTO {

  private String password;
  private String username;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

}
