package in.feekeeper.member.dto;

import java.util.List;

public record MemberCsvImportResponse(int totalRows, int importedCount, int failedCount, List<RowError> errors) {
    public MemberCsvImportResponse {
        errors = List.copyOf(errors);
    }

    public record RowError(int rowNumber, String field, String message) { }
}
