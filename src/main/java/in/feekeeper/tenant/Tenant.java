package in.feekeeper.tenant;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant {
    @Id private UUID id;
    @Column(name = "business_name", nullable = false, length = 150) private String businessName;
    @Column(name = "owner_name", nullable = false, length = 120) private String ownerName;
    @Column(nullable = false, length = 13) private String phone;
    @Column(name = "upi_id", length = 100) private String upiId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public Tenant(String businessName, String ownerName, String phone, String upiId, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.phone = phone;
        this.upiId = upiId;
        this.createdAt = createdAt;
    }

    public void updateSettings(String businessName, String ownerName, String phone, String upiId) {
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.phone = phone;
        this.upiId = upiId;
    }
}
