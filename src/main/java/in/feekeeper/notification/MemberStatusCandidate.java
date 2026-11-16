package in.feekeeper.notification;

import in.feekeeper.member.MemberStatus;

import java.time.LocalDate;
import java.util.UUID;

public record MemberStatusCandidate(UUID memberId, LocalDate expiryDate, MemberStatus currentStatus) { }
