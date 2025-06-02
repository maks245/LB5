package org.example.app.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.example.app.dto.JwtResponse; // Переконайтесь, що цей DTO існує
import org.example.app.dto.LoginRequest; // Переконайтесь, що цей DTO існує
import org.example.app.dto.ProfileUpdateRequest;
import org.example.app.dto.SignupRequest; // Переконайтесь, що цей DTO існує
import org.example.app.models.Role;
import org.example.app.models.User;
import org.example.app.repository.RoleRepository;
import org.example.app.repository.UserRepository;
import org.example.app.security.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // Завантажує повний контекст Spring
@AutoConfigureMockMvc // Автоматично конфігурує MockMvc
@ActiveProfiles("test") // Активує профіль 'test' для використання application-test.properties
@Transactional // Кожна транзакція тесту автоматично відкочується
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // Для виконання HTTP-запитів
    @Autowired
    private ObjectMapper objectMapper; // Для серіалізації/десеріалізації JSON
    @Autowired
    @Mock
    private UserRepository userRepository; // Для перевірки стану БД
    @Autowired
    private RoleRepository roleRepository; // Для перевірки стану БД
    @Autowired
    private PasswordEncoder encoder;
    @InjectMocks // This injects the mocked UserRepository into AuthService
    private AuthService authService;// Щоб можна було закодувати пароль для тесту

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // Видаляє користувачів, що каскадно видалить user_roles
        roleRepository.deleteAll(); // Видаляє ролі

        // 1. Ініціалізація ролей (якщо вони ще не існують)
        // Використовуємо Optional для безпечної перевірки наявності
        String userRoleName = "ROLE_USER";
        String adminRoleName = "ROLE_ADMIN";
        String moderatorRoleName = "ROLE_MODERATOR";

        Role userRole = roleRepository.findByName(userRoleName)
                .orElseGet(() -> roleRepository.save(new Role(userRoleName)));

        Role adminRole = roleRepository.findByName(adminRoleName)
                .orElseGet(() -> roleRepository.save(new Role(adminRoleName)));

        Role moderatorRole = roleRepository.findByName(moderatorRoleName)
                .orElseGet(() -> roleRepository.save(new Role(moderatorRoleName)));


        // 2. Створення тестового користувача
        // Перевіряємо, чи існує користувач з таким ім'ям, щоб уникнути дублікатів,
        // якщо setUp викликається кілька разів у межах одного профілю (хоча @BeforeEach має очищати)
        if (userRepository.findByUsername("testuser").isEmpty()) {
            User testUser = new User("testuser", "test@example.com", encoder.encode("password"));
            Set<Role> roles = new HashSet<>();
            roles.add(userRole); // Додаємо роль "ROLE_USER"
            testUser.setRoles(roles);
            userRepository.save(testUser);
        }

        // Можна створити й інших тестових користувачів, наприклад, адміністратора
        if (userRepository.findByUsername("adminuser").isEmpty()) {
            User adminUser = new User("adminuser", "admin@example.com", encoder.encode("adminpassword"));
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole); // Додаємо роль "ROLE_ADMIN"
            adminUser.setRoles(roles);
            userRepository.save(adminUser);
        }

    }
    protected String obtainJwtToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        String loginRequestBody = objectMapper.writeValueAsString(loginRequest);

        System.out.println("Attempting to sign in with username: " + username + ", password: " + password); // DEBUG

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestBody))
                .andExpect(status().isOk()) // ЦЕЙ АСЕРТ ПОВИНЕН ПРОЙТИ! Якщо ні, проблема в логіні.
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Login Response Status: " + result.getResponse().getStatus()); // DEBUG
        System.out.println("Login Response Body: " + responseBody); // DEBUG

        // Варіант 1: якщо у вас є клас JwtResponse
        try {
            JwtResponse jwtResponse = objectMapper.readValue(responseBody, JwtResponse.class);
            String accessToken = jwtResponse.getAccessToken();
            System.out.println("Obtained JWT Token (from JwtResponse): " + accessToken); // DEBUG
            return accessToken;
        } catch (Exception e) {
            System.err.println("Error parsing Login Response Body into JwtResponse: " + e.getMessage()); // DEBUG
            // Варіант 2: якщо клас JwtResponse відсутній або не працює, спробуйте парсити через JsonNode
            try {
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                String accessToken = jsonNode.get("accessToken").asText(); // АБО jsonNode.get("token").asText(), залежить від вашого API
                System.out.println("Obtained JWT Token (from JsonNode): " + accessToken); // DEBUG
                return accessToken;
            } catch (Exception e2) {
                System.err.println("Error parsing Login Response Body as JsonNode: " + e2.getMessage()); // DEBUG
                throw new RuntimeException("Failed to obtain JWT token from response", e2);
            }
        }
    }

    // Самостійна робота: Інтеграційний тест для публічного ендпоінта /api/auth/signup
    @Test
    @DisplayName("Should register a new user successfully and save to database")
    void signUp_success() throws Exception {
        // Arrange
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("newUser");
        signupRequest.setEmail("newuser@example.com");
        signupRequest.setPassword("newPass123");
        signupRequest.setRoles(Collections.singleton("user")); // Приклад ролі

        // Act
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated()) // Очікуємо 200 OK
                .andExpect(jsonPath("$.message").value("User registered successfully!")); // Перевіряємо відповідь

        // Assert - Перевірка змін у базі даних
        Optional<User> registeredUserOptional = userRepository.findByUsername("newUser");
        assertThat(registeredUserOptional).isPresent(); // Перевіряємо, що користувач існує
        User registeredUser = registeredUserOptional.get();

        assertThat(registeredUser.getEmail()).isEqualTo("newuser@example.com");
        // Перевіряємо, що пароль закодований
        assertThat(encoder.matches("newPass123", registeredUser.getPassword())).isTrue();
        // Перевіряємо, що користувачу присвоєна роль
        assertThat(registeredUser.getRoles()).extracting(Role::getName).contains("ROLE_USER");
    }

    @Test
    @DisplayName("Should not register user if username already exists")
    @Sql(scripts = "/scripts/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void signUp_usernameAlreadyExists() throws Exception {
        long initialUserCount = userRepository.count(); // Отримуємо початкову кількість користувачів (має бути 2: testuser, adminuser)

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser"); // Спроба зареєструвати існуючого
        signupRequest.setEmail("another@example.com"); // Уникайте дублювання email, навіть якщо username зайнятий
        signupRequest.setPassword("password");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Username is already taken!"));

        // Перевіряємо, що кількість користувачів не змінилася
        assertThat(userRepository.count()).isEqualTo(initialUserCount);// Має бути лише один "testuser"
    }

    // Тест для захищеного JWT ендпоінта (наприклад, оновлення профілю)
    @Test
    @Sql(scripts = "/scripts/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/scripts/clean-up.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateProfile_withValidJwt_shouldSucceed() throws Exception {
        // 1. Вхід користувача для отримання JWT
        String loginRequestJson = "{\"username\":\"testuser\",\"password\":\"password123\"}";

        MvcResult loginResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        // 2. Витягуємо JWT токен з відповіді
        String loginResponseContent = loginResult.getResponse().getContentAsString();
        String jwtToken = JsonPath.read(loginResponseContent, "$.accessToken");


        // 3. Оновлення профілю з використанням отриманого JWT токена
        String updateProfileJson = "{\"username\":\"updatedUser\",\"email\":\"updated@example.com\"}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .content(updateProfileJson))
                .andExpect(status().isOk());

        // --- 3. Перевірка змін у базі даних ---
        Optional<User> updatedUserOptional = userRepository.findByUsername("updatedUser");
        assertThat(updatedUserOptional).isPresent();
        User updatedUser = updatedUserOptional.get();

        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(userRepository.findByUsername("testuser")).isEmpty(); // Перевіряємо, що старого користувача немає
    }

    @Test
    @DisplayName("Should not update user profile without JWT token")
    @Sql(scripts = "/scripts/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateProfile_withoutJwt_shouldReturnUnauthorized() throws Exception {
        // Arrange
        ProfileUpdateRequest profileUpdateRequest = new ProfileUpdateRequest("updatedUser", "updated@example.com");
        String jsonProfileUpdateRequest = objectMapper.writeValueAsString(profileUpdateRequest);

        // Act & Assert
        mockMvc.perform(post("/api/user/profile") // Або PutMapping
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonProfileUpdateRequest))
                .andExpect(status().isUnauthorized()); // Очікуємо 401 Unauthorized
    }

    @Test
    @DisplayName("Should not update user profile with invalid JWT token")
    @Sql(scripts = "/scripts/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateProfile_withInvalidJwt_shouldReturnForbidden() throws Exception {
        // Arrange
        ProfileUpdateRequest profileUpdateRequest = new ProfileUpdateRequest("updatedUser", "updated@example.com");
        String jsonProfileUpdateRequest = objectMapper.writeValueAsString(profileUpdateRequest);

        // Act & Assert
        mockMvc.perform(post("/api/user/profile") // Або PutMapping
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonProfileUpdateRequest))
                .andExpect(status().isUnauthorized()); // Очікуємо 401 Unauthorized
    }
}