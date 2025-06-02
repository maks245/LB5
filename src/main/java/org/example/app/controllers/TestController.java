package org.example.app.controllers;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/all")
    public String allAccess() {
        return "Public content.";
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')") // Доступно для USER та ADMIN
    public String userAccess() {
        return "User content.";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')") // Доступно лише для ADMIN
    public String adminAccess() {
        return "Admin content.";
    }
}