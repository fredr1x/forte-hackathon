package fortehackathon.service;

import fortehackathon.dto.*;
import fortehackathon.entity.*;
import fortehackathon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TeamResponse createTeam(User pm, CreateTeamRequest request) {
        validatePmRole(pm);

        if (pm.getTeam() != null) {
            throw new RuntimeException("You already have a team");
        }

        Team team = Team.builder()
                .name(request.getName())
                .projectManager(pm)
                .jiraProjectKey(request.getJiraProjectKey())
                .jiraUrl(request.getJiraUrl())
                .members(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        team = teamRepository.save(team);

        pm.setTeam(team);
        userRepository.save(pm);

        log.info("Team {} created by PM {}", team.getName(), pm.getUsername());

        return mapToResponse(team);
    }

    @Transactional
    public TeamMemberResponse addMember(User pm, AddTeamMemberRequest request) {
        validatePmRole(pm);

        if (pm.getTeam() == null) {
            throw new RuntimeException("Create a team first");
        }

        User member = userRepository.findByUsername(request.getUsername())
                .orElseGet(() -> createNewMember(request, pm.getTeam()));

        if (member.getTeam() != null && !member.getTeam().getId().equals(pm.getTeam().getId())) {
            throw new RuntimeException("User is already in another team");
        }

        member.setTeam(pm.getTeam());
        member.setRole(Role.valueOf(request.getRole()));
        member.setEmail(request.getEmail());
        member.setJiraUsername(request.getJiraUsername());
        member = userRepository.save(member);

        log.info("Added member {} to team {}", member.getUsername(), pm.getTeam().getName());

        return mapMemberToResponse(member);
    }

    public TeamResponse getTeam(User user) {
        if (user.getTeam() == null) {
            throw new RuntimeException("You are not part of any team");
        }

        return mapToResponse(user.getTeam());
    }

    public List<TeamMemberResponse> getMembers(User user) {
        if (user.getTeam() == null) {
            throw new RuntimeException("You are not part of any team");
        }

        return user.getTeam().getMembers().stream()
                .map(this::mapMemberToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeMember(User pm, Long memberId) {
        validatePmRole(pm);

        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (member.getTeam() == null || !member.getTeam().getId().equals(pm.getTeam().getId())) {
            throw new RuntimeException("Member is not in your team");
        }

        if (member.getRole() == Role.PROJECT_MANAGER) {
            throw new RuntimeException("Cannot remove Project Manager from team");
        }

        member.setTeam(null);
        userRepository.save(member);

        log.info("Removed member {} from team {}", member.getUsername(), pm.getTeam().getName());
    }

    private User createNewMember(AddTeamMemberRequest request, Team team) {
        return User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode("changeme123")) // Временный пароль
                .email(request.getEmail())
                .jiraUsername(request.getJiraUsername())
                .role(Role.valueOf(request.getRole()))
                .team(team)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TeamResponse mapToResponse(Team team) {
        List<TeamMemberResponse> members = team.getMembers() != null
                ? team.getMembers().stream()
                .map(this::mapMemberToResponse)
                .collect(Collectors.toList())
                : List.of();

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .projectManager(team.getProjectManager().getUsername())
                .members(members)
                .jiraProjectKey(team.getJiraProjectKey())
                .build();
    }

    private TeamMemberResponse mapMemberToResponse(User user) {
        return TeamMemberResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .email(user.getEmail())
                .build();
    }

    private void validatePmRole(User user) {
        if (user.getRole() != Role.PROJECT_MANAGER) {
            throw new RuntimeException("Only Project Managers can perform this action");
        }
    }
}
