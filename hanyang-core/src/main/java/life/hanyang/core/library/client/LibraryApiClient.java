package life.hanyang.core.library.client;

import life.hanyang.core.library.dto.PyxisSeatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LibraryApiClient {
    private final RestClient restClient;

    public LibraryApiClient(@Value("${api.library.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public PyxisSeatResponse fetchSeatStatus() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/pyxis-api/2/seat-rooms")
                    .queryParam("smufMethodCode", "PC")
                    .queryParam("roomTypeId", 7)
                    .queryParam("branchGroupId", 2)
                    .build())
                .retrieve()
                .body(PyxisSeatResponse.class);
    }
}
