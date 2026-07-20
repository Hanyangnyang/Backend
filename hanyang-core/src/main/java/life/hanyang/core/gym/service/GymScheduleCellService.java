package life.hanyang.core.gym.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.gym.domain.GymPeriod;
import life.hanyang.core.gym.domain.GymScheduleCell;
import life.hanyang.core.gym.dto.GymScheduleCellDto;
import life.hanyang.core.gym.dto.GymScheduleCreateRequest;
import life.hanyang.core.gym.dto.GymScheduleUpdateDto; // 👈 추가
import life.hanyang.core.gym.repository.GymPeriodRepository;
import life.hanyang.core.gym.repository.GymScheduleCellRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Map; // 👈 추가
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GymScheduleCellService {
    private final GymScheduleCellRepository gymScheduleCellRepository;
    private final GymPeriodRepository gymPeriodRepository;

    @Transactional
    public void saveSchedules(List<GymScheduleCreateRequest> requests){
        List<GymScheduleCell> allCellsToSave = new java.util.ArrayList<>();
        for (GymScheduleCreateRequest request : requests) {
            GymPeriod gymPeriod = gymPeriodRepository.findById(request.getPeriodId())
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 운영기간 입니다. ID: "+ request.getPeriodId()));
            List<GymScheduleCellDto> newSchedules = request.getSchedules();
            validateOperatingHours(gymPeriod, newSchedules);
            validateIncomingSchedules(newSchedules);
            validateAgainstExistingSchedules(gymPeriod.getId(), newSchedules);
            List<GymScheduleCell> cells = request.getSchedules().stream()
                    .map(dto -> new GymScheduleCell(
                            gymPeriod,
                            dto.getDayOfWeek(),
                            dto.getStartTime(),
                            dto.getEndTime(),
                            dto.getClassId(),
                            dto.getClassName()
                    ))
                    .toList();
            
            // 1. 자바 List 바구니에 담을 때는 addAll()을 사용합니다.
            allCellsToSave.addAll(cells);
        }
        // 2. JPA 레포지토리를 통해 DB에 저장할 때는 saveAll()을 사용합니다.
        gymScheduleCellRepository.saveAll(allCellsToSave);
    }

    @Transactional
    public void deleteSchedules(List<Long> scheduleCellIds){
        long count = gymScheduleCellRepository.countByIdIn(scheduleCellIds);
        // TODO: 삭제 API의 멱등성 보장을 위해 count 조회 검증 로직 제거 검토
        if (count != scheduleCellIds.size()) {
            throw new EntityNotFoundException("존재하지 않는 시간표 ID가 포함되어 있습니다.");
        }
        gymScheduleCellRepository.deleteAllByIdInBatch(scheduleCellIds);
    }

    private boolean isOverlapping(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    private void validateOperatingHours(GymPeriod gymPeriod, List<GymScheduleCellDto> newSchedules) {
        LocalTime periodStart = gymPeriod.getStartTime();
        LocalTime periodEnd = gymPeriod.getEndTime();

        for (GymScheduleCellDto cell : newSchedules) {
            if (cell.getStartTime().isBefore(periodStart) || cell.getEndTime().isAfter(periodEnd)){
                throw new BusinessException(String.format(
                        "수업 시간은 체육관 운영 시간 내에 있어야 합니다. (체육관 운영 시간: %s ~ %s, 요청된 수업 시간: %s ~ %s)",
                        periodStart, periodEnd, cell.getStartTime(), cell.getEndTime())
                        ,ErrorCode.INVALID_INPUT_VALUE);
            }
        }
    }
    private void validateIncomingSchedules(List<GymScheduleCellDto> newSchedules) {
        for (int i = 0; i < newSchedules.size(); i++) {
            GymScheduleCellDto c1 = newSchedules.get(i);

            if (!c1.getStartTime().isBefore(c1.getEndTime())){
                throw new BusinessException(String.format("시작 시간은 종료 시간보다 빨라야 합니다. (%s %s ~ %s)", c1.getDayOfWeek(), c1.getStartTime(), c1.getEndTime()), ErrorCode.INVALID_INPUT_VALUE);
            }

            for (int j = i + 1; j < newSchedules.size(); j++) {
                GymScheduleCellDto c2 = newSchedules.get(j);

                if (c1.getDayOfWeek() == c2.getDayOfWeek() &&
                isOverlapping(c1.getStartTime(), c1.getEndTime(), c2.getStartTime(), c2.getEndTime())) {
                    throw new BusinessException(String.format(
                             "요청된 시간표 리스트 내에서 시간이 중복됩니다. (요일: %s, 시간대: %s ~ %s / %s ~ %s)",
                            c1.getDayOfWeek(), c1.getStartTime(), c1.getEndTime(), c2.getStartTime(), c2.getEndTime())
                            ,ErrorCode.INVALID_INPUT_VALUE);
                }
            }
        }
    }

    private void validateAgainstExistingSchedules(Long periodId, List<GymScheduleCellDto> newSchedules) {
        List<GymScheduleCell> existingCells = gymScheduleCellRepository.findByGymPeriodId(periodId);

        for (GymScheduleCellDto newCell : newSchedules) {
            for (GymScheduleCell existingCell : existingCells) {
                if (newCell.getDayOfWeek() == existingCell.getDayOfWeek() &&
                        isOverlapping(newCell.getStartTime(), newCell.getEndTime(), existingCell.getStartTime(), existingCell.getEndTime())) {
                    throw new BusinessException(String.format(
                            "이미 DB에 등록된 시간표와 시간대가 중복됩니다. (요일: %s, 중복 시간대: %s ~ %s, 기존 수업명: %s)",
                            newCell.getDayOfWeek(), newCell.getStartTime(), newCell.getEndTime(), existingCell.getClassName())
                            , ErrorCode.DUPLICATE_RESOURCE);
                }
            }
        }
    }

    @Transactional
    public void updateSchedule(Long cellId, GymScheduleCellDto dto) {
        GymScheduleCell cell = gymScheduleCellRepository.findById(cellId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 시간표 셀입니다. ID: " + cellId));

        validateOperatingHours(cell.getGymPeriod(), List.of(dto));
        validateIncomingSchedules(List.of(dto));

        // 나 자신을 제외한 다른 기존 시간표와 시간 중복 검사
        List<GymScheduleCell> existingCells = gymScheduleCellRepository.findByGymPeriodId(cell.getGymPeriod().getId());
        for (GymScheduleCell existingCell : existingCells) {
            if (existingCell.getId().equals(cellId)) {
                continue;
            }
            if (dto.getDayOfWeek() == existingCell.getDayOfWeek() &&
                    isOverlapping(dto.getStartTime(), dto.getEndTime(), existingCell.getStartTime(), existingCell.getEndTime())) {
                throw new BusinessException(String.format(
                        "이미 DB에 등록된 시간표와 시간대가 중복됩니다. (요일: %s, 중복 시간대: %s ~ %s, 기존 수업명: %s)",
                        dto.getDayOfWeek(), dto.getStartTime(), dto.getEndTime(), existingCell.getClassName()), ErrorCode.DUPLICATE_RESOURCE);
            }
        }

        cell.updateSchedule(
                dto.getDayOfWeek(),
                dto.getStartTime(),
                dto.getEndTime(),
                dto.getClassId(),
                dto.getClassName()
        );
    }

    @Transactional
    public void updateSchedules(List<GymScheduleUpdateDto> requests) {
        if (requests.isEmpty()) return;

        // 1. 수정할 ID 리스트 일괄 조회 (N+1 방지)
        List<Long> cellIds = requests.stream().map(GymScheduleUpdateDto::id).toList();
        List<GymScheduleCell> cellsToUpdate = gymScheduleCellRepository.findAllById(cellIds);
        if (cellsToUpdate.size() != requests.size()) {
            throw new EntityNotFoundException("존재하지 않는 시간표 ID가 포함되어 있습니다.");
        }

        // 2. 수정을 적용할 엔티티 Map 캐싱
        Map<Long, GymScheduleCell> cellMap = cellsToUpdate.stream()
                .collect(Collectors.toMap(GymScheduleCell::getId, cell -> cell));

        // 3. 수정 요청 목록 내의 시간 정합성 및 상호 중복 검사
        for (int i = 0; i < requests.size(); i++) {
            GymScheduleUpdateDto c1 = requests.get(i);
            if (!c1.startTime().isBefore(c1.endTime())) {
                throw new BusinessException(String.format("시작 시간은 종료 시간보다 빨라야 합니다. (%s %s ~ %s)", c1.dayOfWeek(), c1.startTime(), c1.endTime()), ErrorCode.INVALID_INPUT_VALUE);
            }
            for (int j = i + 1; j < requests.size(); j++) {
                GymScheduleUpdateDto c2 = requests.get(j);
                if (c1.dayOfWeek() == c2.dayOfWeek() &&
                        isOverlapping(c1.startTime(), c1.endTime(), c2.startTime(), c2.endTime())) {
                    throw new BusinessException(String.format(
                            "요청된 수정 시간표 리스트 내에서 시간이 중복됩니다. (요일: %s, 시간대: %s ~ %s / %s ~ %s)",
                            c1.dayOfWeek(), c1.startTime(), c1.endTime(), c2.startTime(), c2.endTime()), ErrorCode.INVALID_INPUT_VALUE);
                }
            }
        }

        // 4. DB의 기존 시간표 중 '수정 대상을 제외한' 나머지 데이터들과 겹치지 않는지 검증
        GymPeriod gymPeriod = cellsToUpdate.get(0).getGymPeriod();
        List<GymScheduleCell> existingCells = gymScheduleCellRepository.findByGymPeriodId(gymPeriod.getId());
        List<GymScheduleCell> restExistingCells = existingCells.stream()
                .filter(existing -> !cellIds.contains(existing.getId()))
                .toList();

        for (GymScheduleUpdateDto req : requests) {
            // 운영 시간 검증
            LocalTime periodStart = gymPeriod.getStartTime();
            LocalTime periodEnd = gymPeriod.getEndTime();
            if (req.startTime().isBefore(periodStart) || req.endTime().isAfter(periodEnd)) {
                throw new BusinessException(String.format(
                        "수업 시간은 체육관 운영 시간 내에 있어야 합니다. (체육관 운영 시간: %s ~ %s, 요청된 수업 시간: %s ~ %s)",
                        periodStart, periodEnd, req.startTime(), req.endTime()), ErrorCode.INVALID_INPUT_VALUE);
            }

            // 다른 등록 데이터와의 중복 검사
            for (GymScheduleCell existing : restExistingCells) {
                if (req.dayOfWeek() == existing.getDayOfWeek() &&
                        isOverlapping(req.startTime(), req.endTime(), existing.getStartTime(), existing.getEndTime())) {
                    throw new BusinessException(String.format(
                            "이미 DB에 등록된 시간표와 시간대가 중복됩니다. (요일: %s, 중복 시간대: %s ~ %s, 기존 수업명: %s)",
                            req.dayOfWeek(), req.startTime(), req.endTime(), existing.getClassName()), ErrorCode.DUPLICATE_RESOURCE);
                }
            }

            // 5. 변경 감지(Dirty Checking) 적용
            GymScheduleCell cell = cellMap.get(req.id());
            cell.updateSchedule(
                    req.dayOfWeek(),
                    req.startTime(),
                    req.endTime(),
                    req.classId(),
                    req.className()
            );
        }
    }
}
