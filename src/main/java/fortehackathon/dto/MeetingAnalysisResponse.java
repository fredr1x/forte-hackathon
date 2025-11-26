package fortehackathon.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MeetingAnalysisResponse {
    private Long meetingId;
    private String status;
    private String message;
    private List<TaskResponse> tasks;
    private LocalDateTime processedAt;
}
