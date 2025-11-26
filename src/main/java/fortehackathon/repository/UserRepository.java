package fortehackathon.repository;

import fortehackathon.entity.Team;
import fortehackathon.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByTelegramId(Long telegramId);

    Optional<User> findByUsernameAndTeam(String username, Team team);

    boolean existsByUsername(String username);

    boolean existsByTelegramId(Long telegramId);
}
