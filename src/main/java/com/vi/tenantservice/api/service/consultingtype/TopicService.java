package com.vi.tenantservice.api.service.consultingtype;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.config.apiclient.TopicAdminServiceApiControllerFactory;
import com.vi.tenantservice.api.service.ConfigurationFileLoader;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.BasicConsultingTypeResponseDTOTitles;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.ConsultingTypeDTO;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.ws.rs.InternalServerErrorException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopicService {

  private final @NonNull TopicAdminServiceApiControllerFactory
      topicAdminServiceApiControllerFactory;
  private final @NonNull SecurityHeaderSupplier securityHeaderSupplier;
  private final @NonNull TenantResolverService tenantResolverService;
  private final @NonNull ConfigurationFileLoader configurationFileLoader;

  @Value("${default.consulting.types.json.path}")
  private String defaultConsultingTypesFilePath;

  public void createDefaultTopic(Long tenantId, Integer consultingTypeId) {
    final File file = configurationFileLoader.loadFrom(defaultConsultingTypesFilePath);
    try {
      ConsultingTypeDTO consultingTypeDTO =
          new ObjectMapper().readValue(file, ConsultingTypeDTO.class);
      consultingTypeDTO.setTenantId(tenantId.intValue());
      createTopicFromConsultingType(consultingTypeId, consultingTypeDTO);
    } catch (IOException ioException) {
      log.error("Error while reading default consulting types configuration file", ioException);
      throw new InternalServerErrorException();
    }
  }

  private void createTopicFromConsultingType(
      Integer consultingTypeId, ConsultingTypeDTO consultingTypeDTO) throws RestClientException {
    var controllerApi = topicAdminServiceApiControllerFactory.createControllerApi();
    addDefaultHeaders(controllerApi.getApiClient());
    controllerApi.createTopic(toTopicDTO(consultingTypeId, consultingTypeDTO));
  }

  private com.vi.tenantservice.topicservice.generated.web.model.TopicMultilingualDTO toTopicDTO(
      Integer consultingTypeId, ConsultingTypeDTO consultingTypeDTO) {

    com.vi.tenantservice.topicservice.generated.web.model.TopicMultilingualDTO topicDTO =
        new com.vi.tenantservice.topicservice.generated.web.model.TopicMultilingualDTO();
    topicDTO.setId(Long.valueOf(consultingTypeId));
    BasicConsultingTypeResponseDTOTitles titles = consultingTypeDTO.getTitles();
    if (titles != null) {
      topicDTO.putNameItem("de", titles.getShort());
      topicDTO.setInternalIdentifier(titles.getShort());
      topicDTO.getTitles().setShort(consultingTypeDTO.getTitles().getShort());
      topicDTO.getTitles().setLong(consultingTypeDTO.getTitles().getLong());
      topicDTO.getTitles().setWelcome(consultingTypeDTO.getTitles().getWelcome());
      topicDTO
          .getTitles()
          .setRegistrationDropdown(consultingTypeDTO.getTitles().getRegistrationDropdown());
    }
    topicDTO.putDescriptionItem("de", consultingTypeDTO.getDescription());
    topicDTO.setStatus("ACTIVE");
    val currentDateTime = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // Format the current date and time
    String formattedCurrentDateTime = currentDateTime.format(formatter);

    topicDTO.setCreateDate(formattedCurrentDateTime);
    topicDTO.setUpdateDate(formattedCurrentDateTime);
    if (consultingTypeDTO.getUrls() != null) {
      topicDTO.setFallbackUrl(consultingTypeDTO.getUrls().getRegistrationPostcodeFallbackUrl());
    }
    return topicDTO;
  }

  private void addDefaultHeaders(com.vi.tenantservice.topicservice.generated.ApiClient apiClient) {
    var headers = this.securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders();

    Optional<Long> optionalTenant = tenantResolverService.tryResolve();
    optionalTenant.ifPresent(aLong -> headers.add("tenantId", aLong.toString()));
    headers.forEach((key, value) -> apiClient.addDefaultHeader(key, value.iterator().next()));
  }
}
