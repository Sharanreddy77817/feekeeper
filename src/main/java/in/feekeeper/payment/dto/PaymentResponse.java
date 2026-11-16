package in.feekeeper.payment.dto;

import in.feekeeper.payment.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID memberId, BigDecimal amountPaid, PaymentMode paymentMode,
                              LocalDate paymentDate, String receiptNumber, LocalDate newExpiryDate,
                              Instant createdAt) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getMemberId(), payment.getAmountPaid(),
                payment.getPaymentMode(), payment.getPaymentDate(), payment.getReceiptNumber(),
                payment.getNewExpiryDate(), payment.getCreatedAt());
    }
}
