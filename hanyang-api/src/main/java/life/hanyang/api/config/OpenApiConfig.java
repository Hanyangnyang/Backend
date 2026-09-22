package life.hanyang.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public GroupedOpenApi userOpenApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .packagesToScan("life.hanyang.user")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info()
                        .title("하냥냥 사용자 API 명세서")
                        .description("하냥냥 사용자 API 명세서입니다.")
                        .version("1.0.0")))
                .build();
    }

    @Bean
    public GroupedOpenApi adminOpenApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .packagesToScan("life.hanyang.admin")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(new Info()
                                .title("하냥냥 어드민 API 명세서")
                                .description("하냥냥 어드민 API 명세서입니다.")
                                .version("1.0.0"))
                        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                        .components(openApi.getComponents() == null
                                ? new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                                : openApi.getComponents().addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))))
                .build();
    }
}
