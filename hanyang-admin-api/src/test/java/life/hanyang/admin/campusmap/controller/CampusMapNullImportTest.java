package life.hanyang.admin.campusmap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import life.hanyang.core.campusmap.repository.*;
import life.hanyang.core.campusmap.service.*;
import life.hanyang.core.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CampusMapNullImportTest {
    static Stream<Arguments> malformedImports() {
        return Stream.of(
                Arguments.of("buildings", "[null]"),
                Arguments.of("smoking-areas", "[null]"),
                Arguments.of("parking-lots", "[null]"),
                Arguments.of("buildings", "[{},null]"),
                Arguments.of("smoking-areas", "[{},null]"),
                Arguments.of("parking-lots", "[{},null]"),
                Arguments.of("buildings", "[{\"openSpaces\":[null]}]"),
                Arguments.of("buildings", "[{\"openSpaces\":[{},null]}]"));
    }

    @ParameterizedTest
    @MethodSource("malformedImports")
    void rejectsNullBeforeResolvingIdsWithoutAccessingDatabase(String resource, String json) throws Exception {
        var buildings = mock(CampusBuildingRepository.class);
        var spaces = mock(OpenSpaceRepository.class);
        var smoking = mock(SmokingAreaRepository.class);
        var parking = mock(ParkingLotRepository.class);
        var entityManager = mock(EntityManager.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = new CampusMapService(buildings, spaces, smoking, parking,
                    factory.getValidator(), entityManager);
            var mvc = MockMvcBuilders.standaloneSetup(new CampusMapAdminController(service,
                            new CampusMapImportReader(new ObjectMapper())))
                    .setControllerAdvice(new CampusMapExceptionHandler(), new GlobalExceptionHandler()).build();
            var file = new MockMultipartFile("file", "map.json", "application/json",
                    json.getBytes(StandardCharsets.UTF_8));
            mvc.perform(multipart("/api/v1/admin/campus-map/" + resource + "/import").file(file))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("C001"));
            verifyNoInteractions(buildings, spaces, smoking, parking, entityManager);
        }
    }
}
