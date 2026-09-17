package life.hanyang.core.global.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Test
    void deleteFileExtractsObjectKeyFromPublicUrl() {
        S3StorageService storageService = new S3StorageService(
                s3Client,
                "https://project.supabase.co/storage/v1/s3"
        );

        storageService.deleteFile(
                "https://project.supabase.co/storage/v1/object/public/banners/banner-id.jpg",
                "banners"
        );

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("banners");
        assertThat(captor.getValue().key()).isEqualTo("banner-id.jpg");
    }
}
