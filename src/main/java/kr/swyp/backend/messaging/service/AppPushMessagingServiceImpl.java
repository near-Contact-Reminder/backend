package kr.swyp.backend.messaging.service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.messaging.domain.AppPushToken;
import kr.swyp.backend.messaging.dto.TokenDto.RegisterAppPushTokenRequest;
import kr.swyp.backend.messaging.dto.TokenDto.UnregisterAppPushTokenRequest;
import kr.swyp.backend.messaging.repository.AppPushTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppPushMessagingServiceImpl implements AppPushMessagingService {

    private final AppPushTokenRepository appPushTokenRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public void registerDevice(UUID memberId, RegisterAppPushTokenRequest request) {
        Optional<AppPushToken> maybeAppPushToken = appPushTokenRepository.findByMemberId(memberId);

        if (maybeAppPushToken.isEmpty()) {
            AppPushToken appPushToken = AppPushToken.builder()
                    .memberId(memberId)
                    .token(request.getToken())
                    .osType(request.getOsType())
                    .build();
            appPushTokenRepository.save(appPushToken);

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new NoSuchElementException("해당 회원을 찾을 수 없습니다."));
            member.updateNotificationAgreedAt(LocalDateTime.now());
            memberRepository.save(member);
        } else {
            // 항목이 존재하면 토큰을 업데이트한다.
            AppPushToken appPushToken = maybeAppPushToken.get();
            appPushToken.updateToken(request.getToken(), request.getOsType());
            appPushTokenRepository.save(appPushToken);
        }
    }

    @Override
    @Transactional
    public void unregisterDevice(UUID memberId, UnregisterAppPushTokenRequest request) {
        appPushTokenRepository.deleteByMemberIdAndToken(memberId, request.getToken());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("해당 회원을 찾을 수 없습니다."));

        member.updateNotificationAgreedAt(null);
    }
}
