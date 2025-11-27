package fortehackathon.prompt;

import java.util.List;

public class RequestPrompt {

    public static String buildTaskExtractionPrompt(String text, List<String> teamMembers) {
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

    public static String buildMeetingAnalysisPrompt(String transcription, List<String> teamMembers) {
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
}
