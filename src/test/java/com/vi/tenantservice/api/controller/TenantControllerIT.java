package com.vi.tenantservice.api.controller;


import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.util.TenantTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@AutoConfigureMockMvc(addFilters = false)
class TenantControllerIT {

  private static final String TENANT_RESOURCE = "/tenant";
  private static final String TENANT_RESOURCE_SLASH = TENANT_RESOURCE + "/";
  private static final String PUBLIC_TENANT_RESOURCE = "/tenant/public/";
  private static final String EXISTING_TENANT = TENANT_RESOURCE_SLASH + "1";
  private static final String NON_EXISTING_TENANT = TENANT_RESOURCE_SLASH + "3";
  private static final String AUTHORITY_TENANT_ADMIN = "tenant-admin";
  private static final String AUTHORITY_WITHOUT_PERMISSIONS = "technical";
  private static final String USERNAME = "not important";
  private static final String EXISTING_SUBDOMAIN = "examplesubdomain";
  private static final String SCRIPT_CONTENT = "<script>some malicious content</script>";

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  TenantTestDataBuilder tenantTestDataBuilder = new TenantTestDataBuilder();

  @Test
  void createTenant_Should_returnStatusOk_When_calledWithValidTenantCreateParamsAndValidAuthority()
      throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    mockMvc.perform(post(TENANT_RESOURCE)
            .with(authentication(builder.withAuthority(AUTHORITY_TENANT_ADMIN).build()))
            .contentType(APPLICATION_JSON)
            .content(tenantTestDataBuilder.withName("tenant").withSubdomain("subdomain").withLicensing()
                .jsonify())
            .contentType(APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  void createTenant_Should_returnStatusForbidden_When_calledWithoutTenantAdminAuthority()
      throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    mockMvc.perform(post(TENANT_RESOURCE)
            .with(authentication(builder.withAuthority(AUTHORITY_WITHOUT_PERMISSIONS).build()))
            .with(user("not important")
                .authorities((GrantedAuthority) () -> AUTHORITY_WITHOUT_PERMISSIONS))
            .contentType(APPLICATION_JSON)
            .content(tenantTestDataBuilder.withName("tenant").withSubdomain("subdomain").withLicensing()
                .jsonify())
            .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void createTenant_Should_notCreateTenant_When_SubdomainIsNotUnique()
      throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    // given
    mockMvc.perform(post(TENANT_RESOURCE)
            .contentType(APPLICATION_JSON)
            .with(authentication(builder.withAuthority(AUTHORITY_TENANT_ADMIN).build()))
            .content(
                tenantTestDataBuilder.withName("tenant").withSubdomain("sub").withLicensing().jsonify())
            .contentType(APPLICATION_JSON))
        .andExpect(status().isOk());
    // when
    mockMvc.perform(post(TENANT_RESOURCE)
            .contentType(APPLICATION_JSON)
            .with(authentication(builder.withAuthority(AUTHORITY_TENANT_ADMIN).build()))
            .content(
                tenantTestDataBuilder.withName("another tenant").withSubdomain("sub").withLicensing()
                    .jsonify())
            .contentType(APPLICATION_JSON))
        .andExpect(status().isConflict())
        .andExpect(header().exists("X-Reason"));
  }

  @Test
  void updateTenant_Should_returnStatusOk_When_calledWithValidTenantCreateParamsAndTenantAdminAuthority()
      throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    mockMvc.perform(put(EXISTING_TENANT)
            .with(authentication(builder.withAuthority(AUTHORITY_TENANT_ADMIN).build()))
            .contentType(APPLICATION_JSON)
            .content(tenantTestDataBuilder.withName("tenant").withSubdomain("changed subdomain")
                .withLicensing().jsonify())
            .contentType(APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  void updateTenant_Should_returnStatusForbidden_When_calledWithValidTenantUpdateParamsAndNoAuthorityToModifyTenant()
      throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    mockMvc.perform(put(EXISTING_TENANT)
            .with(authentication(builder.withAuthority("not-a-valid-admin").build()))
            .contentType(APPLICATION_JSON)
            .content(tenantTestDataBuilder.withName("tenant").withSubdomain("changed subdomain")
                .withLicensing().jsonify())
            .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateTenant_Should_returnStatusNotFound_When_UpdateAttemptForNonExistingTenant()
      throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    mockMvc.perform(put(NON_EXISTING_TENANT)
            .with(authentication(builder.withAuthority(AUTHORITY_TENANT_ADMIN).build()))
            .content(tenantTestDataBuilder.withName("tenant").withSubdomain("changed subdomain")
                .withLicensing().jsonify())
            .contentType(APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void getTenant_Should_returnStatusOk_When_calledWithExistingTenantIdAndForAuthorityThatIsTenantAdmin()
      throws Exception {
    mockMvc.perform(get(EXISTING_TENANT)
            .with(user("not important").authorities((GrantedAuthority) () -> AUTHORITY_TENANT_ADMIN))
            .contentType(APPLICATION_JSON)
        ).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").exists())
        .andExpect(jsonPath("$.subdomain").exists())
        .andExpect(jsonPath("$.licensing").exists())
        .andExpect(jsonPath("$.theming").exists())
        .andExpect(jsonPath("$.content").exists());
  }

  @Test
  void getRestrictedTenantDataBySubdomain_Should_returnStatusOk_When_calledWithExistingTenantSubdomainAndNoAuthentication()
      throws Exception {
    mockMvc.perform(get(PUBLIC_TENANT_RESOURCE + EXISTING_SUBDOMAIN)
            .contentType(APPLICATION_JSON)
        ).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(jsonPath("$.name").exists())
        .andExpect(jsonPath("$.subdomain").doesNotExist())
        .andExpect(jsonPath("$.licensing").doesNotExist())
        .andExpect(jsonPath("$.theming").exists())
        .andExpect(jsonPath("$.content").exists());
  }

  @Test
  void getTenant_Should_returnStatusOk_When_calledWithExistingTenantIdAndForTenantAdminAuthority()
      throws Exception {
    mockMvc.perform(get(EXISTING_TENANT)
            .with(user("not important").authorities((GrantedAuthority) () -> AUTHORITY_TENANT_ADMIN))
            .contentType(APPLICATION_JSON)
        ).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void updateTenant_Should_sanitizeInput_When_calledWithExistingTenantIdAndForTenantAdminAuthority()
      throws Exception {
    String jsonRequest = prepareRequestWithInvalidScriptContent();

    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    mockMvc.perform(put(EXISTING_TENANT)
            .with(authentication(builder.withAuthority(AUTHORITY_TENANT_ADMIN).build()))
            .contentType(APPLICATION_JSON)
            .content(jsonRequest)
            .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("name"))
        .andExpect(jsonPath("$.subdomain").value("subdomain"))
        .andExpect(jsonPath("$.content.impressum").value("<b>impressum</b>"))
        .andExpect(jsonPath("$.content.claim").value("<b>claim</b>"));
  }

  private String prepareRequestWithInvalidScriptContent() {
    return tenantTestDataBuilder.withName(appendMalciousScript("name"))
        .withSubdomain(appendMalciousScript("subdomain"))
        .withContent(appendMalciousScript("<b>impressum</b>"), appendMalciousScript("<b>claim</b>"))
        .jsonify();
  }

  private String appendMalciousScript(String content) {
    return content + SCRIPT_CONTENT;
  }

  @Test
  void getTenant_Should_returnStatusForbidden_When_calledWithExistingTenantIdAndForAuthorityWithoutPermissions()
      throws Exception {
    mockMvc.perform(get(EXISTING_TENANT)
        .with(user(USERNAME).authorities((GrantedAuthority) () -> AUTHORITY_WITHOUT_PERMISSIONS))
        .contentType(APPLICATION_JSON)
    ).andExpect(status().isForbidden());
  }

  @Test
  void getTenant_Should_returnStatusNotFound_When_calledWithNotExistingTenantId()
      throws Exception {
    mockMvc.perform(get(NON_EXISTING_TENANT)
            .with(user("not important").authorities((GrantedAuthority) () -> AUTHORITY_TENANT_ADMIN))
            .contentType(APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void getTenant_Should_returnStatusForbidden_When_calledWithoutAnyAuthorization()
      throws Exception {
    mockMvc.perform(get(EXISTING_TENANT)
            .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

}
