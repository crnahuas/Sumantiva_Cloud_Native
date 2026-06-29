package cl.duoc.cloudnative.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Bean
    S3Client s3Client(StorageProperties storageProperties) {
        return S3Client.builder()
                .region(Region.of(storageProperties.getS3Region()))
                .build();
    }
}
