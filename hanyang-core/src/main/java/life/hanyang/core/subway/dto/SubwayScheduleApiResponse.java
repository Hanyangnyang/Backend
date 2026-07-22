package life.hanyang.core.subway.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SubwayScheduleApiResponse {
    private Response response;

    @Getter
    @NoArgsConstructor
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @NoArgsConstructor
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @NoArgsConstructor
    public static class Body {
        private Items items;
        private String pageNo;
        private String numOfRows;
        private int totalCount;
    }

    @Getter
    @NoArgsConstructor
    public static class Items{
        private List<TrainScheduleItem> item;
    }

    @Getter
    @NoArgsConstructor
    public static class TrainScheduleItem{
        private String trainno;
        private String upbdnbSe;
        private String wkndSe;
        private String lineNm;
        private String stnNm;
        private String arvlStnNm;
        private String trainDptreTm;
        private String trainArvlTm;
    }
}
