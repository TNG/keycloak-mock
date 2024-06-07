package com.tngtech.keycloakmock.examplebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@ConfigurationPropertiesScan("com.tngtech.keycloakmock.examplebackend.config")
public class ExampleBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExampleBackendApplication.class, args);
  }

  @Bean
  public GrantedAuthorityDefaults grantedAuthorityDefaults() {
    // Remove the default ROLE_ prefix that spring boot otherwise expects
    return new GrantedAuthorityDefaults("");
  }

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("http://localhost:3000");
      }
    };
  }
}
