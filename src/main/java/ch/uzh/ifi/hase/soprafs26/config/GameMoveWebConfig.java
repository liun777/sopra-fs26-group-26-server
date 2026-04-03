package ch.uzh.ifi.hase.soprafs26.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// add the GameMoveAuthorizationInterceptor to the app
@Configuration
public class GameMoveWebConfig implements WebMvcConfigurer {

    private final GameMoveAuthorizationInterceptor gameMoveAuthorizationInterceptor;

    public GameMoveWebConfig(GameMoveAuthorizationInterceptor gameMoveAuthorizationInterceptor) {
        this.gameMoveAuthorizationInterceptor = gameMoveAuthorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(gameMoveAuthorizationInterceptor)
                .addPathPatterns("/games/*/moves/*");
    }
}
