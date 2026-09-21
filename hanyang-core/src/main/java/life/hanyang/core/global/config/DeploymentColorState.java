package life.hanyang.core.global.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeploymentColorState {

    private static final Path ACTIVE_COLOR_FILE = Path.of("/app/runtime/active-color");

    @Value("${DEPLOY_COLOR:blue}")
    private String deploymentColor;

    public boolean isActive() {
        if (!Files.exists(ACTIVE_COLOR_FILE)) {
            return true;
        }

        try {
            return deploymentColor.equals(Files.readString(ACTIVE_COLOR_FILE).trim());
        } catch (IOException exception) {
            return false;
        }
    }
}
