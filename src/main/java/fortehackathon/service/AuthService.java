package fortehackathon.service;

import fortehackathon.dto.AuthResponse;
import fortehackathon.dto.PMLoginRequest;
import fortehackathon.dto.RegisterRequest;
import fortehackathon.dto.TokenValidationResponse;
import fortehackathon.entity.Role;
import fortehackathon.entity.Team;
import fortehackathon.entity.User;
import fortehackathon.repository.TeamRepository;
import fortehackathon.repository.UserRepository;
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
        if (!jiraService.validateCredentials(request.getJiraEmail(), request.getJiraApiToken())) {
            throw new RuntimeException("Invalid Jira credentials");
        }

        var user = userRepository.findByUsername(request.getJiraEmail())
                .orElseGet(() -> createNewPm(request));

        if (request.getTelegramId() != null && !request.getTelegramId().equals(user.getTelegramId())) {
            user.setTelegramId(request.getTelegramId());
        }

        user.setJiraUsername(request.getJiraEmail());
        user.setJiraApiToken(request.getJiraApiToken());
        user.setLastLogin(LocalDateTime.now());
        user = userRepository.save(user);

        var token = jwtService.generateToken(user.getUsername());

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

        var team = request.getTeamId() != null
                ? teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("Team not found: " + request.getTeamId()))
                : null;

        var user = buildUser(request, team);

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

    public TokenValidationResponse validateToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new TokenValidationResponse(false, "Missing or invalid Authorization header");
        }

        var token = authHeader.substring(7);

        try {
            var username = jwtService.extractUsername(token);

            if (!jwtService.validateToken(token, username)) {
                return new TokenValidationResponse(false, "Token is invalid or expired");
            }

            boolean exists = userRepository.existsByUsername(username);
            if (!exists) {
                return new TokenValidationResponse(false, "User does not exist");
            }

            return new TokenValidationResponse(true, "Token is valid");

        } catch (Exception e) {
            return new TokenValidationResponse(false, "Token validation error: " + e.getMessage());
        }
    }


    private User createNewPm(PMLoginRequest request) {
        return User.builder()
                .username(request.getJiraEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .telegramId(request.getTelegramId())
                .role(Role.PROJECT_MANAGER)
                .jiraUsername(request.getJiraEmail())
                .jiraApiToken(request.getJiraApiToken())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private User buildUser(RegisterRequest request, Team team) {
        return User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .telegramId(request.getTelegramId())
                .role(Role.valueOf(request.getRole()))
                .team(team)
                .createdAt(LocalDateTime.now())
                .lastLogin(LocalDateTime.now())
                .build();
    }
}
