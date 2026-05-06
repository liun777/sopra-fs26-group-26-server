package ch.uzh.ifi.hase.soprafs26.config.settings;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Global gameplay defaults loaded from `app.game.*`
 * Lobby timer settings (from `app.lobby-settings.*`) may override some values per lobby/game!
 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.game")
public class GameSettingsProperties {

    // Minimum number of players required to start a game
    @Min(2)
    private int minPlayers = 2;

    // Maximum number of players allowed in one game
    @Min(2)
    private int maxPlayers = 4;

    // Number of hand cards dealt to each player at game start
    @Min(1)
    private int starterCardsPerPlayer = 4;

    // Default initial peek duration when no lobby-specific value is provided
    @Min(1)
    private long initialPeekSeconds = 10;

    // Default turn duration when no lobby-specific value is provided
    @Min(1)
    private long turnSeconds = 30;

    // Legacy default ability-phase duration, kept for compatibility
    @Min(1)
    private long abilitySeconds = 30;

    // Default reveal duration (seconds) after peek/spy before the phase auto-ends
    @Min(1)
    private long postPeekAutoEndSeconds = 5;

    // Default swap ability window (seconds) when no lobby-specific value is provided
    @Min(1)
    private long abilitySwapSeconds = 10;

    // Cabo cards unveil duration shown at round end before rematch decisions begin
    @Min(1)
    private long caboRevealSeconds = 30;

    // Rematch decision duration after a round ends
    @Min(1)
    private long rematchDecisionSeconds = 60;

    private int scoreLimit = 100;

    private int roundLimit = 100;

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getStarterCardsPerPlayer() {
        return starterCardsPerPlayer;
    }

    public void setStarterCardsPerPlayer(int starterCardsPerPlayer) {
        this.starterCardsPerPlayer = starterCardsPerPlayer;
    }

    public long getInitialPeekSeconds() {
        return initialPeekSeconds;
    }

    public void setInitialPeekSeconds(long initialPeekSeconds) {
        this.initialPeekSeconds = initialPeekSeconds;
    }

    public long getTurnSeconds() {
        return turnSeconds;
    }

    public void setTurnSeconds(long turnSeconds) {
        this.turnSeconds = turnSeconds;
    }

    public long getAbilitySeconds() {
        return abilitySeconds;
    }

    public void setAbilitySeconds(long abilitySeconds) {
        this.abilitySeconds = abilitySeconds;
    }

    public long getPostPeekAutoEndSeconds() {
        return postPeekAutoEndSeconds;
    }

    public void setPostPeekAutoEndSeconds(long postPeekAutoEndSeconds) {
        this.postPeekAutoEndSeconds = postPeekAutoEndSeconds;
    }

    public long getAbilitySwapSeconds() {
        return abilitySwapSeconds;
    }

    public void setAbilitySwapSeconds(long abilitySwapSeconds) {
        this.abilitySwapSeconds = abilitySwapSeconds;
    }

    public long getRematchDecisionSeconds() {
        return rematchDecisionSeconds;
    }

    public void setRematchDecisionSeconds(long rematchDecisionSeconds) {
        this.rematchDecisionSeconds = rematchDecisionSeconds;
    }

    public long getCaboRevealSeconds() {
        return caboRevealSeconds;
    }

    public void setCaboRevealSeconds(long caboRevealSeconds) {
        this.caboRevealSeconds = caboRevealSeconds;
    }

    public int getScoreLimit() {
        return scoreLimit;
    }

    public void setScoreLimit(int scoreLimit) {
        this.scoreLimit = scoreLimit;
    }

    public int getRoundLimit() {
        return roundLimit;
    }

    public void setRoundLimit(int roundLimit) {
        this.roundLimit = roundLimit;
    }
}
