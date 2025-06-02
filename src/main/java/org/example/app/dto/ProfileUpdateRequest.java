package org.example.app.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class ProfileUpdateRequest {
    @Size(min = 3, max = 20)
    private String username;

    @Size(max = 50)
    @Email
    private String email;

    // Constructors
    public ProfileUpdateRequest() {}

    public ProfileUpdateRequest(String username, String email) {
        this.username = username;
        this.email = email;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
