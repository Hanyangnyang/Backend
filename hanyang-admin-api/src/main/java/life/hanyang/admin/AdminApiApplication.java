package life.hanyang.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "life.hanyang")
@EnableJpaRepositories(basePackages = "life.hanyang.core")
@EntityScan(basePackages = "life.hanyang.core")
public class AdminApiApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(AdminApiApplication.class, args);
    }
}