package kr.swyp.backend.authentication.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.swyp.backend.authentication.dto.MemberInfo;
import kr.swyp.backend.authentication.dto.SocialLoginDto;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.domain.MemberSocialLoginInfo;
import kr.swyp.backend.member.domain.Role;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.enums.SocialLoginProviderType;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.member.repository.MemberSocialLoginInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialLoginServiceImpl implements SocialLoginService {

    private final Map<SocialLoginProviderType, SocialLoginStrategy> strategies;
    private final MemberSocialLoginInfoRepository memberSocialLoginInfoRepository;
    private final MemberRepository memberRepository;

    public SocialLoginServiceImpl(List<SocialLoginStrategy> strategyList,
            MemberSocialLoginInfoRepository memberSocialLoginInfoRepository,
            MemberRepository memberRepository) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(SocialLoginStrategy::getProviderType,
                        Function.identity()));
        this.memberSocialLoginInfoRepository = memberSocialLoginInfoRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public MemberInfo login(SocialLoginProviderType providerType, SocialLoginDto.Request request) {
        SocialLoginStrategy strategy = strategies.get(providerType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported provider type: " + providerType);
        }

        SocialLoginDto.UserInfo userInfo = strategy.getUserInfo(request);
        Member member = findOrCreateMember(userInfo, providerType);

        return MemberInfo.builder()
                .memberId(member.getMemberId())
                .username(member.getUsername())
                .roleType(RoleType.USER)
                .build();
    }

    private Member findOrCreateMember(SocialLoginDto.UserInfo userInfo,
            SocialLoginProviderType providerType) {
        Optional<MemberSocialLoginInfo> maybeMemberSocialLoginInfo =
                memberSocialLoginInfoRepository.findByProviderIdAndProviderType(
                        userInfo.getProviderId(), providerType);

        if (maybeMemberSocialLoginInfo.isPresent()) {
            Member existingMember = maybeMemberSocialLoginInfo.get().getMember();
            if (existingMember.getWithdrawnAt() != null) {
                existingMember.reactivate();
                memberRepository.save(existingMember);
            }
            return existingMember;
        }

        // 이메일로 기존 회원 검색 (탈퇴한 회원 포함)
        Optional<Member> maybeMemberByEmail = memberRepository.findByUsername(userInfo.getEmail());
        if (maybeMemberByEmail.isPresent()) {
            Member existingMember = maybeMemberByEmail.get();
            if (existingMember.getWithdrawnAt() != null) {
                // 탈퇴한 회원 재활성화
                existingMember.reactivate();
                existingMember.addRole(RoleType.USER);
                memberRepository.save(existingMember);

                // 새로운 소셜 로그인 정보 연결
                MemberSocialLoginInfo newSocialLoginInfo = MemberSocialLoginInfo.builder()
                        .providerId(userInfo.getProviderId())
                        .providerType(providerType)
                        .member(existingMember)
                        .build();
                memberSocialLoginInfoRepository.save(newSocialLoginInfo);

                return existingMember;
            } else {
                // 이미 활성화된 회원이 다른 소셜 로그인으로 시도하는 경우
                throw new IllegalStateException("이미 가입된 이메일입니다: " + userInfo.getEmail());
            }
        }

        Member newMember = Member.builder()
                .username(userInfo.getEmail())
                .nickname(userInfo.getNickname())
                .password(" ") // 소셜 로그인이므로 비밀번호는 불필요
                .isActive(true)
                .build();

        Role role = Role.builder()
                .member(newMember)
                .roleType(RoleType.USER)
                .build();
        newMember.getRoles().add(role);

        MemberSocialLoginInfo newSocialLoginInfo = MemberSocialLoginInfo.builder()
                .providerId(userInfo.getProviderId())
                .providerType(providerType)
                .member(newMember)
                .build();

        memberRepository.save(newMember);
        memberSocialLoginInfoRepository.save(newSocialLoginInfo);

        return newMember;
    }
}