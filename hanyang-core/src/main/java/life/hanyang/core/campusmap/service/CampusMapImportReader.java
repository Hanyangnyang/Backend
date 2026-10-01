package life.hanyang.core.campusmap.service;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Component
public class CampusMapImportReader {
    private final ObjectMapper mapper;

    public CampusMapImportReader(ObjectMapper mapper) {
        this.mapper = mapper.copy()
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature())
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    }

    public <T> List<T> read(MultipartFile file, Class<T> type) {
        if (file.isEmpty()) throw new IllegalArgumentException("JSON 파일이 비어 있습니다.");
        if (file.getSize() > 5 * 1024 * 1024) throw new BusinessException(ErrorCode.FILE_SIZE_LIMIT_EXCEEDED);
        try (var input = file.getInputStream()) {
            return mapper.readValue(input, mapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new BusinessException("올바른 JSON 배열 파일을 업로드해주세요.", ErrorCode.INVALID_INPUT_VALUE, e);
        }
    }
}
