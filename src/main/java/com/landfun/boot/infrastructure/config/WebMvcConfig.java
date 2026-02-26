package com.landfun.boot.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.landfun.boot.infrastructure.web.AuthInterceptor;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.time.ZoneId;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/health",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/webjars/**");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, LocalDateTime>() {
            @Override
            public LocalDateTime convert(String source) {
                if (source == null || source.trim().isEmpty()) {
                    return null;
                }
                try {
                    if (source.endsWith("Z") || source.contains("+")
                            || (source.contains("-") && source.lastIndexOf('-') > 10)) {
                        return ZonedDateTime.parse(source).withZoneSameInstant(ZoneId.systemDefault())
                                .toLocalDateTime();
                    }
                    return LocalDateTime.parse(source, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e) {
                    // fallback to standard parsing
                    return LocalDateTime.parse(source);
                }
            }
        });
    }
}
