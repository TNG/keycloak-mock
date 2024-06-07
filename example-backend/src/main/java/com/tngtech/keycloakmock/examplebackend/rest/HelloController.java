package com.tngtech.keycloakmock.examplebackend.rest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloController {
  @GetMapping("/hello")
  public String hello(@AuthenticationPrincipal Jwt user) {
    return "Hello " + (user.hasClaim("name") ? user.getClaimAsString("name") : user.getSubject());
  }
}
