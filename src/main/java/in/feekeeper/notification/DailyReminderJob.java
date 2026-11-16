package in.feekeeper.notification;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyReminderJob {
    private final DailyReminderService service;

    public DailyReminderJob(DailyReminderService service) { this.service = service; }

    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Kolkata")
    @SchedulerLock(name = "dailyMemberStatusAndWhatsAppReminders", lockAtMostFor = "PT30M")
    public void runDaily() { service.run(); }
}
