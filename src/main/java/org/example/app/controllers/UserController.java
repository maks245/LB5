package org.example.app.controllers;
import org.example.app.dto.ProfileUpdateRequest;
import org.example.app.models.User;
import org.example.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getUserProfile() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = userDetails.getUsername();
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            return ResponseEntity.ok(userOptional.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest profileUpdateRequest) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUsername = userDetails.getUsername();

        Optional<User> userOptional = userRepository.findByUsername(currentUsername);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User userToUpdate = userOptional.get();

        if (profileUpdateRequest.getUsername() != null && !profileUpdateRequest.getUsername().isEmpty()) {
            // Перевірка, чи нове ім'я користувача не зайняте (якщо воно змінюється)
            if (!currentUsername.equals(profileUpdateRequest.getUsername()) && userRepository.existsByUsername(profileUpdateRequest.getUsername())) {
                return ResponseEntity.badRequest().body("Error: Username is already taken!");
            }
            userToUpdate.setUsername(profileUpdateRequest.getUsername());
        }
        if (profileUpdateRequest.getEmail() != null && !profileUpdateRequest.getEmail().isEmpty()) {
            // Перевірка, чи новий email не зайнятий (якщо він змінюється)
            if (!userToUpdate.getEmail().equals(profileUpdateRequest.getEmail()) && userRepository.existsByEmail(profileUpdateRequest.getEmail())) {
                return ResponseEntity.badRequest().body("Error: Email is already in use!");
            }
            userToUpdate.setEmail(profileUpdateRequest.getEmail());
        }

        userRepository.save(userToUpdate);
        return ResponseEntity.ok("Profile updated successfully!");
    }
}