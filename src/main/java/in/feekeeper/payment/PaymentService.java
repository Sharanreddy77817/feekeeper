package in.feekeeper.payment;

import in.feekeeper.common.exception.*;
import in.feekeeper.member.*;
import in.feekeeper.payment.dto.*;
import in.feekeeper.tenant.TenantContext;
import in.feekeeper.tenant.TenantRepository;
import in.feekeeper.promotion.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final MemberRepository members;
    private final ReceiptSequenceRepository sequences;
    private final MemberStatusCalculator statusCalculator;
    private final Clock clock;
    private final PromotionService promotions;
    private final TenantRepository tenants;

    public PaymentService(PaymentRepository payments, MemberRepository members,
                          ReceiptSequenceRepository sequences, MemberStatusCalculator statusCalculator, Clock clock) {
        this(payments,members,sequences,statusCalculator,clock,null,null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public PaymentService(PaymentRepository payments, MemberRepository members,
                          ReceiptSequenceRepository sequences, MemberStatusCalculator statusCalculator, Clock clock,
                          PromotionService promotions, TenantRepository tenants) {
        this.payments = payments; this.members = members; this.sequences = sequences;
        this.statusCalculator = statusCalculator; this.clock = clock;
        this.promotions=promotions; this.tenants=tenants;
    }

    @Transactional
    public PaymentResponse markPaid(UUID memberId, MarkPaidRequest request, String idempotencyKey) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        String key = validateIdempotencyKey(idempotencyKey);
        var replay = payments.findByTenantIdAndIdempotencyKey(tenantId, key);
        if (replay.isPresent()) return PaymentResponse.from(replay.get());

        Member member = members.findForUpdate(memberId, tenantId)
                .orElseThrow(() -> new NotFoundException("Member not found"));
        replay = payments.findByTenantIdAndIdempotencyKey(tenantId, key);
        if (replay.isPresent()) return PaymentResponse.from(replay.get());

        BigDecimal amount = request.amountPaid() == null ? member.getPlanAmount() : request.amountPaid();
        if (amount.signum() <= 0) throw new BadRequestException("INVALID_PAYMENT_AMOUNT", "Payment amount must be greater than zero");

        LocalDate today = LocalDate.now(clock);
        LocalDate base = member.getExpiryDate().isAfter(today) ? member.getExpiryDate() : today;
        LocalDate newExpiry = base.plusDays(member.getPlanDurationDays());
        BigDecimal discount=BigDecimal.ZERO;
        UUID promotionId=null;
        String currency="INR";
        if(tenants!=null){var tenant=tenants.findById(tenantId).orElseThrow(()->new NotFoundException("Tenant not found"));currency=tenant.getCurrencyCode();}
        if(request.promotionCode()!=null&&!request.promotionCode().isBlank()){
            if(promotions==null) throw new BadRequestException("PROMOTION_UNAVAILABLE","Promotion validation is unavailable");
            var validation=promotions.validate(new in.feekeeper.promotion.dto.PromotionValidationRequest(request.promotionCode(),amount,member.getPlanDurationDays()));
            discount=validation.calculatedDiscount(); promotionId=validation.id();
            amount=amount.subtract(discount).max(BigDecimal.ZERO);
        }
        member.recordPromotion(discount,promotionId,clock.instant());
        String receiptNumber = nextReceiptNumber(tenantId, today.getYear());
        Payment payment = new Payment(tenantId, memberId, amount, request.paymentMode(), today,
                receiptNumber, key, newExpiry, clock.instant(),currency,discount);
        payments.save(payment);
        member.renew(newExpiry, statusCalculator.calculate(newExpiry), clock.instant());
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> list(UUID memberId, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate))
            throw new BadRequestException("INVALID_DATE_RANGE", "fromDate must not be after toDate");
        return payments.search(TenantContext.getCurrentTenantId(), memberId, fromDate, toDate, pageable)
                .map(PaymentResponse::from);
    }

    private String nextReceiptNumber(UUID tenantId, int year) {
        sequences.initialize(tenantId, year);
        ReceiptSequence sequence = sequences.findForUpdate(tenantId, year)
                .orElseThrow(() -> new IllegalStateException("Receipt sequence could not be initialized"));
        return "FK-%d-%06d".formatted(year, sequence.nextValue());
    }

    private String validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100)
            throw new BadRequestException("INVALID_IDEMPOTENCY_KEY", "Idempotency-Key must contain 1 to 100 characters");
        return key.trim();
    }
}
