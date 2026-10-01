package life.hanyang.core.campusmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import life.hanyang.core.campusmap.dto.CampusMapDto.*;
import life.hanyang.core.campusmap.service.CampusMapImportReader;
import life.hanyang.core.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class CampusMapImportReaderTest {
    private final ObjectMapper sharedMapper = new ObjectMapper();
    private final CampusMapImportReader reader = new CampusMapImportReader(sharedMapper);

    private MockMultipartFile file(String json) {
        return new MockMultipartFile("file", "buildings.json", "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readsLegacyBuildingAndNestedSpacesWithTrailingCommaWithoutChangingSharedMapper() {
        var result = reader.read(file("""
                [{"id":"building-102","buildingNumber":"102","name":"학생복지관",
                  "englishName":"Student Welfare Building","aliases":["복지관"],"campus":"ANSAN",
                  "coordinates":{"latitude":37.2979595559148,"longitude":126.834367540313},
                  "description":"","primaryColleges":[],
                  "openSpaces":[{"id":"openspace-01","floor":"3층","name":"오픈스페이스","hint":null}],
                  "facilities":["카페"],"imageUrl":[]},]
                """), BuildingRequest.class);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).openSpaces().get(0).floor()).isEqualTo("3층");
        assertThat(result.get(0).coordinates().getLatitude()).isEqualTo(37.2979595559148);
        assertThatThrownBy(() -> sharedMapper.readTree("[1,]")).isInstanceOf(java.io.IOException.class);
    }

    @Test
    void rejectsNonArrayMalformedUnknownFieldsAndMultipleJsonDocuments() {
        for (String json : new String[]{"{}", "[{", "[{}] []", "[{\"unexpected\":1}]", "[{\"campus\":\"INVALID\"}]"}) {
            assertThatThrownBy(() -> reader.read(file(json), BuildingRequest.class)).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void rejectsEmptyAndOversizedFiles() {
        assertThatThrownBy(() -> reader.read(file(""), BuildingRequest.class)).isInstanceOf(IllegalArgumentException.class);
        var large = new MockMultipartFile("file", new byte[5 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> reader.read(large, BuildingRequest.class)).isInstanceOf(BusinessException.class);
    }
}
