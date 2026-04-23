package ch.uzh.ifi.hase.soprafs26.constant;

public enum GameStatus {
    INITIAL_PEEK, // start of round, players memorize their cards
    ROUND_ACTIVE, // normal gameplay, players take turns
    ABILITY_PEEK_SELF, // player is using peek on own card (7/8)
    ABILITY_PEEK_OPPONENT, // player is using peek on opponent card (9/10)
    ABILITY_SWAP, // player is swapping cards with opponent (11/12)
    CABO_REVEAL, // round ended, all cards unveiled before rematch voting starts
    ROUND_AWAITING_REMATCH, // round is finished, waiting for rematch/no-rematch decision
    ROUND_ENDED // round is over, scores are shown
}
