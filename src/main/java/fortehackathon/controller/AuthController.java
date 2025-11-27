package fortehackathon.controller;

import fortehackathon.dto.*;
import fortehackathon.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Методы для регистрации и входа пользователей")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Вход Project Manager через JIRA",
            description = "Позволяет Project Manager войти в систему, используя email и API-токен JIRA. " +
                          "Возвращает JWT-токен для последующих запросов.",
            requestBody = @RequestBody(
                    description = "Данные для входа Project Manager",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PMLoginRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешная аутентификация",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
            }
    )
    @PostMapping("/pm-login")
    public ResponseEntity<AuthResponse> pmLogin(@RequestBody PMLoginRequest request) {
        return ResponseEntity.ok(authService.authenticatePm(request));
    }

    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Позволяет создать нового пользователя в системе с указанием имени, email, пароля и роли.",
            requestBody = @RequestBody(
                    description = "Данные для регистрации нового пользователя",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RegisterRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пользователь успешно зарегистрирован",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Ошибка в данных регистрации")
            }
    )
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerUser(request));
    }

    @Operation(
            summary = "Проверка валидности JWT-токена",
            description = "Позволяет проверить действительность токена авторизации. " +
                          "Возвращает статус 200, если токен действителен, иначе 401.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Токен действителен",
                            content = @Content(schema = @Schema(implementation = TokenValidationResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Токен недействителен")
            }
    )
    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @RequestHeader("Authorization") String authHeader) {

        var response = authService.validateToken(authHeader);
        return ResponseEntity.status(response.isValid() ? 200 : 401).body(response);
    }
}
