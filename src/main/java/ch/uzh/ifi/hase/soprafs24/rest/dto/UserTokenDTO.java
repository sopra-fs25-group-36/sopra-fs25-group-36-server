package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class UserTokenDTO {
    private String token;
    private Long id;

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
