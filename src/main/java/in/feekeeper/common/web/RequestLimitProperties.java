package in.feekeeper.common.web;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.request-limits")
public record RequestLimitProperties(@Min(1024) long maxBodyBytes) { }
