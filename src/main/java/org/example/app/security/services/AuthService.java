package org.example.app.security.services;

import org.example.app.exceptions.UsernameAlreadyExistsException;
import org.example.app.exceptions.EmailAlreadyExistsException;
import org.example.app.models.Role; // Використовуємо ваш клас Role
import org.example.app.models.User;
import org.example.app.dto.SignupRequest;
import org.example.app.repository.RoleRepository;
import org.example.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    public User registerUser(SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new UsernameAlreadyExistsException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Error: Email is already in use!");
        }

        // Створюємо нового користувача
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRoles(); // Отримуємо ролі як String Set
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            // Шукаємо роль за назвою "ROLE_USER"
            Role userRole = roleRepository.findByName("ROLE_USER") // Припускаємо, що name - це String
                    .orElseThrow(() -> new RuntimeException("Error: Role ROLE_USER is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(roleName -> { // Перебираємо назви ролей
                switch (roleName) { // Порівнюємо назви ролей як String
                    case "admin":
                        Role adminRole = roleRepository.findByName("ROLE_ADMIN") // Припустимо "ROLE_ADMIN"
                                .orElseThrow(() -> new RuntimeException("Error: Role ROLE_ADMIN is not found."));
                        roles.add(adminRole);
                        break;
                    case "mod":
                        Role modRole = roleRepository.findByName("ROLE_MODERATOR") // Припустимо "ROLE_MODERATOR"
                                .orElseThrow(() -> new RuntimeException("Error: Role ROLE_MODERATOR is not found."));
                        roles.add(modRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName("ROLE_USER") // Припустимо "ROLE_USER"
                                .orElseThrow(() -> new RuntimeException("Error: Role ROLE_USER is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        return userRepository.save(user);
    }
}