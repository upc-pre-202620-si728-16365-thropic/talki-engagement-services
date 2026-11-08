package com.thropic.talki.progress.interfaces.rest;

import com.thropic.talki.progress.domain.model.UserProgress;
import com.thropic.talki.progress.infrastructure.persistence.UserProgressRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/progress")
public class ProgressController {

    private final UserProgressRepository progressRepository;

    public ProgressController(UserProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    @Operation(summary = "Consultar el resumen de progreso de un estudiante",
            description = "Un usuario sin evaluaciones recibe el resumen en cero. El userId lo declara quien "
                    + "llama: el servicio aún no valida el JWT. currentStreak cuenta sesiones, no días.")
    @ApiResponse(responseCode = "200", description = "Resumen del estudiante",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject("""
                    {
                      "userId": "42",
                      "totalSessions": 1,
                      "totalMinutes": 5,
                      "averageScore": 87.0,
                      "bestScore": 87,
                      "currentStreak": 1
                    }""")))
    @ApiResponse(responseCode = "400", description = "Falta userId", content = @Content)
    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getDashboard(@RequestParam String userId) {
        UserProgress progress = progressRepository.findByUserId(userId)
                .orElse(new UserProgress(userId));
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "totalSessions", progress.getTotalSessions(),
                "totalMinutes", progress.getTotalMinutes(),
                "averageScore", progress.getAverageScore(),
                "bestScore", progress.getBestScore(),
                "currentStreak", progress.getCurrentStreak()
        ));
    }
}
