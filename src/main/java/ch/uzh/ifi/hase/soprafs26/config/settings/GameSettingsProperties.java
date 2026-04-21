package ch.uzh.ifi.hase.soprafs26.config.settings;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Tunable gameplay settings loaded from application*.properties under `app.game.*`.
 * Keep defaults sensible so local dev works even if a key is missing.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.game")
public class GameSettingsProperties {

    // Minimum number of players required to start a game.
    @Min(2)
    private int minPlayers = 2;

    // Maximum number of players allowed in one game.
    @Min(2)
    private int maxPlayers = 4;

    // Number of hand cards dealt to each player at game start.
    @Min(1)
    private int starterCardsPerPlayer = 4;

    // Length of the initial "memorize cards" phase.
    @Min(1)
    private long initialPeekSeconds = 10;

    // Turn timeout for normal round actions.
    @Min(1)
    private long turnSeconds = 30;

    // Fallback timeout for ability phases.
    @Min(1)
    private long abilitySeconds = 30;

    // How long peek/spy result stays visible before turn auto-ends.
    @Min(1)
    private long postPeekAutoEndSeconds = 5;

    // Maximum time players can decide rematch vs no-rematch after Cabo round closes.
    @Min(1)
    private long rematchDecisionSeconds = 60;

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

    public long getRematchDecisionSeconds() {
        return rematchDecisionSeconds;
    }

    public void setRematchDecisionSeconds(long rematchDecisionSeconds) {
        this.rematchDecisionSeconds = rematchDecisionSeconds;
    }
}
