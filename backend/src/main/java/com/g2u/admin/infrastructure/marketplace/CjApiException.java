package com.g2u.admin.infrastructure.marketplace;

public class CjApiException extends RuntimeException {

    private final int code;

    public CjApiException(String message) {
        super(message);
        this.code = 0;
    }

    public CjApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public CjApiException(String message, Throwable cause) {
        super(message, cause);
        this.code = 0;
    }

    public int getCode() {
        return code;
    }
}
