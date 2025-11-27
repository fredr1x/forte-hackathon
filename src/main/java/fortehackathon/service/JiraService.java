package fortehackathon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fortehackathon.entity.Priority;
import fortehackathon.entity.Task;
import fortehackathon.entity.TaskStatus;
import fortehackathon.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class JiraService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${jira.url}")
    private String jiraUrl;

    public String createIssue(User user, Task task) {
        try {
            String endpoint = jiraUrl + "/rest/api/3/issue";
            var request = buildRequestEntity(user, buildIssuePayload(user, task));

            var response = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to create Jira issue: " + response.getBody());
            }

            var responseBody = objectMapper.readTree(response.getBody());
            return responseBody.get("key").asText();

        } catch (Exception e) {
            log.error("Error creating Jira issue", e);
            throw new RuntimeException("Failed to create Jira issue: " + e.getMessage());
        }
    }

    public void updateIssue(User user, String issueKey, Task task) {
        try {
            if (task.getStatus() != null) {
                transitionIssue(user, issueKey, task.getStatus());
            }

            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> fields = new HashMap<>();
            if (task.getSummary() != null) fields.put("summary", task.getSummary());
            if (task.getDescription() != null) fields.put("description", createDescription(task.getDescription()));
            payload.put("fields", fields);

            var request = buildRequestEntity(user, payload);
            restTemplate.exchange(jiraUrl + "/rest/api/3/issue/" + issueKey, HttpMethod.PUT, request, String.class);

        } catch (Exception e) {
            log.error("Error updating Jira issue", e);
            throw new RuntimeException("Failed to update Jira issue: " + e.getMessage());
        }
    }

    public boolean validateCredentials(String username, String apiToken) {
        try {
            String endpoint = jiraUrl + "/rest/api/3/myself";
            HttpHeaders headers = createAuthHeaders(username, apiToken);
            HttpEntity<String> request = new HttpEntity<>(headers);

            var response = restTemplate.exchange(endpoint, HttpMethod.GET, request, String.class);
            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("Error validating Jira credentials", e);
            return false;
        }
    }

    public String getIssueUrl(User user, String issueKey) {
        return user.getTeam().getJiraUrl() + "/browse/" + issueKey;
    }

    private HttpHeaders createAuthHeaders(User user) {
        return createAuthHeaders(user.getJiraUsername(), user.getJiraApiToken());
    }

    private HttpHeaders createAuthHeaders(String username, String apiToken) {
        String auth = username + ":" + apiToken;
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encoded);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpEntity<String> buildRequestEntity(User user, Map<String, Object> payload) throws Exception {
        return new HttpEntity<>(objectMapper.writeValueAsString(payload), createAuthHeaders(user));
    }

    private Map<String, Object> buildIssuePayload(User user, Task task) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("project", Map.of("key", user.getTeam().getJiraProjectKey()));
        fields.put("summary", task.getSummary());
        fields.put("description", createDescription(task.getDescription()));
        fields.put("issuetype", Map.of("name", "Task"));

        if (task.getAssignee() != null && task.getAssignee().getJiraUsername() != null) {
            fields.put("assignee", Map.of("name", task.getAssignee().getJiraUsername()));
        }

        if (task.getPriority() != null) {
            fields.put("priority", Map.of("name", mapPriority(task.getPriority())));
        }

        if (task.getDeadline() != null) {
            fields.put("duedate", task.getDeadline().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        return Map.of("fields", fields);
    }

    private void transitionIssue(User user, String issueKey, TaskStatus status) {
        try {
            String transitionId = getTransitionId(status);

            Map<String, Object> payload = Map.of("transition", Map.of("id", transitionId));
            HttpEntity<String> request = buildRequestEntity(user, payload);

            restTemplate.exchange(jiraUrl + "/rest/api/3/issue/" + issueKey + "/transitions",
                    HttpMethod.POST, request, String.class);

        } catch (Exception e) {
            log.error("Error transitioning Jira issue", e);
        }
    }

    private Map<String, Object> createDescription(String text) {
        return Map.of(
                "type", "doc",
                "version", 1,
                "content", List.of(
                        Map.of(
                                "type", "paragraph",
                                "content", List.of(Map.of("type", "text", "text", text))
                        )
                )
        );
    }

    private String mapPriority(Priority priority) {
        return switch (priority) {
            case LOW -> "Low";
            case MEDIUM -> "Medium";
            case HIGH -> "High";
            case CRITICAL -> "Highest";
        };
    }

    private String getTransitionId(TaskStatus status) {
        return switch (status) {
            case TODO -> "11";
            case IN_PROGRESS -> "21";
            case IN_REVIEW -> "31";
            case DONE -> "41";
            case BLOCKED -> "51";
        };
    }
}
