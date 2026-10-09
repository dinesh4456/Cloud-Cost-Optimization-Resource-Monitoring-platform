package com.cloudops.optimizer.config;

import com.cloudops.optimizer.user.Role;
import com.cloudops.optimizer.user.User;
import com.cloudops.optimizer.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OptimizerProperties properties;

    public AdminUserInitializer(
            UserRepository userRepository, PasswordEncoder passwordEncoder, OptimizerProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = properties.getAdmin().getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User admin = new User(
                email,
                passwordEncoder.encode(properties.getAdmin().getPassword()),
                properties.getAdmin().getName(),
                Role.ADMIN);
        userRepository.save(admin);
        log.info("Created default ADMIN user {}", email);
    }
}
