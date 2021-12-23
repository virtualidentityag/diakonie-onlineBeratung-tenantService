package com.vi.tenantservice.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import springfox.documentation.annotations.ApiIgnore;

@Controller
@ApiIgnore
public class CustomSwaggerController {

  @Value("${springfox.docuPath}")
  private String docuPath;

  @RequestMapping(value = "${springfox.docuPath}")
  public String index() {
    return "redirect:" + docuPath + "/swagger-ui.html";
  }
}
