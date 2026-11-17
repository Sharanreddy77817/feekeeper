package in.feekeeper.notification;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MetaCloudApiPayloadMapperTest {
    @Test
    void mapsApprovedTemplateAndCredentialsToCloudApiRequest() {
        ReminderMessage message = new ReminderMessage(UUID.randomUUID(), UUID.randomUUID(), "+919876543210",
                "Asha", "Fit Gym", new BigDecimal("700"), LocalDate.of(2026, 11, 20), "fit@bank",
                ReminderVariant.OVERDUE);
        WhatsAppProperties properties = new WhatsAppProperties("meta", "https://graph.facebook.com/v27.0",
                "access-token-secret", "123456789", "fee_reminder", "en_IN", Duration.ofSeconds(1),
                Duration.ofSeconds(1), 1, Duration.ZERO, 100);

        ProviderPayloadMapper.ProviderRequest request = new MetaCloudApiPayloadMapper(new ReminderMessageFormatter())
                .map(message, "unused", properties);

        assertThat(request.path()).isEqualTo("/123456789/messages");
        assertThat(request.headers()).containsEntry("Authorization", "Bearer access-token-secret");
        assertThat(request.body()).containsEntry("messaging_product", "whatsapp")
                .containsEntry("to", "919876543210").containsEntry("type", "template");
        Map<?, ?> template = (Map<?, ?>) request.body().get("template");
        assertThat(template.get("name")).isEqualTo("fee_reminder");
        Map<?, ?> body = (Map<?, ?>) ((List<?>) template.get("components")).get(0);
        List<?> parameters = (List<?>) body.get("parameters");
        assertThat(((Map<?, ?>) parameters.get(0)).get("text")).isEqualTo("Asha");
        assertThat(((Map<?, ?>) parameters.get(2)).get("text")).isEqualTo("Your membership fee is overdue.");
        assertThat(((Map<?, ?>) parameters.get(3)).get("text")).isEqualTo("700.00");
        assertThat(((Map<?, ?>) parameters.get(4)).get("text")).isEqualTo("20 Nov 2026");
        assertThat(((Map<?, ?>) parameters.get(5)).get("text")).isEqualTo("fit@bank");
    }
}
