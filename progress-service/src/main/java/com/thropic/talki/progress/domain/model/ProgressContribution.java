package com.thropic.talki.progress.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Registro de la evaluación de una sesión ya incorporada al progreso. La restricción
 * única sobre session_id impide aplicar dos veces la misma evaluación.
 */
@Entity
@Table(name = "progress_contributions")
public class ProgressContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    protected ProgressContribution() {}

    public ProgressContribution(String sessionId, String userId, int overallScore, int durationSeconds) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.overallScore = overallScore;
        this.durationSeconds = durationSeconds;
        this.appliedAt = Instant.now();
    }

    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
}
