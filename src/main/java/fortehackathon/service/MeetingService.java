package fortehackathon.service;

import fortehackathon.dto.*;
import fortehackathon.entity.*;
import fortehackathon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AIService aiService;
    private final JiraService jiraService;

    @Transactional
    public MeetingAnalysisResponse analyzeMeeting(User user, MultipartFile file) {
        validatePmRole(user);

        try {
            Meeting meeting = Meeting.builder()
                    .team(user.getTeam())
                    .fileName(file.getOriginalFilename())
                    .uploadedAt(LocalDateTime.now())
                    .processingStatus(ProcessingStatus.UPLOADED)
                    .build();

            meeting = meetingRepository.save(meeting);

            log.info("Meeting uploaded: {}", meeting.getId());

            processMeetingAsync(meeting.getId(), file.getBytes(), user);

            return MeetingAnalysisResponse.builder()
                    .meetingId(meeting.getId())
                    .status(ProcessingStatus.PROCESSING.name())
                    .message("Meeting is being processed")
                    .build();

        } catch (IOException e) {
            log.error("Error uploading meeting file", e);
            throw new RuntimeException("Failed to upload meeting: " + e.getMessage());
        }
    }

    @Async
    @Transactional
    public void processMeetingAsync(Long meetingId, byte[] audioData, User user) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        try {
            meeting.setProcessingStatus(ProcessingStatus.PROCESSING);
            meetingRepository.save(meeting);

            log.info("Transcribing meeting {}", meetingId);
            String transcription = aiService.transcribeAudio(audioData);
            meeting.setTranscription(transcription);
            meetingRepository.save(meeting);

            log.info("Extracting tasks from meeting {}", meetingId);
            List<String> teamMembers = user.getTeam().getMembers().stream()
                    .map(User::getUsername)
                    .collect(Collectors.toList());

            List<TaskExtractionResult> extractedTasks =
                    aiService.extractTasksFromTranscription(transcription, teamMembers);

            for (TaskExtractionResult extraction : extractedTasks) {
                Task task = Task.builder()
                        .summary(extraction.getSummary())
                        .description(extraction.getDescription())
                        .team(user.getTeam())
                        .status(TaskStatus.TODO)
                        .priority(Priority.valueOf(extraction.getPriority()))
                        .deadline(extraction.getDeadline())
                        .meeting(meeting)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                if (extraction.getAssigneeName() != null) {
                    userRepository.findByUsernameAndTeam(extraction.getAssigneeName(), user.getTeam())
                            .ifPresent(task::setAssignee);
                }

                // Создаем в Jira
                String jiraKey = jiraService.createIssue(user, task);
                task.setJiraKey(jiraKey);
                task.setJiraUrl(jiraService.getIssueUrl(user, jiraKey));

                taskRepository.save(task);
                log.info("Created task {} from meeting {}", jiraKey, meetingId);
            }

            meeting.setProcessingStatus(ProcessingStatus.COMPLETED);
            meeting.setProcessedAt(LocalDateTime.now());
            meetingRepository.save(meeting);

            log.info("Meeting {} processed successfully, created {} tasks",
                    meetingId, extractedTasks.size());

        } catch (Exception e) {
            log.error("Error processing meeting " + meetingId, e);
            meeting.setProcessingStatus(ProcessingStatus.FAILED);
            meetingRepository.save(meeting);
        }
    }

    public MeetingAnalysisResponse getMeetingStatus(User user, Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        validateTeamAccess(user, meeting);

        List<TaskResponse> tasks = meeting.getGeneratedTasks() != null
                ? meeting.getGeneratedTasks().stream()
                .map(this::mapTaskToResponse)
                .collect(Collectors.toList())
                : List.of();

        return MeetingAnalysisResponse.builder()
                .meetingId(meeting.getId())
                .status(meeting.getProcessingStatus().name())
                .tasks(tasks)
                .processedAt(meeting.getProcessedAt())
                .build();
    }

    private TaskResponse mapTaskToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .key(task.getJiraKey())
                .summary(task.getSummary())
                .assignee(task.getAssignee() != null ? task.getAssignee().getUsername() : null)
                .deadline(task.getDeadline())
                .url(task.getJiraUrl())
                .build();
    }

    private void validatePmRole(User user) {
        if (user.getRole() != Role.PROJECT_MANAGER) {
            throw new RuntimeException("Only Project Managers can perform this action");
        }
    }

    private void validateTeamAccess(User user, Meeting meeting) {
        if (!meeting.getTeam().getId().equals(user.getTeam().getId())) {
            throw new RuntimeException("Access denied");
        }
    }
}
