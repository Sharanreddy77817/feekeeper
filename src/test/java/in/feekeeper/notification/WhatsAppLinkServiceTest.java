package in.feekeeper.notification;

import in.feekeeper.auth.*;
import in.feekeeper.common.exception.NotFoundException;
import in.feekeeper.member.*;
import in.feekeeper.tenant.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WhatsAppLinkServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant(),
            ZoneId.of("Asia/Kolkata"));
    @Mock MemberRepository members;
    @Mock TenantRepository tenants;
    private UUID tenantId;
    private WhatsAppLinkService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Tenant tenant = new Tenant("Fit Gym", "Owner", "+919876543211", "fit@bank", CLOCK.instant());
        tenantId = tenant.getId();
        var principal = new AuthenticatedUser(UUID.randomUUID(), tenantId, "owner@example.com", UserRole.OWNER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant));
        service = new WhatsAppLinkService(members, tenants, new ReminderMessageFormatter(), CLOCK);
    }

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void createsCorrectPrefilledWaMeUrl() {
        Member member = new Member(tenantId, "Asha", "9876543210", "Monthly", new BigDecimal("1000.00"),
                30, TODAY, TODAY.plusDays(3), MemberStatus.EXPIRING_SOON, CLOCK.instant());
        when(members.findByIdAndTenantIdAndDeletedAtIsNull(member.getId(), tenantId)).thenReturn(Optional.of(member));

        String url = service.create(member.getId()).url();
        String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);

        assertThat(url).startsWith("https://wa.me/919876543210?text=");
        assertThat(decoded).contains("Hello Asha", "Fit Gym", "₹1000.00", "fit@bank");
    }

    @Test
    void anotherTenantsMemberReturnsNotFound() {
        UUID foreignMember = UUID.randomUUID();
        when(members.findByIdAndTenantIdAndDeletedAtIsNull(foreignMember, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(foreignMember)).isInstanceOf(NotFoundException.class);
        verify(tenants, never()).findById(any());
    }
}
