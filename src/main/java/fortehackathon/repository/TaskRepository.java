package fortehackathon.repository;

import fortehackathon.entity.Task;
import fortehackathon.entity.TaskStatus;
import fortehackathon.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByTeam(Team team);

    List<Task> findByTeamAndStatus(Team team, TaskStatus status);

    List<Task> findByTeamAndDeadlineBefore(Team team, LocalDateTime deadline);
}