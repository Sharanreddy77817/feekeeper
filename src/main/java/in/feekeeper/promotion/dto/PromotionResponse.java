package in.feekeeper.promotion.dto;
import in.feekeeper.promotion.Promotion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public record PromotionResponse(UUID id,String code,String name,BigDecimal discountPercent,BigDecimal flatDiscount,LocalDate validFrom,LocalDate validTo,boolean active,Integer applicablePlanDays,BigDecimal calculatedDiscount) {
 public static PromotionResponse from(Promotion p,BigDecimal discount){return new PromotionResponse(p.getId(),p.getCode(),p.getName(),p.getDiscountPercent(),p.getFlatDiscount(),p.getValidFrom(),p.getValidTo(),p.isActive(),p.getApplicablePlanDays(),discount);}
}
