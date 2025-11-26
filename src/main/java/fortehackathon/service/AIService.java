package fortehackathon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fortehackathon.dto.TaskExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api}")
    private String openAiApiKey;

    @Value("${openai.url}")
    private String openAiApiUrl;

    public TaskExtractionResult extractTaskFromText(String text, List<String> teamMembers) {
        try {
            String prompt = buildTaskExtractionPrompt(text, teamMembers);
            String response = callOpenAI(prompt);
            return parseTaskExtraction(response);
        } catch (Exception e) {
            log.error("Error extracting task from text", e);
            throw new RuntimeException("Failed to extract task: " + e.getMessage());
        }
    }

    public List<TaskExtractionResult> extractTasksFromTranscription(
            String transcription,
            List<String> teamMembers
    ) {
        try {
            String prompt = buildMeetingAnalysisPrompt(transcription, teamMembers);
            String response = callOpenAI(prompt);
            return parseMultipleTasks(response);
        } catch (Exception e) {
            log.error("Error extracting tasks from transcription", e);
            throw new RuntimeException("Failed to extract tasks: " + e.getMessage());
        }
    }

    public String transcribeAudio(byte[] audioData) {
        try {
            // todo add whisper logic
            String endpoint = openAiApiUrl + "/audio/transcriptions";

            var headers = new HttpHeaders();
            headers.setBearerAuth(openAiApiKey);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            log.info("Transcribing audio file of size: {} bytes", audioData.length);

            return "Transcribed meeting content would appear here";

        } catch (Exception e) {
            log.error("Error transcribing audio", e);
            throw new RuntimeException("Failed to transcribe audio: " + e.getMessage());
        }
    }

    private String callOpenAI(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAiApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a Scrum Master assistant that extracts tasks from text."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3
        );

        HttpEntity<String> request = new HttpEntity<>(
                objectMapper.writeValueAsString(requestBody),
                headers
        );

        ResponseEntity<String> response = restTemplate.exchange(
                openAiApiUrl + "/chat/completions",
                HttpMethod.POST,
                request,
                String.class
        );

        JsonNode jsonResponse = objectMapper.readTree(response.getBody());
        return jsonResponse.get("choices").get(0).get("message").get("content").asText();
    }

    private String buildTaskExtractionPrompt(String text, List<String> teamMembers) {
        return String.format("""
            Extract task information from the following text and return it in JSON format:
            
            Text: "%s"
            
            Available team members: %s
            
            Return JSON with this structure:
            {
                "summary": "Brief task title",
                "description": "Detailed description",
                "assignee": "Team member name or null",
                "priority": "LOW/MEDIUM/HIGH/CRITICAL",
                "deadline": "ISO date or null"
            }
            
            Only return valid JSON, no additional text.
            """, text, String.join(", ", teamMembers));
    }

    private String buildMeetingAnalysisPrompt(String transcription, List<String> teamMembers) {
        return String.format("""
            Analyze this meeting transcription and extract all action items and tasks.
            Return them as a JSON array.
            
            Transcription: "%s"
            
            Available team members: %s
            
            Return JSON array with this structure:
            [
                {
                    "summary": "Brief task title",
                    "description": "Detailed description",
                    "assignee": "Team member name or null",
                    "priority": "LOW/MEDIUM/HIGH/CRITICAL",
                    "deadline": "ISO date or null"
                }
            ]
            
            Only return valid JSON array, no additional text.
            """, transcription, String.join(", ", teamMembers));
    }

    private TaskExtractionResult parseTaskExtraction(String jsonResponse) throws Exception {
        JsonNode node = objectMapper.readTree(jsonResponse);

        TaskExtractionResult result = new TaskExtractionResult();
        result.setSummary(node.get("summary").asText());
        result.setDescription(node.get("description").asText());
        result.setAssigneeName(node.has("assignee") && !node.get("assignee").isNull()
                ? node.get("assignee").asText() : null);
        result.setPriority(node.get("priority").asText());

        if (node.has("deadline") && !node.get("deadline").isNull()) {
            result.setDeadline(LocalDateTime.parse(
                    node.get("deadline").asText(),
                    DateTimeFormatter.ISO_DATE_TIME
            ));
        }

        return result;
    }

    private List<TaskExtractionResult> parseMultipleTasks(String jsonResponse) throws Exception {
        JsonNode arrayNode = objectMapper.readTree(jsonResponse);
        List<TaskExtractionResult> results = new ArrayList<>();

        for (JsonNode node : arrayNode) {
            var result = new TaskExtractionResult();
            result.setSummary(node.get("summary").asText());
            result.setDescription(node.get("description").asText());
            result.setAssigneeName(node.has("assignee") && !node.get("assignee").isNull()
                    ? node.get("assignee").asText() : null);
            result.setPriority(node.get("priority").asText());

            if (node.has("deadline") && !node.get("deadline").isNull()) {
                result.setDeadline(LocalDateTime.parse(
                        node.get("deadline").asText(),
                        DateTimeFormatter.ISO_DATE_TIME
                ));
            }

            results.add(result);
        }

        return results;
    }
}