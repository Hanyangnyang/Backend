package life.hanyang.admin.partnership.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import life.hanyang.core.partnership.domain.MerchantCategory;
import life.hanyang.core.partnership.dto.MerchantExportResponse;
import life.hanyang.core.partnership.service.PartnershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PartnershipExportControllerTest {
    private final PartnershipService service = mock(PartnershipService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new PartnershipAdminController(service,
                new ObjectMapper().findAndRegisterModules())).build();
    }

    @Test
    void downloadsRawJsonArrayWithImportCompatibleFieldNames() throws Exception {
        when(service.exportMerchants()).thenReturn(List.of(new MerchantExportResponse(
                "알촌", MerchantCategory.RESTAURANT, false,
                new MerchantExportResponse.Location(37.0, 126.0, "안산"), "🍚", "12345",
                List.of("알밥", "돈까스"), List.of())));
        mvc.perform(get("/api/v1/admin/partnership/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"merchants-partnerships.json\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$[0].name").value("알촌"))
                .andExpect(jsonPath("$[0].category").value("food"))
                .andExpect(jsonPath("$[0].is_active").value(false))
                .andExpect(jsonPath("$[0].representative_menus[0]").value("알밥"))
                .andExpect(jsonPath("$[0].location.full_address").value("안산"))
                .andExpect(jsonPath("$.success").doesNotExist());
        verify(service).exportMerchants();
        verifyNoMoreInteractions(service);
    }

    @Test
    void emptyDatabaseDownloadsAnEmptyArray() throws Exception {
        when(service.exportMerchants()).thenReturn(List.of());
        mvc.perform(get("/api/v1/admin/partnership/export"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
