package mygroupdi.myspringapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SuppressWarnings("deprecation")
@SpringBootApplication
public class MyspringappApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyspringappApplication.class, args);
	}

	@Bean
   public WebMvcConfigurer corsConfigurer() {
	
      return new WebMvcConfigurerAdapter() {
         @Override
         public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/users").allowedOrigins("http://localhost:3000").allowedMethods("GET", "POST","PUT", "DELETE");
            registry.addMapping("/user").allowedOrigins("http://localhost:3000").allowedMethods("GET", "POST","PUT", "DELETE");
         }
      };
   }

}


