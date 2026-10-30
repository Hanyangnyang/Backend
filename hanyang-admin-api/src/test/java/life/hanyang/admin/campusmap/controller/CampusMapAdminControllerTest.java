package life.hanyang.admin.campusmap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import life.hanyang.core.campusmap.dto.CampusMapDto.*;
import life.hanyang.core.campusmap.service.*;
import life.hanyang.core.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.charset.StandardCharsets;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CampusMapAdminControllerTest {
    private final CampusMapService service = mock(CampusMapService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new CampusMapAdminController(service, new CampusMapImportReader(new ObjectMapper())))
                .setControllerAdvice(new CampusMapExceptionHandler(), new GlobalExceptionHandler()).build();
    }

    private MockMultipartFile file(String contents) {
        return new MockMultipartFile("file", "map.json", "application/json", contents.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void buildingImportAcceptsFileAndReturnsCounts() throws Exception {
        when(service.importBuildings(anyList())).thenReturn(new ImportResult(1, 1, 0));
        mvc.perform(multipart("/api/v1/admin/campus-map/buildings/import").file(file("""
                [{"id":"building-101","buildingNumber":"101","name":"본관","campus":"ANSAN",
                  "coordinates":{"latitude":null,"longitude":null},
                  "openSpaces":[{"id":"openspace-01","name":"열람실","floor":null,"hint":null}]}]
                """)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.buildings").value(1)).andExpect(jsonPath("$.data.openSpaces").value(1));
        verify(service).importBuildings(argThat(r -> r.size() == 1 && r.get(0).openSpaces().size() == 1));
    }

    @Test
    void smokingImportAcceptsExistingJsonShape() throws Exception {
        when(service.importSmokingAreas(anyList())).thenReturn(new ImportResult(0, 0, 1));
        mvc.perform(multipart("/api/v1/admin/campus-map/smoking-areas/import").file(file("""
                [{"id":"ansan-smoking-01","name":"문과대 건물 흡연부스","type":"BOOTH","campus":"ANSAN",
                  "coordinates":{"latitude":37.3002924187283,"longitude":126.83539445133155},
                  "hasAshtray":true,"description":"설명","imageUrl":[]}]
                """)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.smokingAreas").value(1));
        verify(service).importSmokingAreas(anyList());
    }

    @Test
    void missingAndMalformedFilesReturnBadRequestWithoutSaving() throws Exception {
        mvc.perform(multipart("/api/v1/admin/campus-map/buildings/import"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("C001"));
        mvc.perform(multipart("/api/v1/admin/campus-map/buildings/import").file(file("not json")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("C001"));
        verifyNoInteractions(service);
    }

    @Test
    void concurrentDatabaseConflictReturns409() throws Exception {
        when(service.importBuildings(anyList())).thenThrow(new DataIntegrityViolationException("database error", new java.sql.SQLException("constraint", "23505")));
        mvc.perform(multipart("/api/v1/admin/campus-map/buildings/import").file(file("[]")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("C005"));
    }
    @Test
    void parkingImportAcceptsProvidedJsonAndReturnsCount() throws Exception {
        when(service.importParkingLots(anyList())).thenReturn(new ParkingImportResult(1));
        mvc.perform(multipart("/api/v1/admin/campus-map/parking-lots/import").file(file("""
                [{"id":"ansan-parking-01","name":"제1주차장","campus":"ANSAN",
                  "coordinates":{"latitude":37.29937450655232,"longitude":126.83785523939314},
                  "capacity":143,"address":"경기 안산시 상록구 학사4길 1",
                  "description":"정문 근처","imageUrl":[]}]
                """)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.parkingLots").value(1));
        verify(service).importParkingLots(argThat(r -> r.size() == 1 && r.get(0).capacity() == 143
                && r.get(0).address().equals("경기 안산시 상록구 학사4길 1")));
    }

    @Test
    void parkingImportRejectsFractionalCapacityInsteadOfTruncating() throws Exception {
        mvc.perform(multipart("/api/v1/admin/campus-map/parking-lots/import")
                .file(file("[{\"capacity\":143.5}]")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("C001"));
        verifyNoInteractions(service);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"23502", "23503", "23514", "22001", "22003"})
    void inputConstraintViolationsReturn400WithoutLeakingDatabaseDetails(String state) throws Exception {
        when(service.importBuildings(anyList())).thenThrow(new DataIntegrityViolationException(
                "private database details", new java.sql.SQLException("private SQL", state)));
        mvc.perform(multipart("/api/v1/admin/campus-map/buildings/import").file(file("[]")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("C001"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private"))));
    }

    @Test
    void unknownIntegrityErrorReturns500InsteadOfMisreportingDuplicate() throws Exception {
        when(service.importBuildings(anyList())).thenThrow(new DataIntegrityViolationException("unknown failure"));
        mvc.perform(multipart("/api/v1/admin/campus-map/buildings/import").file(file("[]")))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error.code").value("C004"));
    }

}
