package life.hanyang.core.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /**
     * 파일을 스토리지에 업로드하고, 접근 가능한 Public URL을 반환합니다.
     * @param file 업로드할 파일
     * @param bucket 스토리지 폴더/버킷명 (예: "banners")
     */
    String uploadFile(MultipartFile file, String bucket);
}