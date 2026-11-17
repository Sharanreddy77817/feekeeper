package in.feekeeper.payment;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import in.feekeeper.auth.AuthenticatedUser;
import in.feekeeper.auth.UserRole;
import in.feekeeper.member.Member;
import in.feekeeper.member.MemberStatus;
import in.feekeeper.member.MemberRepository;
import in.feekeeper.tenant.Tenant;
import in.feekeeper.tenant.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReceiptServiceTest {
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final TenantRepository tenants = mock(TenantRepository.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsBrandedInstituteReceiptWithPaymentAndMemberDetails() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(UUID.randomUUID(), tenantId, "owner@example.in", UserRole.OWNER), "", null));
        Payment payment = new Payment(tenantId, memberId, new BigDecimal("700.00"), PaymentMode.UPI,
                LocalDate.of(2026, 11, 17), "FK-2026-000001", "receipt-test",
                LocalDate.of(2027, 2, 15), Instant.parse("2026-11-17T10:00:00Z"));
        Member member = new Member(tenantId, "Asha Sharma", "+919876543210", "Monthly Coaching",
                new BigDecimal("700.00"), 90, LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 17),
                MemberStatus.ACTIVE, Instant.parse("2026-11-17T10:00:00Z"));
        Tenant tenant = new Tenant("Sharma Fitness Institute", "Ravi Sharma", "+919812345678",
                "sharma@okaxis", Instant.parse("2026-01-01T00:00:00Z"));
        when(payments.findByIdAndTenantId(paymentId, tenantId)).thenReturn(Optional.of(payment));
        when(members.findByIdAndTenantId(memberId, tenantId)).thenReturn(Optional.of(member));
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant));

        byte[] pdf = new ReceiptService(payments, members, tenants).createPdf(paymentId);
        PdfReader reader = new PdfReader(pdf);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();

        assertThat(pdf).startsWith("%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        assertThat(text).contains("Sharma Fitness Institute", "PAYMENT", "RECEIVED", "FK-2026-000001",
                "Asha Sharma", "Monthly Coaching", "INR 700.00", "UPI", "15 Feb 2027",
                "sharma@okaxis", "computer-generated payment receipt");
    }
}
