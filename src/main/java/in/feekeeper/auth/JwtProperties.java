package in.feekeeper.auth;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @Min(1) long expiryMinutes
) { }
