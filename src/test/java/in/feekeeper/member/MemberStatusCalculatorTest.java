package in.feekeeper.member;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;

class MemberStatusCalculatorTest {
    private final LocalDate today = LocalDate.of(2026, 11, 16);
    private final MemberStatusCalculator calculator = new MemberStatusCalculator(
            Clock.fixed(today.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant(), ZoneId.of("Asia/Kolkata")));

    @Test void expiryYesterdayIsOverdue() { assertThat(calculator.calculate(today.minusDays(1))).isEqualTo(MemberStatus.OVERDUE); }
    @Test void expiryTodayIsExpiringSoon() { assertThat(calculator.calculate(today)).isEqualTo(MemberStatus.EXPIRING_SOON); }
    @Test void expiryInFiveDaysIsExpiringSoon() { assertThat(calculator.calculate(today.plusDays(5))).isEqualTo(MemberStatus.EXPIRING_SOON); }
    @Test void expiryInSixDaysIsActive() { assertThat(calculator.calculate(today.plusDays(6))).isEqualTo(MemberStatus.ACTIVE); }
}
