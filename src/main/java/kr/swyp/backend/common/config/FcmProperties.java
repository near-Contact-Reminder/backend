package kr.swyp.backend.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "swyp.fcm")
public class FcmProperties {

    private String credentialsPath;
    private boolean enabled = true;
}
