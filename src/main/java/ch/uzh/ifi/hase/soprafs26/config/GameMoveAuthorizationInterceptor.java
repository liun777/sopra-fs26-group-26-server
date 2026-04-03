package ch.uzh.ifi.hase.soprafs26.config;

import ch.uzh.ifi.hase.soprafs26.service.GameService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

// Add a "Current Player" check to all incoming move requests; return a 403 Forbidden if it's not their turn. #30
@Component
public class GameMoveAuthorizationInterceptor implements HandlerInterceptor {

    private final GameService gameService;

    public GameMoveAuthorizationInterceptor(GameService gameService) {
        this.gameService = gameService;
    }

    // gets triggered if an endpoint that matches the scheme /games/gameId/moves/moveName is called 
    // runs before controller's method body (in our case eg before gameService.moveDrawFromDrawPile(gameId)) 
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        // let CORS succeed without verification
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // get gameId
        String gameId = getGameIdFromRequest(request);
        // if no gameId recognized - wrong uri shape and thus no verification here
        if (gameId == null) {
            return true;
        }

        String token = request.getHeader("Authorization");
        try {
            // verify current player based on token
            gameService.verifyMoveCallerIsCurrentPlayer(gameId, token);
        } catch (ResponseStatusException ex) {
            String reason = ex.getReason();
            // send error to client
            response.sendError(ex.getStatusCode().value(), reason != null ? reason : "");
            // do not run controller
            return false;
        }
        return true;
    }

    static String getGameIdFromRequest(HttpServletRequest request) {
        String path = request.getRequestURI();

        // remove context path (eg "/api" etc from the uri prefix)
        String cp = request.getContextPath();
        if (cp != null && !cp.isEmpty() && path.startsWith(cp)) {
            path = path.substring(cp.length());
        }

        // has to start with "/" and cannot be just "/"
        if (!path.startsWith("/") || path.length() < 2) {
            return null;
        }

        // remove "/" from prefix before splitting 
        String[] seg = path.substring(1).split("/", 4);
        // uri shape check
        if (seg.length < 4 || !"games".equals(seg[0]) || !"moves".equals(seg[2])) {
            return null;
        }
        // verify gameId and moveName exist
        String id = seg[1];
        String move = seg[3];
        if (id.isEmpty() || move.isBlank()) {
            return null;
        }
        return id;
    }
}
