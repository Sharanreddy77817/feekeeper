package in.feekeeper.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiProblem(Instant timestamp, int status, String errorCode, String message, String path,
                         List<FieldViolation> fieldErrors) {
    public record FieldViolation(String field, String message) { }
}
