package fortehackathon.controller;

import fortehackathon.dto.MeetingAnalysisResponse;
import fortehackathon.entity.User;
import fortehackathon.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping("/analyze")
    public ResponseEntity<MeetingAnalysisResponse> analyzeMeeting(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(meetingService.analyzeMeeting(user, file));
    }

    @GetMapping("/{meetingId}/status")
    public ResponseEntity<MeetingAnalysisResponse> getMeetingStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long meetingId
    ) {
        return ResponseEntity.ok(meetingService.getMeetingStatus(user, meetingId));
    }
}
