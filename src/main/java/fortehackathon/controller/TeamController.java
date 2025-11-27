package fortehackathon.controller;

import fortehackathon.dto.*;
import fortehackathon.entity.User;
import fortehackathon.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
@Tag(name = "Команды", description = "Методы для управления командами и их участниками")
public class TeamController {

    private final TeamService teamService;

    @Operation(
            summary = "Создание команды",
            description = "Позволяет создать новую команду",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Команда успешно создана",
                            content = @Content(schema = @Schema(implementation = TeamResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
            }
    )
    @PostMapping("/create")
    public ResponseEntity<TeamResponse> createTeam(
            @AuthenticationPrincipal User user,
            @RequestBody CreateTeamRequest request
    ) {
        return ResponseEntity.ok(teamService.createTeam(user, request));
    }

    @Operation(
            summary = "Добавление участника в команду",
            description = "Позволяет добавить нового участника в команду",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Участник успешно добавлен",
                            content = @Content(schema = @Schema(implementation = TeamMemberResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
            }
    )
    @PostMapping("/members")
    public ResponseEntity<TeamMemberResponse> addMember(
            @AuthenticationPrincipal User user,
            @RequestBody AddTeamMemberRequest request
    ) {
        return ResponseEntity.ok(teamService.addMember(user, request));
    }

    @Operation(
            summary = "Получение информации о команде",
            description = "Возвращает данные о команде пользователя",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Информация о команде успешно получена",
                            content = @Content(schema = @Schema(implementation = TeamResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
            }
    )
    @GetMapping
    public ResponseEntity<TeamResponse> getTeam(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(teamService.getTeam(user));
    }

    @Operation(
            summary = "Получение участников команды",
            description = "Возвращает список всех участников команды пользователя",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список участников успешно получен",
                            content = @Content(schema = @Schema(implementation = TeamMemberResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
            }
    )
    @GetMapping("/members")
    public ResponseEntity<List<TeamMemberResponse>> getMembers(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(teamService.getMembers(user));
    }

    @Operation(
            summary = "Удаление участника из команды",
            description = "Позволяет удалить участника по его ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Участник успешно удален"),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
                    @ApiResponse(responseCode = "404", description = "Участник не найден")
            }
    )
    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal User user,
            @PathVariable Long memberId
    ) {
        teamService.removeMember(user, memberId);
        return ResponseEntity.ok().build();
    }
}
