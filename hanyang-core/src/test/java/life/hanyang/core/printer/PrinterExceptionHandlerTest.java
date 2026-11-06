package life.hanyang.core.printer;

import life.hanyang.core.printer.service.PrinterExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;
import static org.assertj.core.api.Assertions.*;

class PrinterExceptionHandlerTest {
    private final PrinterExceptionHandler handler = new PrinterExceptionHandler();

    @ParameterizedTest
    @CsvSource({"23505,409", "23503,409", "23502,400", "23514,400", "22001,400", "22003,400", "XX000,500"})
    void classifiesSqlStatesWithoutExposingDatabaseMessage(String state, int status) {
        var result = handler.handleIntegrity(new DataIntegrityViolationException("private submitted value",
                new SQLException("private SQL and parameters", state)));
        assertThat(result.getStatusCode().value()).isEqualTo(status);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getError().getMessage()).doesNotContain("private");
    }

    @Test
    void unknownCauseIsServerErrorAndNestedSqlStateIsRecognized() {
        assertThat(handler.handleIntegrity(new DataIntegrityViolationException("unknown")).getStatusCode().value()).isEqualTo(500);
        var sql = new SQLException("wrapper");
        sql.setNextException(new SQLException("nested", "23503"));
        assertThat(handler.handleIntegrity(new DataIntegrityViolationException("wrapper", new RuntimeException(sql)))
                .getStatusCode().value()).isEqualTo(409);
    }
}
