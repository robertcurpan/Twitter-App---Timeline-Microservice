package com.twitterapp.timelinemicroservice.exception.exceptions;

public class PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException extends Exception {
    public PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException() {
        super();
    }

    public PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException(String message) {
        super(message);
    }
}
