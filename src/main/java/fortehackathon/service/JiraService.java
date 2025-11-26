package fortehackathon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fortehackathon.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
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

            HttpHeaders headers = createAuthHeaders(user);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> issueData = new HashMap<>();

            // Fields
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

            issueData.put("fields", fields);

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(issueData),
                    headers
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                return responseBody.get("key").asText();
            }

            throw new RuntimeException("Failed to create Jira issue: " + response.getBody());

        } catch (Exception e) {
            log.error("Error creating Jira issue", e);
            throw new RuntimeException("Failed to create Jira issue: " + e.getMessage());
        }
    }

    public void updateIssue(User user, String issueKey, Task task) {
        try {
            String endpoint = jiraUrl + "/rest/api/3/issue/" + issueKey;

            HttpHeaders headers = createAuthHeaders(user);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> updateData = new HashMap<>();
            Map<String, Object> fields = new HashMap<>();

            if (task.getSummary() != null) {
                fields.put("summary", task.getSummary());
            }
            if (task.getDescription() != null) {
                fields.put("description", createDescription(task.getDescription()));
            }
            if (task.getStatus() != null) {
                transitionIssue(user, issueKey, task.getStatus());
            }

            updateData.put("fields", fields);

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(updateData),
                    headers
            );

            restTemplate.exchange(
                    endpoint,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

        } catch (Exception e) {
            log.error("Error updating Jira issue", e);
            throw new RuntimeException("Failed to update Jira issue: " + e.getMessage());
        }
    }

    private void transitionIssue(User user, String issueKey, TaskStatus status) {
        try {
            String transitionId = getTransitionId(status);
            if (transitionId == null) return;

            String endpoint = jiraUrl + "/rest/api/3/issue/" + issueKey + "/transitions";

            HttpHeaders headers = createAuthHeaders(user);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> transitionData = Map.of(
                    "transition", Map.of("id", transitionId)
            );

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(transitionData),
                    headers
            );

            restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    request,
                    String.class
            );

        } catch (Exception e) {
            log.error("Error transitioning Jira issue", e);
        }
    }

    public boolean validateCredentials(String username, String apiToken) {
        try {
            String endpoint = jiraUrl + "/rest/api/3/myself";

            HttpHeaders headers = new HttpHeaders();
            String auth = username + ":" + apiToken;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("Error validating Jira credentials", e);
            return false;
        }
    }

    private HttpHeaders createAuthHeaders(User user) {
        HttpHeaders headers = new HttpHeaders();
        String auth = user.getJiraUsername() + ":" + user.getJiraApiToken();
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        return headers;
    }

    private Map<String, Object> createDescription(String text) {
        // Jira uses Atlassian Document Format (ADF)
        return Map.of(
                "type", "doc",
                "version", 1,
                "content", java.util.List.of(
                        Map.of(
                                "type", "paragraph",
                                "content", java.util.List.of(
                                        Map.of("type", "text", "text", text)
                                )
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
            case TODO -> "11"; // Обычно To Do
            case IN_PROGRESS -> "21"; // In Progress
            case IN_REVIEW -> "31"; // In Review
            case DONE -> "41"; // Done
            case BLOCKED -> "51"; // Blocked
        };
    }

    public String getIssueUrl(User user, String issueKey) {
        return user.getTeam().getJiraUrl() + "/browse/" + issueKey;
    }
}