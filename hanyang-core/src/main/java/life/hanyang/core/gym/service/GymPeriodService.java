package life.hanyang.core.gym.service;

import life.hanyang.core.gym.domain.GymPeriod;
import life.hanyang.core.gym.domain.GymPeriodType;
import life.hanyang.core.gym.domain.GymScheduleCell;
import life.hanyang.core.gym.domain.GymSemesterNo;
import life.hanyang.core.gym.dto.GymPeriodRequest;
import life.hanyang.core.gym.dto.GymPeriodResponse;
import life.hanyang.core.gym.repository.GymPeriodRepository;
import life.hanyang.core.gym.repository.GymScheduleCellRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GymPeriodService {
    private final GymPeriodRepository gymPeriodRepository;
    private final GymScheduleCellRepository gymScheduleCellRepository;

    public List<GymPeriodResponse> getGymPeriods() {
        List<GymPeriod> gymPeriods = gymPeriodRepository.findAll();
        List<GymScheduleCell> cells = gymScheduleCellRepository.findAll();
        
        // 학기 ID별로 시간표 셀들을 그룹핑
        Map<Long, List<GymScheduleCell>> cellsByPeriodId = cells.stream()
                .collect(Collectors.groupingBy(cell -> cell.getGymPeriod().getId()));
                
        return gymPeriods.stream()
                .map(period -> new GymPeriodResponse(
                        period, 
                        cellsByPeriodId.getOrDefault(period.getId(), List.of())
                ))
                .toList();
    }

    @Transactional
    public void savePeriod(
            GymPeriodRequest request) {
        boolean isDuplicate = gymPeriodRepository.existsByYearAndSemesterAndPeriodType(
                    request.year(),
                GymSemesterNo.valueOf(request.semester().toUpperCase()),
                GymPeriodType.valueOf(request.periodType().toUpperCase())
                );

        if (isDuplicate) {
            throw new IllegalArgumentException(String.format(
                    "(중복) 이미 등록된 학기 정보입니다. (연도: %d, 학기: %s, 유형: %s)",
                    request.year(), request.semester(), request.periodType()
            ));
        }
        gymPeriodRepository.save(request.toEntity());
    }

    @Transactional
    public void deletePeriod(Long periodId) {
        GymPeriod gymPeriod = gymPeriodRepository.findById(periodId)
                        .orElseThrow(() -> new IllegalArgumentException("해당 학기 정보가 존재하지 않습니다. ID: " +periodId));
        gymPeriodRepository.delete(gymPeriod);
    }
}