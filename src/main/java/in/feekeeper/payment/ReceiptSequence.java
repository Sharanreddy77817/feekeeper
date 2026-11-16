package in.feekeeper.payment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.util.UUID;

@Entity
@Table(name = "receipt_sequences")
@IdClass(ReceiptSequenceId.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReceiptSequence {
    @Id @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Id @Column(nullable = false) private int year;
    @Column(name = "last_value", nullable = false) private long lastValue;
}
