package life.hanyang.core.library.dto;

import java.util.List;

public record PyxisSeatResponse(
        boolean success,
        String code,
        String message,
        PyxisSeatData data
) {
    public record PyxisSeatData(
            int totalCount,
            List<PyxisSeatItem> list
    ) {}

    public record PyxisSeatItem(
            int id,
            String name,
            int floor,
            PyxisSeatsInfo seats
    ) {}

    public record PyxisSeatsInfo(
            int total,
            int occupied,
            int waiting,
            int available,
            int unavailable
    ) {}
}