package fortehackathon.repository;

import fortehackathon.entity.Meeting;
import fortehackathon.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByTeamOrderByUploadedAtDesc(Team team);
}