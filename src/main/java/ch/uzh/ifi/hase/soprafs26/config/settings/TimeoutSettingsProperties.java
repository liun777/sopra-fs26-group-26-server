package ch.uzh.ifi.hase.soprafs26.config.settings;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * User presence/disconnect timing controls loaded from `app.timeouts.*`.
 * These values directly affect AFK, grace disconnect, and auto-logout behavior.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.timeouts")
public class TimeoutSettingsProperties {

    // Grace window after websocket disconnect before permanent disconnect handling starts.
    @Min(1)
    private long websocketGraceSeconds = 90;

    // AFK threshold; if last heartbeat is older than this, user is considered idle.
    @Min(1)
    private long idleSeconds = 300;

    // Long inactivity threshold after which auth token is invalidated (except active games).
    @Min(1)
    private long autoLogoutSeconds = 21600;

    // Scheduler frequency for checking idle users.
    @Min(1000)
    private long idleCheckIntervalMs = 60000;

    // Scheduler frequency for checking auto-logout users.
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
