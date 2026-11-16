package in.feekeeper.payment;

import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class ReceiptSequenceId implements Serializable {
    private UUID tenantId;
    private int year;
}
