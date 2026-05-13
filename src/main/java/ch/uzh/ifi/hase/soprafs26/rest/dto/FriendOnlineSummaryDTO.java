package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class FriendOnlineSummaryDTO {
    private int friendsOnline;
    private int playing;
    private int lobby;
    private int spectating;

    public int getFriendsOnline() {
        return friendsOnline;
    }

    public void setFriendsOnline(int friendsOnline) {
        this.friendsOnline = friendsOnline;
    }

    public int getPlaying() {
        return playing;
    }

    public void setPlaying(int playing) {
        this.playing = playing;
    }

    public int getLobby() {
        return lobby;
    }

    public void setLobby(int lobby) {
        this.lobby = lobby;
    }

    public int getSpectating() {
        return spectating;
    }

    public void setSpectating(int spectating) {
        this.spectating = spectating;
    }
}

