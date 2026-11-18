package in.feekeeper.promotion.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record PromotionRequest(@NotBlank @Size(max=50) String code,@NotBlank @Size(max=100) String name,
 @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal discountPercent,
 @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal flatDiscount,
 @NotNull LocalDate validFrom,@NotNull LocalDate validTo,@Min(1) @Max(3650) Integer applicablePlanDays) {
 @AssertTrue public boolean isSingleDiscount(){return (discountPercent==null) != (flatDiscount==null);}
 @AssertTrue public boolean isDateRangeValid(){return validFrom==null||validTo==null||!validFrom.isAfter(validTo);}
}
