package fortehackathon.service;

import fortehackathon.dto.*;
import fortehackathon.entity.*;
import fortehackathon.repository.*;
import fortehackathon.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JiraService jiraService;

    @Transactional
    public AuthResponse authenticatePm(PMLoginRequest request) {
        if (!jiraService.validateCredentials(request.getUsername(), request.getPassword())) {
            throw new RuntimeException("Invalid Jira credentials");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseGet(() -> createNewPm(request));

        if (request.getTelegramId() != null && !request.getTelegramId().equals(user.getTelegramId())) {
            user.setTelegramId(request.getTelegramId());
        }

        user.setJiraUsername(request.getUsername());
        user.setJiraApiToken(request.getPassword());
        user.setLastLogin(LocalDateTime.now());
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername());

        log.info("PM {} authenticated successfully", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .userId(user.getId())
                .build();
    }

    @Transactional
    public AuthResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (request.getTelegramId() != null &&
            userRepository.existsByTelegramId(request.getTelegramId())) {
            throw new RuntimeException("Telegram account already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .telegramId(request.getTelegramId())
                .role(Role.valueOf(request.getRole()))
                .createdAt(LocalDateTime.now())
                .lastLogin(LocalDateTime.now())
                .build();

        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername());

        log.info("User {} registered successfully", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .userId(user.getId())
                .build();
    }

    private User createNewPm(PMLoginRequest request) {
        return User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .telegramId(request.getTelegramId())
                .role(Role.PROJECT_MANAGER)
                .jiraUsername(request.getUsername())
                .jiraApiToken(request.getPassword())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
