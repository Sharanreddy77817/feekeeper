package in.feekeeper.notification;

import in.feekeeper.member.MemberStatus;

import java.time.LocalDate;
import java.util.UUID;

public record MemberStatusCandidate(UUID memberId, LocalDate expiryDate, MemberStatus currentStatus, String timeZone) {
 public MemberStatusCandidate(UUID memberId,LocalDate expiryDate,MemberStatus currentStatus){this(memberId,expiryDate,currentStatus,"Asia/Kolkata");}
}
