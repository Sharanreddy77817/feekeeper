package in.feekeeper.notification;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import in.feekeeper.member.MemberStatus;

import java.time.*;
import java.util.*;

@Repository
class JdbcSystemMemberRepository implements SystemMemberRepository {
    private final JdbcClient jdbc;

    JdbcSystemMemberRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public List<MemberStatusCandidate> findStatusPageAfter(UUID afterId, int limit) {
        String afterPredicate = afterId == null ? "" : "and id > :afterId";
        var statement = jdbc.sql("""
                select id, expiry_date, status from members
                 where deleted_at is null %s
                 order by id limit :limit
                """.formatted(afterPredicate)).param("limit", limit);
        if (afterId != null) statement = statement.param("afterId", afterId);
        return statement.query((rs, rowNum) -> new MemberStatusCandidate(rs.getObject("id", UUID.class),
                rs.getObject("expiry_date", LocalDate.class), MemberStatus.valueOf(rs.getString("status")))).list();
    }

    @Override
    public void updateStatus(UUID memberId, MemberStatus status, Instant updatedAt) {
        jdbc.sql("update members set status = :status, updated_at = :updatedAt where id = :memberId and deleted_at is null")
                .param("status", status.name()).param("updatedAt", updatedAt).param("memberId", memberId).update();
    }

    @Override
    public List<ReminderCandidate> findPageAfter(UUID afterId, int limit) {
        String afterPredicate = afterId == null ? "" : "where m.id > :afterId";
        var statement = jdbc.sql("""
                select m.id, m.tenant_id, m.phone, m.full_name, m.plan_amount, m.expiry_date,
                       m.last_reminder_sent_date, m.deleted_at, t.business_name, t.upi_id
                  from members m
                  join tenants t on t.id = m.tenant_id
                 %s
                 order by m.id
                 limit :limit
                """.formatted(afterPredicate)).param("limit", limit);
        if (afterId != null) statement = statement.param("afterId", afterId);
        return statement.query((rs, rowNum) -> new ReminderCandidate(
                        rs.getObject("id", UUID.class), rs.getObject("tenant_id", UUID.class),
                        rs.getString("phone"), rs.getString("full_name"), rs.getBigDecimal("plan_amount"),
                        rs.getObject("expiry_date", LocalDate.class),
                        rs.getObject("last_reminder_sent_date", LocalDate.class),
                        rs.getTimestamp("deleted_at") == null ? null : rs.getTimestamp("deleted_at").toInstant(),
                        rs.getString("business_name"),
                        rs.getString("upi_id"))).list();
    }

    @Override
    public boolean markReminderSent(UUID memberId, UUID tenantId, LocalDate sentDate, Instant updatedAt) {
        return jdbc.sql("""
                update members set last_reminder_sent_date = :sentDate, updated_at = :updatedAt
                 where id = :memberId and tenant_id = :tenantId and deleted_at is null
                   and (last_reminder_sent_date is null or last_reminder_sent_date <> :sentDate)
                """).param("sentDate", sentDate).param("updatedAt", updatedAt)
                .param("memberId", memberId).param("tenantId", tenantId).update() == 1;
    }
}
