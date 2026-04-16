package com.g2u.admin.infrastructure.marketplace;

public class CjRateLimitException extends CjApiException {

    public CjRateLimitException() {
        super(429, "CJ Dropshipping API rate limit exceeded");
    }
}
