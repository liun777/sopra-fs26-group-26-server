package ch.uzh.ifi.hase.soprafs26;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RestController
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public String helloWorld() {
		return "The application is running.";
	}

	@Bean(destroyMethod = "shutdown")
	public ScheduledExecutorService gameScheduler() {
		return Executors.newScheduledThreadPool(10);
	}

	/*
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**").allowedOrigins("*").allowedMethods("*");
			}
		};
	}
	*/

	@Bean
	public WebMvcConfigurer corsConfigurer() {
    	return new WebMvcConfigurer() {
     		@Override
        	public void addCorsMappings(CorsRegistry registry) {
            	registry.addMapping("/**")
                    .allowedOrigins(
                        "http://localhost:3000",
                        "https://sopra-fs26-group-26-client.vercel.app"
                    )
                    .allowedMethods("*")
                    .allowCredentials(true);
        	}
   	 	};
	}
}
