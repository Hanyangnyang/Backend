package life.hanyang.api.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ActiveColorScheduledTaskAspect {

    private static final Path ACTIVE_COLOR_FILE = Path.of("/app/runtime/active-color");

    @Value("${DEPLOY_COLOR:blue}")
    private String deploymentColor;

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object runOnlyOnActiveColor(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!Files.exists(ACTIVE_COLOR_FILE)
            || deploymentColor.equals(Files.readString(ACTIVE_COLOR_FILE).trim())) {
            return joinPoint.proceed();
        }

        return null;
    }
}
