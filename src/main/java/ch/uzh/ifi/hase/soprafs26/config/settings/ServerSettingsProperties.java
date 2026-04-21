package ch.uzh.ifi.hase.soprafs26.config.settings;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Server runtime knobs loaded from `app.server.*`.
 * Used for CORS and shared scheduler sizing.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.server")
public class ServerSettingsProperties {

    // Shared scheduler size used for game timers and scheduled transitions.
    @Min(1)
    private int gameSchedulerThreadPoolSize = 10;

    // Frontend origins allowed to call backend HTTP endpoints.
    @NotEmpty
    private List<String> corsAllowedOrigins = new ArrayList<>(List.of(
            "http://localhost:3000",
            "https://sopra-fs26-group-26-client.vercel.app"
    ));

    public int getGameSchedulerThreadPoolSize() {
        return gameSchedulerThreadPoolSize;
    }

    public void setGameSchedulerThreadPoolSize(int gameSchedulerThreadPoolSize) {
        this.gameSchedulerThreadPoolSize = gameSchedulerThreadPoolSize;
    }

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }
}
