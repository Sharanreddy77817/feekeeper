package in.feekeeper.payment.dto;

import in.feekeeper.payment.PaymentMode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record MarkPaidRequest(
        @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal amountPaid,
        @NotNull PaymentMode paymentMode
) { }
