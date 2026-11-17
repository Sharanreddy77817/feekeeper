package in.feekeeper.member;

import in.feekeeper.common.exception.BadRequestException;

import java.util.ArrayList;
import java.util.List;

final class MemberCsvParser {
    private MemberCsvParser() { }

    static List<List<String>> parse(String csv) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean quoteClosed = false;

        for (int i = 0; i < csv.length(); i++) {
            char current = csv.charAt(i);
            if (quoted) {
                if (current == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        quoteClosed = true;
                    }
                } else {
                    field.append(current);
                }
                continue;
            }

            if (quoteClosed && current != ',' && current != '\r' && current != '\n' && !Character.isWhitespace(current)) {
                throw malformed("Unexpected character after a quoted CSV field");
            }
            if (current == '"' && field.length() == 0 && !quoteClosed) {
                quoted = true;
            } else if (current == ',') {
                record.add(field.toString());
                field.setLength(0);
                quoteClosed = false;
            } else if (current == '\r' || current == '\n') {
                if (current == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
                record.add(field.toString());
                addNonEmptyRecord(records, record);
                record = new ArrayList<>();
                field.setLength(0);
                quoteClosed = false;
            } else {
                field.append(current);
            }
        }

        if (quoted) throw malformed("CSV ends inside a quoted field");
        if (!record.isEmpty() || field.length() > 0 || quoteClosed) {
            record.add(field.toString());
            addNonEmptyRecord(records, record);
        }
        return records;
    }

    private static void addNonEmptyRecord(List<List<String>> records, List<String> record) {
        if (record.stream().anyMatch(value -> !value.isBlank())) records.add(List.copyOf(record));
    }

    private static BadRequestException malformed(String message) {
        return new BadRequestException("INVALID_CSV", message);
    }
}
