package in.feekeeper.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.whatsapp")
public record WhatsAppProperties(
        String provider,
        String baseUrl,
        String apiKey,
        String phoneNumberId,
        String templateName,
        String languageCode,
        Duration connectTimeout,
        Duration readTimeout,
        int retryAttempts,
        Duration retryBackoff,
        int batchSize
) { }
