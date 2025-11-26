package fortehackathon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "teams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "pm_id")
    private User projectManager;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    private List<User> members;

    private String jiraProjectKey;

    private String jiraUrl;

    private LocalDateTime createdAt;
}