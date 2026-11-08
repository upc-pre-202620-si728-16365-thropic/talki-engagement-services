package com.thropic.talki.progress.contract;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.thropic.talki.progress.infrastructure.persistence.UserProgressRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Un evento que no se puede aplicar queda visible en la cola de fallos, sin efectos. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProgressDeadLetterIT extends AbstractRabbitIT {

    private static final String DLQ = "progress.score-ready.dlq";

    @Autowired
    private UserProgressRepository progressRepository;

    @Test
    void malformedMessage_shouldEndInDeadLetterQueue() {
        Message malformed = MessageBuilder.withBody("{not-json".getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .build();

        rabbitTemplate.send(EXCHANGE, "scoring.completed", malformed);

        assertThat(rabbitTemplate.receive(DLQ, 15_000))
                .as("el mensaje inválido debe quedar en la cola de fallos").isNotNull();
    }

    @Test
    void eventWithoutSessionId_shouldEndInDeadLetterQueueWithoutChangingProgress() throws IOException {
        long before = progressRepository.count();
        ObjectNode event = (ObjectNode) json.readTree(
                new ClassPathResource("contracts/scoring.completed.v1.json").getInputStream());
        event.remove("sessionId");
        event.put("userId", "user-sin-sesion");
        Message message = MessageBuilder.withBody(json.writeValueAsBytes(event))
                .setContentType("application/json")
                .build();

        rabbitTemplate.send(EXCHANGE, "scoring.completed", message);

        assertThat(rabbitTemplate.receive(DLQ, 15_000))
                .as("sin sessionId no se puede garantizar una sola aplicación").isNotNull();
        assertThat(progressRepository.count()).isEqualTo(before);
    }
}
