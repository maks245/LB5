package org.example.app.dto;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    // -------------------------------------------------------------

    // Можливо, у вас є також конструктор за замовчуванням, його теж можна залишити
    public LoginRequest() {
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
