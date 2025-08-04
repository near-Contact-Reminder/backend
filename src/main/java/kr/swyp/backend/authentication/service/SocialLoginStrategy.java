package kr.swyp.backend.authentication.service;

import static kr.swyp.backend.authentication.dto.SocialLoginDto.Request;

import kr.swyp.backend.authentication.dto.SocialLoginDto.UserInfo;
import kr.swyp.backend.member.enums.SocialLoginProviderType;

public interface SocialLoginStrategy {

    UserInfo getUserInfo(Request request);

    SocialLoginProviderType getProviderType();
}
