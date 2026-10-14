package com.cloudops.optimizer.user;

import com.cloudops.optimizer.activity.ActivityLogService;
import com.cloudops.optimizer.common.BusinessException;
import com.cloudops.optimizer.common.CurrentUserService;
import com.cloudops.optimizer.common.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public UserController(
            UserRepository userRepository,
            ActivityLogService activityLogService,
            CurrentUserService currentUserService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<User> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    @PatchMapping("/{id}/role")
    public User updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        User currentUser = currentUserService.getCurrentUserOrNull();
        if (currentUser != null && currentUser.getId().equals(id)) {
            throw new BusinessException("You cannot change your own administrative role");
        }

        String roleStr = body.getOrDefault("role", "USER");
        Role newRole = Role.valueOf(roleStr.toUpperCase());
        targetUser.setRole(newRole);
        User saved = userRepository.save(targetUser);

        activityLogService.record(
                currentUser,
                "USER_ROLE_CHANGED",
                "USER",
                String.valueOf(saved.getId()),
                "Updated role of " + saved.getEmail() + " to " + newRole.name()
        );

        return saved;
    }

    @PatchMapping("/{id}/password")
    public Map<String, String> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        String newPassword = body.get("password");
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new BusinessException("Password must be at least 8 characters long");
        }

        targetUser.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(targetUser);

        User currentUser = currentUserService.getCurrentUserOrNull();
        activityLogService.record(
                currentUser,
                "PASSWORD_RESET",
                "USER",
                String.valueOf(targetUser.getId()),
                "Admin reset password for " + targetUser.getEmail()
        );

        return Map.of(
                "status", "success",
                "message", "Password successfully reset for " + targetUser.getEmail()
        );
    }
}
