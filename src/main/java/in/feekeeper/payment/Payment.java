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
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
