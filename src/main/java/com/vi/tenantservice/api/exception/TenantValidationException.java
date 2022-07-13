package com.vi.tenantservice.api.exception;

import com.vi.tenantservice.api.exception.httpresponse.CustomHttpHeader;
import com.vi.tenantservice.api.exception.httpresponse.HttpStatusExceptionReason;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@Getter
public class TenantValidationException extends RuntimeException {

    private final HttpHeaders customHttpHeaders;
    private final HttpStatus httpStatus;

    public TenantValidationException(HttpStatusExceptionReason exceptionReason) {
        super();
        this.customHttpHeaders = new CustomHttpHeader(exceptionReason)
            .buildHeader();
        this.httpStatus = HttpStatus.CONFLICT;
    }

    public TenantValidationException(HttpStatusExceptionReason exceptionReason, HttpStatus status) {
        super();
        this.customHttpHeaders = new CustomHttpHeader(exceptionReason)
            .buildHeader();
        this.httpStatus = status;
    }
}
