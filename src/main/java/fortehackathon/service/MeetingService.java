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
            var meeting = Meeting.builder()
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
        var meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        try {
            meeting.setProcessingStatus(ProcessingStatus.PROCESSING);
            meetingRepository.save(meeting);

            log.info("Transcribing meeting {}", meetingId);
            String transcription = aiService.transcribeAudio(audioData);
            meeting.setTranscription(transcription);
            meetingRepository.save(meeting);

            log.info("Extracting tasks from meeting {}", meetingId);
            var teamMembers = user.getTeam().getMembers().stream()
                    .map(User::getUsername)
                    .collect(Collectors.toList());

            var extractedTasks =
                    aiService.extractTasksFromTranscription(transcription, teamMembers);

            for (var extraction : extractedTasks) {
                var jiraKey = buildAndSave(user, extraction, meeting);
                log.info("Created task {} from meeting {}", jiraKey, meetingId);
            }

            meeting.setProcessingStatus(ProcessingStatus.COMPLETED);
            meeting.setProcessedAt(LocalDateTime.now());
            meetingRepository.save(meeting);

            log.info("Meeting {} processed successfully, created {} tasks",
                    meetingId, extractedTasks.size());

        } catch (Exception e) {
            log.error("Error processing meeting {}", meetingId, e);
            meeting.setProcessingStatus(ProcessingStatus.FAILED);
            meetingRepository.save(meeting);
        }
    }

    public MeetingAnalysisResponse getMeetingStatus(User user, Long meetingId) {
        var meeting = meetingRepository.findById(meetingId)
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

    @Transactional
    public MeetingAnalysisResponse analyzeTranscript(User user, MeetingTranscriptRequest request) {
        validatePmRole(user);

        var meeting = Meeting.builder()
                .team(user.getTeam())
                .transcription(request.getTranscript())
                .uploadedAt(request.getMeetingDate() != null ? request.getMeetingDate() : LocalDateTime.now())
                .processingStatus(ProcessingStatus.PROCESSING)
                .build();

        meeting = meetingRepository.save(meeting);

        log.info("Processing transcript for meeting {}", meeting.getId());

        processTranscriptAsync(meeting.getId(), request.getTranscript(), user);

        return MeetingAnalysisResponse.builder()
                .meetingId(meeting.getId())
                .status(ProcessingStatus.PROCESSING.name())
                .message("Transcript is being processed")
                .build();
    }

    @Async
    @Transactional
    public void processTranscriptAsync(Long meetingId, String transcript, User user) {
        var meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        try {
            meeting.setProcessingStatus(ProcessingStatus.PROCESSING);
            meetingRepository.save(meeting);

            var extractedTasks = aiService.extractTasksFromTranscription(transcript,
                    user.getTeam().getMembers().stream().map(User::getUsername).toList());

            for (var extraction : extractedTasks) {
                buildAndSave(user, extraction, meeting);
            }

            meeting.setProcessingStatus(ProcessingStatus.COMPLETED);
            meeting.setProcessedAt(LocalDateTime.now());
            meetingRepository.save(meeting);

            log.info("Transcript for meeting {} processed successfully", meetingId);

        } catch (Exception e) {
            log.error("Error processing transcript for meeting {}", meetingId, e);
            meeting.setProcessingStatus(ProcessingStatus.FAILED);
            meetingRepository.save(meeting);
        }
    }

    private Task buildAndSave(User user, TaskExtractionResult extraction, Meeting meeting) {
        var task = Task.builder()
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

        var jiraKey = jiraService.createIssue(user, task);
        task.setJiraKey(jiraKey);
        task.setJiraUrl(jiraService.getIssueUrl(user, jiraKey));

        return taskRepository.save(task);
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
