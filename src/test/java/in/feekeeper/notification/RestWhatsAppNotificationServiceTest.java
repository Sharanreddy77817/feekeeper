package in.feekeeper.notification;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RestWhatsAppNotificationServiceTest {
    @Test
    void retriesServerFailuresWithBackoffThenReturnsSuccess() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://provider.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://provider.test/send")).andRespond(withServerError());
        server.expect(once(), requestTo("https://provider.test/send")).andRespond(withServerError());
        server.expect(once(), requestTo("https://provider.test/send"))
                .andRespond(withSuccess("{\"messageId\":\"abc-123\"}", MediaType.APPLICATION_JSON));
        RestWhatsAppNotificationService service = service(builder.build(), 3);

        NotificationResult result = service.send(message());

        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("abc-123");
        server.verify();
    }

    @Test
    void doesNotRetryClientFailure() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://provider.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://provider.test/send"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));
        RestWhatsAppNotificationService service = service(builder.build(), 3);

        NotificationResult result = service.send(message());

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("HTTP 400");
        server.verify();
    }

    private RestWhatsAppNotificationService service(RestClient client, int attempts) {
        WhatsAppProperties properties = new WhatsAppProperties("aisensy", "https://provider.test", "secret", "",
                "template", "en", Duration.ofSeconds(1), Duration.ofSeconds(1), attempts, Duration.ZERO, 100);
        ProviderPayloadMapper mapper = (message, text, props) ->
                new ProviderPayloadMapper.ProviderRequest("/send", Map.of(), Map.of("text", text));
        return new RestWhatsAppNotificationService(client, properties, new ReminderMessageFormatter(), mapper);
    }

    private ReminderMessage message() {
        return new ReminderMessage(UUID.randomUUID(), UUID.randomUUID(), "+919876543210", "Asha", "Fit Gym",
                new BigDecimal("1000.00"), LocalDate.of(2026, 11, 19), "fit@bank",
                ReminderVariant.EXPIRING_IN_3_DAYS);
    }
}
