package com.book.aiwebgenerator.manager;

import com.book.aiwebgenerator.config.R2ClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class R2ManagerTest {

    @Mock
    private R2ClientConfig r2ClientConfig;

    @Mock
    private S3Client s3Client;

    @TempDir
    private Path tempDir;

    private R2Manager r2Manager;

    @BeforeEach
    void setUp() {
        r2Manager = new R2Manager();
        ReflectionTestUtils.setField(r2Manager, "r2ClientConfig", r2ClientConfig);
        ReflectionTestUtils.setField(r2Manager, "s3Client", s3Client);
        ReflectionTestUtils.setField(r2Manager, "publicBaseUrl", "https://public.example.com/");
        ReflectionTestUtils.setField(r2Manager, "objectPrefix", "/ai-web-generator/");
    }

    @Test
    void uploadFileUsesNormalizedKeyAndReturnsPublicUrl() throws IOException {
        when(r2ClientConfig.getBucket()).thenReturn("test-bucket");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag").build());
        File diagramFile = Files.writeString(tempDir.resolve("diagram.svg"), "<svg/>").toFile();

        String url = r2Manager.uploadFile("/mermaid/abc/diagram.svg", diagramFile);

        assertEquals("https://public.example.com/ai-web-generator/mermaid/abc/diagram.svg", url);
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertEquals("test-bucket", requestCaptor.getValue().bucket());
        assertEquals("ai-web-generator/mermaid/abc/diagram.svg", requestCaptor.getValue().key());
        assertEquals("image/svg+xml", requestCaptor.getValue().contentType());
    }
}
