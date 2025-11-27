package fortehackathon.controller;

import fortehackathon.dto.*;
import fortehackathon.entity.User;
import fortehackathon.service.TaskService;
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
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Задачи", description = "Методы для управления задачами и их статусом")
public class TaskController {

    private final TaskService taskService;

    @Operation(
            summary = "Создание новой задачи",
            description = "Позволяет создать задачу с указанием всех необходимых полей",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Задача успешно создана",
                            content = @Content(schema = @Schema(implementation = TaskResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
            }
    )
    @PostMapping("/create")
    public ResponseEntity<TaskResponse> createTask(
            @AuthenticationPrincipal User user,
            @RequestBody CreateTaskRequest request
    ) {
        return ResponseEntity.ok(taskService.createTask(user, request));
    }

    @Operation(
            summary = "Создание задачи из текста",
            description = "Позволяет создать задачу, передав текстовое описание",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Задача успешно создана из текста",
                            content = @Content(schema = @Schema(implementation = TaskResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
            }
    )
    @PostMapping("/create-from-text")
    public ResponseEntity<TaskResponse> createTaskFromText(
            @AuthenticationPrincipal User user,
            @RequestBody TextTaskRequest request
    ) {
        return ResponseEntity.ok(taskService.createTaskFromText(user, request));
    }

    @Operation(
            summary = "Получение списка задач",
            description = "Возвращает список задач пользователя. Можно фильтровать по статусу",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список задач успешно получен",
                            content = @Content(schema = @Schema(implementation = TaskResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
            }
    )
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(taskService.getTasks(user, status));
    }

    @Operation(
            summary = "Получение задачи по ID",
            description = "Возвращает информацию о конкретной задаче по её идентификатору",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Задача успешно получена",
                            content = @Content(schema = @Schema(implementation = TaskResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Задача не найдена"),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
            }
    )
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(
            @AuthenticationPrincipal User user,
            @PathVariable Long taskId
    ) {
        return ResponseEntity.ok(taskService.getTask(user, taskId));
    }

    @Operation(
            summary = "Обновление задачи",
            description = "Позволяет обновить поля существующей задачи",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Задача успешно обновлена",
                            content = @Content(schema = @Schema(implementation = TaskResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Задача не найдена"),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
            }
    )
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @AuthenticationPrincipal User user,
            @PathVariable Long taskId,
            @RequestBody UpdateTaskRequest request
    ) {
        return ResponseEntity.ok(taskService.updateTask(user, taskId, request));
    }
}
