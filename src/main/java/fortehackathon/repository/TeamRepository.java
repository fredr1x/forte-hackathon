package fortehackathon.repository;

import fortehackathon.entity.Team;
import fortehackathon.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByProjectManager(User pm);
}