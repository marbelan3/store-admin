package com.g2u.admin.infrastructure.marketplace;

public class CjAuthException extends CjApiException {

    public CjAuthException(String message) {
        super(401, message);
    }
}
