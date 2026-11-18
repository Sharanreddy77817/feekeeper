package in.feekeeper.promotion;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name="promotions", uniqueConstraints=@UniqueConstraint(name="uk_promotions_tenant_code", columnNames={"tenant_id","code"}))
@Filter(name="tenantFilter", condition="tenant_id = :tenantId")
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class Promotion {
    @Id private UUID id;
    @Column(name="tenant_id",nullable=false) private UUID tenantId;
    @Column(nullable=false,length=50) private String code;
    @Column(nullable=false,length=100) private String name;
    @Column(name="discount_percent",precision=5,scale=2) private BigDecimal discountPercent;
    @Column(name="flat_discount",precision=12,scale=2) private BigDecimal flatDiscount;
    @Column(name="valid_from",nullable=false) private LocalDate validFrom;
    @Column(name="valid_to",nullable=false) private LocalDate validTo;
    @Column(nullable=false) private boolean active=true;
    @Column(name="applicable_plan_days") private Integer applicablePlanDays;
    public Promotion(UUID tenant,String code,String name,BigDecimal percent,BigDecimal flat,LocalDate from,LocalDate to,Integer days) {
        this.id=UUID.randomUUID(); this.tenantId=tenant; this.code=code.trim().toUpperCase(); this.name=name.trim();
        this.discountPercent=percent; this.flatDiscount=flat; this.validFrom=from; this.validTo=to; this.applicablePlanDays=days;
    }
    public BigDecimal discount(BigDecimal amount) {
        BigDecimal value=discountPercent!=null ? amount.multiply(discountPercent).movePointLeft(2) : flatDiscount;
        return value.min(amount).max(BigDecimal.ZERO).setScale(2,java.math.RoundingMode.HALF_UP);
    }
}
