package com.cloudops.optimizer.user;

import com.cloudops.optimizer.activity.ActivityLogService;
import com.cloudops.optimizer.common.BusinessException;
import com.cloudops.optimizer.common.CurrentUserService;
import com.cloudops.optimizer.common.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
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

    public UserController(
            UserRepository userRepository,
            ActivityLogService activityLogService,
            CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.currentUserService = currentUserService;
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
}
