package fortehackathon.service;

import fortehackathon.dto.TaskExtractionResult;
import fortehackathon.prompt.RequestPrompt;
import fortehackathon.properties.OpenAIProperties;
import fortehackathon.util.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIService {

    private final OpenAiChatModel openAiChatModel;
    private final JsonParser jsonParser;
    private final OpenAIProperties openAIProperties;

    public TaskExtractionResult extractTaskFromText(String text, List<String> teamMembers) {
        try {
            String prompt = RequestPrompt.buildTaskExtractionPrompt(text, teamMembers);
            String response = callOpenAI(prompt);
            return jsonParser.parseTaskExtraction(response);
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
            String prompt = RequestPrompt.buildMeetingAnalysisPrompt(transcription, teamMembers);
            String response = callOpenAI(prompt);
            return jsonParser.parseMultipleTasks(response);
        } catch (Exception e) {
            log.error("Error extracting tasks from transcription", e);
            throw new RuntimeException("Failed to extract tasks: " + e.getMessage());
        }
    }

    public String transcribeAudio(byte[] audioData) {
        try {
            // todo add whisper logic
            String endpoint = openAIProperties.getOpenAIURL() + "/audio/transcriptions";

            var headers = new HttpHeaders();
            headers.setBearerAuth(openAIProperties.getOpenAIAPI());
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            log.info("Transcribing audio file of size: {} bytes", audioData.length);

            return "Transcribed meeting content would appear here";

        } catch (Exception e) {
            log.error("Error transcribing audio", e);
            throw new RuntimeException("Failed to transcribe audio: " + e.getMessage());
        }
    }

    private String callOpenAI(String prompt) {

        var response = openAiChatModel.call(new Prompt(
                prompt,
                getOptionsForTaskExtraction()));

        return response.getResult().getOutput().getText();
    }

    private String taskExtractionJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "summary": { "type": "string" },
                "description": { "type": "string" },
                "assignee": { "type": ["string", "null"] },
                "priority": { "type": "string" },
                "deadline": { "type": ["string", "null"], "format": "date-time" }
              },
              "required": ["summary", "description", "priority"]
            }
            """;
    }


    private OpenAiChatOptions getOptionsForTaskExtraction() {
        return OpenAiChatOptions.builder()
                .model("gpt-4.1-mini")
                .maxTokens(1500)
                .temperature(0.2)
                .responseFormat(
                        ResponseFormat.builder()
                                .type(ResponseFormat.Type.JSON_SCHEMA)
                                .jsonSchema(taskExtractionJsonSchema())
                                .build()
                )
                .build();
    }
}