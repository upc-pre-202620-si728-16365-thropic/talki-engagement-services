package com.thropic.talki.progress.application;

import com.thropic.talki.progress.domain.event.ScoringCompletedEvent;
import com.thropic.talki.progress.domain.model.ProgressContribution;
import com.thropic.talki.progress.domain.model.UserProgress;
import com.thropic.talki.progress.infrastructure.persistence.ProgressContributionRepository;
import com.thropic.talki.progress.infrastructure.persistence.UserProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Incorpora una evaluación al progreso del propietario, una sola vez por sesión. */
@Service
public class ProgressProjectionService {

    private final UserProgressRepository progressRepository;
    private final ProgressContributionRepository contributionRepository;

    public ProgressProjectionService(UserProgressRepository progressRepository,
                                     ProgressContributionRepository contributionRepository) {
        this.progressRepository = progressRepository;
        this.contributionRepository = contributionRepository;
    }

    /**
     * El registro de la contribución y el resumen se confirman juntos: o se aplican
     * ambos o ninguno.
     *
     * @return el progreso actualizado, o vacío si la sesión ya estaba incorporada
     */
    @Transactional
    public Optional<UserProgress> apply(ScoringCompletedEvent event) {
        if (contributionRepository.existsBySessionId(event.getSessionId())) {
            return Optional.empty();
        }
        contributionRepository.save(new ProgressContribution(
                event.getSessionId(), event.getUserId(),
                event.getOverallScore(), event.getDurationSeconds()));
        UserProgress progress = progressRepository.findByUserId(event.getUserId())
                .orElseGet(() -> new UserProgress(event.getUserId()));
        progress.recordSession(event.getOverallScore(), event.getDurationSeconds());
        return Optional.of(progressRepository.save(progress));
    }

    @Transactional(readOnly = true)
    public boolean alreadyApplied(String sessionId) {
        return contributionRepository.existsBySessionId(sessionId);
    }
}
