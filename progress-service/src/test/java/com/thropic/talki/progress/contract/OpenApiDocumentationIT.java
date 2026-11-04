package com.thropic.talki.progress.contract;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** La documentación publicada describe el contrato real, con ejemplos y sin datos de otro curso. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiDocumentationIT extends AbstractRabbitIT {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void dashboard_shouldDocumentARealExampleAndTheMissingUserIdError() {
        JsonNode responses = apiDocs().at("/paths/~1v1~1progress~1dashboard/get/responses");

        assertThat(responses.at("/200/content/application~1json").toString())
                .contains("totalSessions").doesNotContain("additionalProp");
        assertThat(responses.has("400")).isTrue();
        assertThat(rest.getForEntity("/v1/progress/dashboard", String.class).getStatusCode().value())
                .isEqualTo(400);
    }

    @Test
    void documentInformation_shouldBelongToTheCurrentCourseAndContext() {
        JsonNode docs = apiDocs();

        assertThat(docs.at("/info/license/name").asText()).contains("1ASI0728");
        assertThat(docs.at("/info/description").asText())
                .contains("Progress & Adaptation").doesNotContain("Angular");
        assertThat(docs.at("/servers/0/url").asText()).isEqualTo("/");
        assertThat(docs.get("servers").toString()).doesNotContain("api.talki.lat");
    }

    private JsonNode apiDocs() {
        return rest.getForObject("/v3/api-docs", JsonNode.class);
    }
}
