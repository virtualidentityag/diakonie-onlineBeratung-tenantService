package com.vi.tenantservice.api.config;

import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Provides the SpringFox (API documentation generation) configuration.
 */
@Configuration
@EnableSwagger2
@Import(BeanValidatorPluginsConfiguration.class)
public class SpringFoxConfig {

  @Value("${springfox.docuTitle}")
  private String docuTitle;
  @Value("${springfox.docuDescription}")
  private String docuDescription;
  @Value("${springfox.docuVersion}")
  private String docuVersion;
  @Value("${springfox.docuTermsUrl}")
  private String docuTermsUrl;
  @Value("${springfox.docuContactName}")
  private String docuContactName;
  @Value("${springfox.docuContactUrl}")
  private String docuContactUrl;
  @Value("${springfox.docuContactEmail}")
  private String docuContactEmail;
  @Value("${springfox.docuLicense}")
  private String docuLicense;
  @Value("${springfox.docuLicenseUrl}")
  private String docuLicenseUrl;

  // White list for path patterns that should be white listed so that swagger UI can be accessed
  // without authorization
  public static final String[] WHITE_LIST =
      new String[]{"/mails/docs", "/mails/docs/**", "/v2/api-docs", "/configuration/ui",
          "/swagger-resources/**", "/configuration/security", "/swagger-ui.html", "/webjars/**"};

  @Bean
  public Docket apiDocket() {
    return new Docket(DocumentationType.SWAGGER_2).select()
        .apis(RequestHandlerSelectors.basePackage("com.vi.tenantservice.api")).build()
        .consumes(getContentTypes()).produces(getContentTypes()).apiInfo(getApiInfo())
        .useDefaultResponseMessages(false).protocols(protocols())
        .directModelSubstitute(LocalTime.class, String.class);
  }

  /**
   * Returns the API protocols (for documentation).
   *
   * @return the provided protocols
   */
  private Set<String> protocols() {
    Set<String> protocols = new HashSet<>();
    protocols.add("https");
    return protocols;
  }

  /**
   * Returns all content types which should be consumed/produced
   */
  private Set<String> getContentTypes() {
    Set<String> contentTypes = new HashSet<>();
    contentTypes.add("application/json");
    return contentTypes;
  }

  /**
   * Returns the API information (defined in application.properties)
   *
   * @return the static api info
   */
  private ApiInfo getApiInfo() {
    return new ApiInfo(docuTitle, docuDescription, docuVersion, docuTermsUrl,
        new Contact(docuContactName, docuContactUrl, docuContactEmail), docuLicense, docuLicenseUrl,
        Collections.emptyList());
  }
}
