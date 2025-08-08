package kr.swyp.backend.authentication.provider;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import kr.swyp.backend.authentication.dto.AppleSocialLoginAuthenticationToken;
import kr.swyp.backend.authentication.dto.MemberInfo;
import kr.swyp.backend.authentication.dto.SocialLoginDto.Request;
import kr.swyp.backend.authentication.service.SocialLoginService;
import kr.swyp.backend.member.dto.MemberDetails;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.enums.SocialLoginProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@RequiredArgsConstructor
public class AppleSocialLoginAuthenticationProvider implements AuthenticationProvider {

    private final SocialLoginService socialLoginService;

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        Map<String, Object> socialLoginInfoMap =
                (Map<String, Object>) authentication.getPrincipal();
        try {
            Request request = Request.builder()
                    .identityToken((String) socialLoginInfoMap.get("identityToken"))
                    .authorizationCode((String) socialLoginInfoMap.get("authorizationCode"))
                    .build();
            MemberInfo memberInfo = socialLoginService.login(
                    (SocialLoginProviderType) socialLoginInfoMap.get("providerType"), request);

            MemberDetails memberDetails = new MemberDetails(memberInfo.getMemberId(),
                    memberInfo.getUsername(), "", getAuthorities(memberInfo.getRoleType()));

            return new UsernamePasswordAuthenticationToken(memberDetails, "",
                    memberDetails.getAuthorities());
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException(e.getMessage());
        }
    }

    private Collection<GrantedAuthority> getAuthorities(RoleType roleType) {
        return Collections.singletonList(new SimpleGrantedAuthority(roleType.name()));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return AppleSocialLoginAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
