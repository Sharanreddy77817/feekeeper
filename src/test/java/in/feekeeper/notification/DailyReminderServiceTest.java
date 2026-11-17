package in.feekeeper.notification;

import org.junit.jupiter.api.*;
import org.mockito.*;
import in.feekeeper.member.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DailyReminderServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(IST).toInstant(), IST);

    @Mock SystemMemberRepository members;
    @Mock WhatsAppNotificationService notifications;
    private DailyReminderService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        WhatsAppProperties properties = new WhatsAppProperties("mock", "http://localhost", "", "", "fee_reminder",
                "en", Duration.ofSeconds(1), Duration.ofSeconds(1), 3, Duration.ZERO, 100);
        service = new DailyReminderService(members, notifications, properties,
                new MemberStatusCalculator(CLOCK), CLOCK);
        when(members.findStatusPageAfter(null, 100)).thenReturn(List.of());
    }

    @Test
    void selectsExactlyThreeDaysOutAndOneThroughSevenDaysOverdue() {
        ReminderCandidate threeDays = candidate(TODAY.plusDays(3), null, null);
        ReminderCandidate overdueOne = candidate(TODAY.minusDays(1), null, null);
        ReminderCandidate overdueSeven = candidate(TODAY.minusDays(7), null, null);
        ReminderCandidate overdueEight = candidate(TODAY.minusDays(8), null, null);
        ReminderCandidate activeOutsideWindow = candidate(TODAY.plusDays(2), null, null);
        ReminderCandidate deleted = candidate(TODAY.plusDays(3), null, CLOCK.instant());
        ReminderCandidate alreadySent = candidate(TODAY.minusDays(1), TODAY, null);
        when(members.findPageAfter(null, 100)).thenReturn(List.of(threeDays, overdueOne, overdueSeven,
                overdueEight, activeOutsideWindow, deleted, alreadySent));
        when(notifications.send(any())).thenReturn(NotificationResult.sent("provider-id"));

        service.run();

        ArgumentCaptor<ReminderMessage> messages = ArgumentCaptor.forClass(ReminderMessage.class);
        verify(notifications, times(3)).send(messages.capture());
        assertThat(messages.getAllValues()).extracting(ReminderMessage::memberId)
                .containsExactly(threeDays.memberId(), overdueOne.memberId(), overdueSeven.memberId());
        assertThat(messages.getAllValues()).extracting(ReminderMessage::variant)
                .containsExactly(ReminderVariant.EXPIRING_IN_3_DAYS, ReminderVariant.OVERDUE, ReminderVariant.OVERDUE);
        verify(members).findStatusPageAfter(null, 100);
        verify(members, times(3)).markReminderSent(any(), any(), eq(TODAY), eq(CLOCK.instant()));
    }

    @Test
    void refreshesStatusesThroughCentralCalculator() {
        MemberStatusCandidate expired = new MemberStatusCandidate(UUID.randomUUID(), TODAY.minusDays(1), MemberStatus.ACTIVE);
        MemberStatusCandidate soon = new MemberStatusCandidate(UUID.randomUUID(), TODAY.plusDays(5), MemberStatus.ACTIVE);
        MemberStatusCandidate active = new MemberStatusCandidate(UUID.randomUUID(), TODAY.plusDays(6), MemberStatus.ACTIVE);
        when(members.findStatusPageAfter(null, 100)).thenReturn(List.of(expired, soon, active));
        when(members.findPageAfter(null, 100)).thenReturn(List.of());

        service.run();

        verify(members).updateStatus(expired.memberId(), MemberStatus.OVERDUE, CLOCK.instant());
        verify(members).updateStatus(soon.memberId(), MemberStatus.EXPIRING_SOON, CLOCK.instant());
        verify(members, never()).updateStatus(eq(active.memberId()), any(), any());
    }

    @Test
    void secondRunOnSameDayDoesNotSendDuplicate() {
        ReminderCandidate unsent = candidate(TODAY.plusDays(3), null, null);
        AtomicBoolean marked = new AtomicBoolean();
        when(members.findPageAfter(null, 100)).thenAnswer(invocation -> List.of(
                candidateWithId(unsent.memberId(), TODAY.plusDays(3), marked.get() ? TODAY : null, null)));
        when(notifications.send(any())).thenReturn(NotificationResult.sent("sent"));
        when(members.markReminderSent(any(), any(), eq(TODAY), any())).thenAnswer(invocation -> {
            marked.set(true);
            return true;
        });

        service.run();
        service.run();

        verify(notifications, times(1)).send(any());
        verify(members, times(1)).markReminderSent(any(), any(), eq(TODAY), any());
    }

    @Test
    void failedSendIsNotMarkedAndDoesNotStopBatch() {
        ReminderCandidate failed = candidate(TODAY.minusDays(2), null, null);
        ReminderCandidate successful = candidate(TODAY.minusDays(3), null, null);
        when(members.findPageAfter(null, 100)).thenReturn(List.of(failed, successful));
        when(notifications.send(any())).thenReturn(NotificationResult.failed("timeout"), NotificationResult.sent("ok"));

        service.run();

        verify(notifications, times(2)).send(any());
        verify(members, never()).markReminderSent(eq(failed.memberId()), any(), any(), any());
        verify(members).markReminderSent(successful.memberId(), successful.tenantId(), TODAY, CLOCK.instant());
    }

    private ReminderCandidate candidate(LocalDate expiry, LocalDate lastSent, Instant deletedAt) {
        return candidateWithId(UUID.randomUUID(), expiry, lastSent, deletedAt);
    }

    private ReminderCandidate candidateWithId(UUID id, LocalDate expiry, LocalDate lastSent, Instant deletedAt) {
        return new ReminderCandidate(id, UUID.randomUUID(), "+919876543210", "Member", new BigDecimal("1000.00"),
                expiry, lastSent, deletedAt, "Business", "business@upi");
    }
}
