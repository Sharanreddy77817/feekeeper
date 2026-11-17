package in.feekeeper.member;

import in.feekeeper.common.exception.BadRequestException;
import in.feekeeper.member.dto.MemberCsvImportResponse;
import in.feekeeper.member.dto.MemberCsvImportResponse.RowError;
import in.feekeeper.member.dto.MemberRequest;
import in.feekeeper.tenant.TenantContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

@Service
public class MemberCsvImportService {
    private static final int MAX_DATA_ROWS = 500;
    private static final List<String> REQUIRED_COLUMNS = List.of(
            "fullname", "phone", "planname", "planamount", "plandurationdays", "joindate");

    private final MemberRepository repository;
    private final MemberStatusCalculator statusCalculator;
    private final Validator validator;
    private final Clock clock;

    public MemberCsvImportService(MemberRepository repository, MemberStatusCalculator statusCalculator,
                                  Validator validator, Clock clock) {
        this.repository = repository;
        this.statusCalculator = statusCalculator;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional
    public MemberCsvImportResponse importCsv(byte[] contents) {
        if (contents == null || contents.length == 0) throw new BadRequestException("EMPTY_CSV", "Choose a non-empty CSV file");
        String csv = new String(contents, StandardCharsets.UTF_8);
        List<List<String>> records = MemberCsvParser.parse(csv);
        if (records.isEmpty()) throw new BadRequestException("EMPTY_CSV", "The CSV file has no header row");
        if (records.size() - 1 > MAX_DATA_ROWS) {
            throw new BadRequestException("CSV_ROW_LIMIT", "A CSV file can contain at most 500 member rows");
        }

        Map<String, Integer> columns = readHeader(records.getFirst());
        List<RowInput> parsed = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        for (int index = 1; index < records.size(); index++) {
            int rowNumber = index + 1;
            List<String> values = records.get(index);
            if (values.size() != records.getFirst().size()) {
                errors.add(new RowError(rowNumber, "row", "Column count does not match the header"));
                continue;
            }
            MemberRequest request = toRequest(values, columns, rowNumber, errors);
            if (request == null) continue;
            Set<ConstraintViolation<MemberRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                violations.stream().sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                        .forEach(violation -> errors.add(new RowError(rowNumber,
                                violation.getPropertyPath().toString().equals("supportedDuration")
                                        ? "planDurationDays" : violation.getPropertyPath().toString(),
                                violation.getMessage())));
                continue;
            }
            parsed.add(new RowInput(rowNumber, request));
        }

        UUID tenantId = TenantContext.getCurrentTenantId();
        List<String> phones = parsed.stream().map(input -> normalizePhone(input.request().phone())).distinct().toList();
        Set<String> usedPhones = new HashSet<>();
        if (!phones.isEmpty()) usedPhones.addAll(repository.findPhonesUsedByTenant(tenantId, phones));

        Set<String> seenInFile = new HashSet<>();
        List<Member> members = new ArrayList<>();
        for (RowInput input : parsed) {
            MemberRequest request = input.request();
            String phone = normalizePhone(request.phone());
            if (usedPhones.contains(phone)) {
                errors.add(new RowError(input.rowNumber(), "phone", "This phone already exists in this business"));
                continue;
            }
            if (!seenInFile.add(phone)) {
                errors.add(new RowError(input.rowNumber(), "phone", "This phone is repeated in the CSV file"));
                continue;
            }
            LocalDate expiry = request.joinDate().plusDays(request.planDurationDays());
            members.add(new Member(tenantId, request.fullName().trim(), phone, request.planName().trim(),
                    request.planAmount(), request.planDurationDays(), request.joinDate(), expiry,
                    statusCalculator.calculate(expiry), clock.instant()));
        }

        repository.saveAll(members);
        int totalRows = records.size() - 1;
        return new MemberCsvImportResponse(totalRows, members.size(), totalRows - members.size(), errors);
    }

    private Map<String, Integer> readHeader(List<String> header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int index = 0; index < header.size(); index++) {
            String name = header.get(index).trim().toLowerCase(Locale.ROOT);
            if (index == 0) name = name.replace("\uFEFF", "");
            if (columns.putIfAbsent(name, index) != null) {
                throw new BadRequestException("INVALID_CSV_HEADER", "The CSV header contains duplicate columns");
            }
        }
        if (!columns.keySet().containsAll(REQUIRED_COLUMNS)) {
            throw new BadRequestException("INVALID_CSV_HEADER",
                    "CSV must include: fullName, phone, planName, planAmount, planDurationDays, joinDate");
        }
        return columns;
    }

    private MemberRequest toRequest(List<String> row, Map<String, Integer> columns, int rowNumber, List<RowError> errors) {
        try {
            return new MemberRequest(value(row, columns, "fullname"), value(row, columns, "phone"),
                    value(row, columns, "planname"), new BigDecimal(value(row, columns, "planamount")),
                    Integer.valueOf(value(row, columns, "plandurationdays")),
                    LocalDate.parse(value(row, columns, "joindate")));
        } catch (NumberFormatException ex) {
            errors.add(new RowError(rowNumber, "planAmount/planDurationDays", "Amount and duration must be valid numbers"));
        } catch (java.time.DateTimeException ex) {
            errors.add(new RowError(rowNumber, "joinDate", "Join date must use YYYY-MM-DD format"));
        }
        return null;
    }

    private String value(List<String> row, Map<String, Integer> columns, String name) {
        return row.get(columns.get(name)).trim();
    }

    private String normalizePhone(String phone) { return phone.startsWith("+91") ? phone : "+91" + phone; }

    private record RowInput(int rowNumber, MemberRequest request) { }
}
