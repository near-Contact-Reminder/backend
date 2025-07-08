package kr.swyp.backend.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import kr.swyp.backend.authentication.filter.CustomUsernamePasswordAuthenticationFilter;
import kr.swyp.backend.authentication.filter.JwtAuthenticationFilter;
import kr.swyp.backend.authentication.filter.RefreshTokenAuthenticationFilter;
import kr.swyp.backend.authentication.filter.SocialLoginAuthenticationFilter;
import kr.swyp.backend.authentication.handler.CustomAccessDeniedHandler;
import kr.swyp.backend.authentication.handler.CustomAuthenticationEntryPoint;
import kr.swyp.backend.authentication.handler.RefreshTokenAuthenticationFailureHandler;
import kr.swyp.backend.authentication.handler.RefreshTokenAuthenticationSuccessHandler;
import kr.swyp.backend.authentication.handler.SocialLoginAuthenticationFailureHandler;
import kr.swyp.backend.authentication.handler.SocialLoginAuthenticationSuccessHandler;
import kr.swyp.backend.authentication.handler.UsernamePasswordAuthenticationFailureHandler;
import kr.swyp.backend.authentication.handler.UsernamePasswordAuthenticationSuccessHandler;
import kr.swyp.backend.authentication.provider.AppleSocialLoginAuthenticationProvider;
import kr.swyp.backend.authentication.provider.KakaoSocialLoginAuthenticationProvider;
import kr.swyp.backend.authentication.provider.RefreshTokenAuthenticationProvider;
import kr.swyp.backend.authentication.provider.TokenProvider;
import kr.swyp.backend.authentication.provider.UsernamePasswordAuthenticationProvider;
import kr.swyp.backend.authentication.service.RefreshTokenService;
import kr.swyp.backend.authentication.service.SocialLoginService;
import kr.swyp.backend.member.service.MemberDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final TokenProvider tokenProvider;
    private final MemberDetailsService memberDetailsService;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final RefreshTokenService refreshTokenService;
    private final SocialLoginService socialLoginService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .exceptionHandling(configurer -> configurer
                        .accessDeniedHandler(new CustomAccessDeniedHandler())
                        .authenticationEntryPoint(
                                new CustomAuthenticationEntryPoint(handlerExceptionResolver)))

                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(configurer -> configurer
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/error/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())

                .addFilterBefore(usernamePasswordAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter(),
                        CustomUsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(refreshTokenAuthenticationFilter(),
                        JwtAuthenticationFilter.class)
                .addFilterBefore(socialLoginAuthenticationFilter(),
                        RefreshTokenAuthenticationFilter.class);
        return http.build();
    }

    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider);
    }

    public SocialLoginAuthenticationFailureHandler socialLoginAuthenticationFailureHandler() {
        return new SocialLoginAuthenticationFailureHandler(handlerExceptionResolver);
    }

    public SocialLoginAuthenticationSuccessHandler socialLoginAuthenticationSuccessHandler() {
        return new SocialLoginAuthenticationSuccessHandler(tokenProvider);
    }

    @Bean
    public CustomUsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter() {
        var filter = new CustomUsernamePasswordAuthenticationFilter(authenticationManager(),
                objectMapper);
        filter.setAuthenticationSuccessHandler(
                new UsernamePasswordAuthenticationSuccessHandler(tokenProvider));
        filter.setAuthenticationFailureHandler(
                new UsernamePasswordAuthenticationFailureHandler(handlerExceptionResolver));
        return filter;
    }

    public RefreshTokenAuthenticationFilter refreshTokenAuthenticationFilter() {
        var filter = new RefreshTokenAuthenticationFilter(authenticationManager(), objectMapper);
        filter.setAuthenticationSuccessHandler(
                new RefreshTokenAuthenticationSuccessHandler(tokenProvider));
        filter.setAuthenticationFailureHandler(
                new RefreshTokenAuthenticationFailureHandler(handlerExceptionResolver));
        return filter;
    }

    public SocialLoginAuthenticationFilter socialLoginAuthenticationFilter() {
        var filter = new SocialLoginAuthenticationFilter(authenticationManager(), objectMapper);
        filter.setAuthenticationSuccessHandler(socialLoginAuthenticationSuccessHandler());
        filter.setAuthenticationFailureHandler(socialLoginAuthenticationFailureHandler());
        return filter;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        List<AuthenticationProvider> providerList = List.of(
                new UsernamePasswordAuthenticationProvider(memberDetailsService,
                        passwordEncoder()),
                new RefreshTokenAuthenticationProvider(refreshTokenService),
                new KakaoSocialLoginAuthenticationProvider(socialLoginService),
                new AppleSocialLoginAuthenticationProvider(socialLoginService));
        return new ProviderManager(providerList);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ErrorAttributes errorAttributes() {
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest,
                    ErrorAttributeOptions options) {
                Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
                return Map.of(
                        "code", "GENERAL_ERROR",
                        "message", errorAttributes.get("error"));
            }
        };
    }
}
