package ch.uzh.ifi.hase.soprafs26.constant;

public enum GameStatus {
    INITIAL_PEEK,  // start of round, players memorize their cards
    ROUND_ACTIVE,  // normal gameplay, players take turns
    ROUND_ENDED    // round is over, scores are shown
}