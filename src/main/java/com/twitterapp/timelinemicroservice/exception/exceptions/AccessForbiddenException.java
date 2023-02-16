package com.twitterapp.timelinemicroservice.exception.exceptions;

public class AccessForbiddenException extends Exception {
    public AccessForbiddenException() {
        super();
    }

    public AccessForbiddenException(String message) {
        super(message);
    }
}
