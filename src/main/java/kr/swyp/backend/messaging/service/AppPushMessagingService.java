package kr.swyp.backend.messaging.service;

import java.util.UUID;
import kr.swyp.backend.messaging.dto.TokenDto.RegisterAppPushTokenRequest;
import kr.swyp.backend.messaging.dto.TokenDto.UnregisterAppPushTokenRequest;

public interface AppPushMessagingService {

    void registerDevice(UUID memberId, RegisterAppPushTokenRequest request);

    void unregisterDevice(UUID memberId, UnregisterAppPushTokenRequest request);
}
