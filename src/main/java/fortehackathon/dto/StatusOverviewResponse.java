package fortehackathon.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatusOverviewResponse {
    private Integer completed;
    private Integer inProgress;
    private Integer todo;
    private Integer overdue;
    private String updatedAt;
}