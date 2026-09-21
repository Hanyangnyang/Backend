package life.hanyang.api.config;

import lombok.RequiredArgsConstructor;
import life.hanyang.core.global.config.DeploymentColorState;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ActiveColorScheduledTaskAspect {

    private final DeploymentColorState deploymentColorState;

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object runOnlyOnActiveColor(ProceedingJoinPoint joinPoint) throws Throwable {
        if (deploymentColorState.isActive()) {
            return joinPoint.proceed();
        }

        return null;
    }
}
