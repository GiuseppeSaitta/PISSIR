package com.mygroupid.rabbitmq.rabbit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SuppressWarnings("deprecation")
@SpringBootApplication
public class SpringbootRabbitApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootRabbitApplication.class, args);
	}

   @Bean
   public WebMvcConfigurer corsConfigurer() {
	
    return new WebMvcConfigurerAdapter() {
         @Override
         public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/task").allowedOrigins("http://localhost:3000").allowedMethods("GET", "POST","PUT", "DELETE");
            registry.addMapping("/auth").allowedOrigins("http://localhost:3000").allowedMethods("GET", "POST","PUT", "DELETE");
            registry.addMapping("/mytasks").allowedOrigins("http://localhost:3000").allowedMethods("GET", "POST","PUT", "DELETE");
         }
      };
   }

}
