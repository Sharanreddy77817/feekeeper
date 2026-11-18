package in.feekeeper.promotion.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record PromotionValidationRequest(@NotBlank String code,@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amount,@NotNull @Min(1) @Max(3650) Integer planDurationDays) {}
