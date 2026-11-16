package in.feekeeper.member;

import org.springframework.stereotype.Component;

import java.time.*;

@Component
public class MemberStatusCalculator {
    private final Clock clock;

    public MemberStatusCalculator(Clock clock) { this.clock = clock; }

    public MemberStatus calculate(LocalDate expiryDate) {
        LocalDate today = LocalDate.now(clock);
        if (expiryDate.isBefore(today)) return MemberStatus.OVERDUE;
        if (!expiryDate.isAfter(today.plusDays(5))) return MemberStatus.EXPIRING_SOON;
        return MemberStatus.ACTIVE;
    }
}
