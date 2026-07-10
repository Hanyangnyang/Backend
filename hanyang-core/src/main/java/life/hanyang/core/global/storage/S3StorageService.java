package life.hanyang.core.global.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Primary
@Service
public class S3StorageService implements StorageService{

    private final S3Client s3Client;
    private final String endpoint;

    public S3StorageService(S3Client s3Client,
                            @Value("${aws.s3.endpoint}") String endpoint) {
        this.s3Client = s3Client;
        this.endpoint = endpoint;
    }
    @Override
    public String uploadFile(MultipartFile file, String bucket) {
        try {
            // 1. 오리지널 파일명에서 안전하게 확장자만 추출
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            // 2. 한글/공백으로 인한 S3 Key 에러를 완벽 차단하기 위해 UUID + 확장자로 파일명 정의
            String fileName = UUID.randomUUID().toString() + extension;

            // 2. S3 업로드 스펙 정의
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .cacheControl("public, max-age=2592000")
                    .build();

            // 3. 파일 바이너리 스트림 전송
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // 4. 외부에서 이미지를 브라우저에 띄울 수 있는 Public CDN 주소로 변환하여 리턴
            // (e.g. S3 주소인 /s3 를 Public 접근 주소인 /object/public 으로 변환)
            String baseUrl = endpoint.replace("/s3", "/object/public");
            return String.format("%s/%s/%s", baseUrl, bucket, fileName);

        } catch (IOException e) {
            throw new RuntimeException("S3 파일 업로드 중 입출력 에러가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
