package life.hanyang.user.printer.controller;

import life.hanyang.core.global.exception.GlobalExceptionHandler;
import life.hanyang.core.printer.domain.PrinterReportStatus;
import life.hanyang.core.printer.dto.PrinterDto.*;
import life.hanyang.core.printer.service.PrinterService;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PrinterControllerTest {
    private final PrinterService service = mock(PrinterService.class);
    private MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new PrinterController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void reportBindsOmittedAndExplicitFalseSeparately() throws Exception {
        UUID printerId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        when(service.reportUpdate(eq(printerId), any())).thenReturn(new Receipt(UUID.randomUUID(), PrinterReportStatus.PENDING));
        mvc.perform(post("/api/v1/campus-map/printers/" + printerId + "/reports")
                        .contentType("application/json").content("""
                        {"reporterDeviceId":"%s","expectedVersion":0,"changes":{"broken":false}}
                        """.formatted(deviceId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("PENDING"));
        verify(service).reportUpdate(eq(printerId), argThat(r -> r.changes().has("broken")
                && !r.changes().get("broken").booleanValue() && !r.changes().has("removed")));
    }

    @Test
    void newReportAllowsUnknownOptionalInformation() throws Exception {
        when(service.reportNew(any())).thenReturn(new Receipt(UUID.randomUUID(), PrinterReportStatus.PENDING));
        mvc.perform(post("/api/v1/campus-map/printer-reports").contentType("application/json").content("""
                {"reporterDeviceId":"%s","printer":{"buildingId":"b-1","detailLocation":"1층"}}
                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated());
        verify(service).reportNew(argThat(r -> r.printer().price() == null && r.printer().broken() == null && r.printer().removed() == null));
    }

    @Test
    void invalidReportsAreRejectedBeforeService() throws Exception {
        for (String body : new String[]{"{}", "{\"reporterDeviceId\":\"invalid\",\"printer\":{}}",
                "{\"reporterDeviceId\":\"" + UUID.randomUUID() + "\",\"printer\":{\"buildingId\":\"b-1\",\"detailLocation\":\" \"}}"}) {
            mvc.perform(post("/api/v1/campus-map/printer-reports").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/campus-map/printers/" + UUID.randomUUID() + "/reports")
                        .contentType("application/json").content("""
                        {"reporterDeviceId":"%s","expectedVersion":0,"changes":[]}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
