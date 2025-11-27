package fortehackathon.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MeetingTranscriptRequest {
    private String transcript;
    private LocalDateTime meetingDate;
}
