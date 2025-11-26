package fortehackathon.service;

import fortehackathon.dto.StatusOverviewResponse;
import fortehackathon.entity.*;
import fortehackathon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatusService {

    private final TaskRepository taskRepository;

    public StatusOverviewResponse getOverview(User user) {
        if (user.getTeam() == null) {
            throw new RuntimeException("You are not part of any team");
        }

        List<Task> allTasks = taskRepository.findByTeam(user.getTeam());

        long completed = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();

        long inProgress = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS ||
                             t.getStatus() == TaskStatus.IN_REVIEW)
                .count();

        long todo = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO)
                .count();

        long overdue = allTasks.stream()
                .filter(t -> t.getDeadline() != null &&
                             t.getDeadline().isBefore(LocalDateTime.now()) &&
                             t.getStatus() != TaskStatus.DONE)
                .count();

        return StatusOverviewResponse.builder()
                .completed((int) completed)
                .inProgress((int) inProgress)
                .todo((int) todo)
                .overdue((int) overdue)
                .updatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
