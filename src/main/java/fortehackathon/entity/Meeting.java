package fortehackathon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "meetings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    private String fileName;

    private String fileUrl;

    @Column(columnDefinition = "TEXT")
    private String transcription;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL)
    private List<Task> generatedTasks;

    private LocalDateTime uploadedAt;

    private LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;
}