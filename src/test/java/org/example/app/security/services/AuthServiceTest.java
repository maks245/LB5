package org.example.app.security.services;

import org.example.app.exceptions.EmailAlreadyExistsException;
import org.example.app.exceptions.UsernameAlreadyExistsException;
import org.example.app.models.Role;
import org.example.app.models.User;
import org.example.app.dto.SignupRequest;
import org.example.app.repository.RoleRepository;
import org.example.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock // ДОДАЙТЕ ЦЕЙ МОК
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest;
    private User newUser;
    private Role userRole;
    private Role adminRole; // Додаємо для ADMIN ролі

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setRoles(null); // За замовчуванням USER

        newUser = new User("testuser", "test@example.com", "encodedPassword");
        newUser.setId(1L);

        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("ROLE_USER"); // Назва ролі як String

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ROLE_ADMIN"); // Назва ролі як String
    }

    // Тест на успішне виконання методу за типових умов
    @Test
    @DisplayName("Should successfully register a new user with default role")
    void registerUser_whenUserIsNew_shouldRegisterSuccessfully() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("rawpassword");

        // 1. Мокування userRepository.save()
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User savedUser = invocation.getArgument(0);
                    savedUser.setId(1L);
                    return savedUser;
                });

        // 2. Мокування roleRepository.findByName()
        // Створіть об'єкт Role і встановіть його ім'я як String
        Role userRole = new Role();
        userRole.setId(1L); // Присвойте ID, якщо ваша модель Role його має
        userRole.setName("ROLE_USER"); // Встановіть назву ролі як рядок!

        // МОКУВАННЯ: Коли RoleRepository.findByName("ROLE_USER") викликається,
        // повернути Optional, що містить створену Role
        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(Optional.of(userRole));

        // Якщо ви використовуєте PasswordEncoder в AuthService, його також потрібно мокнути
        // if (passwordEncoder != null) {
        //     when(passwordEncoder.encode(any(String.class))).thenReturn("encodedPassword");
        // }


        User registeredUser = authService.registerUser(signupRequest);

        assertEquals(1L, registeredUser.getId(), "User ID should be 1L");
        assertEquals("testuser", registeredUser.getUsername());
        assertEquals("test@example.com", registeredUser.getEmail());

        // Опційно: перевірте, чи був викликаний findByName з правильним аргументом
        verify(roleRepository).findByName("ROLE_USER");

        // Додаткові перевірки (наприклад, що користувачу була встановлена роль)
        // assertNotNull(registeredUser.getRoles());
        // assertFalse(registeredUser.getRoles().isEmpty());
        // assertTrue(registeredUser.getRoles().stream().anyMatch(r -> "ROLE_USER".equals(r.getName())));

    }

    // Тест на обробку виняткової ситуації (користувач з таким ім'ям вже існує)
    @Test
    @DisplayName("Should throw UsernameAlreadyExistsException when username is already taken")
    void registerUser_whenUsernameExists_shouldThrowException() {
        // Arrange
        when(userRepository.existsByUsername(signupRequest.getUsername())).thenReturn(true);

        // Act & Assert
        UsernameAlreadyExistsException thrown = assertThrows(
                UsernameAlreadyExistsException.class,
                () -> authService.registerUser(signupRequest),
                "Should throw UsernameAlreadyExistsException"
        );
        assertEquals("Error: Username is already taken!", thrown.getMessage());

        verify(userRepository, times(1)).existsByUsername(signupRequest.getUsername());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(encoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // Тест на обробку виняткової ситуації (користувач з таким email вже існує)
    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email is already in use")
    void registerUser_whenEmailExists_shouldThrowException() {
        // Arrange
        when(userRepository.existsByUsername(signupRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(signupRequest.getEmail())).thenReturn(true);

        // Act & Assert
        EmailAlreadyExistsException thrown = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.registerUser(signupRequest),
                "Should throw EmailAlreadyExistsException"
        );
        assertEquals("Error: Email is already in use!", thrown.getMessage());

        verify(userRepository, times(1)).existsByUsername(signupRequest.getUsername());
        verify(userRepository, times(1)).existsByEmail(signupRequest.getEmail());
        verify(encoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // Тест, що перевіряє взаємодію з PasswordEncoder
    @Test
    @DisplayName("Should call PasswordEncoder to encode the password")
    void registerUser_shouldCallPasswordEncoder() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole)); // Мок для ролі
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        authService.registerUser(signupRequest);

        // Assert
        verify(encoder, times(1)).encode(signupRequest.getPassword());
    }

    // Додатковий тест: перевірка реєстрації користувача з конкретною роллю (наприклад, ADMIN)
    @Test
    @DisplayName("Should register admin user when 'admin' role is specified")
    void registerUser_whenAdminRoleSpecified_shouldRegisterAsAdmin() {
        // Arrange
        signupRequest.setRoles(Collections.singleton("admin")); // Встановлюємо роль 'admin' (як String)

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole)); // Шукаємо за String
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setRoles(Collections.singleton(adminRole)); // Переконайтеся, що ролі встановлюються правильно
            return user;
        });

        // Act
        User registeredUser = authService.registerUser(signupRequest);

        // Assert
        assertNotNull(registeredUser);
        assertTrue(registeredUser.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")));
        verify(roleRepository, times(1)).findByName("ROLE_ADMIN"); // Перевірка виклику з String
        verify(userRepository, times(1)).save(any(User.class));
    }
}