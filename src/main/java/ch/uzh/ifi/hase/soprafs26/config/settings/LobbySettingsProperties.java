package ch.uzh.ifi.hase.soprafs26.config.settings;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Host-configurable lobby/game timer ranges and defaults.
 * These values are used to clamp incoming lobby settings updates.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.lobby-settings")
public class LobbySettingsProperties {

    @Min(1)
    private long afkTimeoutDefaultSeconds = 300;
    @Min(1)
    private long afkTimeoutMinSeconds = 180;
    @Min(1)
    private long afkTimeoutMaxSeconds = 1200;

    @Min(1)
    private long initialPeekDefaultSeconds = 10;
    @Min(1)
    private long initialPeekMinSeconds = 3;
    @Min(1)
    private long initialPeekMaxSeconds = 60;

    @Min(1)
    private long turnDefaultSeconds = 30;
    @Min(1)
    private long turnMinSeconds = 10;
    @Min(1)
    private long turnMaxSeconds = 60;

    @Min(1)
    private long abilityRevealDefaultSeconds = 5;
    @Min(1)
    private long abilityRevealMinSeconds = 3;
    @Min(1)
    private long abilityRevealMaxSeconds = 10;

    @Min(1)
    private long rematchDecisionDefaultSeconds = 60;
    @Min(1)
    private long rematchDecisionMinSeconds = 10;
    @Min(1)
    private long rematchDecisionMaxSeconds = 60;

    public long getAfkTimeoutDefaultSeconds() { return afkTimeoutDefaultSeconds; }
    public void setAfkTimeoutDefaultSeconds(long afkTimeoutDefaultSeconds) { this.afkTimeoutDefaultSeconds = afkTimeoutDefaultSeconds; }
    public long getAfkTimeoutMinSeconds() { return afkTimeoutMinSeconds; }
    public void setAfkTimeoutMinSeconds(long afkTimeoutMinSeconds) { this.afkTimeoutMinSeconds = afkTimeoutMinSeconds; }
    public long getAfkTimeoutMaxSeconds() { return afkTimeoutMaxSeconds; }
    public void setAfkTimeoutMaxSeconds(long afkTimeoutMaxSeconds) { this.afkTimeoutMaxSeconds = afkTimeoutMaxSeconds; }

    public long getInitialPeekDefaultSeconds() { return initialPeekDefaultSeconds; }
    public void setInitialPeekDefaultSeconds(long initialPeekDefaultSeconds) { this.initialPeekDefaultSeconds = initialPeekDefaultSeconds; }
    public long getInitialPeekMinSeconds() { return initialPeekMinSeconds; }
    public void setInitialPeekMinSeconds(long initialPeekMinSeconds) { this.initialPeekMinSeconds = initialPeekMinSeconds; }
    public long getInitialPeekMaxSeconds() { return initialPeekMaxSeconds; }
    public void setInitialPeekMaxSeconds(long initialPeekMaxSeconds) { this.initialPeekMaxSeconds = initialPeekMaxSeconds; }

    public long getTurnDefaultSeconds() { return turnDefaultSeconds; }
    public void setTurnDefaultSeconds(long turnDefaultSeconds) { this.turnDefaultSeconds = turnDefaultSeconds; }
    public long getTurnMinSeconds() { return turnMinSeconds; }
    public void setTurnMinSeconds(long turnMinSeconds) { this.turnMinSeconds = turnMinSeconds; }
    public long getTurnMaxSeconds() { return turnMaxSeconds; }
    public void setTurnMaxSeconds(long turnMaxSeconds) { this.turnMaxSeconds = turnMaxSeconds; }

    public long getAbilityRevealDefaultSeconds() { return abilityRevealDefaultSeconds; }
    public void setAbilityRevealDefaultSeconds(long abilityRevealDefaultSeconds) { this.abilityRevealDefaultSeconds = abilityRevealDefaultSeconds; }
    public long getAbilityRevealMinSeconds() { return abilityRevealMinSeconds; }
    public void setAbilityRevealMinSeconds(long abilityRevealMinSeconds) { this.abilityRevealMinSeconds = abilityRevealMinSeconds; }
    public long getAbilityRevealMaxSeconds() { return abilityRevealMaxSeconds; }
    public void setAbilityRevealMaxSeconds(long abilityRevealMaxSeconds) { this.abilityRevealMaxSeconds = abilityRevealMaxSeconds; }

    public long getRematchDecisionDefaultSeconds() { return rematchDecisionDefaultSeconds; }
    public void setRematchDecisionDefaultSeconds(long rematchDecisionDefaultSeconds) { this.rematchDecisionDefaultSeconds = rematchDecisionDefaultSeconds; }
    public long getRematchDecisionMinSeconds() { return rematchDecisionMinSeconds; }
    public void setRematchDecisionMinSeconds(long rematchDecisionMinSeconds) { this.rematchDecisionMinSeconds = rematchDecisionMinSeconds; }
    public long getRematchDecisionMaxSeconds() { return rematchDecisionMaxSeconds; }
    public void setRematchDecisionMaxSeconds(long rematchDecisionMaxSeconds) { this.rematchDecisionMaxSeconds = rematchDecisionMaxSeconds; }
}

