package life.hanyang.core.global.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class MockStorageService implements StorageService {
    @Override
    public String uploadFile(MultipartFile file, String bucket) {
        String randomFileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        return "https://hanyang-life-storage.supabase.co/storage/v1/object/public/" + bucket + "/" + randomFileName;
    }
}
