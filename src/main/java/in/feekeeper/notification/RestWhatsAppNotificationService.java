package in.feekeeper.notification;

import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.web.client.*;

import java.util.Map;

public class RestWhatsAppNotificationService implements WhatsAppNotificationService {
    private static final Logger log = LoggerFactory.getLogger(RestWhatsAppNotificationService.class);
    private final RestClient client;
    private final WhatsAppProperties properties;
    private final ReminderMessageFormatter formatter;
    private final ProviderPayloadMapper mapper;

    public RestWhatsAppNotificationService(RestClient client, WhatsAppProperties properties,
                                           ReminderMessageFormatter formatter, ProviderPayloadMapper mapper) {
        this.client = client;
        this.properties = properties;
        this.formatter = formatter;
        this.mapper = mapper;
    }

    @Override
    public NotificationResult send(ReminderMessage message) {
        ProviderPayloadMapper.ProviderRequest request = mapper.map(message, formatter.format(message), properties);
        int attempts = Math.max(1, properties.retryAttempts());
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                ResponseEntity<Map> response = client.post().uri(request.path())
                        .headers(headers -> request.headers().forEach(headers::set))
                        .contentType(MediaType.APPLICATION_JSON).body(request.body())
                        .retrieve().toEntity(Map.class);
                return NotificationResult.sent(extractMessageId(response.getBody()));
            } catch (HttpClientErrorException ex) {
                return NotificationResult.failed("Provider rejected request with HTTP " + ex.getStatusCode().value());
            } catch (HttpServerErrorException | ResourceAccessException ex) {
                if (attempt == attempts) return NotificationResult.failed("Provider unavailable after retries");
                if (!backoff()) return NotificationResult.failed("Notification retry interrupted");
            } catch (RestClientException ex) {
                log.warn("WhatsApp provider request failed for member {}", message.memberId());
                return NotificationResult.failed("Provider request failed");
            }
        }
        return NotificationResult.failed("Provider request failed");
    }

    private String extractMessageId(Map<?, ?> body) {
        if (body == null) return null;
        Object id = body.get("messageId");
        if (id == null) id = body.get("message_id");
        if (id == null) id = body.get("id");
        if (id == null && body.get("messages") instanceof java.util.List<?> messages && !messages.isEmpty()
                && messages.get(0) instanceof Map<?, ?> firstMessage) id = firstMessage.get("id");
        return id == null ? null : id.toString();
    }

    private boolean backoff() {
        try {
            Thread.sleep(Math.max(0, properties.retryBackoff().toMillis()));
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
