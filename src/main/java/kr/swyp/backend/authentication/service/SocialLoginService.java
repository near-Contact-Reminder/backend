package kr.swyp.backend.authentication.service;

import kr.swyp.backend.authentication.dto.MemberInfo;
import kr.swyp.backend.authentication.dto.SocialLoginDto;
import kr.swyp.backend.member.enums.SocialLoginProviderType;

public interface SocialLoginService {
    MemberInfo login(SocialLoginProviderType providerType, SocialLoginDto.Request request);
}