package com.vi.tenantservice.api.tenant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@AllArgsConstructor
@NoArgsConstructor
@Component
@Slf4j
public class CookieTenantResolver implements TenantResolver {

  private static final String TENANT_ID = "tenantId";

  @Value("${feature.multitenancy.with.single.domain.enabled}")
  private boolean multitenancyWithSingleDomain;

  @Override
  public Optional<Long> resolve(HttpServletRequest request) {
    return resolveTenantFromRequest(request);
  }

  public Optional<Long> resolveTenantFromRequest(HttpServletRequest request) {

    if (!multitenancyWithSingleDomain) {
      return Optional.empty();
    }

    Cookie token = WebUtils.getCookie(request, "keycloak");

    if (token == null) {
      return Optional.empty();
    }

    return resolveFromCookieValue(token);
  }

  private Optional<Long> resolveFromCookieValue(Cookie token) {
    String[] chunks = token.getValue().split("\\.");
    Base64.Decoder decoder = Base64.getUrlDecoder();
    String payload = new String(decoder.decode(chunks[1]));
    var objectMapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      Map<String, Object> map = objectMapper.readValue(payload, Map.class);
      Integer tenantIdFromCookie = (Integer) map.get(TENANT_ID);
      return tenantIdFromCookie == null
          ? Optional.empty()
          : Optional.of(Long.valueOf(tenantIdFromCookie));
    } catch (JsonProcessingException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean canResolve(HttpServletRequest request) {
    return resolve(request).isPresent();
  }
}
