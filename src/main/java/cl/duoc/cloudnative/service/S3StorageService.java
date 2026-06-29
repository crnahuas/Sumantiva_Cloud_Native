package cl.duoc.cloudnative.service;

import cl.duoc.cloudnative.config.StorageProperties;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public S3StorageService(S3Client s3Client, StorageProperties storageProperties) {
        this.s3Client = s3Client;
        this.storageProperties = storageProperties;
    }

    public void upload(Path file, String s3Key, String transportista, String fecha) {
        ensureBucketConfigured();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageProperties.getS3Bucket())
                .key(s3Key)
                .contentType("application/pdf")
                .metadata(java.util.Map.of(
                        "transportista", transportista,
                        "fecha", fecha
                ))
                .build();
        s3Client.putObject(request, RequestBody.fromFile(file));
    }

    public byte[] download(String s3Key) {
        ensureBucketConfigured();
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(storageProperties.getS3Bucket())
                .key(s3Key)
                .build();
        ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(request);
        return bytes.asByteArray();
    }

    public void delete(String s3Key) {
        ensureBucketConfigured();
        s3Client.deleteObject(builder -> builder.bucket(storageProperties.getS3Bucket()).key(s3Key));
    }

    private void ensureBucketConfigured() {
        if (storageProperties.getS3Bucket() == null || storageProperties.getS3Bucket().isBlank()) {
            throw new IllegalStateException("Configura APP_STORAGE_S3_BUCKET antes de usar S3");
        }
    }
}
