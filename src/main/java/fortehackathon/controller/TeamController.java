package fortehackathon.controller;

import fortehackathon.dto.*;
import fortehackathon.entity.User;
import fortehackathon.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping("/create")
    public ResponseEntity<TeamResponse> createTeam(
            @AuthenticationPrincipal User user,
            @RequestBody CreateTeamRequest request
    ) {
        return ResponseEntity.ok(teamService.createTeam(user, request));
    }

    @PostMapping("/members")
    public ResponseEntity<TeamMemberResponse> addMember(
            @AuthenticationPrincipal User user,
            @RequestBody AddTeamMemberRequest request
    ) {
        return ResponseEntity.ok(teamService.addMember(user, request));
    }

    @GetMapping
    public ResponseEntity<TeamResponse> getTeam(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(teamService.getTeam(user));
    }

    @GetMapping("/members")
    public ResponseEntity<List<TeamMemberResponse>> getMembers(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(teamService.getMembers(user));
    }

    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal User user,
            @PathVariable Long memberId
    ) {
        teamService.removeMember(user, memberId);
        return ResponseEntity.ok().build();
    }
}