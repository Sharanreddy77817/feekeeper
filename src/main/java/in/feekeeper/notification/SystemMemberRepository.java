package in.feekeeper.notification;

import java.time.*;
import java.util.*;
import in.feekeeper.member.MemberStatus;

/** System-only cross-tenant data access. This type must never be injected into an API controller. */
public interface SystemMemberRepository {
    List<MemberStatusCandidate> findStatusPageAfter(UUID afterId, int limit);
    void updateStatus(UUID memberId, MemberStatus status, Instant updatedAt);
    List<ReminderCandidate> findPageAfter(UUID afterId, int limit);
    boolean markReminderSent(UUID memberId, UUID tenantId, LocalDate sentDate, Instant updatedAt);
}
