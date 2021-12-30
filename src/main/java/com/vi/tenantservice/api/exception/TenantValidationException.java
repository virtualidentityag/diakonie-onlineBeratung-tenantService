package com.vi.tenantservice.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.BAD_REQUEST, reason="Tenant validation exception")
public class TenantValidationException extends RuntimeException {
    public TenantValidationException(String message) {
        super(message);
    }
}
