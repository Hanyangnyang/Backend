package life.hanyang.core.partnership.service;


import life.hanyang.core.partnership.domain.Merchant;
import life.hanyang.core.partnership.domain.MerchantCategory;
import life.hanyang.core.partnership.dto.MerchantRequest;
import life.hanyang.core.partnership.dto.MerchantResponse;
import life.hanyang.core.partnership.repository.MerchantRepository;
import life.hanyang.core.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

@Transactional(readOnly = true)
public class MerchantService {
    private final MerchantRepository merchantRepository;


    public List<MerchantResponse> getAllMerchants() {
        return merchantRepository.findAll().stream()
                .map(MerchantResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createMerchants(List<MerchantRequest> requests){
        List<Merchant> entities = requests.stream()
                .map(MerchantRequest::toEntity)
                .toList();
        merchantRepository.saveAll(entities);
    }

    @Transactional
    public MerchantResponse updateMerchant(Long id, MerchantRequest request) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 가맹점이 존재하지 않습니다. id: " + id));
        merchant.update(
                request.storeName(),
                MerchantCategory.valueOf(request.category().toUpperCase()),
                request.isActive(),
                request.emoji(),
                request.latitude(),
                request.longitude(),
                request.fullAddress(),
                request.kakaoPlaceId()
        );
        return MerchantResponse.from(merchant);
    }

    @Transactional
    public void deleteMerchants(List<Long> merchantIds){
        Long count = merchantRepository.countByMerchantIdIn(merchantIds);
        if (count != merchantIds.size()) {
            throw new EntityNotFoundException("존재하지 않는 업체의 ID가 포함되어 있습니다.");
        }
        // 데이터 양이 얼마 되지 않아 deleteAllById 방식으로 편리하게 제거함
        merchantRepository.deleteAllById(merchantIds);
    }
}
