package com.thropic.talki.progress.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.thropic.talki.progress.domain.model.UserProgress;
import com.thropic.talki.progress.infrastructure.persistence.UserProgressRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** SB1-05-A: una evaluación válida actualiza el resumen una sola vez. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProgressIdempotencyIT extends AbstractRabbitIT {

    @Autowired
    private UserProgressRepository progressRepository;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void dashboard_shouldChangeOnceWhenTheSameEvaluationArrivesTwice() throws IOException {
        String userId = "user-" + UUID.randomUUID();
        String sessionId = "session-" + UUID.randomUUID();
        String laterSession = "session-" + UUID.randomUUID();

        JsonNode before = dashboard(userId);
        assertThat(before.get("totalSessions").asInt()).isZero();

        publishScoringCompleted(sessionId, userId, 87, 300);
        awaitTotalSessions(userId, 1);

        publishScoringCompleted(sessionId, userId, 87, 300);
        // El segundo evento de otra sesión se procesa después del duplicado: cuando
        // llega, el duplicado ya se consumió y su efecto (si lo hubiera) sería visible.
        publishScoringCompleted(laterSession, userId, 67, 120);
        awaitAverage(userId, 77.0);

        JsonNode after = dashboard(userId);
        assertThat(after.get("totalSessions").asInt())
                .as("el evento repetido no debe contar otra sesión").isEqualTo(2);
        assertThat(after.get("totalMinutes").asInt()).isEqualTo(7);
        assertThat(after.get("averageScore").asDouble()).isEqualTo(77.0);
        assertThat(after.get("bestScore").asInt()).isEqualTo(87);
    }

    @Test
    void dashboard_shouldKeepEachUserIsolated() throws IOException {
        String owner = "user-" + UUID.randomUUID();
        String other = "user-" + UUID.randomUUID();

        publishScoringCompleted("session-" + UUID.randomUUID(), owner, 90, 60);
        awaitTotalSessions(owner, 1);

        assertThat(dashboard(other).get("totalSessions").asInt()).isZero();
    }

    private JsonNode dashboard(String userId) {
        return rest.getForObject("/v1/progress/dashboard?userId={user}", JsonNode.class, userId);
    }

    private void awaitTotalSessions(String userId, int expected) {
        await().atMost(Duration.ofSeconds(15)).until(() -> progressRepository.findByUserId(userId)
                .map(UserProgress::getTotalSessions).orElse(0) == expected);
    }

    private void awaitAverage(String userId, double expected) {
        await().atMost(Duration.ofSeconds(15)).until(() -> progressRepository.findByUserId(userId)
                .map(UserProgress::getAverageScore).orElse(0.0) == expected);
    }
}
