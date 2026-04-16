package com.g2u.admin.web.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceType, String field, Object value) {
        super(resourceType + " already exists with " + field + ": " + value);
    }
}
