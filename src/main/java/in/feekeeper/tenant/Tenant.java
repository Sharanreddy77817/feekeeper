package in.feekeeper.tenant;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Locale;
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
    @Column(name = "currency_code", nullable = false, length = 3) private String currencyCode = "INR";
    @Column(name = "currency_symbol", nullable = false, length = 8) private String currencySymbol = "₹";
    @Column(name = "time_zone", nullable = false, length = 50) private String timeZone = "Asia/Kolkata";
    @Column(name = "receipt_theme", nullable = false, length = 50) private String receiptTheme = "ANDROMEDA";
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public Tenant(String businessName, String ownerName, String phone, String upiId, Instant createdAt) {
        this(businessName, ownerName, phone, upiId, "ANDROMEDA", createdAt);
    }

    public Tenant(String businessName, String ownerName, String phone, String upiId, String receiptTheme, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.phone = phone;
        this.upiId = upiId;
        this.receiptTheme = normalizeTheme(receiptTheme);
        this.createdAt = createdAt;
    }

    public void updateSettings(String businessName, String ownerName, String phone, String upiId) {
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.phone = phone;
        this.upiId = upiId;
    }

    public void updateInternationalSettings(String currencyCode, String currencySymbol, String timeZone) {
        this.currencyCode = currencyCode;
        this.currencySymbol = currencySymbol;
        this.timeZone = timeZone;
    }

    public void updateReceiptTheme(String receiptTheme) {
        this.receiptTheme = normalizeTheme(receiptTheme);
    }

    public void updateSettings(String businessName, String ownerName, String phone, String upiId,
                               String currencyCode, String currencySymbol, String timeZone) {
        updateSettings(businessName, ownerName, phone, upiId);
        updateInternationalSettings(currencyCode, currencySymbol, timeZone);
    }

    public void updateSettings(String businessName, String ownerName, String phone, String upiId,
                               String currencyCode, String currencySymbol, String timeZone, String receiptTheme) {
        updateSettings(businessName, ownerName, phone, upiId);
        updateInternationalSettings(currencyCode, currencySymbol, timeZone);
        updateReceiptTheme(receiptTheme);
    }

    private String normalizeTheme(String theme) {
        if (theme == null || theme.isBlank()) {
            return "ANDROMEDA";
        }
        String clean = theme.trim().toUpperCase(Locale.ROOT);
        return switch (clean) {
            case "NEBULA", "MILKY_WAY" -> clean;
            default -> "ANDROMEDA";
        };
    }
}
