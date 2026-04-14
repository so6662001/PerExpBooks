package com.qiankubx.common.config;

import com.qiankubx.common.interceptor.AgreementInterceptor;
import com.qiankubx.common.interceptor.AuthInterceptor;
import com.qiankubx.common.interceptor.MemberInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AgreementInterceptor agreementInterceptor;
    private final MemberInterceptor memberInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",
                        "/v1/auth/**",
                        "/agreement/**",
                        "/v1/agreement/**",
                        "/api/v1/member/wx-notify",
                        "/api/v1/member/ali-notify",
                        "/api/v1/member/plans",
                        "/api/v1/analytics/report",
                        "/api/v1/analytics/performance",
                        "/api/v1/analytics/error",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/error"
                )
                .order(1);

        registry.addInterceptor(agreementInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",
                        "/v1/auth/**",
                        "/agreement/**",
                        "/v1/agreement/**",
                        "/user/agreement/**",
                        "/v1/user/agreement/**",
                        "/api/v1/analytics/report",
                        "/api/v1/analytics/performance",
                        "/api/v1/analytics/error",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/error"
                )
                .order(2);

        registry.addInterceptor(memberInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",
                        "/v1/auth/**",
                        "/agreement/**",
                        "/v1/agreement/**",
                        "/user/**",
                        "/v1/user/**",
                        "/api/v1/member/**",
                        "/api/v1/coupon/**",
                        "/api/v1/team/**",
                        "/api/v1/promotion/**",
                        "/api/v1/withdrawal/**",
                        "/api/v1/analytics/report",
                        "/api/v1/analytics/performance",
                        "/api/v1/analytics/error",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/error"
                )
                .order(3);
    }
}
