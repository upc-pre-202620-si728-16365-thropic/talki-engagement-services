package com.thropic.talki.progress.contract;

import com.thropic.talki.progress.domain.model.UserProgress;
import com.thropic.talki.progress.infrastructure.persistence.UserProgressRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Consume scoring.completed con el contrato real que publica scoring-service. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ScoringCompletedProjectionIT extends AbstractRabbitIT {

    @Autowired
    private UserProgressRepository progressRepository;

    @Test
    void scoringCompleted_shouldUpdateOwnerProgress() throws IOException {
        String userId = "user-" + UUID.randomUUID();
        assertThat(progressRepository.findByUserId(userId)).as("antes del evento").isEmpty();

        publishScoringCompleted("session-" + UUID.randomUUID(), userId, 87, 300);

        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(progressRepository.findByUserId(userId)).isPresent());
        UserProgress progress = progressRepository.findByUserId(userId).orElseThrow();
        assertThat(progress.getTotalSessions()).isEqualTo(1);
        assertThat(progress.getTotalMinutes()).isEqualTo(5);
        assertThat(progress.getAverageScore()).isEqualTo(87.0);
        assertThat(progress.getBestScore()).isEqualTo(87);
    }
}
