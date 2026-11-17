package in.feekeeper.notification;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;
import java.net.http.HttpClient;
import java.util.Locale;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class NotificationConfiguration {
    @Bean
    LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource)).usingDbTime().build());
    }

    @Bean
    WhatsAppNotificationService whatsAppNotificationService(WhatsAppProperties properties,
                                                              ReminderMessageFormatter formatter) {
        String provider = properties.provider().toLowerCase(Locale.ROOT);
        if (provider.equals("mock")) return new MockWhatsAppNotificationService(formatter);
        if (properties.apiKey() == null || properties.apiKey().isBlank())
            throw new IllegalStateException("WHATSAPP_API_KEY is required for provider " + provider);

        ProviderPayloadMapper mapper = switch (provider) {
            case "aisensy" -> new AiSensyPayloadMapper();
            case "interakt" -> new InteraktPayloadMapper();
            case "meta" -> {
                if (properties.phoneNumberId() == null || properties.phoneNumberId().isBlank())
                    throw new IllegalStateException("WHATSAPP_PHONE_NUMBER_ID is required for provider meta");
                yield new MetaCloudApiPayloadMapper(formatter);
            }
            default -> throw new IllegalStateException("WHATSAPP_PROVIDER must be mock, aisensy, interakt, or meta");
        };
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        RestClient client = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
        return new RestWhatsAppNotificationService(client, properties, formatter, mapper);
    }
}
