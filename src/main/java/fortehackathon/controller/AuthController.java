package fortehackathon.controller;

import fortehackathon.dto.*;
import fortehackathon.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/pm-login")
    public ResponseEntity<AuthResponse> pmLogin(@RequestBody PMLoginRequest request) {
        return ResponseEntity.ok(authService.authenticatePm(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerUser(request));
    }

    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String token) {
        // todo add validation logic
        return ResponseEntity.ok().build();
    }
}
