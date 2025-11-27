package fortehackathon.service;

import fortehackathon.dto.*;
import fortehackathon.entity.*;
import fortehackathon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final JiraService jiraService;
    private final AIService aiService;

    @Transactional
    public TaskResponse createTask(User user, CreateTaskRequest request) {
        validatePmRole(user);

        Task task = Task.builder()
                .summary(request.getSummary())
                .description(request.getDescription())
                .team(user.getTeam())
                .status(TaskStatus.TODO)
                .priority(Priority.valueOf(request.getPriority()))
                .deadline(request.getDeadline())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
            task.setAssignee(assignee);
        }

        String jiraKey = jiraService.createIssue(user, task);
        task.setJiraKey(jiraKey);
        task.setJiraUrl(jiraService.getIssueUrl(user, jiraKey));

        Task savedTask = taskRepository.save(task);

        log.info("Created task {} in Jira and DB", jiraKey);

        return mapToResponse(savedTask);
    }

    @Transactional
    public TaskResponse createTaskFromText(User user, TextTaskRequest request) {
        validatePmRole(user);

        List<String> teamMembers = user.getTeam().getMembers().stream()
                .map(User::getUsername)
                .collect(Collectors.toList());

        TaskExtractionResult extraction = aiService.extractTaskFromText(
                request.getDescription(),
                teamMembers
        );

        Task task = Task.builder()
                .summary(extraction.getSummary())
                .description(extraction.getDescription())
                .team(user.getTeam())
                .status(TaskStatus.TODO)
                .priority(Priority.valueOf(extraction.getPriority()))
                .deadline(extraction.getDeadline())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (extraction.getAssigneeName() != null) {
            userRepository.findByUsernameAndTeam(extraction.getAssigneeName(), user.getTeam())
                    .ifPresent(task::setAssignee);
        }

        String jiraKey = jiraService.createIssue(user, task);
        task.setJiraKey(jiraKey);
        task.setJiraUrl(jiraService.getIssueUrl(user, jiraKey));

        Task savedTask = taskRepository.save(task);

        log.info("Created task {} from text using AI", jiraKey);

        return mapToResponse(savedTask);
    }

    public List<TaskResponse> getTasks(User user, String status) {
        List<Task> tasks;

        if (status != null) {
            tasks = taskRepository.findByTeamAndStatus(
                    user.getTeam(),
                    TaskStatus.valueOf(status)
            );
        } else {
            tasks = taskRepository.findByTeam(user.getTeam());
        }

        return tasks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse getTask(User user, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        validateTeamAccess(user, task);

        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(User user, Long taskId, UpdateTaskRequest request) {
        validatePmRole(user);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        validateTeamAccess(user, task);

        if (request.getSummary() != null) {
            task.setSummary(request.getSummary());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(TaskStatus.valueOf(request.getStatus()));
        }
        if (request.getPriority() != null) {
            task.setPriority(Priority.valueOf(request.getPriority()));
        }
        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
            task.setAssignee(assignee);
        }

        task.setUpdatedAt(LocalDateTime.now());

        jiraService.updateIssue(user, task.getJiraKey(), task);

        Task updatedTask = taskRepository.save(task);

        log.info("Updated task {}", task.getJiraKey());

        return mapToResponse(updatedTask);
    }

    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .key(task.getJiraKey())
                .summary(task.getSummary())
                .description(task.getDescription())
                .assignee(task.getAssignee() != null ? task.getAssignee().getUsername() : null)
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .deadline(task.getDeadline())
                .url(task.getJiraUrl())
                .createdAt(task.getCreatedAt())
                .build();
    }

    private void validatePmRole(User user) {
        if (user.getRole() != Role.PROJECT_MANAGER) {
            throw new RuntimeException("Only Project Managers can perform this action");
        }
    }

    private void validateTeamAccess(User user, Task task) {
        if (!task.getTeam().getId().equals(user.getTeam().getId())) {
            throw new RuntimeException("Access denied");
        }
    }
}
