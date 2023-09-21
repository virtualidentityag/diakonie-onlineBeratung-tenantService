package com.vi.tenantservice.api.config.apiclient;

import com.vi.tenantservice.topicservice.generated.ApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TopicAdminServiceApiControllerFactory {

  @Value("${consulting.type.service.api.url}")
  private String consultingTypeServiceApiUrl;

  @Autowired private RestTemplate restTemplate;

  public com.vi.tenantservice.topicservice.generated.web.TopicAdminControllerApi
      createControllerApi() {
    var apiClient = new ApiClient(restTemplate).setBasePath(this.consultingTypeServiceApiUrl);
    return new com.vi.tenantservice.topicservice.generated.web.TopicAdminControllerApi(apiClient);
  }
}
