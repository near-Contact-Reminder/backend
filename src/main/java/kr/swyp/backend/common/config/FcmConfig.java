package kr.swyp.backend.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FcmConfig {

    private final FcmProperties fcmProperties;
    private final Environment environment;

    @PostConstruct
    public void initialize() {
        if (!fcmProperties.isEnabled()) {
            log.info("FCM is disabled. Skipping initialization.");
            return;
        }

        // 테스트 프로파일에서는 초기화 건너뛰기
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("test".equals(profile)) {
                log.info("Test profile detected. Skipping FCM initialization.");
                return;
            }
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                String credentialsPath = fcmProperties.getCredentialsPath();

                // classpath 또는 파일 시스템 경로 지원
                try (InputStream serviceAccount = credentialsPath.startsWith("classpath:")
                        ? new ClassPathResource(
                        credentialsPath.substring("classpath:".length())).getInputStream()
                        : new FileInputStream(credentialsPath)) {

                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();

                    FirebaseApp.initializeApp(options);
                    log.info("Firebase application has been initialized successfully.");
                }
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase application.", e);
            throw new RuntimeException("Failed to initialize FCM", e);
        }
    }
}
