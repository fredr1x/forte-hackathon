package fortehackathon.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private Long telegramId;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String email;

    private String jiraUsername;

    private String jiraApiToken;

    private LocalDateTime createdAt;

    private LocalDateTime lastLogin;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
}