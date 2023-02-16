package com.twitterapp.timelinemicroservice.exception.exceptions;

public class AuthorizationHeaderMissingException extends Exception {
    public AuthorizationHeaderMissingException() {
        super();
    }

    public AuthorizationHeaderMissingException(String message) {
        super(message);
    }
}
