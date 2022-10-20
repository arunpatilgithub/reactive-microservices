package com.ap.util.util.http;

import lombok.Value;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

@Value
public class HttpErrorInfo {

    ZonedDateTime timestamp;
    String path;
    HttpStatus httpStatus;
    String message;

    public HttpErrorInfo() {
        this.timestamp = null;
        this.httpStatus = null;
        this.path = null;
        this.message = null;
    }

    public HttpErrorInfo(HttpStatus httpStatus, String path, String message) {
        this.timestamp = ZonedDateTime.now();
        this.httpStatus = httpStatus;
        this.path = path;
        this.message = message;
    }
    
}
