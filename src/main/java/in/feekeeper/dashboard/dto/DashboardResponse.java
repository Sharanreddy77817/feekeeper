package in.feekeeper.dashboard.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        long totalActiveMembers,
        long expiringIn5Days,
        long overdueMembers,
        BigDecimal revenueThisMonth
) { }
