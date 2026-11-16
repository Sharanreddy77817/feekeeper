package in.feekeeper.member.dto;

import in.feekeeper.member.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record MemberResponse(UUID id, String fullName, String phone, String planName,
                             BigDecimal planAmount, int planDurationDays, LocalDate joinDate,
                             LocalDate expiryDate, MemberStatus status, LocalDate lastReminderSentDate,
                             Instant createdAt, Instant updatedAt) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getFullName(), member.getPhone(), member.getPlanName(),
                member.getPlanAmount(), member.getPlanDurationDays(), member.getJoinDate(), member.getExpiryDate(),
                member.getStatus(), member.getLastReminderSentDate(), member.getCreatedAt(), member.getUpdatedAt());
    }
}
