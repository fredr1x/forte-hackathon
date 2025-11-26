package fortehackathon.controller;

import fortehackathon.dto.*;
import fortehackathon.entity.User;
import fortehackathon.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/create")
    public ResponseEntity<TaskResponse> createTask(
            @AuthenticationPrincipal User user,
            @RequestBody CreateTaskRequest request
    ) {
        return ResponseEntity.ok(taskService.createTask(user, request));
    }

    @PostMapping("/create-from-text")
    public ResponseEntity<TaskResponse> createTaskFromText(
            @AuthenticationPrincipal User user,
            @RequestBody TextTaskRequest request
    ) {
        return ResponseEntity.ok(taskService.createTaskFromText(user, request));
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(taskService.getTasks(user, status));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(
            @AuthenticationPrincipal User user,
            @PathVariable Long taskId
    ) {
        return ResponseEntity.ok(taskService.getTask(user, taskId));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @AuthenticationPrincipal User user,
            @PathVariable Long taskId,
            @RequestBody UpdateTaskRequest request
    ) {
        return ResponseEntity.ok(taskService.updateTask(user, taskId, request));
    }
}