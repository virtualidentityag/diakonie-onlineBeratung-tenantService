package com.vi.tenantservice.api.controller;


import static com.vi.tenantservice.api.authorisation.UserRole.SINGLE_TENANT_ADMIN;
import static com.vi.tenantservice.api.authorisation.UserRole.TENANT_ADMIN;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.authorisation.Authority;
import com.vi.tenantservice.api.authorisation.UserRole;
import com.vi.tenantservice.api.config.apiclient.ApplicationSettingsApiControllerFactory;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.api.util.MultilingualTenantTestDataBuilder;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.ApplicationSettingsDTO;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.ApplicationSettingsDTOMultitenancyWithSingleDomainEnabled;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@AutoConfigureMockMvc(addFilters = false)
class TenantControllerIT {

    private static final String TENANT_RESOURCE = "/tenant";

    private static final String TENANTADMIN_RESOURCE = "/tenantadmin";
    private static final String TENANT_RESOURCE_SLASH = TENANT_RESOURCE + "/";

    private static final String TENANTADMIN_RESOURCE_SLASH = TENANTADMIN_RESOURCE + "/";
    private static final String PUBLIC_TENANT_RESOURCE = "/tenant/public/";
    private static final String PUBLIC_TENANT_RESOURCE_BY_ID = "/tenant/public/id/";
    private static final String PUBLIC_SINGLE_TENANT_RESOURCE = PUBLIC_TENANT_RESOURCE + "single";
    private static final String EXISTING_TENANT = TENANT_RESOURCE_SLASH + "1";

    private static final String EXISTING_TENANT_VIA_ADMIN = TENANTADMIN_RESOURCE_SLASH + "1";
    private static final String EXISTING_PUBLIC_TENANT = PUBLIC_TENANT_RESOURCE_BY_ID + "1";

    private static final String NON_EXISTING_TENANT_VIA_ADMIN = TENANTADMIN_RESOURCE_SLASH + "3";
    private static final String NON_EXISTING_TENANT = TENANT_RESOURCE_SLASH + "3";
    private static final String NON_EXISTING_PUBLIC_TENANT = PUBLIC_TENANT_RESOURCE_BY_ID + "3";
    private static final String AUTHORITY_WITHOUT_PERMISSIONS = "technical";
    private static final String USERNAME = "not important";
    private static final String EXISTING_SUBDOMAIN = "examplesubdomain";
    private static final String SCRIPT_CONTENT = "<script>error</script>";

    @Autowired
    private WebApplicationContext context;

    @MockBean
    AuthorisationService authorisationService;

    @MockBean
    ApplicationSettingsService applicationSettingsService;

    @MockBean
    ApplicationSettingsApiControllerFactory applicationSettingsApiControllerFactory;

    @MockBean
    ConsultingTypeServiceApiControllerFactory consultingTypeServiceApiControllerFactory;

    @MockBean
    com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi consultingTypeControllerApi;

    @MockBean
    SecurityHeaderSupplier securityHeaderSupplier;
    @MockBean
    TenantResolverService tenantResolverService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        givenSingleTenantAdminCanChangeLegalTexts(true);
      when(consultingTypeServiceApiControllerFactory.createControllerApi()).thenReturn(consultingTypeControllerApi);
      when(consultingTypeControllerApi.getApiClient()).thenReturn(mock(
          com.vi.tenantservice.consultingtypeservice.generated.ApiClient.class));
      when(securityHeaderSupplier.getCsrfHttpHeaders()).thenReturn(mock(HttpHeaders.class));
    }

    private void giveAuthorisationServiceReturnProperAuthoritiesForRole(UserRole userRole) {
        when(authorisationService.hasAuthority(Mockito.any())).thenAnswer(
                invocation -> Authority.getAuthoritiesByUserRole(userRole).contains(invocation.getArgument(0))
        );
    }

    MultilingualTenantTestDataBuilder multilingualTenantTestDataBuilder = new MultilingualTenantTestDataBuilder();

    @Test
    void createTenant_Should_returnStatusOk_When_calledWithValidTenantCreateParamsAndValidAuthority()
            throws Exception {
        AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
        giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
        mockMvc.perform(post(TENANTADMIN_RESOURCE)
                        .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                        .contentType(APPLICATION_JSON)
                        .content(multilingualTenantTestDataBuilder.withName("tenant").withSubdomain("subdomain").withLicensing()
                                .jsonify())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void createTenant_Should_returnStatusForbidden_When_calledWithoutTenantAdminAuthority()
            throws Exception {
        AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
        mockMvc.perform(post(TENANTADMIN_RESOURCE)
                        .with(authentication(builder.withUserRole(AUTHORITY_WITHOUT_PERMISSIONS).build()))
                        .with(user("not important")
                                .authorities((GrantedAuthority) () -> AUTHORITY_WITHOUT_PERMISSIONS))
                        .contentType(APPLICATION_JSON)
                        .content(multilingualTenantTestDataBuilder.withId(1L).withName("tenant").withSubdomain("subdomain").withLicensing()
                                .jsonify())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTenant_Should_notCreateTenant_When_SubdomainIsNotUnique()
            throws Exception {
        AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
        giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
        // given
        mockMvc.perform(post(TENANTADMIN_RESOURCE)
                        .contentType(APPLICATION_JSON)
                        .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                        .content(
                                multilingualTenantTestDataBuilder.withName("tenant").withSubdomain("sub").withLicensing().jsonify())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
        // when
        mockMvc.perform(post(TENANTADMIN_RESOURCE)
                        .contentType(APPLICATION_JSON)
                        .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                        .content(
                                multilingualTenantTestDataBuilder.withId(2L).withName("another tenant").withSubdomain("sub").withLicensing()
                                        .jsonify())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(header().exists("X-Reason"));
    }

  @Test
  void createTenant_Should_returnStatus_When_calledWithTenantDataAndTenantId()
      throws Exception {
    AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
    giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc.perform(post(TENANTADMIN_RESOURCE)
            .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
            .contentType(APPLICATION_JSON)
            .content(multilingualTenantTestDataBuilder.withId(1L).withName("tenant").withSubdomain("subdomain").withLicensing()
                .jsonify())
            .contentType(APPLICATION_JSON))
        .andExpect(status().isConflict());
  }

    @Test
    void updateTenant_Should_returnStatusOk_When_calledWithValidTenantCreateParamsAndTenantAdminAuthority()
            throws Exception {
        when(authorisationService.hasRole("tenant-admin")).thenReturn(true);
        AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
        giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
        mockMvc.perform(put("/tenantadmin/1")
                        .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                        .contentType(APPLICATION_JSON)
                        .content(multilingualTenantTestDataBuilder.withId(1L).withName("tenant").withSubdomain("changed subdomain")
                                .withSettingActiveLanguages(Lists.newArrayList("fr", "pl"))
                                .withLicensing().jsonify())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subdomain").value("changed subdomain"))
                .andExpect(jsonPath("$.settings.topicsInRegistrationEnabled").value("true"))
                .andExpect(jsonPath("$.settings.activeLanguages").value(Lists.newArrayList("fr", "pl")));
    }


    @Test
    void updateTenant_Should_returnStatusOk_When_calledWithValidTenantCreateParamsAndSingleTenantAdminAuthority()
            throws Exception {

        when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(1L));
        when(authorisationService.hasRole(SINGLE_TENANT_ADMIN.getValue())).thenReturn(true);
        AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
        mockMvc.perform(put(EXISTING_TENANT_VIA_ADMIN)
                        .with(authentication(builder.withUserRole(SINGLE_TENANT_ADMIN.getValue()).build()))
                        .contentType(APPLICATION_JSON)
                        .content(multilingualTenantTestDataBuilder.withId(1L).withName("tenant")
                                .withLicensing(5)
                                .withSubdomain("happylife")
                                .withSettingTopicsInRegistrationEnabled(true)
                                .withTranslatedImpressum("de", "new impressum")
                                .jsonify())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subdomain").value("happylife"))
                .andExpect(jsonPath("$.settings.topicsInRegistrationEnabled").value("true"))
                .andExpect(jsonPath("$.content.impressum['de']").value("new impressum"))
                .andExpect(jsonPath("$.settings.featureToolsEnabled").value("true"));
    }

    @Test
    void updateTenant_Should_returnStatusForbidden_When_attemptToChangeLegalTextBySingleTenantAdminIfItsDisallowed()
            throws Exception {

        givenSingleTenantAdminCanChangeLegalTexts(false);

        when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(1L));
        when(authorisationService.hasRole(SINGLE_TENANT_ADMIN.getValue())).thenReturn(true);
        AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
        mockMvc.perform(put(EXISTING_TENANT_VIA_ADMIN)
                        .with(authentication(builder.withUserRole(SINGLE_TENANT_ADMIN.getValue()).build()))
                        .contentType(APPLICATION_JSON)
                        .content(multilingualTenantTestDataBuilder.withId(1L).withName("tenant")
                                .withLicensing(5)
                                .withSubdomain("happylife")
                                .withSettingTopicsInRegistrationEnabled(true)
                                .withTranslatedImpressum("de", "new impressum")
                                .jsonify())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private void givenSingleTenantAdminCanChangeLegalTexts(boolean value) {
        ApplicationSettingsDTO settingsDTO = new ApplicationSettingsDTO();
        settingsDTO.setLegalContentChangesBySingleTenantAdminsAllowed(new ApplicationSettingsDTOMultitenancyWithSingleDomainEnabled().value(value));
        when(applicationSettingsService.getApplicationSettings()).thenReturn(settingsDTO);
    }

    @Test
    void updateTenant_Should_returnStatusForbidden_When_calledWithValidTenantUpdateParamsAndNoAuthorityToModifyTenant()
            throws Exception {
        AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
        mockMvc.perform(put(EXISTING_TENANT_VIA_ADMIN)
                        .with(authentication(builder.withUserRole("not-a-valid-admin").build()))
                        .contentType(APPLICATION_JSON)
                        .content(multilingualTenantTestDataBuilder.withId(1L).withName("tenant").withSubdomain("changed subdomain")
                                .withLicensing().jsonify())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTenant_Should_returnStatusNotFound_When_UpdateAttemptForNonExistingTenant()
            throws Exception {
        AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
        giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
        mockMvc.perform(put(NON_EXISTING_TENANT_VIA_ADMIN)
                        .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                        .content(
                                multilingualTenantTestDataBuilder.withId(1L).withName("tenant").withSubdomain("changed subdomain")
                                        .withLicensing().jsonify())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTenant_Should_returnSettings() throws Exception {
        var builder = new AuthenticationMockBuilder();
        giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
        mockMvc.perform(get(EXISTING_TENANT).with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build())).contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("settings.featureStatisticsEnabled", is(true)))
                .andExpect(jsonPath("settings.featureTopicsEnabled", is(true)))
                .andExpect(jsonPath("settings.topicsInRegistrationEnabled", is(true)))
                .andExpect(jsonPath("settings.featureDemographicsEnabled", is(true)))
                .andExpect(jsonPath("settings.featureAppointmentsEnabled", is(true)))
                .andExpect(jsonPath("settings.featureGroupChatV2Enabled", is(true)))
                .andExpect(jsonPath("settings.featureAttachmentUploadDisabled", is(false)))
                .andExpect(jsonPath("settings.activeLanguages", is(Lists.newArrayList("de"))));

    }

    @Test
    void getTenant_Should_returnStatusOk_When_calledWithExistingTenantIdAndForAuthorityThatIsTenantAdmin()
            throws Exception {
        var builder = new AuthenticationMockBuilder();
        giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);;
        mockMvc.perform(get(EXISTING_TENANT)
                        .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                        .contentType(APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.subdomain").exists())
                .andExpect(jsonPath("$.licensing").exists())
                .andExpect(jsonPath("$.theming").exists())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.settings").exists());
    }

    @Test
    void getAllTenants_Should_returnForbidden_When_calledForAuthorityThatIsSingleTenantAdmin()
            throws Exception {
        mockMvc.perform(get(TENANT_RESOURCE)
                .with(user("not important").authorities((GrantedAuthority) SINGLE_TENANT_ADMIN::getValue))
                .contentType(APPLICATION_JSON)
        ).andExpect(status().isForbidden());
    }

    @Test
    void getAllTenants_Should_returnStatusBasicTenantLicensingData_When_calledForAuthorityThatIsTenantAdmin()
            throws Exception {
        var builder = new AuthenticationMockBuilder();
        mockMvc.perform(get(TENANT_RESOURCE)
                        .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                        .contentType(APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].subdomain").exists())
                .andExpect(jsonPath("$[0].licensing").exists())
                .andExpect(jsonPath("$[0].theming").doesNotExist())
                .andExpect(jsonPath("$[0].content").doesNotExist());
    }

    @Test
    void getRestrictedTenantDataBySubdomain_Should_returnStatusOk_When_calledWithExistingTenantSubdomainAndNoAuthentication()
            throws Exception {
        mockMvc.perform(get(PUBLIC_TENANT_RESOURCE + EXISTING_SUBDOMAIN)
                        .contentType(APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.subdomain").exists())
                .andExpect(jsonPath("$.licensing").doesNotExist())
                .andExpect(jsonPath("$.theming").exists())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.settings").exists())
        ;
    }

    @Test
    void getRestrictedTenantDataByTenantId_Should_returnStatusOk_When_calledWithExistingTenantIdAndNoAuthentication()
            throws Exception {

        Map<String, String> aMap = new HashMap<String, String>();
        aMap.put("de", "de transl");
        aMap.put("en", "en transl");

        String s = new ObjectMapper().writeValueAsString(aMap);
        mockMvc.perform(get(EXISTING_PUBLIC_TENANT)
                        .contentType(APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.subdomain").exists())
                .andExpect(jsonPath("$.licensing").doesNotExist())
                .andExpect(jsonPath("$.theming").exists())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.settings").exists());
    }

    @Test
    void getRestrictedTenantDataByTenantId_Should_returnStatusNotFound_When_calledWithNonExistingTenantIdAndNoAuthentication()
            throws Exception {
        mockMvc.perform(get(NON_EXISTING_PUBLIC_TENANT)
                .contentType(APPLICATION_JSON)
        ).andExpect(status().isNotFound());
    }

    @Test
    void getTenant_Should_returnStatusOk_When_calledWithExistingTenantIdAndForTenantAdminAuthority()
            throws Exception {
        var builder = new AuthenticationMockBuilder();
        giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
        mockMvc.perform(get(EXISTING_TENANT)
                        .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                        .contentType(APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

  @Test
  void getTenant_Should_returnStatusOk_When_calledWithExistingTenantIdAndForSingleTenantAdminAuthority()
          throws Exception {
        var builder = new AuthenticationMockBuilder();
        giveAuthorisationServiceReturnProperAuthoritiesForRole(SINGLE_TENANT_ADMIN);
        when(authorisationService.findTenantIdInAccessToken()).thenReturn(Optional.of(1L));
    mockMvc.perform(get(EXISTING_TENANT)
                    .with(authentication(builder.withUserRole(SINGLE_TENANT_ADMIN.getValue()).build()))
                    .contentType(APPLICATION_JSON)
            ).andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void getTenant_Should_returnStatusOk_When_calledWithExistingTenantIdAndForRestrictedAgencyAdminAuthority()
          throws Exception {
        var builder = new AuthenticationMockBuilder();
        giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
    mockMvc.perform(get(EXISTING_TENANT)
                    .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                    .contentType(APPLICATION_JSON)
            ).andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
  }

    @Test
    void updateTenant_Should_sanitizeInput_When_calledWithExistingTenantIdAndForTenantAdminAuthority()
            throws Exception {
        String jsonRequest = prepareRequestWithInvalidScriptContent();
        when(authorisationService.hasRole(TENANT_ADMIN.getValue())).thenReturn(true);
        giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
        AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
        mockMvc.perform(put(EXISTING_TENANT_VIA_ADMIN)
                        .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                        .contentType(APPLICATION_JSON)
                        .content(jsonRequest)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("name"))
                .andExpect(jsonPath("$.subdomain").value("subdomain"))
                .andExpect(jsonPath("$.content.impressum['de']").value("<b>impressum</b>"))
                .andExpect(jsonPath("$.content.claim['de']").value("<b>claim</b>"));
    }

    @Test
    void updateTenant_Should_throwValidationException_When_calledWithExistingTenantIdAndForTenantAdminAuthorityButInvalidLanguageCode()
            throws Exception {
        String jsonRequest = prepareRequestWithInvalidLanguageContent();

        AuthenticationMockBuilder builder = new AuthenticationMockBuilder();
        giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
        mockMvc.perform(put(EXISTING_TENANT_VIA_ADMIN)
                        .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
                        .contentType(APPLICATION_JSON)
                        .content(jsonRequest)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private String prepareRequestWithInvalidScriptContent() {
        return multilingualTenantTestDataBuilder.withId(1L).withName(appendMalciousScript("name"))
                .withSubdomain(appendMalciousScript("subdomain"))
                .withContent(appendMalciousScript("<b>impressum</b>"), appendMalciousScript("<b>claim</b>"))
                .jsonify();
    }

    private String prepareRequestWithInvalidLanguageContent() {
        return multilingualTenantTestDataBuilder.withId(1L).withName(appendMalciousScript("name"))
                .withSubdomain(appendMalciousScript("subdomain"))
                .withTranslatedImpressum("abc", "impressum")
                .jsonify();
    }

    private String appendMalciousScript(String content) {
        return content + SCRIPT_CONTENT;
    }

    @Test
    void getAllTenant_Should_returnStatusForbidden_When_calledWithExistingTenantIdAndForAuthorityWithoutPermissions()
            throws Exception {
        mockMvc.perform(get(EXISTING_TENANT)
                .with(user(USERNAME).authorities((GrantedAuthority) () -> AUTHORITY_WITHOUT_PERMISSIONS))
                .contentType(APPLICATION_JSON)
        ).andExpect(status().isForbidden());
    }

    @Test
    void getTenant_Should_returnStatusForbidden_When_calledWithExistingTenantIdAndForAuthorityWithoutPermissions()
            throws Exception {
        mockMvc.perform(get(TENANT_RESOURCE)
                .with(user(USERNAME).authorities((GrantedAuthority) () -> AUTHORITY_WITHOUT_PERMISSIONS))
                .contentType(APPLICATION_JSON)
        ).andExpect(status().isForbidden());
    }

    @Test
    void getTenant_Should_returnStatusNotFound_When_calledWithNotExistingTenantId()
            throws Exception {


        var builder = new AuthenticationMockBuilder();
        giveAuthorisationServiceReturnProperAuthoritiesForRole(TENANT_ADMIN);
        mockMvc.perform(get(NON_EXISTING_TENANT)
                        .with(authentication(builder.withUserRole(TENANT_ADMIN.getValue()).build()))
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

    @Test
    @Sql(value = "/database/SingleTenantData.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/database/MultiTenantData.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    void getRestrictedSingleTenantData_Should_returnOkAndTheRequestedTenantData() throws Exception {
        mockMvc.perform(get(PUBLIC_SINGLE_TENANT_RESOURCE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getRestrictedSingleTenantData_Should_returnConflict_When_notExactlyOneTenantIsFound()
            throws Exception {
        mockMvc.perform(get(PUBLIC_SINGLE_TENANT_RESOURCE))
                .andExpect(status().isBadRequest());
    }
}
