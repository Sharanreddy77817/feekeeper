package in.feekeeper.member;

import in.feekeeper.auth.AuthenticatedUser;
import in.feekeeper.auth.UserRole;
import in.feekeeper.member.dto.MemberCsvImportResponse;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberCsvImportServiceTest {
    private final MemberRepository repository = mock(MemberRepository.class);
    private final UUID tenantId = UUID.randomUUID();

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void validatesAndBatchesTwoHundredCsvMembers() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(UUID.randomUUID(), tenantId, "owner@example.in", UserRole.OWNER), "", null));
        when(repository.findPhonesUsedByTenant(eq(tenantId), anyCollection())).thenReturn(java.util.List.of());
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var clock = Clock.fixed(LocalDate.of(2026, 11, 17).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant(),
                ZoneId.of("Asia/Kolkata"));
        var service = new MemberCsvImportService(repository, new MemberStatusCalculator(clock), validator, clock);
        StringBuilder csv = new StringBuilder("fullName,phone,planName,planAmount,planDurationDays,joinDate\n");
        for (int index = 0; index < 200; index++) {
            csv.append("Member ").append(index).append(",7%09d".formatted(100_000_000 + index))
                    .append(",Monthly,700.00,30,2026-11-01\n");
        }

        MemberCsvImportResponse response = service.importCsv(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThat(response.totalRows()).isEqualTo(200);
        assertThat(response.importedCount()).isEqualTo(200);
        assertThat(response.failedCount()).isZero();
        assertThat(response.errors()).isEmpty();
        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(members -> {
            int count = 0;
            for (Object ignored : members) count++;
            return count == 200;
        }));
    }
}
