package in.feekeeper.promotion;
import in.feekeeper.common.exception.*;
import in.feekeeper.promotion.dto.*;
import in.feekeeper.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Service public class PromotionService {
 private final PromotionRepository repository; private final Clock clock;
 public PromotionService(PromotionRepository repository,Clock clock){this.repository=repository;this.clock=clock;}
 @Transactional(readOnly=true) public List<PromotionResponse> list(){LocalDate today=LocalDate.now(clock);return repository.findByTenantIdAndActiveTrue(TenantContext.getCurrentTenantId()).stream().filter(p->!today.isBefore(p.getValidFrom())&&!today.isAfter(p.getValidTo())).map(p->PromotionResponse.from(p,null)).toList();}
 @Transactional public PromotionResponse create(PromotionRequest request){var tenant=TenantContext.getCurrentTenantId();var p=new Promotion(tenant,request.code(),request.name(),request.discountPercent(),request.flatDiscount(),request.validFrom(),request.validTo(),request.applicablePlanDays());return PromotionResponse.from(repository.save(p),null);}
 @Transactional(readOnly=true) public PromotionResponse validate(PromotionValidationRequest request){var p=repository.findByTenantIdAndCode(TenantContext.getCurrentTenantId(),request.code().trim().toUpperCase()).orElseThrow(()->new NotFoundException("Promotion not found")); LocalDate today=LocalDate.now(clock); if(!p.isActive()||today.isBefore(p.getValidFrom())||today.isAfter(p.getValidTo())||(p.getApplicablePlanDays()!=null&&!p.getApplicablePlanDays().equals(request.planDurationDays()))) throw new BadRequestException("PROMOTION_NOT_VALID","Promotion is not valid for this date or plan"); return PromotionResponse.from(p,p.discount(request.amount()));}
 public Promotion getValid(String code,int days,BigDecimal amount){validate(new PromotionValidationRequest(code,amount,days));return repository.findByTenantIdAndCode(TenantContext.getCurrentTenantId(),code.trim().toUpperCase()).orElseThrow(()->new NotFoundException("Promotion not found"));}
}
