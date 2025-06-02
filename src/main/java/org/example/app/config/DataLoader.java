package org.example.app.config;
import org.example.app.models.Role;
import org.example.app.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataLoader {

    @Bean
    public CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            System.out.println("--- Initializing Roles ---");

            // Створюємо ROLE_USER, якщо її немає
            roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

            // Створюємо ROLE_ADMIN, якщо її немає
            roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));

            System.out.println("Default roles initialized.");
        };
    }
}