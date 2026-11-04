package com.thropic.talki.progress.infrastructure.messaging.consumer;

import com.thropic.talki.progress.application.ProgressProjectionService;
import com.thropic.talki.progress.domain.event.ScoringCompletedEvent;
import com.thropic.talki.progress.domain.model.UserProgress;
import com.thropic.talki.progress.infrastructure.persistence.ProgressContributionRepository;
import com.thropic.talki.progress.infrastructure.persistence.UserProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringCompletedConsumerTest {

    @Mock
    private UserProgressRepository progressRepository;

    @Mock
    private ProgressContributionRepository contributionRepository;

    private ScoringCompletedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ScoringCompletedConsumer(
                new ProgressProjectionService(progressRepository, contributionRepository));
    }

    @Test
    void onScoringCompleted_whenUserHasNoProgress_shouldCreateNew() {
        when(progressRepository.findByUserId("user-001")).thenReturn(Optional.empty());
        when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.onScoringCompleted(buildEvent("user-001", 85, 300));

        ArgumentCaptor<UserProgress> captor = ArgumentCaptor.forClass(UserProgress.class);
        verify(progressRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalSessions()).isEqualTo(1);
        assertThat(captor.getValue().getBestScore()).isEqualTo(85);
    }

    @Test
    void onScoringCompleted_whenUserAlreadyHasProgress_shouldUpdateExisting() {
        UserProgress existing = new UserProgress("user-002");
        existing.recordSession(70, 300);
        when(progressRepository.findByUserId("user-002")).thenReturn(Optional.of(existing));
        when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.onScoringCompleted(buildEvent("user-002", 90, 600));

        ArgumentCaptor<UserProgress> captor = ArgumentCaptor.forClass(UserProgress.class);
        verify(progressRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalSessions()).isEqualTo(2);
        assertThat(captor.getValue().getBestScore()).isEqualTo(90);
    }

    @Test
    void onScoringCompleted_shouldAlwaysPersistProgress() {
        when(progressRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.onScoringCompleted(buildEvent("user-003", 60, 180));

        verify(progressRepository, times(1)).save(any(UserProgress.class));
    }

    @Test
    void onScoringCompleted_whenSessionAlreadyApplied_shouldNotTouchProgress() {
        when(contributionRepository.existsBySessionId("session-user-004")).thenReturn(true);

        consumer.onScoringCompleted(buildEvent("user-004", 60, 180));

        verify(progressRepository, never()).save(any());
        verify(contributionRepository, never()).save(any());
    }

    @Test
    void onScoringCompleted_whenConcurrentDeliveryWins_shouldTreatItAsDuplicate() {
        when(contributionRepository.existsBySessionId("session-user-005")).thenReturn(false, true);
        when(contributionRepository.save(any())).thenThrow(new DataIntegrityViolationException("session_id"));

        assertThatCode(() -> consumer.onScoringCompleted(buildEvent("user-005", 60, 180)))
                .doesNotThrowAnyException();
    }

    @Test
    void onScoringCompleted_whenViolationIsNotADuplicate_shouldFailForRetry() {
        when(contributionRepository.existsBySessionId("session-user-006")).thenReturn(false);
        when(progressRepository.save(any())).thenThrow(new DataIntegrityViolationException("user_id"));
        when(progressRepository.findByUserId("user-006")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consumer.onScoringCompleted(buildEvent("user-006", 60, 180)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private ScoringCompletedEvent buildEvent(String userId, int score, int duration) {
        ScoringCompletedEvent event = new ScoringCompletedEvent();
        event.setEventId("evt-" + userId);
        event.setEventType("scoring.completed");
        event.setOccurredAt(Instant.now());
        event.setSessionId("session-" + userId);
        event.setUserId(userId);
        event.setOverallScore(score);
        event.setDurationSeconds(duration);
        event.setSilenceRatio(0.07);
        event.setWordsPerMinute(140.0);
        return event;
    }
}
