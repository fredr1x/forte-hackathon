package fortehackathon.controller;

import fortehackathon.dto.*;
import fortehackathon.entity.User;
import fortehackathon.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Tag(name = "Митинги", description = "Методы для загрузки и анализа митингов, а также получения статуса")
public class MeetingController {

    private final MeetingService meetingService;

    @Operation(
            summary = "Анализ митинга из файла",
            description = "Позволяет загрузить аудиофайл митинга для последующего анализа и генерации задач. " +
                          "Возвращает идентификатор митинга и статус обработки.",
            requestBody = @RequestBody(
                    description = "Аудиофайл митинга в формате multipart/form-data",
                    required = true,
                    content = @Content(schema = @Schema(type = "string", format = "binary"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Митинг успешно загружен и анализируется",
                            content = @Content(schema = @Schema(implementation = MeetingAnalysisResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Ошибка при загрузке файла или неверный формат")
            }
    )
    @PostMapping("/analyze")
    public ResponseEntity<MeetingAnalysisResponse> analyzeMeeting(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(meetingService.analyzeMeeting(user, file));
    }

    @Operation(
            summary = "Анализ митинга из текстового транскрипта",
            description = "Позволяет отправить текстовый транскрипт митинга для анализа и генерации задач. " +
                          "Транскрипт может содержать метки времени и имя участника.",
            requestBody = @RequestBody(
                    description = "Данные транскрипта митинга",
                    required = true,
                    content = @Content(schema = @Schema(implementation = MeetingTranscriptRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Транскрипт успешно принят и анализируется",
                            content = @Content(schema = @Schema(implementation = MeetingAnalysisResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Ошибка при обработке транскрипта")
            }
    )
    @PostMapping("/analyze/transcript")
    public ResponseEntity<MeetingAnalysisResponse> analyzeTranscript(
            @AuthenticationPrincipal User user,
            @RequestBody MeetingTranscriptRequest request
    ) {
        MeetingAnalysisResponse response = meetingService.analyzeTranscript(user, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Получение статуса анализа митинга",
            description = "Позволяет получить текущий статус обработки митинга, а также список созданных задач.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Статус митинга получен успешно",
                            content = @Content(schema = @Schema(implementation = MeetingAnalysisResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Митинг с указанным ID не найден"),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен для текущего пользователя")
            }
    )
    @GetMapping("/{meetingId}/status")
    public ResponseEntity<MeetingAnalysisResponse> getMeetingStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long meetingId
    ) {
        return ResponseEntity.ok(meetingService.getMeetingStatus(user, meetingId));
    }
}
