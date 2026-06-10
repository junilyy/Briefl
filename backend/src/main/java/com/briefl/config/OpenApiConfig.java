package com.briefl.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
	info = @Info(
		title = "BRIEFL API",
		version = "v1",
		description = "AI 기반 관심 주식 뉴스 브리프 서비스 API 문서",
		license = @License(name = "Private")
	)
)
public class OpenApiConfig {
}
