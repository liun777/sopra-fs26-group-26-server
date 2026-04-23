package ch.uzh.ifi.hase.soprafs26.config.settings;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Global timeout defaults loaded from `app.timeouts.*`
 * These values are used as system-wide defaults and may be overridden by lobby-specific timer settings!
 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.timeouts")
public class TimeoutSettingsProperties {

    // Default websocket-disconnect grace window
    @Min(1)
    private long websocketGraceSeconds = 300;

    // Default AFK threshold based on last heartbeat activity
    @Min(1)
    private long idleSeconds = 300;

    // Default long-inactivity threshold before auto logout (except active games)
    @Min(1)
    private long autoLogoutSeconds = 3600;

    // Poll interval (milliseconds) for idle-user checks
    @Min(1000)
    private long idleCheckIntervalMs = 60000;

    // Poll interval (milliseconds) for auto-logout checks
    @Min(1000)
    private long autoLogoutCheckIntervalMs = 300000;

    public long getWebsocketGraceSeconds() {
        return websocketGraceSeconds;
    }

    public void setWebsocketGraceSeconds(long websocketGraceSeconds) {
        this.websocketGraceSeconds = websocketGraceSeconds;
    }

    public long getIdleSeconds() {
        return idleSeconds;
    }

    public void setIdleSeconds(long idleSeconds) {
        this.idleSeconds = idleSeconds;
    }

    public long getAutoLogoutSeconds() {
        return autoLogoutSeconds;
    }

    public void setAutoLogoutSeconds(long autoLogoutSeconds) {
        this.autoLogoutSeconds = autoLogoutSeconds;
    }

    public long getIdleCheckIntervalMs() {
        return idleCheckIntervalMs;
    }

    public void setIdleCheckIntervalMs(long idleCheckIntervalMs) {
        this.idleCheckIntervalMs = idleCheckIntervalMs;
    }

    public long getAutoLogoutCheckIntervalMs() {
        return autoLogoutCheckIntervalMs;
    }

    public void setAutoLogoutCheckIntervalMs(long autoLogoutCheckIntervalMs) {
        this.autoLogoutCheckIntervalMs = autoLogoutCheckIntervalMs;
    }
}
