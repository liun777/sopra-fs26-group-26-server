package ch.uzh.ifi.hase.soprafs26.config;

import java.security.Principal;

// instances of this class are used by StompAuthChannelInterceptor to assign identities to websocket connections
public class StompPrincipal implements Principal {

    // user's id in our case
    private final String name;

    public StompPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
