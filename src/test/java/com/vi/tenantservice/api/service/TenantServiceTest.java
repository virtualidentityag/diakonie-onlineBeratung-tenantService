package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.repository.TenantRepository;
import com.vi.tenantservice.api.service.consultingtype.ApplicationSettingsService;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.ApplicationSettingsDTO;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.ApplicationSettingsDTOMainTenantSubdomainForSingleDomainMultitenancy;
import java.time.LocalDateTime;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

  @Mock private TenantRepository tenantRepository;

  @Mock ApplicationSettingsService applicationSettingsService;

  @InjectMocks private TenantService tenantService;

  @AfterEach
  void tearDown() {
    ReflectionTestUtils.setField(tenantService, "multitenancyWithSingleDomain", false);
  }

  @Test
  void create_Should_CreateTenantAndSetCreationDate() {
    // given
    TenantEntity tenantEntity = new TenantEntity();
    // when
    tenantService.create(tenantEntity);
    // then
    verify(tenantRepository).save(tenantEntity);
    verify(tenantRepository).findBySubdomain(tenantEntity.getSubdomain());
    assertThat(tenantEntity.getCreateDate()).isNotNull();
    assertThat(tenantEntity.getUpdateDate()).isNotNull();
  }

  @Test
  void create_Should_CreateTenantButNotValidateSubdomainUniqness_When_SingleDomainModeIsEnabled() {
    // given
    ReflectionTestUtils.setField(tenantService, "multitenancyWithSingleDomain", true);
    TenantEntity tenantEntity = new TenantEntity();
    givenApplicationSettingsWithMainTenantSubdomain("app");
    // when
    tenantService.create(tenantEntity);
    // then
    verify(tenantRepository).save(tenantEntity);
    verifyNoMoreInteractions(tenantRepository);
    assertThat(tenantEntity.getCreateDate()).isNotNull();
    assertThat(tenantEntity.getSubdomain()).isEmpty();
    assertThat(tenantEntity.getUpdateDate()).isNotNull();
  }

  @Test
  void update_Should_UpdateTenantAndModifyUpdateDate() {
    // given
    EasyRandom random = new EasyRandom();
    TenantEntity tenantEntity = random.nextObject(TenantEntity.class);
    LocalDateTime previousUpdateDate = tenantEntity.getUpdateDate();

    // when
    tenantService.update(tenantEntity);

    // then
    verify(tenantRepository).save(tenantEntity);
    verify(tenantRepository).findBySubdomain(tenantEntity.getSubdomain());
    assertThat(tenantEntity.getUpdateDate()).isNotNull();
    assertThat(tenantEntity.getUpdateDate()).isNotEqualTo(previousUpdateDate);
  }

  @Test
  void update_Should_UpdateButNotValidateSubdomainUniqness_When_SingleDomainModeIsEnabled() {
    // given
    ReflectionTestUtils.setField(tenantService, "multitenancyWithSingleDomain", true);
    EasyRandom random = new EasyRandom();
    TenantEntity tenantEntity = random.nextObject(TenantEntity.class);
    LocalDateTime previousUpdateDate = tenantEntity.getUpdateDate();
    givenApplicationSettingsWithMainTenantSubdomain("subdomain");

    // when
    tenantService.update(tenantEntity);

    // then
    verify(tenantRepository).save(tenantEntity);
    verifyNoMoreInteractions(tenantRepository);
    assertThat(tenantEntity.getUpdateDate()).isNotNull();
    assertThat(tenantEntity.getUpdateDate()).isNotEqualTo(previousUpdateDate);
  }

  private void givenApplicationSettingsWithMainTenantSubdomain(String subdomain) {
    when(applicationSettingsService.getApplicationSettings())
        .thenReturn(
            new ApplicationSettingsDTO()
                .mainTenantSubdomainForSingleDomainMultitenancy(
                    new ApplicationSettingsDTOMainTenantSubdomainForSingleDomainMultitenancy()
                        .value(subdomain)));
  }

  @Test
  void findTenantById_Should_CallFindById() {
    // given
    long tenantId = 1L;
    // when
    tenantService.findTenantById(tenantId);
    // then
    verify(tenantRepository).findById(tenantId);
  }

  @Test
  void getAllTenants_Should_CallFindAllTenants() {
    // when
    tenantService.getAllTenants();
    // then
    verify(tenantRepository).findAll();
  }
}
