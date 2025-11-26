package fortehackathon.controller;

import fortehackathon.dto.StatusOverviewResponse;
import fortehackathon.entity.User;
import fortehackathon.service.StatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
public class StatusController {

    private final StatusService statusService;

    @GetMapping("/overview")
    public ResponseEntity<StatusOverviewResponse> getOverview(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(statusService.getOverview(user));
    }
}