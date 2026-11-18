package in.feekeeper.member.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MemberRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = "^(?:\\+91)?[6-9]\\d{9}$", message = "must be a valid Indian mobile number") String phone,
        @NotBlank @Size(max = 100) String planName,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal planAmount,
        @NotNull @Min(1) @Max(3650) Integer planDurationDays,
        @NotNull LocalDate joinDate
) {
}
