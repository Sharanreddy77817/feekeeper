package in.feekeeper.payment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(name = "uk_payments_tenant_receipt", columnNames = {"tenant_id", "receipt_number"}))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "member_id", nullable = false) private UUID memberId;
    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2) private BigDecimal amountPaid;
    @Enumerated(EnumType.STRING) @Column(name = "payment_mode", nullable = false, length = 10) private PaymentMode paymentMode;
    @Column(name = "payment_date", nullable = false) private LocalDate paymentDate;
    @Column(name = "receipt_number", nullable = false, length = 50) private String receiptNumber;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @Column(name = "new_expiry_date") private LocalDate newExpiryDate;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public Payment(UUID tenantId, UUID memberId, BigDecimal amountPaid, PaymentMode paymentMode,
                   LocalDate paymentDate, String receiptNumber, String idempotencyKey,
                   LocalDate newExpiryDate, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.memberId = memberId;
        this.amountPaid = amountPaid;
        this.paymentMode = paymentMode;
        this.paymentDate = paymentDate;
        this.receiptNumber = receiptNumber;
        this.idempotencyKey = idempotencyKey;
        this.newExpiryDate = newExpiryDate;
        this.createdAt = createdAt;
    }
}
