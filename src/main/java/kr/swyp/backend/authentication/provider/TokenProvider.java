package kr.swyp.backend.authentication.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import kr.swyp.backend.authentication.dto.TokenDto.JwtDto;
import kr.swyp.backend.authentication.dto.TokenDto.JwtDto.RefreshTokenInfo;
import kr.swyp.backend.authentication.dto.TokenDto.RefreshTokenInfoResponse;
import kr.swyp.backend.authentication.service.RefreshTokenService;
import kr.swyp.backend.member.dto.MemberDetails;
import kr.swyp.backend.member.enums.RoleType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class TokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityInMilliSeconds;
    private final ObjectMapper objectMapper;
    private final RefreshTokenService refreshTokenService;

    public TokenProvider(@Value("${swyp.jwt.secret}") String secretKey,
            @Value("${swyp.jwt.access-token-validity-in-milli-seconds}")
            long accessTokenValidityInMilliSeconds,
            ObjectMapper objectMapper,
            RefreshTokenService refreshTokenService) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidityInMilliSeconds = accessTokenValidityInMilliSeconds;
        this.objectMapper = objectMapper;
        this.refreshTokenService = refreshTokenService;
    }

    public void generateToken(HttpServletResponse response, Authentication authentication)
            throws IOException {
        String accessToken = this.generateAccessToken(authentication);
        RefreshTokenInfoResponse refreshToken = generateRefreshToken(authentication);

        JwtDto jwtDto = JwtDto.builder()
                .accessToken(accessToken)
                .refreshTokenInfo(RefreshTokenInfo.builder()
                        .token(refreshToken.getRefreshToken())
                        .expiresAt(refreshToken.getExpiresAt()).build())
                .build();

        String result = objectMapper.writeValueAsString(jwtDto);
        response.getWriter().write(result);
    }

    public Authentication getAuthentication(String token) {
        // 토큰 복호화
        Claims claims = this.parseClaims(token);
        if (!StringUtils.hasText(claims.get("auth").toString())) {
            throw new IllegalArgumentException("권한 정보가 없는 토큰입니다.");
        }
        // 클레임에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(
                        claims.get("auth").toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .toList();

        // UserDetails 객체를 만들어서 Authentication 반환
        UserDetails principal = new MemberDetails(UUID.fromString((String) claims.get("user")),
                claims.getSubject(),
                "",
                authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("[토큰 검증 중 에러] 유효하지 않은 JWT입니다. 애러 메시지: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.info("[토큰 검증 중 에러] 만료된 JWT입니다. 애러 메시지: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.info("[토큰 검증 중 에러] 지원하지 않는 JWT입니다. 애러 메시지: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.info("[토큰 검증 중 에러] JWT claims 문자열이 빈 값 입니다. 애러 메시지: {}", e.getMessage());
        } catch (SignatureException e) {
            log.info("[토큰 검증 중 에러] JWT 서명이 유효하지 않습니다. 애러 메시지: {}", e.getMessage());
        }
        return false;
    }

    public String generateAccessToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        MemberDetails principal = (MemberDetails) authentication.getPrincipal();

        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + this.accessTokenValidityInMilliSeconds);

        return Jwts.builder()
                .subject(authentication.getName())
                .claim("auth", authorities)
                .claim("user", principal.getMemberId())
                .expiration(accessTokenExpiresIn)
                .signWith(secretKey)
                .compact();
    }

    private RefreshTokenInfoResponse generateRefreshToken(Authentication authentication) {
        MemberDetails principal = (MemberDetails) authentication.getPrincipal();

        // authorities에서 첫 번째 권한만 사용 (또는 가장 높은 권한을 사용하는 로직으로 변경 가능)
        String authority = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("권한이 없습니다."));

        return refreshTokenService.renew(principal.getMemberId(), RoleType.fromKey(authority));
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}