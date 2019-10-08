package com.tngtech.keycloakmock.examplebackend.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VipController {
  @GetMapping("/vip")
  @PreAuthorize("hasRole('vip')")
  public String hello() {
    return "you may feel very special here";
  }
}
