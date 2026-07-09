package life.hanyang.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("하냥냥 백엔드 API 명세서")
                        .description("하냥냥 백엔드 API 명세서입니다.")
                        .version("1.0.0"));
    }
}
