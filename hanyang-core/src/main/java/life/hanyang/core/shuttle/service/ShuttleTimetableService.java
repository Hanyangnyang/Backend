package life.hanyang.core.shuttle.service;

import life.hanyang.core.shuttle.domain.ShuttleDayType;
import life.hanyang.core.shuttle.domain.ShuttlePeriod;
import life.hanyang.core.shuttle.domain.ShuttleRoute;
import life.hanyang.core.shuttle.dto.ShuttleTimetableRequest;
import life.hanyang.core.shuttle.dto.ShuttleTimetableResponse;
import life.hanyang.core.shuttle.domain.ShuttleTimetable;
import life.hanyang.core.shuttle.repository.ShuttleTimetableRepository;
import life.hanyang.core.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShuttleTimetableService {
    private final ShuttleTimetableRepository shuttleTimetableRepository;

    @Transactional
    @CacheEvict(cacheNames = "shuttleTimetable", allEntries = true)
    public void createAllTimetable(List<ShuttleTimetableRequest> requests) {
        //1. 기존의 시간표 데이터를 싹 지우기 (Reset)
        shuttleTimetableRepository.deleteAll();
        List<ShuttleTimetable> entities = requests.stream()
                .map(req -> ShuttleTimetable.builder()
                                .shuttleRoute(ShuttleRoute.valueOf(req.getRoute()))
                                .shuttlePeriod(ShuttlePeriod.valueOf(req.getPeriod()))
                                .shuttleDayType(ShuttleDayType.valueOf(req.getDayType()))
                                .departureTime(LocalTime.parse(req.getDep()))
                                .build()
                )
                .collect(Collectors.toList());
        //2. 일괄 저장하기
        shuttleTimetableRepository.saveAll(entities);
    }

    @Cacheable(cacheNames = "shuttleTimetable", key = "#period.name() + '_' + #dayType.name()")
    public List<ShuttleTimetable> getTimetablesByPeriodAndType(ShuttlePeriod period, ShuttleDayType dayType){
        return shuttleTimetableRepository.findByShuttlePeriodAndShuttleDayType(period, dayType);
    }


    @CacheEvict(cacheNames = "shuttleTimetable", allEntries = true)
    @Transactional
    public void deleteAllTimetable(){
        shuttleTimetableRepository.deleteAll();
    }

    @Cacheable(cacheNames = "shuttleTimetable", key = "'all'")
    public List<ShuttleTimetable> getAllTimetable() {
        return shuttleTimetableRepository.findAll();
    }

    @Transactional
    @CacheEvict(cacheNames = "shuttleTimetable", allEntries = true)
    public void deleteTimetable(List<Long> ids){
        shuttleTimetableRepository.deleteAllByIdInBatch(ids);
    }

    @Transactional
    @CacheEvict(cacheNames = "shuttleTimetable", allEntries = true)
    public ShuttleTimetableResponse createTimetable(ShuttleTimetableRequest request) {
        ShuttleTimetable entity = ShuttleTimetable.builder()
                .shuttleRoute(ShuttleRoute.valueOf(request.getRoute()))
                .shuttlePeriod(ShuttlePeriod.valueOf(request.getPeriod()))
                .shuttleDayType(ShuttleDayType.valueOf(request.getDayType()))
                .departureTime(LocalTime.parse(request.getDep()))
                .build();
        ShuttleTimetable saved = shuttleTimetableRepository.save(entity);
        return ShuttleTimetableResponse.from(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "shuttleTimetable", allEntries = true)
    public ShuttleTimetableResponse updateTimetable(Long id, ShuttleTimetableRequest request) {
        ShuttleTimetable entity = shuttleTimetableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 시간표가 존재하지 않습니다. id: " + id));
        entity.update(
                ShuttleRoute.valueOf(request.getRoute()),
                ShuttlePeriod.valueOf(request.getPeriod()),
                ShuttleDayType.valueOf(request.getDayType()),
                LocalTime.parse(request.getDep())
        );
        return ShuttleTimetableResponse.from(entity);
    }
}