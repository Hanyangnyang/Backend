package life.hanyang.admin.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "admin.auth")
public class AdminAuthProperties {

    private Jwt jwt;
    private List<Account> accounts;

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs;
    }

    @Getter
    @Setter
    public static class Account {
        private String username;
        private String password;
    }
}
