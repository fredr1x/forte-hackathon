package fortehackathon.properties;


import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class OpenAIProperties {

    @Value("${openai.api}")
    private String openAIAPI;
}