package in.feekeeper.auth;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.auth-rate-limit")
public record AuthRateLimitProperties(@Min(1) int attempts, @Min(1) long windowMinutes) { }
