package com.cloudops.optimizer.auth;

import com.cloudops.optimizer.activity.ActivityLogService;
import com.cloudops.optimizer.auth.dto.AuthResponse;
import com.cloudops.optimizer.auth.dto.LoginRequest;
import com.cloudops.optimizer.auth.dto.RegisterRequest;
import com.cloudops.optimizer.common.BusinessException;
import com.cloudops.optimizer.config.OptimizerProperties;
import com.cloudops.optimizer.security.JwtService;
import com.cloudops.optimizer.user.Role;
import com.cloudops.optimizer.user.User;
import com.cloudops.optimizer.user.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OptimizerProperties properties;
    private final ActivityLogService activityLogService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OptimizerProperties properties,
            ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("An account with this email already exists");
        }
        User user = new User(email, passwordEncoder.encode(request.getPassword()), request.getFullName().trim(), Role.USER);
        userRepository.save(user);
        activityLogService.record(user, "REGISTER", "USER", String.valueOf(user.getId()), "New user registered");
        return issue(user, response);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new BusinessException("Invalid email or password"));
        if (!user.isEnabled() || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Invalid email or password");
        }
        activityLogService.record(user, "LOGIN", "USER", String.valueOf(user.getId()), "User signed in");
        return issue(user, response);
    }

    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie(properties.getJwt().getCookieName(), "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private AuthResponse issue(User user, HttpServletResponse response) {
        String token = jwtService.createToken(user.getEmail(), user.getRole().name());
        Cookie cookie = new Cookie(properties.getJwt().getCookieName(), token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (properties.getJwt().getExpirationMs() / 1000));
        response.addCookie(cookie);
        return new AuthResponse(token, user.getEmail(), user.getFullName(), user.getRole());
    }
}
