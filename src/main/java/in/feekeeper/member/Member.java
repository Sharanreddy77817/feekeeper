package in.feekeeper.member;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "members", uniqueConstraints = @UniqueConstraint(name = "uk_members_tenant_phone", columnNames = {"tenant_id", "phone"}))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "full_name", nullable = false, length = 120) private String fullName;
    @Column(nullable = false, length = 13) private String phone;
    @Column(name = "plan_name", nullable = false, length = 100) private String planName;
    @Column(name = "plan_amount", nullable = false, precision = 12, scale = 2) private BigDecimal planAmount;
    @Column(name = "plan_duration_days", nullable = false) private int planDurationDays;
    @Column(name = "join_date", nullable = false) private LocalDate joinDate;
    @Column(name = "expiry_date", nullable = false) private LocalDate expiryDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MemberStatus status;
    @Column(name = "last_reminder_sent_date") private LocalDate lastReminderSentDate;
    @Column(name = "deleted_at") private Instant deletedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name = "applied_promotion_id") private UUID appliedPromotionId;

    public Member(UUID tenantId, String fullName, String phone, String planName, BigDecimal planAmount,
                  int planDurationDays, LocalDate joinDate, LocalDate expiryDate, MemberStatus status, Instant now) {
        this.id = UUID.randomUUID(); this.tenantId = tenantId; this.fullName = fullName; this.phone = phone;
        this.planName = planName; this.planAmount = planAmount; this.planDurationDays = planDurationDays;
        this.joinDate = joinDate; this.expiryDate = expiryDate; this.status = status;
        this.createdAt = now; this.updatedAt = now;
    }

    public void update(String fullName, String phone, String planName, BigDecimal planAmount,
                       int planDurationDays, LocalDate joinDate, LocalDate expiryDate,
                       MemberStatus status, Instant now) {
        this.fullName = fullName;
        this.phone = phone;
        this.planName = planName;
        this.planAmount = planAmount;
        this.planDurationDays = planDurationDays;
        this.joinDate = joinDate;
        this.expiryDate = expiryDate;
        this.status = status;
        this.updatedAt = now;
    }

    public void softDelete(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public void renew(LocalDate newExpiryDate, MemberStatus status, Instant now) {
        this.expiryDate = newExpiryDate;
        this.status = status;
        this.updatedAt = now;
    }
    public void recordPromotion(BigDecimal discount,UUID promotionId,Instant now){this.discountAmount=discount;this.appliedPromotionId=promotionId;this.updatedAt=now;}
}
