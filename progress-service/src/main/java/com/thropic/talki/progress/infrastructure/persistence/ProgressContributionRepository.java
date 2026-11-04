package com.thropic.talki.progress.infrastructure.persistence;

import com.thropic.talki.progress.domain.model.ProgressContribution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressContributionRepository extends JpaRepository<ProgressContribution, Long> {
    boolean existsBySessionId(String sessionId);
}
