package in.feekeeper.payment;

import in.feekeeper.auth.*;
import in.feekeeper.common.exception.*;
import in.feekeeper.member.*;
import in.feekeeper.payment.dto.MarkPaidRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final Clock CLOCK = Clock.fixed(
            TODAY.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant(), ZoneId.of("Asia/Kolkata"));

    @Mock PaymentRepository payments;
    @Mock MemberRepository members;
    @Mock ReceiptSequenceRepository sequences;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PaymentService(payments, members, sequences, new MemberStatusCalculator(CLOCK), CLOCK);
        var principal = new AuthenticatedUser(UUID.randomUUID(), TENANT_ID, "owner@example.com", UserRole.OWNER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(payments.findByTenantIdAndIdempotencyKey(eq(TENANT_ID), anyString())).thenReturn(Optional.empty());
        when(sequences.findForUpdate(TENANT_ID, TODAY.getYear()))
                .thenReturn(Optional.of(new ReceiptSequence(TENANT_ID, TODAY.getYear(), 0)));
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @ParameterizedTest
    @ValueSource(ints = {30, 90, 180})
    void extendsExpiredMemberBySupportedPlanDuration(int duration) {
        Member member = member(duration, TODAY.minusDays(3));
        when(members.findForUpdate(member.getId(), TENANT_ID)).thenReturn(Optional.of(member));

        var result = service.markPaid(member.getId(), new MarkPaidRequest(null, PaymentMode.UPI), "key-" + duration);

        assertThat(result.amountPaid()).isEqualByComparingTo("1000.00");
        assertThat(result.newExpiryDate()).isEqualTo(TODAY.plusDays(duration));
        assertThat(member.getExpiryDate()).isEqualTo(TODAY.plusDays(duration));
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void renewingEarlyExtendsFromCurrentExpiry() {
        Member member = member(30, TODAY.plusDays(10));
        when(members.findForUpdate(member.getId(), TENANT_ID)).thenReturn(Optional.of(member));

        var result = service.markPaid(member.getId(), new MarkPaidRequest(new BigDecimal("900.00"), PaymentMode.CASH), "early");

        assertThat(result.newExpiryDate()).isEqualTo(TODAY.plusDays(40));
        assertThat(result.amountPaid()).isEqualByComparingTo("900.00");
    }

    @Test
    void receiptNumbersAreUniqueWithinTenant() {
        Member first = member(30, TODAY);
        Member second = member(30, TODAY);
        ReceiptSequence sequence = new ReceiptSequence(TENANT_ID, TODAY.getYear(), 0);
        when(sequences.findForUpdate(TENANT_ID, TODAY.getYear())).thenReturn(Optional.of(sequence));
        when(members.findForUpdate(first.getId(), TENANT_ID)).thenReturn(Optional.of(first));
        when(members.findForUpdate(second.getId(), TENANT_ID)).thenReturn(Optional.of(second));

        var one = service.markPaid(first.getId(), new MarkPaidRequest(null, PaymentMode.UPI), "one");
        var two = service.markPaid(second.getId(), new MarkPaidRequest(null, PaymentMode.CASH), "two");

        assertThat(one.receiptNumber()).isEqualTo("FK-2026-000001");
        assertThat(two.receiptNumber()).isEqualTo("FK-2026-000002");
    }

    @Test
    void idempotencyReplayReturnsOriginalWithoutLockingMember() {
        Member member = member(30, TODAY);
        Payment original = new Payment(TENANT_ID, member.getId(), new BigDecimal("1000.00"), PaymentMode.UPI,
                TODAY, "FK-2026-000007", "replay", TODAY.plusDays(30), CLOCK.instant());
        when(payments.findByTenantIdAndIdempotencyKey(TENANT_ID, "replay")).thenReturn(Optional.of(original));

        var result = service.markPaid(member.getId(), new MarkPaidRequest(null, PaymentMode.UPI), "replay");

        assertThat(result.id()).isEqualTo(original.getId());
        assertThat(result.receiptNumber()).isEqualTo("FK-2026-000007");
        verifyNoInteractions(members);
        verify(payments, never()).save(any());
    }

    @Test
    void memberFromAnotherTenantIsNotFound() {
        UUID foreignMemberId = UUID.randomUUID();
        when(members.findForUpdate(foreignMemberId, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markPaid(foreignMemberId,
                new MarkPaidRequest(null, PaymentMode.UPI), "foreign"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsNonPositiveAmount() {
        Member member = member(30, TODAY);
        when(members.findForUpdate(member.getId(), TENANT_ID)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.markPaid(member.getId(),
                new MarkPaidRequest(BigDecimal.ZERO, PaymentMode.UPI), "zero"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("greater than zero");
        verify(payments, never()).save(any());
    }

    private Member member(int duration, LocalDate expiry) {
        return new Member(TENANT_ID, "Test Member", "+919876543210", "Standard",
                new BigDecimal("1000.00"), duration, TODAY.minusDays(60), expiry,
                expiry.isBefore(TODAY) ? MemberStatus.OVERDUE : MemberStatus.ACTIVE, CLOCK.instant());
    }
}
