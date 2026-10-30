package life.hanyang.core.partnership.service;


import life.hanyang.core.partnership.domain.Merchant;
import life.hanyang.core.partnership.domain.MerchantCategory;
import life.hanyang.core.partnership.dto.MerchantRequest;
import life.hanyang.core.partnership.dto.MerchantResponse;
import life.hanyang.core.partnership.repository.MerchantRepository;
import life.hanyang.core.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Validator;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

@Transactional(readOnly = true)
public class MerchantService {
    private final MerchantRepository merchantRepository;
    private final Validator validator;


    @Cacheable(cacheNames = "merchant", key = "'all:representative-menus:v1'")
    public List<MerchantResponse> getAllMerchants() {
        return merchantRepository.findAll().stream()
                .map(MerchantResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(cacheNames = {"merchant", "partnership"}, allEntries = true)
    public void createMerchants(List<MerchantRequest> requests){
        if (requests == null) throw new IllegalArgumentException("업체 목록이 필요합니다.");
        requests.forEach(this::validate);
        List<Merchant> entities = requests.stream()
                .map(MerchantRequest::toEntity)
                .toList();
        merchantRepository.saveAll(entities);
    }

    @Transactional
    @CacheEvict(cacheNames = {"merchant", "partnership"}, allEntries = true)
    public MerchantResponse updateMerchant(Long id, MerchantRequest request) {
        validate(request);
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 가맹점이 존재하지 않습니다. id: " + id));
        merchant.update(
                request.storeName(),
                request.category(),
                request.isActive(),
                request.emoji(),
                request.latitude(),
                request.longitude(),
                request.fullAddress(),
                request.kakaoPlaceId()
        );
        merchant.updateRepresentativeMenus(request.representativeMenus());
        return MerchantResponse.from(merchant);
    }

    @Transactional
    @CacheEvict(cacheNames = {"merchant", "partnership"}, allEntries = true)
    public void deleteMerchants(List<Long> merchantIds){
        if (merchantIds == null || merchantIds.isEmpty()) {
            return;
        }
        merchantRepository.deleteAllById(merchantIds);
    }
    private void validate(Object request) {
        if (request == null) throw new IllegalArgumentException("업체 항목은 null일 수 없습니다.");
        var errors = validator.validate(request);
        if (!errors.isEmpty()) throw new IllegalArgumentException(errors.stream()
                .map(e -> e.getPropertyPath() + ": " + e.getMessage()).sorted()
                .collect(java.util.stream.Collectors.joining(", ")));
    }

}
