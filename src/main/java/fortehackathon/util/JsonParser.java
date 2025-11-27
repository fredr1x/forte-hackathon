package fortehackathon.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fortehackathon.dto.TaskExtractionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JsonParser {

    private final ObjectMapper objectMapper;

    public TaskExtractionResult parseTaskExtraction(String jsonResponse) throws Exception {
        JsonNode node = objectMapper.readTree(jsonResponse);
        return parseNode(node);
    }

    public List<TaskExtractionResult> parseMultipleTasks(String jsonResponse) throws Exception {
        JsonNode arrayNode = objectMapper.readTree(jsonResponse);
        List<TaskExtractionResult> results = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            results.add(parseNode(node));
        }
        return results;
    }

    private TaskExtractionResult parseNode(JsonNode node) {
        TaskExtractionResult result = new TaskExtractionResult();
        result.setSummary(getText(node, "summary"));
        result.setDescription(getText(node, "description"));
        result.setAssigneeName(getTextOrNull(node));
        result.setPriority(getText(node, "priority"));
        result.setDeadline(getDateTimeOrNull(node));
        return result;
    }

    private String getText(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : "";
    }

    private String getTextOrNull(JsonNode node) {
        return (node.has("assignee") && !node.get("assignee").isNull()) ? node.get("assignee").asText() : null;
    }

    private LocalDateTime getDateTimeOrNull(JsonNode node) {
        if (node.has("deadline") && !node.get("deadline").isNull()) {
            return LocalDateTime.parse(node.get("deadline").asText(), DateTimeFormatter.ISO_DATE_TIME);
        }
        return null;
    }
}
