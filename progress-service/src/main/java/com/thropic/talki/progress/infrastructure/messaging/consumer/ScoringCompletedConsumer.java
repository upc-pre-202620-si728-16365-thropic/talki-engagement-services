package com.thropic.talki.progress.infrastructure.messaging.consumer;

import com.thropic.talki.progress.application.ProgressProjectionService;
import com.thropic.talki.progress.domain.event.ScoringCompletedEvent;
import com.thropic.talki.progress.domain.model.UserProgress;
import com.thropic.talki.progress.infrastructure.messaging.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ScoringCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(ScoringCompletedConsumer.class);

    private final ProgressProjectionService projectionService;

    public ScoringCompletedConsumer(ProgressProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void onScoringCompleted(ScoringCompletedEvent event) {
        log.info("[progress-service] Actualizando progreso — userId={} sessionId={} score={}",
                event.getUserId(), event.getSessionId(), event.getOverallScore());

        Optional<UserProgress> updated;
        try {
            updated = projectionService.apply(event);
        } catch (DataIntegrityViolationException race) {
            // Otra entrega incorporó esta sesión a la vez. Si la violación fue por otra
            // causa (p. ej. primer progreso del usuario creado en paralelo), se reintenta.
            if (!projectionService.alreadyApplied(event.getSessionId())) {
                throw race;
            }
            updated = Optional.empty();
        }

        updated.ifPresentOrElse(
                progress -> log.info("[progress-service] Progreso guardado — userId={} totalSessions={} avg={}",
                        event.getUserId(), progress.getTotalSessions(), progress.getAverageScore()),
                () -> log.warn("[progress-service] scoring.completed duplicado ignorado — sessionId={}",
                        event.getSessionId()));
    }
}
