package com.thropic.talki.progress.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;

import java.io.IOException;

/** Base de las pruebas *IT: un único RabbitMQ real para toda la ejecución. */
public abstract class AbstractRabbitIT {

    protected static final String EXCHANGE = "talki.events";
    private static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3.12-management");

    static {
        RABBIT.start();
    }

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    }

    protected final ObjectMapper json = new ObjectMapper();

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    /** Publica el evento con la forma real de scoring-service, incluido su __TypeId__. */
    protected void publishScoringCompleted(String sessionId, String userId, int overallScore,
                                           int durationSeconds) throws IOException {
        ObjectNode event = (ObjectNode) json.readTree(
                new ClassPathResource("contracts/scoring.completed.v1.json").getInputStream());
        event.put("sessionId", sessionId);
        event.put("userId", userId);
        event.put("overallScore", overallScore);
        event.put("durationSeconds", durationSeconds);
        Message message = MessageBuilder.withBody(json.writeValueAsBytes(event))
                .setContentType("application/json")
                .setHeader("__TypeId__", "com.thropic.talki.scoring.domain.event.ScoringCompletedEvent")
                .build();
        rabbitTemplate.send(EXCHANGE, "scoring.completed", message);
    }
}
