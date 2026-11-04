package com.thropic.talki.progress.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_progress")
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    private int totalSessions;
    private int totalMinutes;
    private double averageScore;
    private int bestScore;
    private int currentStreak;

    public UserProgress() {}

    public UserProgress(String userId) {
        this.userId = userId;
    }

    public void recordSession(int scoreObtained, int durationSeconds) {
        totalSessions++;
        totalMinutes += durationSeconds / 60;
        averageScore = ((averageScore * (totalSessions - 1)) + scoreObtained) / totalSessions;
        if (scoreObtained > bestScore) bestScore = scoreObtained;
        currentStreak++;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String u) { this.userId = u; }
    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int t) { this.totalSessions = t; }
    public int getTotalMinutes() { return totalMinutes; }
    public void setTotalMinutes(int t) { this.totalMinutes = t; }
    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double a) { this.averageScore = a; }
    public int getBestScore() { return bestScore; }
    public void setBestScore(int b) { this.bestScore = b; }
    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int c) { this.currentStreak = c; }
}
