package life.hanyang.core.partnership.service;

import life.hanyang.core.partnership.domain.Merchant;
import life.hanyang.core.partnership.domain.MerchantCategory;
import life.hanyang.core.partnership.domain.Partnership;
import life.hanyang.core.partnership.dto.MerchantCreateWithPartnershipsRequest;
import life.hanyang.core.partnership.dto.PartnershipDetailDto;
import life.hanyang.core.partnership.dto.PartnershipDetailResponse;
import life.hanyang.core.partnership.dto.PartnershipUpdateDto;
import life.hanyang.core.partnership.repository.MerchantRepository;
import life.hanyang.core.partnership.repository.PartnershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnershipService {
    private final MerchantRepository merchantRepository;
    private final PartnershipRepository partnershipRepository;


    //DB 테이블(Merchant, Partnership) 전체 초기화 후 전달받은 데이터 적재
    @Transactional
    public void resetAndLoadPartnerships(List<MerchantCreateWithPartnershipsRequest> requests) {
        // 1. 기존 테이블 데이터를 FK 역순으로 지우기
        partnershipRepository.deleteAllInBatch();
        merchantRepository.deleteAllInBatch();

        // 2. DTO -> Entity 변환 및 관계 맺기
        List<Merchant> merchantsToSave = new ArrayList<>();

        for (MerchantCreateWithPartnershipsRequest request : requests) {
            // 2. 부모 Merchant 빌드
            Merchant merchant = Merchant.builder()
                    .storeName(request.getName())
                    .merchantCategory(MerchantCategory.valueOf(request.getCategory().toUpperCase()))
                    .emoji(request.getEmoji())
                    .isActive(request.getIsActive())
                    .latitude(request.getLocation() != null ? request.getLocation().getLatitude() : null)
                    .longitude(request.getLocation() != null ? request.getLocation().getLongitude() : null)
                    .fullAddress(request.getLocation() != null ? request.getLocation().getFullAddress() : null)
                    .build();

            // 3. 자식 Partnership 빌드 및 양방향 연관관계 맺기
            for (var partDto : request.getPartnerships()) {
                Partnership partnership = Partnership.builder()
                        .department(partDto.getCollegeName())
                        .benefit(partDto.getBenefit())
                        .startDate(partDto.getPeriod().getStartDate())
                        .endDate(partDto.getPeriod().getEndDate())
                        .isActive(partDto.getPeriod().getIsActive())
                        .conditions(partDto.getConditions())
                        .sourceUrl(partDto.getSourceUrl())
                        .build();

                // 연관관계 편의 메서드 호출
                merchant.addPartnership(partnership);
            }
            merchantsToSave.add(merchant);
        }
        merchantRepository.saveAll(merchantsToSave);
    }

    @Transactional(readOnly = true)
    public List<PartnershipDetailResponse> getAvailablePartnerships() {
        List<Merchant> merchants = merchantRepository.findAllWithPartnerships();
        LocalDate today = LocalDate.now();

        return merchants.stream()
                .map(merchant -> {
                    List<PartnershipDetailResponse.PartnershipInfo> activePartnerships = merchant.getPartnerships().stream()
                            .filter(Partnership::getIsActive)
                            .filter(p -> !today.isBefore(p.getStartDate()) && !today.isAfter(p.getEndDate()))
                            .map(PartnershipDetailResponse.PartnershipInfo::new)
                            .toList();

                    return new PartnershipDetailResponse(merchant, activePartnerships);
                })
                .toList();
    }

    public void addPartnership(Long merchantId, PartnershipDetailDto request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("해당 업체가 존재하지 않습니다. id: " + merchantId));
        Partnership partnership = Partnership.builder()
                .department(request.getCollegeName())
                .benefit(request.getBenefit())
                .startDate(request.getPeriod().getStartDate())
                .endDate(request.getPeriod().getEndDate())
                .isActive(request.getPeriod().getIsActive())
                .conditions(request.getConditions())
                .sourceUrl(request.getSourceUrl())
                .build();

        merchant.addPartnership(partnership);
    }

    @Transactional
    public void deletePartnership(Long partnershipId) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                        .orElseThrow(() -> new IllegalArgumentException("해당 제휴 정보가 존재하지 않습니다. id: " + partnershipId));
        partnershipRepository.delete(partnership);
    }

    @Transactional
    public void updatePartnership(Long partnershipId, PartnershipUpdateDto request) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new IllegalArgumentException("해당 제휴 정보가 존재하지 않습니다. id: " + partnershipId));
        
        partnership.update(
                request.getDepartment(),
                request.getBenefit(),
                request.getConditions(),
                request.getSourceUrl(),
                request.getPhotoOrder(),
                request.getStartDate(),
                request.getEndDate(),
                request.getIsActive()
        );

        if (request.getMerchantId() != null && 
            (partnership.getMerchant() == null || !partnership.getMerchant().getMerchantId().equals(request.getMerchantId()))) {
            Merchant newMerchant = merchantRepository.findById(request.getMerchantId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 업체가 존재하지 않습니다. id: " + request.getMerchantId()));
            partnership.changeMerchant(newMerchant);
        }
    }
}