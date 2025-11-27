package fortehackathon.controller;

import fortehackathon.dto.StatusOverviewResponse;
import fortehackathon.entity.User;
import fortehackathon.service.StatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
@Tag(name = "Статус", description = "Методы для получения статуса системы и обзора")
public class StatusController {

    private final StatusService statusService;

    @Operation(
            summary = "Получение обзора статуса",
            description = "Позволяет получить сводку по текущему статусу задач, митингов и другой информации, " +
                          "доступной пользователю.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Обзор статуса успешно получен",
                            content = @Content(schema = @Schema(implementation = StatusOverviewResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен для текущего пользователя")
            }
    )
    @GetMapping("/overview")
    public ResponseEntity<StatusOverviewResponse> getOverview(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(statusService.getOverview(user));
    }
}
